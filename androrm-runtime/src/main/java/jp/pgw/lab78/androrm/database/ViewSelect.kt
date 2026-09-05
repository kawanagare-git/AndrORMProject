package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.Constants.ARGUMENT_DELIMITER
import jp.pgw.lab78.androrm.common.Constants.DEFAULT_LIMIT_VALUE
import jp.pgw.lab78.androrm.common.Constants.DEFAULT_OFFSET_VALUE
import jp.pgw.lab78.androrm.common.MessageConstants.AE00004
import jp.pgw.lab78.androrm.common.MessageConstants.AE00005
import jp.pgw.lab78.androrm.common.MessageConstants.AE00006
import jp.pgw.lab78.androrm.common.database.function.SqlAggregateFunction
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.meta.EntityMeta
import jp.pgw.lab78.androrm.common.meta.PropertyMeta
import jp.pgw.lab78.androrm.database.DmlConstant.MULTI_SPACE_REGEX
import jp.pgw.lab78.androrm.database.condition.ConditionBuilder
import jp.pgw.lab78.androrm.database.condition.base.BaseSelect
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.condition.sealed.Order
import jp.pgw.lab78.androrm.database.meta.RuntimeEntityMetaFactory
import jp.pgw.lab78.androrm.database.meta.SelectClause
import jp.pgw.lab78.androrm.database.queryparts.JoinType
import jp.pgw.lab78.androrm.database.reference.TableRef
import jp.pgw.lab78.androrm.database.view.ViewLiteralValueHolder
import jp.pgw.lab78.androrm.database.view.ViewSqlLiteralRenderer
import kotlin.reflect.KClass

/**
 * ## VIEW 定義用 SELECT 文生成クラス
 * ### 既存 SELECT の句構造と条件 DSL を使用し、値だけを SQLite SQL リテラルとして展開する
 * ### 実行用 Select とは別型であり、DB の SELECT 実行 API へ渡すことはできない
 * @param fromTable SELECT 元
 * @param isDistinct DISTINCT を使用する場合 true
 * @author Masahiro Inoue
 * @since 2026-08-31
 */
class ViewSelect<T : SelectEntity>(
    private val fromTable: TableRef<out T>,
    private val isDistinct: Boolean = false,
) : BaseSelect<T, ViewSelect<T>>() {
    /**
     * ## VIEW 定義用 SELECT 生成
     * @param fromEntity SELECT 元 Entity
     * @param isDistinct DISTINCT を使用する場合 true
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    constructor(
        fromEntity: KClass<out T>,
        isDistinct: Boolean = false,
    ) : this(
        TableRef(
            entityClass = fromEntity,
            alias = RuntimeEntityMetaFactory().create(fromEntity).tableAlias,
        ),
        isDistinct,
    )

    /** 主 Entity の正規化済みメタ情報 */
    override val mainEntityMeta = runtimeEntityMetaFactory.create(fromTable.entityClass)

    /** SELECT 対象列 */
    private val selectColumnList = mutableListOf<String>()

    /** CREATE VIEW の列数検証に使用する SELECT 出力列数 */
    internal val outputColumnCount: Int
        get() = selectColumnList.size

    /** 集約関数使用フラグ */
    private var hasAggregateFunction = false

    /** 並び替えカラム */
    private val orderColumns = mutableListOf<Order>()

    /** 最大読み出し行数 */
    private var limitValue: Int? = null

    /** 読み出し開始行 */
    private var offsetValue: Int? = null

    init {
        validateEntityMeta(mainEntityMeta)
        appendSelectableColumns(mainEntityMeta, fromTable.alias)
        queryStructureMap[SelectClause.SELECT] =
            mutableListOf("from ${mainEntityMeta.tableName} ${fromTable.alias}")
        usedEntityClasses += fromTable
        registerTableAlias(mainEntityMeta.tableName, fromTable.alias)
    }

    /**
     * ## VIEW 定義用条件値保持領域生成
     * @return SQL リテラル化を行う値保持領域
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    override fun createConditionValueHolder(): QueryWithBindValues = ViewLiteralValueHolder()

    /**
     * ## JOIN 対象登録
     * @param joinedTable 結合対象
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    override fun onTableJoined(joinedTable: TableRef<out SelectEntity>) {
        val entityMeta = runtimeEntityMetaFactory.create(joinedTable.entityClass)
        validateEntityMeta(entityMeta)
        registerTableAlias(entityMeta.tableName, joinedTable.alias)
        appendSelectableColumns(entityMeta, joinedTable.alias)
        usedEntityClasses += joinedTable
    }

    /**
     * ## JOIN 指定
     * @param joinType JOIN 種別
     * @param joinedEntity 結合対象 Entity
     * @param on ON 条件
     * @return 自身
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    fun join(
        joinType: JoinType,
        joinedEntity: KClass<out SelectEntity>,
        on: ConditionBuilder.() -> Unit,
    ): ViewSelect<T> = join(
        joinType,
        TableRef(joinedEntity, RuntimeEntityMetaFactory().create(joinedEntity).tableAlias),
        on,
    )

    /**
     * ## JOIN 指定
     * @param joinType JOIN 種別
     * @param joinedEntity 結合対象 Entity
     * @return ON 条件指定用モデル
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    fun join(joinType: JoinType, joinedEntity: KClass<out SelectEntity>) =
        join(
            joinType,
            TableRef(joinedEntity, RuntimeEntityMetaFactory().create(joinedEntity).tableAlias),
        )

    /**
     * ## 並び順指定
     * @param by 並び替え DSL
     * @return 自身
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    fun order(by: OrderDsl.() -> Unit): ViewSelect<T> = self.also {
        duplicateMethodCallValidator.validateNoDuplicateMethodCall(SelectClause.ORDER)
        isBuild = false
        orderColumns += OrderDsl().apply(by).orders
    }

    /**
     * ## LIMIT 指定
     * @param limitValue 最大行数
     * @return OFFSET 指定用モデル
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    fun limit(limitValue: Int = DEFAULT_LIMIT_VALUE): LimitClause {
        duplicateMethodCallValidator.validateNoDuplicateMethodCall(SelectClause.LIMIT)
        require(limitValue >= 0) { AE00004 }
        isBuild = false
        this.limitValue = limitValue
        return LimitClause()
    }

    /**
     * ## LIMIT 指定後操作
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    inner class LimitClause internal constructor() {
        /**
         * ## OFFSET 指定
         * @param offsetValue 読み飛ばす行数
         * @return VIEW 定義用 SELECT
         * @author Masahiro Inoue
         * @since 2026-08-31
         */
        fun offset(offsetValue: Int = DEFAULT_OFFSET_VALUE): ViewSelect<T> =
            this@ViewSelect.also {
                duplicateMethodCallValidator.validateNoDuplicateMethodCall(SelectClause.OFFSET)
                require(offsetValue >= 0) { AE00005 }
                isBuild = false
                this@ViewSelect.offsetValue = offsetValue
            }

        /**
         * ## SQL 生成
         * @return VIEW 定義用 SELECT 文
         * @author Masahiro Inoue
         * @since 2026-08-31
         */
        fun build(): String = this@ViewSelect.build()
    }

    /**
     * ## SELECT 対象列追加
     * @param entityMeta Entity メタ情報
     * @param tableAlias SQL エイリアス
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    private fun appendSelectableColumns(entityMeta: EntityMeta, tableAlias: String) {
        entityMeta.properties.filterNot { it.hideFromSelect }.forEach { propertyMeta ->
            if (SqlAggregateFunction.entries.any { it.name == propertyMeta.functionType?.name }) {
                hasAggregateFunction = true
            }
            selectColumnList += buildSelectExpression(entityMeta, tableAlias, propertyMeta)
        }
    }

    /**
     * ## SELECT 対象式生成
     * @param entityMeta Entity メタ情報
     * @param tableAlias SQL エイリアス
     * @param propertyMeta プロパティメタ情報
     * @return SELECT 対象式
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    @Suppress("SpreadOperator")
    private fun buildSelectExpression(
        entityMeta: EntityMeta,
        tableAlias: String,
        propertyMeta: PropertyMeta,
    ): String = if (!propertyMeta.isFunction) {
        "$tableAlias.${propertyMeta.columnName} as ${tableAlias}_${propertyMeta.aliasName}"
    } else {
        val functionExpression = propertyMeta.rawFunction.takeIf { it.isNotBlank() }
            ?: run {
                val functionType = propertyMeta.functionType
                    ?: error(AE00006.format(propertyMeta.propertyName))
                val args = propertyMeta.functionArgs
                    .map { resolveFunctionArgument(entityMeta, tableAlias, it) }
                    .toTypedArray()
                functionType.build(*args)
            }
        "$functionExpression as ${tableAlias}_${propertyMeta.aliasName}"
    }

    /**
     * ## VIEW 定義用 SELECT 文生成
     * @return bind parameter を含まない SELECT 文
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    override fun build(): String {
        if (!isBuild) {
            isBuild = true
            if (hasAggregateFunction) ensureGroupByColumns()
            buildInClauseDefinitionOrder {
                addClauseIfNotEmpty(SelectClause.ORDER, ARGUMENT_DELIMITER, orderColumns.toList())
                limitValue?.let { queryStructureMap[SelectClause.LIMIT] = mutableListOf("limit $it") }
                offsetValue?.let { queryStructureMap[SelectClause.OFFSET] = mutableListOf("offset $it") }
            }
            val selectKeyword = "select ${if (isDistinct) "distinct " else ""}"
            val clauses = SelectClause.entries
                .joinToString(" ") { queryStructureMap[it]?.joinToString(" ") ?: " " }
            query = "$selectKeyword${selectColumnList.joinToString(", ")} $clauses"
                .replace(MULTI_SPACE_REGEX, " ")
                .trim()
            require(!ViewSqlLiteralRenderer.hasSqlParameter(query)) {
                "ViewSelect must not contain SQLite bind parameters: $query"
            }
            require(bindValues.isEmpty()) {
                "ViewSelect must not retain bind values."
            }
        }
        return query
    }
}
