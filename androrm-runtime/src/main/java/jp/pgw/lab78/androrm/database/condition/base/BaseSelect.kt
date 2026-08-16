package jp.pgw.lab78.androrm.database.condition.base

import jp.pgw.lab78.androrm.common.Constants.ARGUMENT_DELIMITER
import jp.pgw.lab78.androrm.common.Constants.LogicalOperator.AND
import jp.pgw.lab78.androrm.common.MessageConstants.AE00003
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.logging.LogLevel.TRACE
import jp.pgw.lab78.androrm.common.logging.LogScope.APP
import jp.pgw.lab78.androrm.common.logging.aop.InfoLog
import jp.pgw.lab78.androrm.common.logging.aop.TraceLog
import jp.pgw.lab78.androrm.common.meta.EntityMeta
import jp.pgw.lab78.androrm.common.meta.EntityMetaValidator
import jp.pgw.lab78.androrm.database.condition.ConditionBuilder
import jp.pgw.lab78.androrm.database.condition.HavingConditionBuilder
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryStructureLike
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.condition.interfaces.SelectBody
import jp.pgw.lab78.androrm.database.condition.sealed.Condition
import jp.pgw.lab78.androrm.database.condition.sealed.GroupByColumn
import jp.pgw.lab78.androrm.database.interfaces.JoinConditionModel
import jp.pgw.lab78.androrm.database.meta.RuntimeEntityMetaFactory
import jp.pgw.lab78.androrm.database.meta.SelectClause
import jp.pgw.lab78.androrm.database.meta.SelectClause.*
import jp.pgw.lab78.androrm.database.queryparts.JoinClauseDelegate
import jp.pgw.lab78.androrm.database.queryparts.JoinType
import jp.pgw.lab78.androrm.database.queryparts.WhereClauseDelegate
import jp.pgw.lab78.androrm.database.reference.TableRef
import jp.pgw.lab78.androrm.database.validation.DuplicateMethodCallValidator
import jp.pgw.lab78.shared.library.Utils.isNull
import java.util.EnumMap
import java.util.logging.Level.WARNING
import java.util.logging.Logger
import kotlin.reflect.full.memberProperties

/**
 * ## Select クラス基盤
 * ### 通常の Select だけではなく サブクエリでも使用できるようにするための基準クラス
 *
 * ### 仕様
 * #### SELECT 系クエリで共有する JOIN、WHERE、HAVING、GROUP BY、バインド値、使用テーブル別名を管理する。
 * #### 句を SQL の定義順で構築し、同一別名の重複、Entity メタ情報、同じ句の重複指定を検証する。
 * #### 自己型 `R` を返すことで通常 SELECT と EXISTS サブクエリの DSL 操作を共通化する。
 * @author Masahiro Inoue
 * @since 2026-07-10
 */
abstract class BaseSelect<T : SelectEntity, R : BaseSelect<T, R>> : QueryWithBindValues(),
    SelectBody<T, R> {
    /** 自分自身をキャストして返す */
    @Suppress("UNCHECKED_CAST")
    val self = this as R

    /** WHERE 句生成委譲 */
    protected val whereDelegate =
        WhereClauseDelegate<BaseSelect<T, R>>(owner = this, ownerName = this.javaClass.simpleName) {
            isBuild = false
        }

    /** JOIN 句生成委譲 */
    protected open val joinDelegate =
        JoinClauseDelegate<BaseSelect<T, R>, SelectEntity>(
            owner = this,
            onChanged = {
                isBuild = false
            },
            onTableJoined = { joinedTable ->
                onTableJoined(

                    joinedTable,
                )
            },
        )

    /** Select クラスで使用するエンティティクラスのリスト */
    internal val usedEntityClasses = mutableListOf<TableRef<out SelectEntity>>()

    /** OUTER JOIN により Entity 自体が null になり得るテーブル参照 */
    protected val nullableByJoinTables = mutableSetOf<TableRef<out SelectEntity>>()

    /** ログ出力移譲 */
    protected val logger: Logger by lazy { APP.create(minLogLevel = TRACE) }

    /** Entity メタ情報生成 */
    protected val runtimeEntityMetaFactory = RuntimeEntityMetaFactory()

    /** 主 Entity の正規化済みメタ情報 */
    protected abstract val mainEntityMeta: EntityMeta

    /** クエリの構文を管理するマップ */
    internal val queryStructureMap: EnumMap<SelectClause, MutableList<String>> =
        enumMapOf<SelectClause, MutableList<String>>()
    internal val queryStructure
        get() = queryStructureMap.mapValues { it.value.toList() }

    /** 関数結果検索条件リスト */
    private val havingConditions = mutableListOf<Condition>()

    /** グループ倍自動生成用リスト */
    private val groupByColumns = mutableListOf<GroupByColumn>()

    /** HAVING 句用バインド値リスト */
    private val havingBindValues: MutableList<Any?> = mutableListOf()

    /** 使用済みテーブルエイリアス */
    private val usedTableAliases = mutableSetOf<String>()

    /** 重複メソッド呼び出し検証インスタンス */
    internal val duplicateMethodCallValidator =
        DuplicateMethodCallValidator<SelectClause>("Select")

    /** ビルドフラグ */
    protected var isBuild: Boolean = false

    /**
     * ## 結合尾テーブル収集
     * ### 外部結合に指定されたテーブル情報を収集する
     * @param joinedTable 結合テーブル
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    protected open fun onTableJoined(
        joinedTable: TableRef<out SelectEntity>,
    ) {
        val joinedEntityMeta = runtimeEntityMetaFactory.create(joinedTable.entityClass)
        validateEntityMeta(joinedEntityMeta)
        registerTableAlias(joinedEntityMeta.tableName, joinedTable.alias)
        usedEntityClasses.add(joinedTable)
    }

    /**
     * ## join メソッド
     * ### テーブル結合を指定する
     * @param joinType 結合方法（LEFT RIGHT CROSS等）を指定
     * @param joinedTable 結合するエンティティクラス（副クラス）
     * @param on 条件を構築するための DSL ブロック。`ConditionBuilder` の拡張ラムダとして記述。
     * @return 結合条件を指定するための中間オブジェクト
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    @InfoLog
    @TraceLog
    override fun join(
        joinType: JoinType,
        joinedTable: TableRef<out SelectEntity>,
        on: ConditionBuilder.() -> Unit,
    ): R = self.also {
        joinDelegate.join(joinType, joinedTable, on)
        if (joinType.nullableByJoin) {
            nullableByJoinTables += joinedTable
        }
    }

    /**
     * ## join メソッド
     * ### テーブル結合を指定する
     * @param joinType 結合方法（LEFT RIGHT CROSS等）を指定
     * @param joinedTable 結合するエンティティクラス（副クラス）
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    @InfoLog
    override fun join(
        joinType: JoinType,
        joinedTable: TableRef<out SelectEntity>,
    ): JoinCondition =
        JoinCondition(
            joinType = joinType,
            joinedTable = joinedTable,
        )

    /**
     * ## JoinCondition クラス
     * ### join メソッド内で使用する結合条件クラス
     * @param joinType 結合方法
     * @param joinedTable 結合するエンティティクラス
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    inner class JoinCondition(
        private val joinType: JoinType,
        private val joinedTable: TableRef<out SelectEntity>,
    ) : JoinConditionModel {

        /** ## on メソッド
         * ### テーブル結合条件を指定する
         * @param block 条件を構築するための DSL ブロック。`ConditionBuilder` の拡張ラムダとして記述。
         * @return 結合元の検索クエリ
         * @author Masahiro Inoue
         * @since 2026-07-10
         */
        @InfoLog
        override fun on(block: ConditionBuilder.() -> Unit): R =
            self.also { join(joinType, joinedTable, block) }
    }

    /**
     * ## where メソッド
     * ### テーブル検索条件を指定する
     * @param block 条件を構築するための DSL ブロック。`ConditionBuilder` の拡張ラムダとして記述。
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    @InfoLog
    override fun where(block: ConditionBuilder.() -> Unit): R =
        self.also { whereDelegate.where(block) }

    /**
     * ## having メソッド
     * ### 集計結果検索条件を指定する
     * @param block 条件を構築するための DSL ブロック。`HavingConditionBuilder` の拡張ラムダとして記述。
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    @InfoLog
    override fun having(block: HavingConditionBuilder.() -> Unit): R =
        self.also {
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
                detectGroupColumns().forEach { column ->
                    groupByColumns += GroupByColumn(column)
                }
            }
        }

    /**
     * ## OUTER JOIN null 対象判定
     * ### LEFT JOIN の結合先 Entity など、Entity 自体が null になり得るかを判定する
     * @param tableRef 判定対象テーブル参照
     * @return Entity 自体が null になり得る場合 true
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    internal fun isNullableByJoin(tableRef: TableRef<out SelectEntity>): Boolean =
        tableRef in nullableByJoinTables

    /**
     * ## テーブルエイリアス登録
     * ### 同一 Select 内で同じ alias が再利用されないよう検証する
     * @param tableName テーブル名
     * @param tableAlias テーブルエイリアス
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    @InfoLog
    protected fun registerTableAlias(
        tableName: String,
        tableAlias: String,
    ) {
        require(usedTableAliases.add(tableAlias)) {
            AE00003.format(tableAlias, tableName)
        }
    }

    /**
     * ## グループ化カラム検出関数
     * ### SELECT 句に指定されたカラムのうち、関数列以外のカラムを抽出する
     * @return 関数列以外のカラムリスト
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    @InfoLog
    protected fun detectGroupColumns() =
        usedEntityClasses.flatMap { tableRef ->
            val entityMeta = runtimeEntityMetaFactory.create(tableRef.entityClass)
            val propertyMap = tableRef.entityClass.memberProperties.associateBy { it.name }
            entityMeta.properties
                .asSequence()
                .filterNot { it.isFunction }
                .filterNot { it.hideFromSelect }
                .map { propertyMeta ->
                    propertyMap[propertyMeta.propertyName]
                        ?: error("Property not found: ${propertyMeta.propertyName}")
                }
                .toList()
        }

    /**
     * ## 追加バインド値取得
     * ### SQL 句の出現順に合わせて bindValues を合成する
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    override fun additionalBindValues(): List<Any?> = buildList {
        addAll(joinDelegate.bindValues)
        addAll(whereDelegate.bindValues)
        addAll(havingBindValues)
    }

    /**
     * ## EntityMeta 検証
     * ### 共通 Validator の結果を runtime 例外に変換する
     * @param entityMeta 検証対象 Entity メタ情報
     * @throws IllegalArgumentException 検証エラーがある場合にスローされる例外
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    @InfoLog
    protected fun validateEntityMeta(entityMeta: EntityMeta) {
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
     * ## 関数引数解決
     * ### 関数引数がプロパティ名ならカラム参照へ変換し
     * ### それ以外（文字列リテラル等）はそのまま返す
     * @param entityMeta 対象 Entity のメタ情報
     * @param tableAlias 連番が付与されたテーブルエイリアス
     * @param arg 関数引数として指定された文字列
     * @return 解決された関数引数（カラム参照または元の文字列）
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    @InfoLog
    internal fun resolveFunctionArgument(
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
            "$tableAlias.${propertyMeta.columnName}"
        }
    }

    /**
     * ## SELECT 文文字列生成関数
     * ### 最終的な Select 文を生成する
     * @return  生成された SQL 文字列
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    abstract override fun build(): String

    /**
     * ## 条件定義順文字列生成
     * ### SELECT 文で定義されている順に文字列を並べ文字列を生成する
     * ### 標準指定では、『JOIN』・『WHERE』・『HAVING』・『GROUP BY（自動生成）』の順番で処理
     * @param block 標準指定以降に並びを追加したい場合に指定。特に必要無ければ {} を記述
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    fun buildInClauseDefinitionOrder(block: () -> Unit) {
        if (joinDelegate.clauses.isNotEmpty()) {
            queryStructureMap[JOIN] = joinDelegate.clauses.toMutableList()
        }
        addClauseIfNotEmpty(WHERE, AND.query, whereDelegate.conditions)
        addClauseIfNotEmpty(HAVING, AND.query, havingConditions.toList())
        addClauseIfNotEmpty(GROUP, ARGUMENT_DELIMITER, groupByColumns.toList())
        block()
    }

    /**
     * ## 構成要素追加
     * ## ローカル関数
     * ### 引数に指定された内容をクエリ構成に追加する
     * @param clauseId クエリの「句」
     * @param separator 区切り文字列
     * @param element 追加する要素
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    internal fun addClauseIfNotEmpty(
        clauseId: SelectClause,
        separator: CharSequence,
        element: List<QueryStructureLike>
    ) {
        if (element.isNotEmpty()) {
            val clause = element.joinToString(separator) { it.build() }
            queryStructureMap[clauseId] = mutableListOf("${clauseId.sql} $clause")
        }
    }

    /**
     * ## enumMapOf メソッド
     * ### EnumMap<K, V> のインスタンスを生成する
     * ### コンビニエンスメソッド
     * @return 生成された EnumMap のインスタンス
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    private inline fun <reified K : Enum<K>, V> enumMapOf(): EnumMap<K, V> =
        EnumMap(K::class.java)
}