package jp.pgw.lab78.androrm.common.database.function

/**
 * ## 関数列挙型
 * ### 列に使用する関数定義
 * @param query クエリに使用される文字列
 * @param isSingleArgument 引数が1つしかないか
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
enum class ColumnFunction(val query: String, val isSingleArgument: Boolean) {
    COUNT("count", true),//
    COUNT_ALL("count", false) {
        /**
         * ## クエリ生成（count_all 専用）
         * ### 関数クエリを生成する
         * @param arg 引数
         * @return 生成されたクエリ文字列
         * @author Masahiro Inoue
         * @since 2025-09-05
         */
        override fun createQuery(vararg arg: String): String = "$query(*)"
    },//
    SUM("sum", true),//
    AVG("avg", true),//
    MAX("max", true),//
    MIN("min", true),//
    GROUP_CONCAT("group_concat", true),//
    LENGTH("length", true),//
    LOWER("lower", true),//
    UPPER("upper", true),//
    REPLACE("replace", false),//
    SUBSTR("substr", false),//
    CONCAT("concat", false) {
        /**
         * ## クエリ生成（concat 専用）
         * ### 関数クエリを生成する
         * @param arg 引数
         * @return 生成されたクエリ文字列
         * @author Masahiro Inoue
         * @since 2025-09-05
         */
        override fun createQuery(vararg arg: String): String = arg.joinToString(" || ")
    },//
    TRIM("trim", false),//
    LTRIM("ltrim", false),//
    RTRIM("rtrim", false),//
    DATE("date", false),//
    TIME("time", false),//
    DATETIME("datetime", false),//
    STRFTIME("strftime", false),//
    JULIANDAY("julianday", false),//
    ABS("abs", false),//
    ROUND("round", false) {
        /**
         * ## クエリ生成（round 専用）
         * ### 関数クエリを生成する
         * @param arg 引数
         * @return 生成されたクエリ文字列
         * @author Masahiro Inoue
         * @since 2025-09-05
         */
        override fun createQuery(vararg arg: String): String = "$query()"
    },//
    RANDOM("random", false),//
    IFNULL("ifnull", false),//
    COALESCE("coalesce", false),
    NULLIF("nullif", false),
    CUSTOM("", false), ;

    /**
     * ## クエリ生成
     * ### 関数クエリを生成する
     * @param arg 引数
     * @return 生成されたクエリ文字列
     * @author Masahiro Inoue
     * @since 2025-09-05
     */
    open fun createQuery(vararg arg: String): String =
        if (isSingleArgument) {
            "$query(${arg.first()})"
        } else {
            "$query(${arg.joinToString(",")})"
        }
}