package jp.pgw.lab78.androrm.common.database.function

import jp.pgw.lab78.androrm.common.dml.interfaces.SqlFunction

/**
 * ## 集約関数 (Aggregate Function)
 * ### GROUP BY 句が必要な関数群を定義する列挙型クラス
 * ### HAVING 句や ORDER 句で使用可能。
 */
enum class SqlAggregateFunction() : SqlFunction<SqlAggregateFunction> {
    COUNT,
    COUNT_ALL,
    SUM,
    AVG,
    MAX,
    MIN,
    TOTAL,
    GROUP_CONCAT;

    override val functionName
        get() = ColumnFunction.valueOf(name).functionName

    override val argumentArity
        get() = ColumnFunction.valueOf(name).argumentArity

    override fun getReturnType(argTypes: List<String>) =
        ColumnFunction.valueOf(name).getReturnType(argTypes)

    override fun build(vararg args: String) =
        ColumnFunction.valueOf(name).build(*args)
}