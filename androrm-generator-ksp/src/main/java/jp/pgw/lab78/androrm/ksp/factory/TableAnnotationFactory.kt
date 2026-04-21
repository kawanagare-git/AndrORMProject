package jp.pgw.lab78.androrm.ksp.factory

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.database.SupportFunction.toSnakeCase
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger.logger
import jp.pgw.lab78.androrm.ksp.logging.LoggerLike

/**
 * ## Table アノテーションファクトリークラス
 * ### KSClassDeclaration から @Table アノテーションを生成するためのクラス
 * ### @Table アノテーションの引数（name と alias）を抽出し、適切な値を設定して AnnotationSpec を生成する
 * @author Masahiro Inoue
 * @since 2026-04-18
 */
class TableAnnotationFactory(
) : LoggerLike by logger {

    companion object {
        /** @Table のシンプルネーム */
        private const val TABLE = "Table"

        /** @Table の変数名定義（name） */
        private const val TABLE_NAME = "name"

        /** @Table の変数名定義（alias） */
        private const val TABLE_ALIAS = "alias"
    }

    /**
     * ## アノテーション生成メソッド
     * ### KSClassDeclaration から @Table アノテーションを生成するためのメソッド
     * ### @Table アノテーションの引数（name と alias）を抽出し、適切な値を設定して AnnotationSpec を生成する
     * @param classDecl 対象クラスの宣言
     * @param aliasExtend エイリアスの拡張部分（省略可能）
     * @return 生成された @Table アノテーションの AnnotationSpec オブジェクト
     * @author Masahiro Inoue
     * @since 2026-04-18
     */
    fun create(
        classDecl: KSClassDeclaration,
        aliasExtend: String
    ): AnnotationSpec {
        traceEntered(classDecl, aliasExtend)
        // KSClassDeclaration から @Table アノテーションを検索する
        val tableAnnotation = classDecl.annotations
            .firstOrNull { it.shortName.asString() == TABLE }
        // @Table name 引数 を抽出し、適切な値を設定する
        val tableName = tableAnnotation?.let { extractTableName(it) }
            ?: classDecl.simpleName.asString().toSnakeCase()
        // @Table alias 引数 を抽出し、適切な値を設定する
        val tableAlias = tableAnnotation?.let {
            generateTableAlias(it, aliasExtend)
        } ?: tableName
        // AnnotationSpec を生成する
        val result = AnnotationSpec.builder(Table::class).apply {
            addMember("$TABLE_NAME = %S", tableName)
            addMember("$TABLE_ALIAS = %S", tableAlias)
        }.build()
        traceExiting(result)
        return result
    }

    /**
     * ## テーブル名抽出メソッド
     * ### @Table アノテーションからテーブル名を抽出するためのメソッド
     * @param tableAnnotation 対象の @Table アノテーション
     * @return 抽出されたテーブル名（null の場合もある）
     * @author Masahiro Inoue
     * @since 2026-04-18
     */
    fun extractTableName(
        tableAnnotation: KSAnnotation,
    ): String? {
        traceEntered(tableAnnotation)
        // @Table アノテーションの引数から name を抽出し、テーブル名を取得する
        val result = tableAnnotation.arguments
            .firstOrNull { it.name?.asString() == TABLE_NAME }
            ?.value
            ?.takeIf { it is String && it.isNotBlank() }
            ?.let { it as String }
        traceExiting(result)
        return result
    }

    /**
     * ## テーブルエイリアス生成メソッド
     * ### @Table アノテーションからテーブルエイリアスを生成するためのメソッド
     * @param tableAnnotation 対象の @Table アノテーション
     * @param extendAlias エイリアスの拡張部分（省略可能）
     * @return 生成されたテーブルエイリアス（null の場合もある）
     * @author Masahiro Inoue
     * @since 2026-04-18
     */
    fun generateTableAlias(
        tableAnnotation: KSAnnotation,
        extendAlias: String
    ): String? {
        traceEntered(tableAnnotation, extendAlias)
        // @Table アノテーションの引数から alias を抽出し、テーブルエイリアスを取得する
        val aliasFromAnnotation = tableAnnotation.arguments
            .firstOrNull { it.name?.asString() == TABLE_ALIAS }
            ?.value as? String
        // アノテーションから取得したエイリアスと、引数で渡されたエイリアスの拡張部分を組み合わせて、最終的なテーブルエイリアスを生成する
        val result = buildAlias(aliasFromAnnotation, extendAlias)
        traceExiting(result)
        return result
    }

    /**
     * ## エイリアス生成ロジックメソッド
     * ### アノテーションから取得したエイリアスと、引数で渡されたエイリアスの拡張部分を組み合わせて、
     * ### 最終的なテーブルエイリアスを生成するためのメソッド
     * @param aliasFromAnnotation アノテーションから取得したエイリアス（null の場合もある）
     * @param extendAlias 引数で渡されたエイリアスの拡張部分（省略可能）
     * @return 生成されたテーブルエイリアス（null の場合もある）
     * @author Masahiro Inoue
     * @since 2026-04-18
     */
    private fun buildAlias(
        aliasFromAnnotation: String?,
        extendAlias: String
    ): String {
        // アノテーションから取得したエイリアスが null または空文字の場合は、空文字を使用する
        val baseAlias = aliasFromAnnotation?.takeIf { it.isNotBlank() } ?: EMPTY_STRING
        // 引数で渡されたエイリアスの拡張部分が null または空文字の場合は、空文字を使用する
        return when {
            baseAlias.isBlank() && extendAlias.isBlank() -> EMPTY_STRING
            baseAlias.isBlank() -> extendAlias
            extendAlias.isBlank() -> baseAlias
            else -> "${baseAlias}_${extendAlias}"
        }
    }
}