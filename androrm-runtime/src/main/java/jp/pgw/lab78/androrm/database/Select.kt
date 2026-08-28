package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.Constants.ARGUMENT_DELIMITER
import jp.pgw.lab78.androrm.common.Constants.DEFAULT_LIMIT_VALUE
import jp.pgw.lab78.androrm.common.Constants.DEFAULT_OFFSET_VALUE
import jp.pgw.lab78.androrm.common.Constants.PRIMARY_DELIMITER
import jp.pgw.lab78.androrm.common.MessageConstants.AE00004
import jp.pgw.lab78.androrm.common.MessageConstants.AE00005
import jp.pgw.lab78.androrm.common.MessageConstants.AE00006
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.logging.aop.InfoLog
import jp.pgw.lab78.androrm.common.logging.aop.TraceLog
import jp.pgw.lab78.androrm.common.meta.EntityMeta
import jp.pgw.lab78.androrm.common.meta.PropertyMeta
import jp.pgw.lab78.androrm.common.database.function.SqlAggregateFunction
import jp.pgw.lab78.androrm.database.DmlConstant.MULTI_SPACE_REGEX
import jp.pgw.lab78.androrm.database.condition.ConditionBuilder
import jp.pgw.lab78.androrm.database.condition.base.BaseSelect
import jp.pgw.lab78.androrm.database.condition.sealed.Order
import jp.pgw.lab78.androrm.database.meta.RuntimeEntityMetaFactory
import jp.pgw.lab78.androrm.database.meta.SelectClause
import jp.pgw.lab78.androrm.database.queryparts.JoinType
import jp.pgw.lab78.androrm.database.reference.TableRef
import java.util.EnumMap
import kotlin.reflect.KClass

/**
 * ## Select 文生成クラス
 * ### select 句を構成する要素を基に select 文を生成します
 *
 * ### 仕様
 * #### Entity メタ情報から選択カラムと FROM 句を構成し、JOIN、WHERE、HAVING、ORDER、LIMIT、OFFSET を定義順で連結する。
 * #### JOIN による nullable 化とテーブル別名を追跡し、条件値は句の出現順でバインド値として公開する。
 * #### 同一インスタンスでは一度だけ指定可能な句があり、条件変更後は生成済み SQL を再構築する。
 * @param fromTable テーブル参照情報
 * @param isDistinct select 文で distinct を指定する場合は true
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
class Select<T : SelectEntity>(
    private val fromTable: TableRef<out T>,
    private val isDistinct: Boolean = false,
) : BaseSelect<T, Select<T>>() {
    /**
     * ## コンストラクタ
     * @param fromEntity エンティティクラス
     * @param isDistinct select 文で distinct を指定する場合は true
     * @author Masahiro Inoue
     * @since 2026-05-12
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

    /**
     * SELECT文生成に関する補助処理を提供する。
     *
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    companion object {
        /**
         * ## テーブル間データ転送用
         * ### 基本的な用途は AndrOrmDatabaseHelper での使用
         * @param tableName セレクト元テーブル
         * @param columnList セレクト元カラム
         * @author Masahiro Inoue
         * @since 2026-05-31
         */
        internal fun tableColumns(tableName: String, columnList: List<String>): String =
            "select ${columnList.joinToString(PRIMARY_DELIMITER)} from $tableName"
    }

    /** select 文の土台 */
    private val selectStatement = "select ${if (isDistinct) "distinct " else ""}%s "

    /** 主 Entity の正規化済みメタ情報 */
    override val mainEntityMeta = runtimeEntityMetaFactory.create(fromTable.entityClass)

    /** 抽出カラムリスト */
    private val selectColumnList = mutableListOf<String>()

    /** 集約関数使用フラグ */
    private var hasAggregateFunction = false

    /** 並び替えカラムリスト */
    private val orderColumns = mutableListOf<Order>()

    /** 最大読み出し行数 */
    private var limitValue: Int? = null

    /** 読み出し開始行 */
    private var offsetValue: Int? = null

    /**
     * ## イニシャライザ
     * ### 一番単純な select 文を生成します
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    init {
        /**
         * 主Entityを検証し、SELECT対象列、FROM句、テーブル別名を初期化する。
         *
         * @author Masahiro Inoue
         * @since 2025-08-01
         */
        @InfoLog
        @TraceLog
        fun initialize() {
            val mainTableName = mainEntityMeta.tableName
            val mainTableAlias = fromTable.alias
            // 主 Entity のメタ情報を検証
            validateEntityMeta(mainEntityMeta)
            // 主テーブルの SELECT 対象列を追加
            appendSelectableColumns(mainEntityMeta, mainTableAlias)
            // from 句とテーブル名の定義を設定
            queryStructureMap[SelectClause.SELECT] =
                mutableListOf("from $mainTableName $mainTableAlias")
            // select 文で使用するエンティティクラスを登録
            usedEntityClasses += fromTable
            registerTableAlias(mainTableName, mainTableAlias)
        }
        initialize()
    }

    /**
     * 結合したテーブルのメタ情報を検証し、SELECT対象列と使用済み別名へ登録する。
     *
     * @param joinedTable 結合したテーブル参照
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    override fun onTableJoined(
        joinedTable: TableRef<out SelectEntity>,
    ) {
        val joinedEntityMeta = runtimeEntityMetaFactory.create(joinedTable.entityClass)
        validateEntityMeta(joinedEntityMeta)
        registerTableAlias(joinedEntityMeta.tableName, joinedTable.alias)
        appendSelectableColumns(joinedEntityMeta, joinedTable.alias)
        usedEntityClasses.add(joinedTable)
    }

    /**
     * ## join メソッド
     * ### テーブル結合を指定する
     * @param joinType 結合方法（LEFT RIGHT CROSS等）を指定
     * @param joinedEntity 結合するエンティティクラス（副クラス）
     * @param on 条件を構築するための DSL ブロック。`ConditionBuilder` の拡張ラムダとして記述。
     * @return 結合条件を指定するための中間オブジェクト
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    @InfoLog
    @TraceLog
    fun join(
        joinType: JoinType,
        joinedEntity: KClass<out SelectEntity>,
        on: ConditionBuilder.() -> Unit,
    ): Select<T> =
        this.join(
            joinType = joinType,
            joinedTable = TableRef(
                joinedEntity, alias = RuntimeEntityMetaFactory().create(joinedEntity).tableAlias,
            ),
            on = on,
        )

    /**
     * ## join メソッド
     * ### テーブル結合を指定する
     * @param joinType 結合方法（LEFT CROSS等）を指定
     * @param joinedEntity 結合するエンティティクラス（副クラス）
     * @return OFFSET の指定または SQL の生成を行うための中間オブジェクト
     * @author Masahiro Inoue
     * @since 2026-01-11
     */
    @InfoLog
    fun join(
        joinType: JoinType,
        joinedEntity: KClass<out SelectEntity>,
    ) =
        join(
            joinType = joinType,
            joinedTable = TableRef(
                joinedEntity, alias = RuntimeEntityMetaFactory().create(joinedEntity).tableAlias,
            ),
        )

    /**
     * ## where メソッド
     * ### テーブル検索条件を指定する
     * @param block 条件を構築するための DSL ブロック。`ConditionBuilder` の拡張ラムダとして記述。
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    @InfoLog
    override fun where(block: ConditionBuilder.() -> Unit): Select<T> =
        self.also { whereDelegate.where(block) }

    /**
     * ## order メソッド
     * ### SELECT 結果の並び順を指定する
     * @param by 並び替え DSL ブロック。`OrderDsl` の拡張ラムダとして記述。
     * @return OFFSET の指定または SQL の生成を行うための中間オブジェクト
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    @InfoLog
    fun order(by: OrderDsl.() -> Unit): Select<T> =
        self.also {
            duplicateMethodCallValidator.validateNoDuplicateMethodCall(SelectClause.ORDER)
            isBuild = false
            val builder = OrderDsl().apply(by)
            orderColumns += builder.orders
        }

    /**
     * ## limit メソッド
     * ### 最大レコード件数を指定する
     * @param limitValue 最大レコード件数
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2026-05-14
     */
    @InfoLog
    fun limit(limitValue: Int = DEFAULT_LIMIT_VALUE): LimitClause =
        LimitClause().also {
            duplicateMethodCallValidator.validateNoDuplicateMethodCall(SelectClause.LIMIT)
            require(limitValue >= 0) { AE00004 }
            isBuild = false
            this.limitValue = limitValue
            return LimitClause()
        }

    /**
     * ## LIMIT 句指定後の操作
     * ### LIMIT 指定後に OFFSET を追加するための中間オブジェクト
     * @author Masahiro Inoue
     * @since 2026-05-14
     */
    inner class LimitClause internal constructor() {

        /**
         * ## OFFSET 指定
         * ### LIMIT 指定後に、先頭からスキップする件数を指定する
         * @param offsetValue スキップする件数
         * @return 自身のインスタンス(this)
         * @author Masahiro Inoue
         * @since 2026-05-14
         */
        @InfoLog
        fun offset(offsetValue: Int = DEFAULT_OFFSET_VALUE): Select<T> =
            this@Select.also {
                duplicateMethodCallValidator.validateNoDuplicateMethodCall(SelectClause.OFFSET)
                require(offsetValue >= 0) { AE00005 }
                isBuild = false
                this@Select.offsetValue = offsetValue
                return this@Select
            }

        /**
         * ## SQL 生成
         * ### LIMIT のみ指定して SQL を生成する
         * @return 生成された SQL
         * @author Masahiro Inoue
         * @since 2026-05-14
         */
        @InfoLog
        fun build(): String = this@Select.build()

        /**
         * ## バインド値
         * ### LIMIT のみ指定した状態でも bindValues を参照できるようにする
         * @return バインド値のリスト
         * @author Masahiro Inoue
         * @since 2026-05-14
         */
        val bindValues: List<Any?>
            get() = this@Select.bindValues
    }

    /**
     * ## 追加バインド値取得
     * ### SQL 句の出現順に合わせて bindValues を合成する
     * @author Masahiro Inoue
     * @since 2026-04-28
     */
    override fun additionalBindValues(): List<Any?> = buildList {
        addAll(super.additionalBindValues())
        limitValue?.let { value -> add(value) }
        offsetValue?.let { value -> add(value) }
    }

    /**
     * ## SELECT 対象列追加
     * ### hideFromSelect = true の列を除外し、SELECT 句リストへ追加する
     * @param entityMeta 対象 Entity のメタ情報
     * @param tableAlias 使用するテーブルエイリアス
     * @author Masahiro Inoue
     * @since 2026-04-28
     */
    @InfoLog
    @TraceLog
    private fun appendSelectableColumns(entityMeta: EntityMeta, tableAlias: String) {
        entityMeta.properties
            .filterNot { it.hideFromSelect }
            .forEach { propertyMeta ->
                // 集約関数がSELECT対象に含まれているか確認
                if (SqlAggregateFunction.entries.any { aggregateFunction -> aggregateFunction.name == propertyMeta.functionType?.name }) {
                    hasAggregateFunction = true
                }
                selectColumnList += buildSelectExpression(entityMeta, tableAlias, propertyMeta)
            }
    }

    /**
     * ## SELECT 句用式生成
     * ### PropertyMeta から SELECT 句断片を生成する
     * @param entityMeta 対象 Entity のメタ情報
     * @param tableAlias 連番が付与されたテーブルエイリアス
     * @param propertyMeta 対象プロパティのメタ情報
     * @return 生成された SELECT 句断片
     * @author Masahiro Inoue
     * @since 2026-04-28
     */
    @InfoLog
    private fun buildSelectExpression(
        entityMeta: EntityMeta,
        tableAlias: String,
        propertyMeta: PropertyMeta
    ): String = if (!propertyMeta.isFunction) {
        // 通常カラムの場合、使用中のテーブルエイリアスとカラム名を組み合わせてエイリアスを付与して返す
        "$tableAlias.${propertyMeta.columnName} as ${tableAlias}_${propertyMeta.aliasName}"
    } else {
        // 関数列の場合、関数式を生成
        val functionExpression = propertyMeta.rawFunction.takeIf { it.isNotBlank() }
            ?: run {
                // 関数タイプが必要な場合、関数タイプを取得
                val functionType = propertyMeta.functionType
                    ?: error(AE00006.format(propertyMeta.propertyName))
                // 関数引数を解決して関数式を生成
                val args = propertyMeta.functionArgs
                    .map { arg -> resolveFunctionArgument(entityMeta, tableAlias, arg) }
                    .toTypedArray()
                // 関数式を生成
                functionType.build(*args)
            }
        // 関数式に使用中のテーブルエイリアスを基準にした別名を付与して返す
        "$functionExpression as ${tableAlias}_${propertyMeta.aliasName}"
    }

    /**
     * ## SELECT 文文字列生成関数
     * ### 最終的な Select 文を生成する
     * @return  生成された SQL 文字列
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    @InfoLog
    override fun build(): String {
        if (!isBuild) {
            isBuild = true
            // 集約関数がSELECT対象に含まれている場合、GROUP BY句を自動生成する
            if (hasAggregateFunction) {
                ensureGroupByColumns()
            }
            val baseStatement = selectStatement.format(selectColumnList.joinToString(", "))
            buildInClauseDefinitionOrder({
                addClauseIfNotEmpty(SelectClause.ORDER, ARGUMENT_DELIMITER, orderColumns.toList())
                limitValue?.let {
                    queryStructureMap[SelectClause.LIMIT] =
                        mutableListOf("${SelectClause.LIMIT.sql} ?")
                }
                offsetValue?.let {
                    queryStructureMap[SelectClause.OFFSET] =
                        mutableListOf("${SelectClause.OFFSET.sql} ?")
                }
            })
            val otherClauses = SelectClause.entries
                .joinToString(" ") { queryStructureMap[it]?.joinToString(" ") ?: " " }
            // select 文を生成
            query = "$baseStatement $otherClauses".replace(MULTI_SPACE_REGEX, " ").trim()
        }
        return query
    }

    /**
     * ## enumMapOf メソッド
     * ### EnumMap<K, V> のインスタンスを生成する
     * ### コンビニエンスメソッド
     * @return 生成された EnumMap のインスタンス
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private inline fun <reified K : Enum<K>, V> enumMapOf(): EnumMap<K, V> =
        EnumMap(K::class.java)
}