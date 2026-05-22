package jp.pgw.lab78.androrm.common.database.function

import jp.pgw.lab78.androrm.common.dml.interfaces.SqlFunction

/**
 * ## スカラー関数 (Scalar Function)
 * GROUP BY が不要な関数。
 * WHERE 句や ORDER 句で使用可能。
 */
enum class SqlScalarFunction() : SqlFunction<SqlScalarFunction> {
    ABS,
    LENGTH,
    LOWER,
    UPPER,
    REPLACE,
    ROUND,
    COALESCE,
    IFNULL,
    CAST,
    CONCAT,
    SUBSTR,
    TRIM,
    LTRIM,
    RTRIM,
    DATE,
    TIME,
    DATETIME,
    STRFTIME,
    JULIANDAY,
    RANDOM,
    SCALAR_MAX,
    SCALAR_MIN,
    CUSTOM, ;

    override val functionName
        get() = ColumnFunction.valueOf(name).functionName

    override val argumentArity
        get() = ColumnFunction.valueOf(name).argumentArity

    override fun getReturnType(argTypes: List<String>) =
        ColumnFunction.valueOf(name).getReturnType(argTypes)

    override fun build(vararg args: String) =
        ColumnFunction.valueOf(name).build(*args)
}
