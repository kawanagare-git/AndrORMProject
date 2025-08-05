package jp.pgw.lab78.androrm.database.function

import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumn
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.database.utility.Functions.extractClassFromProperty
import jp.pgw.lab78.androrm.database.utility.Functions.getAlias
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * ## having 用関数定義
 * ### 「having by」で使用する関数を列挙子として定義
 * @param sql SQL文に展開される文字列
 * @param supportedInSQLite SQLiteでサポートされているか
 * @param supportedInPostgres PostgreSQLでサポートされているか
 * @param supportedInMySQL MySQLでサポートされているか
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
enum class AggregateFunction(
    val sql: String,
    val supportedInSQLite: Boolean,
    val supportedInPostgres: Boolean,
    val supportedInMySQL: Boolean
) {
    COUNT("count", true, true, true),
    SUM("sum", true, true, true),
    AVG("avg", true, true, true),
    MIN("min", true, true, true),
    MAX("max", true, true, true),
    GROUP_CONCAT("group_concat", true, false, true),
    STRING_AGG("string_agg", false, true, false);

    /**
     * ## 関数生成メソッド
     * ###
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <T : Entity>create(column: KProperty1<T, *>, isDistinct: Boolean = false): String {
        val distinct = if (isDistinct) "DISTINCT " else ""
        val alias = getAlias(extractClassFromProperty(column) as KClass<out Entity>)
        return "${this.sql}($distinct$alias${column.getColumn()})"
    }
}
