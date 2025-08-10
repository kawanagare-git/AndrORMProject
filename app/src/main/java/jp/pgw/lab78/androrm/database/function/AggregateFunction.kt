package jp.pgw.lab78.androrm.database.function

import jp.pgw.lab78.androrm.database.function.interfaces.FunctionBuilderLike

/**
 * ## having 用関数定義
 * ### 「having by」で使用する関数を列挙子として定義
 * @param sql SQL文に展開される文字列
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
enum class AggregateFunction(
    val sql: String,
): FunctionBuilderLike by FunctionBuilder(sql) {
    COUNT("count"),
    SUM("sum"),
    AVG("avg"),
    MIN("min"),
    MAX("max"),
    GROUP_CONCAT("group_concat"),
    STRING_AGG("string_agg");
}
