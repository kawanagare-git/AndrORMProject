package jp.pgw.lab78.generated.database.function

import jp.pgw.lab78.generated.common.SupportFunction.simpleNameToSnakeCase
import jp.pgw.lab78.generated.ksp.interfaces.entity.Entity
import jp.pgw.lab78.generated.utility.Functions.extractClassFromProperty
import jp.pgw.lab78.generated.utility.Functions.getAlias
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

enum class AggregateFunction(
    val sql: String, val supportedInSQLite: Boolean
    , val supportedInPostgres: Boolean
    , val supportedInMySQL: Boolean) {
    COUNT("count", true, true, true),
    SUM("sum", true, true, true),
    AVG("avg", true, true, true),
    MIN("min", true, true, true),
    MAX("max", true, true, true),
    GROUP_CONCAT("group_concat", true, false, true),
    STRING_AGG("string_agg", false, true, false);

    fun <T : Entity>create(column: KProperty1<T, *>, isDistinct: Boolean = false): String {
        val distinct = if (isDistinct) "DISTINCT " else ""
        val alias = getAlias(extractClassFromProperty(column) as KClass<out Entity>)
        return "${this.sql}($distinct$alias${column.simpleNameToSnakeCase()})"
    }
}
