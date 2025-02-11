package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.database.SupportFunction.getAlias
import jp.pgw.lab78.androrm.database.SupportFunction.simpleNameToSnakeCase
import jp.pgw.lab78.androrm.database.interfaces.SelectEntity
import java.util.EnumMap
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * ## Select 文生成クラス
 * ### select 句を構成する要素を基に select 文を生成します
 */
class Select<T : SelectEntity>(private val entityClass: KClass<T>,private val isDistinct : Boolean = false) {
    /** クエリの構文を管理するマップ */
    private  val queryStructureMap  = enumMapOf<SelectIdentifier, List<String>>()

    /**
     * ## select 文を構成要素列挙クラス
     * ### セレクト文を構成する要素を列挙子として構成する
     */
    private enum class SelectIdentifier{
        SELECT,JOIN,WHERE,GROUP,HAVING,ORDER
    }

    /**
     * ## 結合方法列挙型
     * ### join メソッドで結合方法を指定するための列挙子
     */
    enum class JoinType{
        INNER,LEFT,RIGHT,CROSS,NATURAL;

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
    init{
        /** テーブル名：クラス名をスネークケース（大文字）に変換 */
        val tableName = SupportFunction.getTableName(entityClass)
        /** カラム名：クラスのメンバー・プロパティ名をスネークケース（大文字）に変換 */
        val columnList = SupportFunction.getColumnDefinitions(entityClass)
        /** distinct の有無 */
        val distinct = if (isDistinct) {" DISTINCT"}else{""}
        queryStructureMap[SelectIdentifier.SELECT] =
            listOf("select$distinct ${columnList.joinToString(", ") { (columnName, _) -> columnName }} from $tableName")
    }

    /**
     * ## join メソッド
     * ### テーブル結合を指定する
     */
    fun <TJ : SelectEntity>join(joinType: JoinType
                                , entityClass: KClass<TJ>
                                , column: KProperty1<T, *>
                                , operator: ComparisonOperator
                                , linkColumn: KProperty1<TJ, *>): Select<T> {
        val tableName = SupportFunction.getTableName(entityClass)
        val columnString = generateColumn(column)
        val linkColumnString = generateColumn(linkColumn)
        queryStructureMap[SelectIdentifier.JOIN] =
            listOf("${joinType.name.lowercase(Locale.ROOT)} "
                    + "join $tableName on $columnString ${operator.symbol} $linkColumnString")
        return this
    }

    /**
     * ## where メソッド
     * ### テーブル検索条件を指定する
     */
    fun where(){}

    /**
     * ## group メソッド
     * ### 集計範囲を指定する
     */
    fun group(){}

    /**
     * ## having メソッド
     * ### 集計結果検索条件を指定する
     */
    fun having(){}

    /**
     * ## order メソッド
     * ### 集計結果検索条件を指定する
     */
    fun order(){}

    /**
     * ## build メソッド
     * ### 最終的な Select 文を生成します
     */
    fun build(): String =
        queryStructureMap.entries.joinToString(separator = " ") { (_, queryList) ->
            queryList.joinToString(separator = " ")
        }

    /**
     * ## enumMapOf メソッド
     * ### EnumMap<K, V> のインスタンスを生成する
     * ### コンビニエンスメソッド
     */
    private inline fun <reified K : Enum<K>, V> enumMapOf(): EnumMap<K, V> =
        EnumMap(K::class.java)

    /**
     * ## カラム生成メソッド
     * ### where メソッドや join メソッドで渡された Entity クラスの property を
     * ### カラム文字列として生成する
     */
    private fun <T : SelectEntity>generateColumn(column: KProperty1<T, *>):String {
        // エンティティクラスの取得
        val entityClass = (column.parameters.first().type.classifier as? KClass<T>)
            ?: error("Could not infer entity class")
        // エイリアスの生成
        val alias = getAlias(entityClass)
            .takeIf { it.isNotBlank() }
            ?.let { "$it." }
            ?: ""
        val columnName = column.simpleNameToSnakeCase()
        return "$alias$columnName"
    }
}
