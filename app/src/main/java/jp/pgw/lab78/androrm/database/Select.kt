package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.database.SupportFunction.getAlias
import jp.pgw.lab78.androrm.database.SupportFunction.getColumnDefinitions
import jp.pgw.lab78.androrm.database.SupportFunction.getTableName
import jp.pgw.lab78.androrm.database.SupportFunction.simpleNameToSnakeCase
import jp.pgw.lab78.androrm.database.interfaces.SelectEntity
import jp.pgw.lab78.androrm.database.sealed.Compare
import jp.pgw.lab78.androrm.database.sealed.Condition
import java.util.EnumMap
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * ## Select 文生成クラス
 * ### select 句を構成する要素を基に select 文を生成します
 */
class Select<T : SelectEntity>(private val entityClass: KClass<T>,private val isDistinct : Boolean = false) {
    /** テーブル名：クラス名をスネークケース（大文字）に変換 */
    private val mainTableName = SupportFunction.getTableName(entityClass)

    /** クエリの構文を管理するマップ */
    private val queryStructureMap = enumMapOf<SelectIdentifier, MutableList<String>>()

    /** 抽出カラムリスト */
    private val selectColumnList = mutableListOf<Pair<String, String>>()

    /** 検索条件リスト */
    private val whereConditions = mutableListOf<Condition>()

    /** 関数結果検索条件リスト */
    private val havingConditions = mutableListOf<Condition>()

    /**
     * ## select 文を構成要素列挙クラス
     * ### セレクト文を構成する要素を列挙子として構成する
     */
    private enum class SelectIdentifier {
        SELECT, JOIN, WHERE, GROUP, HAVING, ORDER
    }

    /**
     * ## 結合方法列挙型
     * ### join メソッドで結合方法を指定するための列挙子
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
     */
    init {
        // カラム名：クラスのメンバー・プロパティ名をスネークケース（大文字）に変換
        val columns = getColumnDefinitions(entityClass)
        selectColumnList += columns
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
     */
    fun <TJ : SelectEntity> join(joinType: JoinType
                                 , joinedEntityClass: KClass<TJ>
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
        val columns = getColumnDefinitions(entityClass)
        selectColumnList += columns
        return this
    }

    /**
     * ## where メソッド
     * ### テーブル検索条件を指定する
     * @param lhsProperty 検索条件のカラム
     * @param operator 検索演算子
     * @param value 検索値
     * @return 自身のインスタンス(this)
     */
    fun <TX : SelectEntity>where(lhsProperty: KProperty1<TX, *>, operator: ComparisonOperator, value: Any): Select<T> {
        whereConditions += mutableListOf(Compare.Value(lhsProperty, operator, value))
        return this
    }

    /**
     * ## where メソッド
     * ### テーブル検索条件を指定する
     * @param block 条件を構築するための DSL ブロック。`ConditionBuilder` の拡張ラムダとして記述。
     * @return 自身のインスタンス(this)
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
     */
    fun <TX : SelectEntity>group(vararg columns: KProperty1<TX, *>): Select<T> {
        val clause = columns.joinToString(", ") { generateColumn(it) }
        queryStructureMap.getOrPut(SelectIdentifier.GROUP) { mutableListOf() }
            .add("group by $clause")
        return this
    }

    /**
     * ## having メソッド
     * ### 集計結果検索条件を指定する
     * @param block 条件を構築するための DSL ブロック。`HavingConditionBuilder` の拡張ラムダとして記述。
     * @return 自身のインスタンス(this)
     */
    fun having(block: HavingConditionBuilder.() -> Unit): Select<T> {
        val builder = HavingConditionBuilder().apply(block)
        havingConditions += builder.buildList()
        return this
    }
    /**
     * ## order メソッド
     * ### 集計結果検索条件を指定する
     * @param columns 並び替え条件
     * @param descending 降順指定: true 降順 / false 昇順
     * @return 自身のインスタンス(this)
     */
    fun order(vararg columns: KProperty1<T, *>, descending: Boolean = false): Select<T> {
        val clause = columns.joinToString(", ") { generateColumn(it) } +
                if (descending) " desc" else ""
        queryStructureMap.getOrPut(SelectIdentifier.ORDER) { mutableListOf() }
            .add("order by $clause")
        return this
    }

    /**
     * ## build メソッド
     * ### 最終的な Select 文を生成します
     * @return  生成された SQL 文字列
     */
    fun build(): String {
        val distinct = if (isDistinct) "distinct " else ""
        val selectClause = "select $distinct" +
                selectColumnList.joinToString(", ") { (col, _) -> col }
        if (whereConditions.isNotEmpty()) {
            val whereClause = whereConditions.joinToString(" AND ") { it.build() }
            queryStructureMap[SelectIdentifier.WHERE] = mutableListOf("where $whereClause")
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
     */
    private inline fun <reified K : Enum<K>, V> enumMapOf(): EnumMap<K, V> =
        EnumMap(K::class.java)

    /**
     * ## カラム生成メソッド
     * ### where メソッドや join メソッドで渡された Entity クラスの property を
     * ### カラム文字列として生成する
     * @param column 生成するカラム
     * @return 生成されたカラム名、エイリアス設定が有れば付与される
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : SelectEntity> generateColumn(column: KProperty1<T, *>): String {
        // エンティティクラスの取得
        val entityClass = (column.parameters.first().type.classifier as? KClass<T>)
            ?: error("Could not infer entity class")
        // エイリアスの生成
        val alias = getAlias(entityClass)
        val columnName = column.simpleNameToSnakeCase()
        return "$alias$columnName"
    }

}