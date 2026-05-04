package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.Constants.COMMA
import jp.pgw.lab78.androrm.common.Constants.LogicalOperator.AND
import jp.pgw.lab78.androrm.common.database.SupportFunction.isFunctionColumn
import jp.pgw.lab78.androrm.common.database.SupportFunction.isHiddenFromSelect
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.logging.LogLevel.*
import jp.pgw.lab78.androrm.common.logging.LogScope.APP
import jp.pgw.lab78.androrm.common.meta.EntityMeta
import jp.pgw.lab78.androrm.common.meta.EntityMetaValidator
import jp.pgw.lab78.androrm.common.meta.PropertyMeta
import jp.pgw.lab78.androrm.database.DmlConstant.ANY_CLOSE_BRACKET_REGEX
import jp.pgw.lab78.androrm.database.DmlConstant.ANY_OPEN_BRACKET_REGEX
import jp.pgw.lab78.androrm.database.DmlConstant.MULTI_SPACE_REGEX
import jp.pgw.lab78.androrm.database.condition.ConditionBuilder
import jp.pgw.lab78.androrm.database.condition.HavingConditionBuilder
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryStructureLike
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.condition.sealed.Condition
import jp.pgw.lab78.androrm.database.condition.sealed.GroupByColumn
import jp.pgw.lab78.androrm.database.condition.sealed.Order
import jp.pgw.lab78.androrm.database.meta.RuntimeEntityMetaFactory
import java.util.EnumMap
import java.util.Locale
import java.util.logging.Level.WARNING
import java.util.logging.Logger
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * ## Select 文生成クラス
 * ### select 句を構成する要素を基に select 文を生成します
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
class Select<T : SelectEntity>(
    fromEntity: KClass<out T>,
    private val isDistinct: Boolean = false,
) : QueryWithBindValues(), QueryStructureLike {
    /** ログ出力移譲 */
    private val logger: Logger by lazy { APP.create(minLogLevel = TRACE) }

    /** Select クラスで使用するエンティティクラスのリスト */
    private val usedEntityClasses = mutableSetOf<KClass<out SelectEntity>>()

    /** Entity メタ情報生成 */
    private val runtimeEntityMetaFactory = RuntimeEntityMetaFactory()

    /** 主 Entity の正規化済みメタ情報 */
    private val mainEntityMeta = runtimeEntityMetaFactory.create(fromEntity)

    /** テーブル名 */
    private val mainTableName = mainEntityMeta.tableName

    /** テーブルエイリアス */
    private val mainTableAlias = mainEntityMeta.tableAlias

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

    /** クエリ格納 */
    private lateinit var query: String

    /** ビルドフラグ */
    private var isBuild: Boolean = false

    /**
     * ## select 文を構成要素列挙クラス
     * ### セレクト文を構成する要素を列挙子として構成する
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private enum class SelectClause(val sql: String) {
        SELECT("select"),
        JOIN("join"),
        WHERE("where"),
        GROUP("group by"),
        HAVING("having"),
        ORDER("order by")
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
        logger.log(TRACE.level, "Select init:enter fromEntity=$fromEntity / isDistinct=$isDistinct")
        // 主 Entity のメタ情報を検証
        validateEntityMeta(mainEntityMeta)
        // カラム名：クラスのメンバー・プロパティ名をスネークケース（大文字）に変換
        mainEntityMeta.properties
            .filterNot { it.hideFromSelect }
            .forEach { propertyMeta ->
                selectColumnList += buildSelectExpression(mainEntityMeta, propertyMeta)
            }
        // from 句とテーブル名の定義を設定
        queryStructureMap[SelectClause.SELECT] =
            mutableListOf("from $mainTableName $mainTableAlias")
        // select 文で使用するエンティティクラスを登録
        usedEntityClasses += fromEntity
        logger.log(TRACE.level, "Select init:returning")
    }

//    fun defineFunctionalColumn(function: ColumnFunction, column: KProperty1<T, *>) = ""

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
    fun join(
        joinType: JoinType,
        joinedEntity: KClass<out SelectEntity>,
        on: ConditionBuilder.() -> Unit
    ): Select<T> {
        logger.log(
            TRACE.level,
            "Select join:enter joinType=$joinType / joinedEntity=$joinedEntity / on=$on"
        )
        isBuild = false
        // 結合テーブル名取得
        val joinedEntityMeta = runtimeEntityMetaFactory.create(joinedEntity)
        validateEntityMeta(joinedEntityMeta)

        val joinedTableName = joinedEntityMeta.tableName
        val joinedTableAlias = joinedEntityMeta.tableAlias
        val joinCondition = ConditionBuilder(this).apply(on).buildList()
        queryStructureMap.getOrPut(SelectClause.JOIN) { mutableListOf() }
            .add(
                "${joinType.name.lowercase(Locale.ROOT)} "
                        + "join $joinedTableName $joinedTableAlias"
                        + " on ${joinCondition.joinToString(" AND ") { it.build() }}"
            )
        usedEntityClasses += joinedEntity
        logger.log(TRACE.level, "Select join:returning $this")
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
    fun join(
        joinType: JoinType,
        joinedEntity: KClass<out SelectEntity>,
    ): JoinCondition<T> {
        logger.log(TRACE.level, "Select join:enter joinType=$joinType / joinedEntity=$joinedEntity")
        isBuild = false
        logger.log(TRACE.level, "Select join:returning $this")
        return JoinCondition(this, joinType, joinedEntity)
    }

    /**
     * ## JoinCondition クラス
     * ### join メソッド内で使用する結合条件クラス
     * @author Masahiro Inoue
     * @since 2026-01-11
     */
    class JoinCondition<T : SelectEntity>(
        private val select: Select<T>,
        private val joinType: JoinType,
        private val joinedEntity: KClass<out SelectEntity>,
    ) {
        init {
            select.logger.log(
                TRACE.level,
                "JoinCondition init:enter select=$select / joinType=$joinType / joinedEntity=$joinedEntity"
            )
            select.logger.log(TRACE.level, "JoinCondition init:returning")
        }

        /** ## on メソッド
         * ### テーブル結合条件を指定する
         * @param block 条件を構築するための DSL ブロック。`ConditionBuilder` の拡張ラムダとして記述。
         * @return 自身のインスタンス(this)
         * @author Masahiro Inoue
         * @since 2026-01-11
         */
        fun on(block: ConditionBuilder.() -> Unit): Select<T> {
            select.logger.log(TRACE.level, "Select join:enter block=$block")
            select.logger.log(TRACE.level, "Select join:returning $this")
            return select.join(joinType, joinedEntity, block)
        }
    }

    /**
     * ## where メソッド
     * ### テーブル検索条件を指定する
     * @param block 条件を構築するための DSL ブロック。`ConditionBuilder` の拡張ラムダとして記述。
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun where(block: ConditionBuilder.() -> Unit): Select<T> {
        logger.log(TRACE.level, "Select where:enter block=$block")
        isBuild = false
        val builder = ConditionBuilder(this).apply(block)
        // Select は builder の中身を意識せず、リストだけ取得して保持
        whereConditions += builder.buildList()
        logger.log(TRACE.level, "Select where:returning $this")
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
    fun having(block: HavingConditionBuilder.() -> Unit): Select<T> {
        logger.log(TRACE.level, "Select having:enter block=$block")
        isBuild = false
        val builder = HavingConditionBuilder(this).apply(block)
        havingConditions += builder.buildList()
        // HAVING 句が指定されると自動的に GROUP BY 句を生成する
        // ただし、関数列が定義されている場合、GROUP BY 句が生成されている可能性がある
        if (groupByColumns.isEmpty()) {
            detectGroupColumns().map { column ->
                groupByColumns += GroupByColumn(column)
            }
        }
        logger.log(DEBUG.level, "Select having:groupByColumns = $groupByColumns")
        logger.log(TRACE.level, "Select having:returning $this")
        return this
    }

    /**
     * ## グループ化カラム検出関数
     * ### SELECT 句に指定されたカラムのうち、関数列以外のカラムを抽出する
     * @return 関数列以外のカラムリスト
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    private fun detectGroupColumns() =
        usedEntityClasses.flatMap { entityClass ->
            logger.log(DEBUG.level, "Select detectGroupColumns:entityClass = $entityClass")
            entityClass.memberProperties
                .filter { !it.isFunctionColumn() }
                .filterNot { it.isHiddenFromSelect() }
                .onEach {
                    logger.log(DEBUG.level, "Select detectGroupColumns:it = $it")
                }
        }

    /**
     * ## order メソッド
     * ### 集計結果検索条件を指定する
     * @param by 並び替え DSL ブロック。`OrderBuilder` の拡張ラムダとして記述。
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun order(by: OrderDsl.() -> Unit): Select<T> {
        logger.log(TRACE.level, "Select order:enter by=$by")
        isBuild = false
        val builder = OrderDsl().apply(by)
        orderColumns += builder.orders
        logger.log(TRACE.level, "Select order:returning $this")
        return this
    }

    /**
     * ## EntityMeta 検証
     * ### 共通 Validator の結果を runtime 例外に変換する
     * @param entityMeta 検証対象 Entity メタ情報
     * @throws IllegalArgumentException 検証エラーがある場合にスローされる例外
     * @author Masahiro Inoue
     * @since 2026-04-28
     */
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
     * ## SELECT 句用式生成
     * ### PropertyMeta から SELECT 句断片を生成する
     * @param entityMeta 対象 Entity のメタ情報
     * @param propertyMeta 対象プロパティのメタ情報
     * @return 生成された SELECT 句断片
     * @author Masahiro Inoue
     * @since 2026-04-28
     */
    private fun buildSelectExpression(entityMeta: EntityMeta, propertyMeta: PropertyMeta): String {
        return if (!propertyMeta.isFunction) {
            // 通常カラムの場合、テーブルエイリアスとカラム名を組み合わせてエイリアスを付与して返す
            "$mainTableAlias.${propertyMeta.columnName} as ${mainTableAlias}_${propertyMeta.aliasName}"
        } else {
            // 関数列の場合、関数式を生成
            val functionExpression = propertyMeta.rawFunction.takeIf { it.isNotBlank() }
                ?: run {
                    // 関数タイプが必要な場合、関数タイプを取得
                    val functionType = propertyMeta.functionType
                        ?: error("Function type is missing for property '${propertyMeta.propertyName}'.")
                    // 関数引数を解決して関数式を生成
                    val args = propertyMeta.functionArgs
                        .map { arg -> resolveFunctionArgument(entityMeta, arg) }
                        .toTypedArray()
                    // 関数式を生成
                    functionType.createQuery(*args)
                }
            // 関数式にエイリアスを付与して返す
            "$functionExpression as ${mainTableAlias}_${propertyMeta.aliasName}"
        }
    }

    /**
     * ## 関数引数解決
     * ### 関数引数がプロパティ名ならカラム参照へ変換し
     * ### それ以外（文字列リテラル等）はそのまま返す
     * @param entityMeta 対象 Entity のメタ情報
     * @param arg 関数引数として指定された文字列
     * @return 解決された関数引数（カラム参照または元の文字列）
     * @author Masahiro Inoue
     * @since 2026-04-28
     */
    private fun resolveFunctionArgument(entityMeta: EntityMeta, arg: String): String {
        // 引数がプロパティ名に一致するか確認し、一致する場合はテーブルエイリアスとカラム名を組み合わせた参照に変換する
        val propertyMeta = entityMeta.properties.firstOrNull { it.propertyName == arg }
        // 一致するプロパティがない場合は引数をそのまま返す（文字列リテラルや数値リテラルなど）
        return if (propertyMeta != null) {
            // プロパティ名に一致する場合、テーブルエイリアスとカラム名を組み合わせた参照に変換して返す
            "$mainTableAlias.${propertyMeta.columnName}"
        } else {
            // 引数をそのまま返す
            arg
        }
    }

    /**
     * ## SELECT 文文字列生成関数
     * ### 最終的な Select 文を生成する
     * @return  生成された SQL 文字列
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    override fun build(): String {
        logger.log(TRACE.level, "Select build:enter")
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
            addClauseIfNotEmpty(SelectClause.GROUP, COMMA, groupByColumns.toList())
            addClauseIfNotEmpty(SelectClause.ORDER, COMMA, orderColumns.toList())
            val otherClauses = SelectClause.entries
                .joinToString(" ") { queryStructureMap[it]?.joinToString(" ") ?: " " }
            // select 文を生成
            query = "$selectClause $otherClauses".replace(ANY_OPEN_BRACKET_REGEX, "(")
                .replace(ANY_CLOSE_BRACKET_REGEX, ")")
                .replace(MULTI_SPACE_REGEX, " ").trim()
        }
        logger.log(TRACE.level, "Select order:returning $query")
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