package jp.pgw.lab78.androrm.database.function

import jp.pgw.lab78.androrm.database.function.interfaces.FunctionBuilderLike

/**
 * ## having 用関数定義
 * ### `having` で使用する関数を列挙子として定義
 *
 * ### 仕様
 * #### 各列挙値を SQLite の集計関数名へ対応付け、型付きカラムの関数式を生成する。
 * @param query クエリに使用される文字列
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
enum class AggregateFunction(
    val query: String,
): FunctionBuilderLike by FunctionBuilder(query) {
    COUNT("count"),
    SUM("sum"),
    AVG("avg"),
    MIN("min"),
    MAX("max"),
    GROUP_CONCAT("group_concat"),
    STRING_AGG("string_agg");
}
