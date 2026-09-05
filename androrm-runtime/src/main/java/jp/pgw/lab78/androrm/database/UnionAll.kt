package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.Constants.ARGUMENT_DELIMITER
import jp.pgw.lab78.androrm.common.Constants.DEFAULT_LIMIT_VALUE
import jp.pgw.lab78.androrm.common.Constants.DEFAULT_OFFSET_VALUE
import jp.pgw.lab78.androrm.common.MessageConstants.AE00004
import jp.pgw.lab78.androrm.common.MessageConstants.AE00005
import jp.pgw.lab78.androrm.common.MessageConstants.AE00039
import jp.pgw.lab78.androrm.common.MessageConstants.AE00040
import jp.pgw.lab78.androrm.common.MessageConstants.AE00041
import jp.pgw.lab78.androrm.common.MessageConstants.AE00042
import jp.pgw.lab78.androrm.common.MessageConstants.AE00043
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.logging.LogLevel.TRACE
import jp.pgw.lab78.androrm.common.logging.LogScope.APP
import jp.pgw.lab78.androrm.common.logging.aop.InfoLog
import jp.pgw.lab78.androrm.common.meta.EntityMeta
import jp.pgw.lab78.androrm.common.meta.EntityMetaValidator
import jp.pgw.lab78.androrm.common.meta.PropertyMeta
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.condition.interfaces.SelectQuery
import jp.pgw.lab78.androrm.database.condition.sealed.Order
import jp.pgw.lab78.androrm.database.meta.RuntimeEntityMetaFactory
import jp.pgw.lab78.androrm.database.meta.SelectClause
import jp.pgw.lab78.androrm.database.utility.EntityManager.extractClassFromProperty
import jp.pgw.lab78.androrm.database.validation.DuplicateMethodCallValidator
import java.util.logging.Level.WARNING
import java.util.logging.Logger
import kotlin.reflect.KClass

/**
 * ## UNION ALL 文生成クラス
 * ### 複数の完成した Select を指定順に結合し、単一の結果Entityへ位置順で対応付ける
 *
 * ### 仕様
 * #### 構成SelectのSQLとバインド値を指定順に連結し、複合結果全体へORDER、LIMIT、OFFSETだけを追加する。
 * #### 結果列はresultEntityの表示対象プロパティ順へ正規化し、将来の派生テーブル参照に利用できる列名を公開する。
 * @param resultEntity UNION ALL結果の列定義に使用するEntity
 * @param unionSelect UNION ALLで結合するSelect
 * @author Masahiro Inoue
 * @since 2026-09-01
 */
class UnionAll<T : SelectEntity>(
    val resultEntity: KClass<T>,
    vararg unionSelect: Select<out SelectEntity>,
) : QueryWithBindValues(), SelectQuery<T> {
    /** UNION ALLで結合するSelect一覧 */
    private val unionSelectList = unionSelect.toList()

    /** Entityメタ情報生成 */
    private val runtimeEntityMetaFactory = RuntimeEntityMetaFactory()

    /** ログ出力 */
    private val logger: Logger by lazy { APP.create(minLogLevel = TRACE) }

    /** 結果Entityの正規化済みメタ情報 */
    private val resultEntityMeta = runtimeEntityMetaFactory.create(resultEntity)

    /** 結果Entityの表示対象プロパティ */
    private val resultProperties = resultEntityMeta.properties.filterNot { it.hideFromSelect }

    /** 並び替え定義 */
    private val orderColumns = mutableListOf<Order>()

    /** 最大読み出し行数 */
    private var limitValue: Int? = null

    /** 読み出し開始行 */
    private var offsetValue: Int? = null

    /** 重複メソッド呼び出し検証 */
    private val duplicateMethodCallValidator =
        DuplicateMethodCallValidator<SelectClause>("UnionAll")

    /**
     * ## 初期検証
     * ### Select数、結果Entityメタ情報、結果カラム名を検証する
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    init {
        require(unionSelectList.size >= MINIMUM_SELECT_COUNT) { AE00039 }
        validateResultEntityMeta(resultEntityMeta)
        validateResultColumnNames()
    }

    /**
     * ## 並び順指定
     * ### UNION ALL全体の結果に適用するORDER BYを指定する
     * @param by 並び替えDSL
     * @return 自身
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    @InfoLog
    fun order(by: OrderDsl.() -> Unit): UnionAll<T> = also {
        duplicateMethodCallValidator.validateNoDuplicateMethodCall(SelectClause.ORDER)
        orderColumns += OrderDsl().apply(by).orders
    }

    /**
     * ## LIMIT指定
     * ### UNION ALL全体の最大読み出し行数を指定する
     * @param limitValue 最大読み出し行数
     * @return OFFSET指定またはSQL生成を行う中間オブジェクト
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    @InfoLog
    fun limit(limitValue: Int = DEFAULT_LIMIT_VALUE): LimitClause {
        duplicateMethodCallValidator.validateNoDuplicateMethodCall(SelectClause.LIMIT)
        require(limitValue >= 0) { AE00004 }
        this.limitValue = limitValue
        return LimitClause()
    }

    /**
     * ## LIMIT指定後操作
     * ### LIMIT指定後にOFFSETを追加するための中間オブジェクト
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    inner class LimitClause internal constructor() {
        /**
         * ## OFFSET指定
         * ### UNION ALL全体で先頭から読み飛ばす行数を指定する
         * @param offsetValue 読み飛ばす行数
         * @return UNION ALL文
         * @author Masahiro Inoue
         * @since 2026-09-01
         */
        @InfoLog
        fun offset(offsetValue: Int = DEFAULT_OFFSET_VALUE): UnionAll<T> =
            this@UnionAll.also {
                duplicateMethodCallValidator.validateNoDuplicateMethodCall(SelectClause.OFFSET)
                require(offsetValue >= 0) { AE00005 }
                this@UnionAll.offsetValue = offsetValue
            }

        /**
         * ## SQL生成
         * ### LIMITのみ指定した状態でUNION ALL文を生成する
         * @return 生成されたSQL
         * @author Masahiro Inoue
         * @since 2026-09-01
         */
        @InfoLog
        fun build(): String = this@UnionAll.build()

        /** LIMITのみ指定した状態で参照するバインド値 */
        val bindValues: List<Any?>
            get() = this@UnionAll.bindValues
    }

    /**
     * ## 追加バインド値取得
     * ### 各Select、LIMIT、OFFSETの順でバインド値を合成する
     * @return SQLのプレースホルダー順に並んだバインド値
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    override fun additionalBindValues(): List<Any?> = buildList {
        unionSelectList.forEach { select ->
            select.build()
            addAll(select.bindValues)
        }
        limitValue?.let(::add)
        offsetValue?.let(::add)
    }

    /**
     * ## UNION ALL文生成
     * ### 各Selectを指定順に結合し、結果列正規化、ORDER、LIMIT、OFFSETを追加する
     * @return 生成されたSQL
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    @InfoLog
    override fun build(): String {
        val componentStatements = unionSelectList.mapIndexed { index, select ->
            select.build().also {
                validateComponentClauses(select, index)
                validateColumnCount(select, index)
            }
        }
        val sourceColumnNames = resolveSelectOutputColumnNames(unionSelectList.first())
        val resultColumnStatement = sourceColumnNames.zip(resultProperties)
            .joinToString(ARGUMENT_DELIMITER) { (sourceColumnName, resultProperty) ->
                "$UNION_RESULT_ALIAS.$sourceColumnName as ${buildResultColumnName(resultProperty)}"
            }
        val compoundStatement = componentStatements.joinToString(" union all ")
        val orderStatement = buildOrderStatement()
        val limitStatement = if (limitValue == null) "" else " limit ?"
        val offsetStatement = if (offsetValue == null) "" else " offset ?"
        query = "select $resultColumnStatement from ($compoundStatement) $UNION_RESULT_ALIAS" +
            orderStatement + limitStatement + offsetStatement
        return query
    }

    /**
     * ## 結果Entityメタ情報検証
     * ### 共通Validatorのエラーを実行時例外へ変換する
     * @param entityMeta 検証対象メタ情報
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    private fun validateResultEntityMeta(entityMeta: EntityMeta) {
        val result = EntityMetaValidator().validate(entityMeta)
        result.warnings.forEach { warning -> logger.log(WARNING, warning) }
        if (result.hasErrors) {
            throw IllegalArgumentException(result.errors.joinToString(System.lineSeparator()))
        }
    }

    /**
     * ## 結果カラム名重複検証
     * ### 派生テーブルの列として公開する結果Entityのカラム名が一意であることを検証する
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    private fun validateResultColumnNames() {
        resultProperties
            .groupBy { it.columnName }
            .filterValues { properties -> properties.size > 1 }
            .keys
            .firstOrNull()
            ?.let { duplicateColumnName ->
                throw IllegalArgumentException(
                    AE00043.format(duplicateColumnName, resultEntity.simpleName),
                )
            }
    }

    /**
     * ## 構成Select句検証
     * ### 複合SELECTの構成要素に置けないORDER、LIMIT、OFFSETを拒否する
     * @param select 検証対象Select
     * @param index Selectの指定位置
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    private fun validateComponentClauses(
        select: Select<out SelectEntity>,
        index: Int,
    ) {
        DISALLOWED_COMPONENT_CLAUSES
            .firstOrNull { clause -> select.queryStructure.containsKey(clause) }
            ?.let { clause ->
                throw IllegalArgumentException(AE00042.format(clause.sql, index + 1))
            }
    }

    /**
     * ## 結果列数検証
     * ### 構成Selectの出力列数が結果Entityの表示対象列数と一致することを検証する
     * @param select 検証対象Select
     * @param index Selectの指定位置
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    private fun validateColumnCount(
        select: Select<out SelectEntity>,
        index: Int,
    ) {
        val actualColumnCount = resolveSelectOutputColumnNames(select).size
        require(actualColumnCount == resultProperties.size) {
            AE00040.format(index + 1, resultProperties.size, actualColumnCount)
        }
    }

    /**
     * ## Select出力カラム名解決
     * ### Selectが使用するEntityとTableRef aliasから実際のSELECT結果カラム名を定義順に取得する
     * @param select 解決対象Select
     * @return SELECT結果カラム名
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    private fun resolveSelectOutputColumnNames(
        select: Select<out SelectEntity>,
    ): List<String> = select.usedEntityClasses.flatMap { tableRef ->
        runtimeEntityMetaFactory.create(tableRef.entityClass)
            .properties
            .filterNot { it.hideFromSelect }
            .map { property -> "${tableRef.alias}_${property.aliasName}" }
    }

    /**
     * ## ORDER BY句生成
     * ### 結果Entityプロパティを正規化後の結果カラム名へ変換する
     * @return ORDER BY句。未指定の場合は空文字
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    private fun buildOrderStatement(): String {
        if (orderColumns.isEmpty()) {
            return ""
        }
        val orderStatement = orderColumns.joinToString(ARGUMENT_DELIMITER) { order ->
            order.build(buildResultColumnName(resolveOrderProperty(order)))
        }
        return " order by $orderStatement"
    }

    /**
     * ## 結果カラム名生成
     * ### 既存Selectと同じ規則で結果Entityのaliasと項目aliasを連結する
     * @param property 結果Entityのプロパティメタ情報
     * @return SELECT結果に公開するカラム名
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    private fun buildResultColumnName(property: PropertyMeta): String =
        "${resultEntityMeta.tableAlias}_${property.aliasName}"

    /**
     * ## ORDER対象プロパティ解決
     * ### ORDERに指定されたプロパティが結果Entityの表示対象列に属することを検証して返す
     * @param order 並び替え定義
     * @return 対応する結果プロパティメタ情報
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    private fun resolveOrderProperty(order: Order): PropertyMeta {
        val propertyName = order.column.name
        val declaringEntity = order.column.extractClassFromProperty()
        return resultProperties.firstOrNull { property ->
            declaringEntity == resultEntity && property.propertyName == propertyName
        } ?: throw IllegalArgumentException(
            AE00041.format(propertyName, resultEntity.simpleName),
        )
    }

    /** UNION ALL生成用定数 */
    companion object {
        /** 必要なSelectの最小件数 */
        private const val MINIMUM_SELECT_COUNT = 2

        /** 複合結果を包む派生テーブルalias */
        private const val UNION_RESULT_ALIAS = "UNION_ALL_RESULT"

        /** 構成Selectへの指定を禁止する句 */
        private val DISALLOWED_COMPONENT_CLAUSES =
            listOf(SelectClause.ORDER, SelectClause.LIMIT, SelectClause.OFFSET)

/**
 * ## UNION ALL文生成
 * ### 複数の完成したSelectをUNION ALLで結合する
 * @param resultEntity UNION ALL結果の列定義に使用するEntity
 * @param unionSelect UNION ALLで結合するSelect
 * @return UNION ALL文生成クラス
 * @author Masahiro Inoue
 * @since 2026-09-01
 */
@Suppress("SpreadOperator")
fun <T : SelectEntity> unionAll(
    resultEntity: KClass<T>,
    vararg unionSelect: Select<out SelectEntity>,
): UnionAll<T> = UnionAll(resultEntity, *unionSelect)
    }
}
