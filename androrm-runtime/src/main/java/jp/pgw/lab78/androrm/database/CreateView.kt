package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.Constants.D_QUOTE
import jp.pgw.lab78.androrm.common.MessageConstants.AE00037
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.ViewDefinitionEntity
import jp.pgw.lab78.androrm.database.interfaces.QueryBuilderLike
import jp.pgw.lab78.androrm.database.meta.RuntimeViewMetaFactory
import jp.pgw.lab78.androrm.database.view.ViewSqlLiteralRenderer
import kotlin.reflect.KClass

/**
 * ## CREATE VIEW 文生成
 * ### ViewDefinitionEntity の列定義と ViewSelect を対応付けて SQLite CREATE VIEW 文を生成する
 * @param entityClass VIEW 定義 Entity
 * @param select VIEW 本体の SELECT
 * @author Masahiro Inoue
 * @since 2026-08-31
 */
class CreateView<T : ViewDefinitionEntity>(
    private val entityClass: KClass<out T>,
    private val select: ViewSelect<out SelectEntity>,
) : QueryBuilderLike<T> {
    /** VIEW メタ情報 */
    val meta = RuntimeViewMetaFactory().create(entityClass)

    /**
     * ## CREATE VIEW 文生成
     * @return CREATE VIEW 文
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    override fun build(): String {
        val selectSql = select.build()
        require(select.outputColumnCount == meta.properties.size) {
            "VIEW column count (${meta.properties.size}) does not match " +
                    "ViewSelect output count (${select.outputColumnCount}) for ${meta.viewName}."
        }
        require(select.bindValues.isEmpty() && !ViewSqlLiteralRenderer.hasSqlParameter(selectSql)) {
            "CREATE VIEW does not support SQLite bind parameters: $selectSql"
        }
        val columns = meta.properties.joinToString(", ") { quoteIdentifier(it.columnName) }
        return "create view ${quoteIdentifier(meta.viewName)} ($columns) as $selectSql"
    }

    /**
     * ## DROP VIEW 文生成
     * @return DROP VIEW IF EXISTS 文
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    fun buildDropQuery(): String = "drop view if exists ${quoteIdentifier(meta.viewName)}"

    companion object {
        /**
         * ## VIEW 名から DROP VIEW 文生成
         * @param viewName 削除対象 VIEW 名
         * @return DROP VIEW IF EXISTS 文
         * @author Masahiro Inoue
         * @since 2026-08-31
         */
        fun buildDropQuery(viewName: String): String =
            "drop view if exists ${quoteIdentifier(viewName)}"

        /** SQL 識別子を検証してダブルクォートで囲む。 */
        private fun quoteIdentifier(identifier: String): String {
            require(identifier.isNotBlank() && '\u0000' !in identifier) {
                AE00037.format(identifier)
            }
            return D_QUOTE + identifier.replace(D_QUOTE, D_QUOTE + D_QUOTE) + D_QUOTE
        }
    }
}
