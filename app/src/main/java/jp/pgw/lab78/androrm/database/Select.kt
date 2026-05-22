package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.Constants.LogicalOperator.AND
import jp.pgw.lab78.androrm.common.Constants.PRIMARY_DELIMITER
import jp.pgw.lab78.androrm.common.database.SupportFunction.isFunctionColumn
import jp.pgw.lab78.androrm.common.database.SupportFunction.isHiddenFromSelect
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.logging.LogLevel.TRACE
import jp.pgw.lab78.androrm.common.logging.LogScope.APP
import jp.pgw.lab78.androrm.common.logging.aop.InfoLog
import jp.pgw.lab78.androrm.common.logging.aop.TraceLog
import jp.pgw.lab78.androrm.common.meta.EntityMeta
import jp.pgw.lab78.androrm.common.meta.EntityMetaValidator
import jp.pgw.lab78.androrm.common.meta.PropertyMeta
import jp.pgw.lab78.androrm.database.DmlConstant.MULTI_SPACE_REGEX
import jp.pgw.lab78.androrm.database.condition.ConditionBuilder
import jp.pgw.lab78.androrm.database.condition.HavingConditionBuilder
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryStructureLike
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.condition.sealed.Condition
import jp.pgw.lab78.androrm.database.condition.sealed.GroupByColumn
import jp.pgw.lab78.androrm.database.condition.sealed.Order
import jp.pgw.lab78.androrm.database.meta.RuntimeEntityMetaFactory
import jp.pgw.lab78.androrm.database.reference.TableRef
import jp.pgw.lab78.androrm.database.utility.Constants.DEFAULT_LIMIT_VALUE
import jp.pgw.lab78.androrm.database.utility.Constants.DEFAULT_OFFSET_VALUE
import jp.pgw.lab78.androrm.database.validation.DuplicateMethodCallValidator
import jp.pgw.lab78.androrm.database.validation.QueryMethodCall
import jp.pgw.lab78.shared.library.Utils.isNull
import java.util.EnumMap
import java.util.Locale
import java.util.logging.Level.WARNING
import java.util.logging.Logger
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * ## Select 文生成クラス
 * ### select 句を構成する要素を基に select 文を生成します
 * @param fromTable テーブル参照情報
 * @param isDistinct select 文で distinct を指定する場合は true
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
class Select<T : SelectEntity>(
    private val fromTable: TableRef<out T>,
    private val isDistinct: Boolean = false,
) : QueryWithBindValues(), QueryStructureLike {
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

    /** ログ出力移譲 */
    private val logger: Logger by lazy { APP.create(minLogLevel = TRACE) }

    /** Select クラスで使用するエンティティクラスのリスト */
    private val usedEntityClasses = mutableListOf<KClass<out SelectEntity>>()

    /** Entity メタ情報生成 */
    private val runtimeEntityMetaFactory = RuntimeEntityMetaFactory()

    /** 主 Entity の正規化済みメタ情報 */
    private val mainEntityMeta = runtimeEntityMetaFactory.create(fromTable.entityClass)

    /** テーブル名 */
    private val mainTableName = mainEntityMeta.tableName

    /** テーブルエイリアス */
    private val mainTableAlias = fromTable.alias

    /** クエリの構文を管理するマップ */
    private val queryStructureMap = enumMapOf<SelectClause, MutableList<String>>()

    /** 抽出カラムリスト */
    private val selectColumnList = mutableListOf<String>()

    /** 検索条件リスト */
    private val whereConditions = mutableListOf<Condition>()

    /** 関数結果検索条件リスト */
    private val havingConditions = mutableListOf<Condition>()

    /** グループ倍自動生成用リスト */
    private val groupByColumns = mutableListOf<GroupByColumn>()

    /** 並び替えカラムリスト */
    private val orderColumns = mutableListOf<Order>()

    /** JOIN 句用バインド値リスト */
    private val joinBindValues: MutableList<Any?> = mutableListOf()

    /** WHERE 句用バインド値リスト */
    private val whereBindValues: MutableList<Any?> = mutableListOf()

    /** HAVING 句用バインド値リスト */
    private val havingBindValues: MutableList<Any?> = mutableListOf()

    /** クエリ格納 */
    private lateinit var query: String

    /** 使用済みテーブルエイリアス */
    private val usedTableAliases = mutableSetOf<String>()

    /** 重複メソッド呼び出し検証インスタンス */
    private val duplicateMethodCallValidator =
        DuplicateMethodCallValidator<SelectClause>("Select")

    /** 最大読み出し行数 */
    private var limitValue: Int? = null

    /** 読み出し開始行 */
    private var offsetValue: Int? = null

    /** ビルドフラグ */
    private var isBuild: Boolean = false

    /**
     * ## select 文を構成要素列挙クラス
     * ### セレクト文を構成する要素を列挙子として構成する
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private enum class SelectClause(
        val sql: String,
        override val methodName: String,
    ) : QueryMethodCall {
        SELECT("select", ""),
        JOIN("join", ""),
        WHERE("where", "where"),
        GROUP("group by", ""),
        HAVING("having", "having"),
        ORDER("order by", "order"),
        LIMIT("limit", "limit"),
        OFFSET("offset", "offset"),
    }

    /**
     * ## 結合方法列挙型
     * ### join メソッドで結合方法を指定するための列挙子
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    enum class JoinType {
        INNER, LEFT, RIGHT, CROSS, NATURAL;
    }

    /**
     * ## コンストラクタ
     * ### 一番単純な select 文を生成します
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    init {
        @InfoLog
        @TraceLog
        fun initialize() {
            // 主 Entity のメタ情報を検証
            validateEntityMeta(mainEntityMeta)
            // 主テーブルの SELECT 対象列を追加
            appendSelectableColumns(mainEntityMeta, mainTableAlias)
            // from 句とテーブル名の定義を設定
            queryStructureMap[SelectClause.SELECT] =
                mutableListOf("from $mainTableName $mainTableAlias")
            // select 文で使用するエンティティクラスを登録
            usedEntityClasses += fromTable.entityClass
            registerTableAlias(mainTableName, mainTableAlias)
        }
        initialize()
    }

    /**
     * ## join メソッド
     * ### テーブル結合を指定する
     * @param joinType 結合方法（LEFT RIGHT CROSS等）を指定
     * @param joinedEntity 結合するエンティティクラス（副クラス）
     * @param on 条件を構築するための DSL ブロック。`ConditionBuilder` の拡張ラムダとして記述。
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    @InfoLog
    @TraceLog
    fun join(
        joinType: JoinType,
        joinedEntity: KClass<out SelectEntity>,
        on: ConditionBuilder.() -> Unit
    ): Select<T> {
        // 結合対象の Entity メタ情報を生成
        val joinedEntityMeta = runtimeEntityMetaFactory.create(joinedEntity)
        // テーブル結合の指定
        return join(
            joinType = joinType,
            joinedTable = TableRef(
                entityClass = joinedEntity,
                alias = joinedEntityMeta.tableAlias,
            ),
            on = on,
        )
    }

    /**
     * ## join メソッド
     * ### テーブル結合を指定する
     * @param joinType 結合方法（LEFT RIGHT CROSS等）を指定
     * @param joinedTable 結合するエンティティクラス（副クラス）
     * @param on 条件を構築するための DSL ブロック。`ConditionBuilder` の拡張ラムダとして記述。
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2026-05-12
     */
    @InfoLog
    @TraceLog
    fun join(
        joinType: JoinType,
        joinedTable: TableRef<out SelectEntity>,
        on: ConditionBuilder.() -> Unit
    ): Select<T> {
        isBuild = false
        // 結合対象の Entity メタ情報を生成
        val joinedEntityMeta = runtimeEntityMetaFactory.create(joinedTable.entityClass)
        validateEntityMeta(joinedEntityMeta)
        // 結合対象のテーブル名とエイリアスを取得
        val joinedTableName = joinedEntityMeta.tableName
        val joinedTableAlias = joinedTable.alias
        // テーブル名とテーブルエイリアスの組み合わせを登録、重複時は例外
        registerTableAlias(joinedTableName, joinedTableAlias)
        // 結合対象のテーブルの SELECT 対象列を追加
        appendSelectableColumns(joinedEntityMeta, joinedTableAlias)
        // 結合条件を生成
        val valueHolder = object : QueryWithBindValues() {}
        val joinCondition = ConditionBuilder(valueHolder).apply(on).buildList()
        // バインド変数の設定
        joinBindValues.addAll(valueHolder.bindValues)
        // join句の生成
        queryStructureMap.getOrPut(SelectClause.JOIN) { mutableListOf() }
            .add(
                "${joinType.name.lowercase(Locale.ROOT)} "
                        + "join $joinedTableName $joinedTableAlias"
                        + " on ${joinCondition.joinToString(" AND ") { it.build() }}"
            )
        // エンティティクラスを登録
        usedEntityClasses += joinedTable.entityClass
        return this
    }

    /**
     * ## join メソッド
     * ### テーブル結合を指定する
     * @param joinType 結合方法（LEFT RIGHT CROSS等）を指定
     * @param joinedEntity 結合するエンティティクラス（副クラス）
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2026-01-11
     */
    @InfoLog
    fun join(
        joinType: JoinType,
        joinedEntity: KClass<out SelectEntity>,
    ): JoinCondition {
        isBuild = false
        // 結合するエンティティのメタ情報を生成
        val joinedEntityMeta = runtimeEntityMetaFactory.create(joinedEntity)

        return JoinCondition(
            joinType = joinType,
            joinedTable = TableRef(
                entityClass = joinedEntity,
                alias = joinedEntityMeta.tableAlias,
            ),
        )
    }

    /**
     * ## join メソッド
     * ### テーブル結合を指定する
     * @param joinType 結合方法（LEFT RIGHT CROSS等）を指定
     * @param joinedTable 結合するエンティティクラス（副クラス）
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2026-05-12
     */
    @InfoLog
    fun join(
        joinType: JoinType,
        joinedTable: TableRef<out SelectEntity>,
    ): JoinCondition {
        isBuild = false
        return JoinCondition(joinType, joinedTable)
    }

    /**
     * ## JoinCondition クラス
     * ### join メソッド内で使用する結合条件クラス
     * @param joinType 結合方法
     * @param joinedTable 結合するエンティティクラス
     * @author Masahiro Inoue
     * @since 2026-01-11
     */
    inner class JoinCondition(
        private val joinType: JoinType,
        private val joinedTable: TableRef<out SelectEntity>,
    ) {

        /** ## on メソッド
         * ### テーブル結合条件を指定する
         * @param block 条件を構築するための DSL ブロック。`ConditionBuilder` の拡張ラムダとして記述。
         * @return 自身のインスタンス(this)
         * @author Masahiro Inoue
         * @since 2026-01-11
         */
        @InfoLog
        fun on(block: ConditionBuilder.() -> Unit): Select<T> =
            this@Select.join(joinType, joinedTable, block)
    }

    /**
     * ## where メソッド
     * ### テーブル検索条件を指定する
     * @param block 条件を構築するための DSL ブロック。`ConditionBuilder` の拡張ラムダとして記述。
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    @InfoLog
    fun where(block: ConditionBuilder.() -> Unit): Select<T> {
        duplicateMethodCallValidator.validateNoDuplicateMethodCall(SelectClause.WHERE)
        isBuild = false
        val valueHolder = object : QueryWithBindValues() {}
        val builder = ConditionBuilder(valueHolder).apply(block)
        // Select は builder の中身を意識せず、リストだけ取得して保持
        whereConditions += builder.buildList()
        // バインド変数の設定
        whereBindValues.addAll(valueHolder.bindValues)
        return this
    }

    /**
     * ## having メソッド
     * ### 集計結果検索条件を指定する
     * @param block 条件を構築するための DSL ブロック。`HavingBuilder` の拡張ラムダとして記述。
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    @InfoLog
    fun having(block: HavingConditionBuilder.() -> Unit): Select<T> {
        duplicateMethodCallValidator.validateNoDuplicateMethodCall(SelectClause.HAVING)
        isBuild = false
        val valueHolder = object : QueryWithBindValues() {}
        val builder = HavingConditionBuilder(valueHolder).apply(block)
        havingConditions += builder.buildList()
        // バインド変数の設定
        havingBindValues.addAll(valueHolder.bindValues)
        // HAVING 句が指定されると自動的に GROUP BY 句を生成する
        // ただし、関数列が定義されている場合、GROUP BY 句が生成されている可能性がある
        if (groupByColumns.isEmpty()) {
            detectGroupColumns().map { column ->
                groupByColumns += GroupByColumn(column)
            }
        }
        return this
    }

    /**
     * ## テーブルエイリアス登録
     * ### 同一 Select 内で同じ alias が再利用されないよう検証する
     * @param tableName テーブル名
     * @param tableAlias テーブルエイリアス
     */
    @InfoLog
    private fun registerTableAlias(
        tableName: String,
        tableAlias: String,
    ) {
        require(usedTableAliases.add(tableAlias)) {
            "Duplicate table alias '$tableAlias' was detected. " +
                    "tableName='$tableName'. " +
                    "Use a different alias with table(..., alias = \"...\")."
        }
    }

    /**
     * ## グループ化カラム検出関数
     * ### SELECT 句に指定されたカラムのうち、関数列以外のカラムを抽出する
     * @return 関数列以外のカラムリスト
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    @InfoLog
    private fun detectGroupColumns() =
        usedEntityClasses.flatMap { entityClass ->
            entityClass.memberProperties
                .filter { !it.isFunctionColumn() }
                .filterNot { it.isHiddenFromSelect() }
        }

    /**
     * ## order メソッド
     * ### 集計結果検索条件を指定する
     * @param by 並び替え DSL ブロック。`OrderBuilder` の拡張ラムダとして記述。
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    @InfoLog
    fun order(by: OrderDsl.() -> Unit): Select<T> {
        duplicateMethodCallValidator.validateNoDuplicateMethodCall(SelectClause.ORDER)
        isBuild = false
        val builder = OrderDsl().apply(by)
        orderColumns += builder.orders
        return this
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
    fun limit(limitValue: Int = DEFAULT_LIMIT_VALUE): LimitClause {
        duplicateMethodCallValidator.validateNoDuplicateMethodCall(SelectClause.LIMIT)
        require(limitValue >= 0) {
            "limitValue must be greater than or equal to 0."
        }
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
        fun offset(offsetValue: Int = DEFAULT_OFFSET_VALUE): Select<T> {
            duplicateMethodCallValidator.validateNoDuplicateMethodCall(SelectClause.OFFSET)
            require(offsetValue >= 0) {
                "offsetValue must be greater than or equal to 0."
            }
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
        fun build(): String {
            return this@Select.build()
        }

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
     */
    protected override fun additionalBindValues(): List<Any?> = buildList {
        addAll(joinBindValues)
        addAll(whereBindValues)
        addAll(havingBindValues)

        limitValue?.let { value ->
            add(value)
        }

        offsetValue?.let { value ->
            add(value)
        }
    }

    /**
     * ## EntityMeta 検証
     * ### 共通 Validator の結果を runtime 例外に変換する
     * @param entityMeta 検証対象 Entity メタ情報
     * @throws IllegalArgumentException 検証エラーがある場合にスローされる例外
     * @author Masahiro Inoue
     * @since 2026-04-28
     */
    @InfoLog
    private fun validateEntityMeta(entityMeta: EntityMeta) {
        val validationResult = EntityMetaValidator().validate(entityMeta)
        // 警告があればログに出力
        validationResult.warnings.forEach { warningMessage ->
            logger.log(WARNING, warningMessage)
        }
        // エラーがあれば例外をスロー
        if (validationResult.hasErrors) {
            throw IllegalArgumentException(
                validationResult.errors.joinToString(System.lineSeparator())
            )
        }
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
                    ?: error("Function type is missing for property '${propertyMeta.propertyName}'.")
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
     * ## 関数引数解決
     * ### 関数引数がプロパティ名ならカラム参照へ変換し
     * ### それ以外（文字列リテラル等）はそのまま返す
     * @param entityMeta 対象 Entity のメタ情報
     * @param tableAlias 連番が付与されたテーブルエイリアス
     * @param arg 関数引数として指定された文字列
     * @return 解決された関数引数（カラム参照または元の文字列）
     * @author Masahiro Inoue
     * @since 2026-04-28
     */
    @InfoLog
    private fun resolveFunctionArgument(
        entityMeta: EntityMeta,
        tableAlias: String,
        arg: String
    ): String {
        // 引数がプロパティ名に一致するか確認し、一致する場合はテーブルエイリアスとカラム名を組み合わせた参照に変換する
        val propertyMeta = entityMeta.properties.firstOrNull { it.propertyName == arg }
        // 一致するプロパティがない場合は引数をそのまま返す（文字列リテラルや数値リテラルなど）
        return if (propertyMeta.isNull()) {
            // 引数をそのまま返す
            arg
        } else {
            // プロパティ名に一致する場合、テーブルエイリアスとカラム名を組み合わせた参照に変換して返す
            "$tableAlias.${propertyMeta?.columnName}"
        }
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
            val selectClause = buildString {
                append("select ")
                if (isDistinct) append("distinct ")
                append(selectColumnList.joinToString(", ") { it })
            }

            /**
             * ## 構成要素追加
             * ## ローカル関数
             * ### 引数に指定された内容をクエリ構成に追加する
             * @param clauseId クエリの「句」
             * @param separator 区切り文字列
             * @param element 追加する要素
             */
            fun addClauseIfNotEmpty(
                clauseId: SelectClause,
                separator: CharSequence,
                element: List<QueryStructureLike>
            ) {
                if (element.isNotEmpty()) {
                    val clause = element.joinToString(separator) { it.build() }
                    queryStructureMap[clauseId] = mutableListOf("${clauseId.sql} $clause")
                }
            }

            addClauseIfNotEmpty(SelectClause.WHERE, AND.query, whereConditions.toList())
            addClauseIfNotEmpty(SelectClause.HAVING, AND.query, havingConditions.toList())
            addClauseIfNotEmpty(SelectClause.GROUP, PRIMARY_DELIMITER, groupByColumns.toList())
            addClauseIfNotEmpty(SelectClause.ORDER, PRIMARY_DELIMITER, orderColumns.toList())
            limitValue?.let {
                queryStructureMap[SelectClause.LIMIT] = mutableListOf("${SelectClause.LIMIT.sql} ?")
            }
            offsetValue?.let {
                queryStructureMap[SelectClause.OFFSET] =
                    mutableListOf("${SelectClause.OFFSET.sql} ?")
            }
            val otherClauses = SelectClause.entries
                .joinToString(" ") { queryStructureMap[it]?.joinToString(" ") ?: " " }
            // select 文を生成
            query = "$selectClause $otherClauses".replace(MULTI_SPACE_REGEX, " ").trim()
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