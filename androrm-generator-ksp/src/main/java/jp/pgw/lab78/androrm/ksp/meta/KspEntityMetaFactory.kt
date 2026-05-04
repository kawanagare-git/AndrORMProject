package jp.pgw.lab78.androrm.ksp.meta

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.FunctionProjection
import jp.pgw.lab78.androrm.common.database.SupportFunction.toSnakeCase
import jp.pgw.lab78.androrm.common.meta.EntityMeta
import jp.pgw.lab78.androrm.common.meta.PropertyMeta
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_ALIAS
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_NAME
import jp.pgw.lab78.androrm.ksp.Constants.FP_ALIAS_FALLBACK
import jp.pgw.lab78.androrm.ksp.Constants.FUNCTION
import jp.pgw.lab78.androrm.ksp.Constants.TABLE
import jp.pgw.lab78.androrm.ksp.Constants.TABLE_ALIAS
import jp.pgw.lab78.androrm.ksp.Constants.TABLE_NAME
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger.logger
import jp.pgw.lab78.androrm.ksp.logging.LoggerLike
import jp.pgw.lab78.androrm.ksp.projectoin.ProjectionDefinition

/**
 * ## KSP Entity メタ情報生成クラス
 * ### KSClassDeclaration と ProjectionDefinition から
 * ### 共通検証用の EntityMeta / PropertyMeta を生成する
 * @author Masahiro Inoue
 * @since 2026-04-29
 */
class KspEntityMetaFactory : LoggerLike by logger {

    /**
     * ## EntityMeta 生成
     * ### KSP 上のクラス定義と ProjectionDefinition から共通メタ情報を生成する
     * @param classDecl 元エンティティクラス
     * @param definition Projection 定義
     * @return 共通メタ情報
     * @throws IllegalArgumentException 引数が不正な場合にスローされる例外
     * @author Masahiro Inoue
     * @since 2026-04-29
     */
    fun create(classDecl: KSClassDeclaration, definition: ProjectionDefinition): EntityMeta {
        logTraceEntered(classDecl, definition)
        // テーブル情報の解決
        val tableAnnotation = classDecl.annotations.firstOrNull {
            it.shortName.asString() == TABLE
        }
        // テーブル名とエイリアスの解決
        val tableName = resolveTableName(classDecl, tableAnnotation)
        val tableAlias = resolveTableAlias(tableAnnotation, tableName)
        // クラスのプロパティを名前でマップ化
        val sourcePropertiesByName = classDecl.getAllProperties()
            .associateBy { it.simpleName.asString() }
        // 通常カラムの PropertyMeta を生成
        val columnMetas = definition.properties.map { columnProjection ->
            // プロジェクション定義のプロパティ名に対応する KSPropertyDeclaration をクラス定義から取得。存在しない場合はエラー
            val sourceProperty = sourcePropertiesByName[columnProjection.property]
                ?: logError(
                    "Property '${columnProjection.property}' is not declared in ${classDecl.qualifiedName?.asString()}."
                )
            // プロジェクション定義とクラス定義を突き合わせて PropertyMeta を生成
            createColumnMeta(
                property = sourceProperty as KSPropertyDeclaration,
                projection = columnProjection
            )
        }
        // 関数列の PropertyMeta を生成
        val functionMetas = definition.functions.map { functionProjection ->
            createFunctionMeta(functionProjection)
        }
        // EntityMeta を生成して返却
        val result = EntityMeta(
            defineEntityQualifiedName = classDecl.qualifiedName?.asString().orEmpty(),
            entityName = classDecl.simpleName.asString() + definition.entityNameExtend,
            tableName = tableName,
            tableAlias = tableAlias,
            properties = columnMetas + functionMetas
        )
        logTraceExiting(result)
        return result
    }

    /**
     * ## 通常カラム用 PropertyMeta 生成
     * ### KSPropertyDeclaration と ColumnProjection から PropertyMeta を生成する
     * @param property 元プロパティ定義
     * @param projection プロジェクション定義
     * @return 共通メタ情報
     * @author Masahiro Inoue
     * @since 2026-04-29
     */
    private fun createColumnMeta(
        property: KSPropertyDeclaration,
        projection: ColumnProjection
    ): PropertyMeta {
        logTraceEntered(property, projection)
        // @Column と @Function の両方をチェック（両方付いている場合は両方の情報を持つ）
        val columnAnnotation = property.annotations.firstOrNull {
            it.shortName.asString() == COLUMN
        }
        // @Function もチェック（あくまで通常カラム用の PropertyMeta なので、あっても functionType などは null/空になる）
        val functionAnnotation = property.annotations.firstOrNull {
            it.shortName.asString() == FUNCTION
        }
        // プロパティ名からデフォルトのカラム名を生成（スネークケース変換）
        val propertyName = property.simpleName.asString()
        val defaultColumnName = propertyName.toSnakeCase()
        // @Column があれば、そこからカラム名とエイリアスを解決。なければデフォルトのカラム名を使用
        val columnName = columnAnnotation
            ?.stringArgument(COLUMN_NAME)
            ?.takeIf { it.isNotBlank() }
            ?: defaultColumnName
        // エイリアスは @Column の alias 引数から解決。なければカラム名をエイリアスとして使用
        val aliasName = columnAnnotation
            ?.stringArgument(COLUMN_ALIAS)
            ?.takeIf { it.isNotBlank() }
            ?: columnName
        // PropertyMeta を生成して返却
        val result = PropertyMeta(
            propertyName = propertyName,
            columnName = columnName,
            aliasName = aliasName,
            isFunction = false,
            hideFromSelect = projection.hideFromSelect,
            hasColumnAnnotation = columnAnnotation != null,
            hasFunctionAnnotation = functionAnnotation != null,
            functionType = null,
            functionArgs = emptyList(),
            rawFunction = ""
        )
        logTraceExiting(result)
        return result
    }

    /**
     * ## 関数列用 PropertyMeta 生成
     * ### FunctionProjection から PropertyMeta を生成する
     * ### 関数列はクラスのプロパティではなく、あくまで ProjectionDefinition 上の定義なので、
     * ### KSPropertyDeclaration は存在しない。したがって関数列用の PropertyMeta 生成は、
     * ### ProjectionDefinition の情報だけで完結させる。
     * @param projection プロジェクション定義
     * @return 共通メタ情報
     * @author Masahiro Inoue
     * @since 2026-04-29
     */
    private fun createFunctionMeta(
        projection: FunctionProjection
    ): PropertyMeta {
        logTraceEntered(projection)
        // エイリアスは ProjectionDefinition の alias 引数から解決。なければ FP_ALIAS_FALLBACK を使用
        val aliasName = projection.alias.ifBlank { FP_ALIAS_FALLBACK }
        // PropertyMeta を生成して返却
        val result = PropertyMeta(
            propertyName = aliasName,
            columnName = aliasName,
            aliasName = aliasName,
            isFunction = true,
            hideFromSelect = projection.hideFromSelect,
            hasColumnAnnotation = false,
            hasFunctionAnnotation = true,
            functionType = projection.function,
            functionArgs = projection.args.toList(),
            rawFunction = projection.raw
        )
        logTraceExiting(result)
        return result
    }

    /**
     * ## テーブル名解決
     * ### KSClassDeclaration と @Table アノテーションからテーブル名を解決する
     * ### @Table の name 引数があればそれをテーブル名として使用。なければクラス名をスネークケース変換してテーブル名とする
     * @param classDecl 元エンティティクラス
     * @param tableAnnotation @Table アノテーション（存在しない場合は null）
     * @return 解決されたテーブル名
     * @author Masahiro Inoue
     * @since 2026-04-29
     */
    private fun resolveTableName(
        classDecl: KSClassDeclaration,
        tableAnnotation: KSAnnotation?
    ): String =
        tableAnnotation
            ?.stringArgument(TABLE_NAME)
            ?.takeIf { it.isNotBlank() }
            ?: classDecl.simpleName.asString().toSnakeCase()

    /**
     * ## テーブルエイリアス解決
     * ### @Table アノテーションからテーブルエイリアスを解決する
     * ### @Table の alias 引数があればそれをテーブルエイリアスとして使用。なければテーブル名をエイリアスとして使用
     * @param tableAnnotation @Table アノテーション（存在しない場合は null）
     * @param tableName 解決されたテーブル名（テーブルエイリアスのフォールバック値として使用）
     * @return 解決されたテーブルエイリア
     * @author Masahiro Inoue
     * @since 2026-04-29
     */
    private fun resolveTableAlias(
        tableAnnotation: KSAnnotation?,
        tableName: String
    ): String =
        tableAnnotation
            ?.stringArgument(TABLE_ALIAS)
            ?.takeIf { it.isNotBlank() }
            ?: tableName

    /**
     * ## 文字列引数抽出
     * ### KSAnnotation から指定した名前の文字列引数を抽出する
     * ### 引数が存在しない、または文字列でない場合は null を返す
     * @param name 引数名
     * @return 引数の文字列値、または null
     * @author Masahiro Inoue
     * @since 2026-04-29
     */
    private fun KSAnnotation.stringArgument(name: String): String? =
        arguments.firstOrNull { it.name?.asString() == name }?.value as? String
}