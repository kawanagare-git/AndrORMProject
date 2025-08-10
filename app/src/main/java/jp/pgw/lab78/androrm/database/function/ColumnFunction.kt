package jp.pgw.lab78.androrm.database.function

import jp.pgw.lab78.androrm.database.function.interfaces.FunctionBuilderLike

enum class ColumnFunction(
    val sql: String,
): FunctionBuilderLike by FunctionBuilder(sql) {
    COUNT("count"),
    SUM("sum"),
    AVG("avg"),
    MAX("max"),
    MIN("min"),
    GROUP_CONCAT("group_concat"),
    LENGTH("length"),
    LOWER("lower"),
    UPPER("UPPER"),
    REPLACE("replace"),
    SUBSTR("substr"),
    TRIM("trim"),
    GLOB("glob"),
    DATE("date"),
    TIME("time"),
    DATETIME("datetime"),
    STRFTIME("strftime"),
    JULIANDAY("julianday"),
    ABS("abs"),
    ROUND("round"),
    RANDOM("random"),
    IFNULL("ifnull"),
    COALESCE("coalesce"),
    NULLIF("nullif"),
}