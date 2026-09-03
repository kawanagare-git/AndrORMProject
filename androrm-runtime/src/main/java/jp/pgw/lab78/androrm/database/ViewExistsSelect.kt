package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.database.DmlConstant.MULTI_SPACE_REGEX
import jp.pgw.lab78.androrm.database.condition.base.BaseSelect
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.meta.SelectClause
import jp.pgw.lab78.androrm.database.reference.TableRef
import jp.pgw.lab78.androrm.database.view.ViewLiteralValueHolder
import jp.pgw.lab78.androrm.database.view.ViewSqlLiteralRenderer

/**
 * ## VIEW 定義用 EXISTS SELECT
 * ### 相関 EXISTS で使用する `select 1` を SQL リテラル方式で生成する
 * @param fromTable EXISTS の参照元
 * @author Masahiro Inoue
 * @since 2026-08-31
 */
class ViewExistsSelect<T : SelectEntity>(
    private val fromTable: TableRef<T>,
) : BaseSelect<T, ViewExistsSelect<T>>() {
    /** 主 Entity の正規化済みメタ情報 */
    override val mainEntityMeta = runtimeEntityMetaFactory.create(fromTable.entityClass)

    init {
        validateEntityMeta(mainEntityMeta)
        queryStructureMap[SelectClause.SELECT] =
            mutableListOf("from ${mainEntityMeta.tableName} ${fromTable.alias}")
        usedEntityClasses += fromTable
        registerTableAlias(mainEntityMeta.tableName, fromTable.alias)
    }

    /** VIEW 定義用リテラル値保持領域を生成する。 */
    override fun createConditionValueHolder(): QueryWithBindValues = ViewLiteralValueHolder()

    /**
     * ## EXISTS SELECT 文生成
     * @return `select 1` 文
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    override fun build(): String {
        if (!isBuild) {
            isBuild = true
            buildInClauseDefinitionOrder {}
            val clauses = SelectClause.entries
                .joinToString(" ") { queryStructureMap[it]?.joinToString(" ") ?: " " }
            query = "select 1 $clauses".replace(MULTI_SPACE_REGEX, " ").trim()
            require(!ViewSqlLiteralRenderer.hasSqlParameter(query)) {
                "ViewExistsSelect must not contain SQLite bind parameters: $query"
            }
        }
        return query
    }
}
