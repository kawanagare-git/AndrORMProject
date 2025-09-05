package jp.pgw.lab78.androrm.common.database.function

/**
 * ## 関数列挙型
 * ### 列に使用する関数定義
 * @param query クエリに使用される文字列
 * @param isSingleArgument 引数が1つしかないか
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
enum class ColumnFunction(val query: String ,val isSingleArgument: Boolean ) {
    COUNT("count",true),
    SUM("sum",true),
    AVG("avg",true),
    MAX("max",true),
    MIN("min",true),
    GROUP_CONCAT("group_concat",false),
    LENGTH("length",true),
    LOWER("lower",true),
    UPPER("upper",true),
    REPLACE("replace",false),
    SUBSTR("substr",false),
    TRIM("trim",false),
    LTRIM("ltrim",false),
    RTRIM("rtrim",false),
    DATE("date",false),
    TIME("time",false),
    DATETIME("datetime",false),
    STRFTIME("strftime",false),
    JULIANDAY("julianday",false),
    ABS("abs",false),
    ROUND("round",false),
    RANDOM("random",false),
    IFNULL("ifnull",false),
    COALESCE("coalesce",false),
    NULLIF("nullif",false),
    CUSTOM("",false),
}