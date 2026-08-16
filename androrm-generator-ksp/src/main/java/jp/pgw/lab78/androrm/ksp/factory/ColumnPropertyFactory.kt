package jp.pgw.lab78.androrm.ksp.factory

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.ksp.toTypeName
import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.database.SupportFunction.toSnakeCase
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.logging.interfaces.LoggerLike
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_ALIAS
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_DEFAULT_VALUE
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_FQN
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_HIDE_FROM_SELECT
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_NAME
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger.logger
import jp.pgw.lab78.androrm.ksp.resolver.KspColumnAnnotationResolver
import jp.pgw.lab78.shared.library.Utils.isNull

/**
 * ## Column プロパティファクトリークラス
 * ### KSPropertyDeclaration から @Column アノテーションを生成するためのクラス
 * ### @Column アノテーションの引数（name、alias、hideFromSelect）を抽出し、適切な値を設定して AnnotationSpec を生成する
 * @param columnAnnotationResolver
 * @author Masahiro Inoue
 * @since 2026-04-21
 */
class ColumnPropertyFactory(private val columnAnnotationResolver: KspColumnAnnotationResolver) :
    LoggerLike by logger {
    /**
     * ## @Column 生成メソッド
     * ### KSPropertyDeclaration から @Column アノテーションを生成するためのメソッド
     * ### @Column の引数（name、alias、hideFromSelect）を抽出し、適切な値を設定して AnnotationSpec を生成する
     * @param ownerClass
     * @param prop 対象プロパティの宣言
     * @param hideFromSelect プロパティが SELECT から隠されるべきかどうかを示すフラグ（デフォルトは false）
     * @return 生成されたプロパティと SELECT 非表示情報を保持する GeneratedProperty オブジェクト
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    fun create(
        ownerClass: KSClassDeclaration,
        prop: KSPropertyDeclaration,
        hideFromSelect: Boolean = false
    ): GeneratedProperty {
        logTraceEntered(prop, hideFromSelect)
        val propertyType = prop.type.toTypeName().let { typeName ->
            if (hideFromSelect) {
                typeName.copy(nullable = true)
            } else {
                typeName
            }
        }
        // KSPropertyDeclaration からプロパティの型と名前を取得し、PropertySpec のビルダーを作成する
        val builder = PropertySpec.builder(
            prop.simpleName.asString(),
            propertyType
        )
        val columnAnnotation =
            columnAnnotationResolver.find(ownerClass = ownerClass, property = prop)
        // 実装先に付与された@Columnは、後で解決済みの@Columnとして追加するため、ここではコピーしない。
        prop.annotations
            .filterNot { annotation ->
                annotation.annotationType.resolve()
                    .declaration
                    .qualifiedName
                    ?.asString() == COLUMN_FQN
            }
            .forEach { annotation ->
                builder.addAnnotation(
                    annotation.toAnnotationSpec(),
                )
            }
        if (columnAnnotation.isNull()) {
            builder.addAnnotation(buildFallbackColumnAnnotation(prop, hideFromSelect))
        } else {
            builder.addAnnotation(
                copyColumnAnnotation(
                    columnAnnotation = columnAnnotation,
                    hideFromSelect = hideFromSelect,
                ),
            )
        }
        // プロパティの PropertySpec を生成する
        val result = GeneratedProperty(
            propertySpec = builder.initializer(prop.simpleName.asString()).build(),
            hideFromSelect = hideFromSelect,
        )
        logTraceExiting(result)
        return result
    }

    /**
     * ## @Column コピー生成メソッド
     * ### KSPropertyDeclaration から @Column を検索し、引数を抽出して AnnotationSpec を生成するためのメソッド
     * @param prop 対象プロパティの宣言
     * @param hideFromSelect プロパティが SELECT から隠されるべきかどうかを示すフラグ
     * @return 生成された @Column の AnnotationSpec オブジェクト。@Column が存在しない場合は null を返す。
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    private fun copyColumnAnnotation(
        columnAnnotation: KSAnnotation,
        hideFromSelect: Boolean,
    ): AnnotationSpec {
        logTraceEntered(columnAnnotation, hideFromSelect)
        // @Column の引数を抽出し、AnnotationSpec を生成する
        val columnName = columnAnnotation.arguments
            .firstOrNull { it.name?.asString() == COLUMN_NAME }
            ?.value as? String
        // @Column alias 引数 を抽出し、適切な値を設定する。null または空白の場合は、プロパティ名をスネークケースに変換して使用する
        val columnAlias = columnAnnotation.arguments
            .firstOrNull { it.name?.asString() == COLUMN_ALIAS }
            ?.value as? String ?: EMPTY_STRING
        // columnAlias が null または空白の場合は、プロパティ名をスネークケースに変換して使用する
        val defaultValue = columnAnnotation.arguments
            .firstOrNull { it.name?.asString() == COLUMN_DEFAULT_VALUE }
            ?.value as? String ?: EMPTY_STRING
        val result = AnnotationSpec.builder(Column::class).apply {
            // カラム名の取得・生成
            columnName?.takeUnless { name -> name.isBlank() }
                ?.let { name -> addMember("$COLUMN_NAME = %S", name) }
            // カラムエイリアスと抽出項目除外を追加
            addMember("$COLUMN_ALIAS = %S", columnAlias)
            addMember("$COLUMN_HIDE_FROM_SELECT = %L", hideFromSelect)
            // デフォルト値の取得・生成
            defaultValue.takeUnless { value -> value.isBlank() }
                ?.let { value -> addMember("$COLUMN_DEFAULT_VALUE = %S", value) }
        }.build()
        logTraceExiting(result)
        return result
    }

    /**
     * ## アノテーション変換メソッド
     * ### KSAnnotation オブジェクトを AnnotationSpec に変換するためのメソッド
     * ### アノテーションの引数を適切に処理して、AnnotationSpec のメンバーとして追加する
     * @receiver KSAnnotation オブジェクト
     * @return 変換された AnnotationSpec オブジェクト
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    private fun KSAnnotation.toAnnotationSpec(): AnnotationSpec {
        logTraceEntered(this)
        // アノテーションの完全修飾名を取得し、AnnotationSpec のビルダーを作成する
        val fqn = this.annotationType.resolve().declaration.qualifiedName!!.asString()
        val builder = AnnotationSpec.builder(com.squareup.kotlinpoet.ClassName.bestGuess(fqn))
        // アノテーションの引数を走査し、適切に処理して AnnotationSpec のメンバーとして追加する
        this.arguments.forEach { arg ->
            val name = arg.name?.asString() ?: return@forEach
            val value = arg.value
            // 引数の型に応じて、AnnotationSpec のメンバーとして追加する
            when (value) {
                is String -> builder.addMember("$name = %S", value)
                is Boolean -> builder.addMember("$name = %L", value)
                is Int -> builder.addMember("$name = %L", value)
                is Enum<*> -> builder.addMember("$name = %T.%L", value::class.java, value.name)
                is List<*> -> {
                    val joined = value.joinToString(", ") {
                        when (it) {
                            is String -> "\"$it\""
                            is Enum<*> -> "${it::class.java.simpleName}.${it.name}"
                            else -> it.toString()
                        }
                    }
                    builder.addMember("$name = [%L]", joined)
                }
            }
        }
        // AnnotationSpec を生成して返す
        val result = builder.build()
        logTraceExiting(result)
        return result
    }

    /**
     * ## @Column 補完メソッド
     * ### KSPropertyDeclaration からデフォルトの @Column を生成するためのメソッド
     * ### プロパティ名をスネークケースに変換して、name 引数として使用する。
     * ### alias 引数は空文字列を使用する。hideFromSelect 引数は引数として渡された値を使用する。
     * @param prop 対象プロパティの宣言
     * @param hideFromSelect プロパティが SELECT から隠されるべきかどうかを示すフラグ
     * @return 生成されたデフォルトの @Column の AnnotationSpec オブジェクト
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    private fun buildFallbackColumnAnnotation(
        prop: KSPropertyDeclaration,
        hideFromSelect: Boolean
    ): AnnotationSpec {
        logTraceEntered(prop, hideFromSelect)
        // プロパティ名をスネークケースに変換して、name 引数として使用する
        val generatedColumnName = prop.simpleName.asString().toSnakeCase()
        // デフォルトの @Column を生成する。name 引数はプロパティ名をスネークケースに変換して使用し、alias 引数は空文字列を使用する。hideFromSelect 引数は引数として渡された値を使用する。
        val result = AnnotationSpec.builder(Column::class).apply {
            addMember("name = %S", generatedColumnName)
            addMember("hideFromSelect = %L", hideFromSelect)
        }.build()
        logTraceExiting(result)
        return result
    }
}