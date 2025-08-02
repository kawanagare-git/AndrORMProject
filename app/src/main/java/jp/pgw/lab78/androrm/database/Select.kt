package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.database.SupportFunction.simpleNameToSnakeCase
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.database.condition.ConditionBuilder
import jp.pgw.lab78.androrm.database.condition.HavingConditionBuilder
import jp.pgw.lab78.androrm.database.condition.sealed.Condition
import jp.pgw.lab78.androrm.database.condition.sealed.Order
import jp.pgw.lab78.androrm.utility.Functions.getAlias
import jp.pgw.lab78.androrm.utility.Functions.getColumnDefinitions
import jp.pgw.lab78.androrm.utility.Functions.getTableName
import java.util.EnumMap
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * ## Select 文生成クラス
 * ### select 句を構成する要素を基に select 文を生成します
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
class Select<T : SelectEntity>(
    private val entityClass: KClass<out T>,
    private val isDistinct : Boolean = false
) {
    /** テーブル名：クラス名をスネークケース（大文字）に変換 */
    private val mainTableName = getTableName(entityClass)

    /** クエリの構文を管理するマップ */
    private val queryStructureMap = enumMapOf<SelectIdentifier, MutableList<String>>()

    /** 抽出カラムリスト */
    private val selectColumnList = mutableListOf<Pair<String, String>>()

    /** 検索条件リスト */
    private val whereConditions = mutableListOf<Condition>()

    /** 関数結果検索条件リスト */
    private val havingConditions = mutableListOf<Condition>()

    /** 並び替えカラムリスト */
    private val orderColumns = mutableListOf<Order>()

    /**
     * ## select 文を構成要素列挙クラス
     * ### セレクト文を構成する要素を列挙子として構成する
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private enum class SelectIdentifier {
        SELECT, JOIN, WHERE, GROUP, HAVING, ORDER
    }

    /**
     * ## 結合方法列挙型
     * ### join メソッドで結合方法を指定するための列挙子
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    enum class JoinType {
        INNER, LEFT, RIGHT, CROSS, NATURAL;

        val sqliteSupported: Boolean
            get() = when (this) {
                INNER, LEFT, CROSS -> true
                else -> false
            }
    }

    /**
     * ## コンストラクタ
     * ### 一番単純な select 文を生成します
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    init {
        // カラム名：クラスのメンバー・プロパティ名をスネークケース（大文字）に変換
        selectColumnList += getColumnDefinitions(entityClass)
        // from 句とテーブル名の定義を設定
        queryStructureMap[SelectIdentifier.SELECT] = mutableListOf(" from $mainTableName")
    }

    /**
     * ## join メソッド
     * ### テーブル結合を指定する
     * @param joinType 結合方法（LEFT RIGHT CROSS等）を指定
     * @param joinedEntityClass 結合するエンティティクラス（副クラス）
     * @param block 条件を構築するための DSL ブロック。`ConditionBuilder` の拡張ラムダとして記述。
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <TJ : SelectEntity> join(joinType: JoinType
                                 , joinedEntityClass: KClass<out TJ>
                                 , block: ConditionBuilder.() -> Unit
    ): Select<T> {
        // 結合テーブル名取得
        val joinedTableName = getTableName(joinedEntityClass).trim()
        val joinCondition = ConditionBuilder().apply(block).buildList()
        queryStructureMap.getOrPut(SelectIdentifier.JOIN) { mutableListOf() }
            .add("${joinType.name.lowercase(Locale.ROOT)} "
                    + "join $joinedTableName on ${joinCondition.joinToString(" AND ") { it.build() }}"
            )
        // カラム名：クラスのメンバー・プロパティ名をスネークケース（大文字）に変換
        selectColumnList += getColumnDefinitions(joinedEntityClass)
        return this
    }

    /**
     * ## where メソッド
     * ### テーブル検索条件を指定する
     * @param condition 条件を構築されたインスタンス
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun where(condition: ConditionBuilder): Select<T> {
        whereConditions += condition.buildList()
        return this
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
        val builder = ConditionBuilder().apply(block)
        whereConditions += builder.buildList()
        return this
    }

    /**
     * ## group メソッド
     * ### 集計範囲を指定する
     * @param columns 集計条件として参照するカラム
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <TX : SelectEntity>group(vararg columns: KProperty1<out TX, *>): Select<T> {
        val clause = columns.joinToString(", ") { generateColumn(it) }
        queryStructureMap.getOrPut(SelectIdentifier.GROUP) { mutableListOf() }
            .add("group by $clause")
        return this
    }

    /**
     * ## having メソッド
     * ### 集計結果検索条件を指定する
     * @param condition 条件を構築されたインスタンス
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun having(condition: HavingConditionBuilder): Select<T> {
        havingConditions += condition.buildList()
        return this
    }

    /**
     * ## having メソッド
     * ### 集計結果検索条件を指定する
     * @param block 条件を構築するための DSL ブロック。`HavingConditionBuilder` の拡張ラムダとして記述。
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun having(block: HavingConditionBuilder.() -> Unit): Select<T> {
        val builder = HavingConditionBuilder().apply(block)
        havingConditions += builder.buildList()
        return this
    }

    /**
     * ## order メソッド
     * ### 集計結果検索条件を指定する
     * @param block 並び替え DSL ブロック。`OrderBuilder` の拡張ラムダとして記述。
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun order(block: OrderBuilder.() -> Unit): Select<T> {
        val builder = OrderBuilder().apply(block)
        orderColumns += builder.buildList()
        return this
    }

    /**
     * ## build メソッド
     * ### 最終的な Select 文を生成します
     * @return  生成された SQL 文字列
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun build(): String {
        val selectClause = "select ${if (isDistinct) "distinct " else ""}" +
                selectColumnList.joinToString(", ") { (col, _) -> col }
        if (whereConditions.isNotEmpty()) {
            val whereClause = whereConditions.joinToString(" AND ") { it.build() }
            queryStructureMap[SelectIdentifier.WHERE] = mutableListOf("where $whereClause")
        }
        if (havingConditions.isNotEmpty()) {
            val havingClause = havingConditions.joinToString(" AND ") { it.build() }
            queryStructureMap[SelectIdentifier.HAVING] = mutableListOf("having by $havingClause")
        }
        if (orderColumns.isNotEmpty()) {
            val orderClause = orderColumns.joinToString(", ") { it.build() }
            queryStructureMap[SelectIdentifier.ORDER] = mutableListOf("order by $orderClause")
        }
        val clauses = SelectIdentifier.entries.joinToString(" ") { identifier ->
                            queryStructureMap[identifier]?.joinToString(" ") ?: ""
                        }
        return StringBuilder().append(selectClause).append(clauses).toString().trim()
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

    /**
     * ## カラム生成メソッド
     * ### where メソッドや join メソッドで渡された Entity クラスの property を
     * ### カラム文字列として生成する
     * @param column 生成するカラム
     * @return 生成されたカラム名、エイリアス設定が有れば付与される
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : SelectEntity> generateColumn(column: KProperty1<out T, *>): String {
        // エンティティクラスの取得
        val entityClass = (column.parameters.first().type.classifier as? KClass<T>)
            ?: error("Could not infer entity class")
        // エイリアスの生成
        val alias = getAlias(entityClass)
        val columnName = column.simpleNameToSnakeCase()
        return "$alias$columnName"
    }

}