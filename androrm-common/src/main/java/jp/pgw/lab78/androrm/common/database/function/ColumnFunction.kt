package jp.pgw.lab78.androrm.common.database.function

/**
 * ## 関数列挙型
 * ### 列に使用する関数定義
 * @param query クエリに使用される文字列
 * @param isSingleArgument 引数が1つしかないか
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
enum class ColumnFunction(
    val query: String,
    val isSingleArgument: Boolean,
    val returnType: String = Any::class.qualifiedName!!
) {
    /** 集約関数(count) */
    COUNT("count", true, Long::class.qualifiedName!!),

    /** 集約関数(count:全行)  */
    COUNT_ALL("count", false, Long::class.qualifiedName!!) {
        /**
         * ## クエリ生成（count_all 専用）
         * ### 関数クエリを生成する
         * @param arg 引数
         * @return 生成されたクエリ文字列
         * @author Masahiro Inoue
         * @since 2025-09-05
         */
        override fun createQuery(vararg arg: String): String = "$query(*)"
    },

    /** 集約関数(sum) */
    SUM("sum", true) {
        override fun getReturnType(argTypes: List<String>): String =
            // 引数の型に基づいて戻り値の型を決定する
            resolveSumReturnType(argTypes)
    },

    /** 集約関数(avg) */
    AVG("avg", true, Double::class.qualifiedName!!),

    /** 集約関数(max) */
    MAX("max", true) {
        override fun getReturnType(argTypes: List<String>): String =
            // 引数の型に基づいて戻り値の型を決定する
            argTypes.reduceOrNull(::widerType) ?: returnType
    },

    /** 集約関数(min) */
    MIN("min", true) {
        override fun getReturnType(argTypes: List<String>): String =
            // 引数の型に基づいて戻り値の型を決定する
            argTypes.reduceOrNull(::widerType) ?: returnType
    },

    /** 集約関数(group_concat) */
    GROUP_CONCAT("group_concat", true, String::class.qualifiedName!!),

    /** 文字列r関数(length) */
    LENGTH("length", true, Int::class.qualifiedName!!),

    /** 文字列関数(lower) */
    LOWER("lower", true, String::class.qualifiedName!!),

    /** 文字列関数(upper) */
    UPPER("upper", true, String::class.qualifiedName!!),

    /** 文字列関数(replace) */
    REPLACE("replace", false, String::class.qualifiedName!!),

    /** 文字列関数(substr) */
    SUBSTR("substr", false, String::class.qualifiedName!!),

    /** 文字列関数(concat) */
    CONCAT("concat", false, String::class.qualifiedName!!) {
        /**
         * ## クエリ生成（concat 専用）
         * ### 関数クエリを生成する
         * @param arg 引数
         * @return 生成されたクエリ文字列
         * @author Masahiro Inoue
         * @since 2025-09-05
         */
        override fun createQuery(vararg arg: String): String = arg.joinToString(" || ")
    },

    /** 文字列関数(trim) */
    TRIM("trim", false, String::class.qualifiedName!!),

    /** 文字列関数(ltrim) */
    LTRIM("ltrim", false, String::class.qualifiedName!!),

    /** 文字列関数(rtrim) */
    RTRIM("rtrim", false, String::class.qualifiedName!!),

    /** 日付関数(date) */
    DATE("date", false, String::class.qualifiedName!!),

    /** 日付関数(time) */
    TIME("time", false, String::class.qualifiedName!!),

    /** 日付関数(datetime) */
    DATETIME("datetime", false, String::class.qualifiedName!!),

    /** 日付関数(strftime) */
    STRFTIME("strftime", false, String::class.qualifiedName!!),

    /** 日付関数(julianday) */
    JULIANDAY("julianday", false, Double::class.qualifiedName!!),

    /** 数値関数(ABS) */
    ABS("abs", false) {
        override fun getReturnType(argTypes: List<String>) =
            // 引数の型に基づいて戻り値の型を決定する
            argTypes.reduceOrNull(::widerType) ?: returnType
    },

    /** 数値関数(ROUND) */
    ROUND("round", false, Double::class.qualifiedName!!) {
        /**
         * ## クエリ生成（round 専用）
         * ### 関数クエリを生成する
         * @param arg 引数
         * @return 生成されたクエリ文字列
         * @author Masahiro Inoue
         * @since 2025-09-05
         */
        override fun createQuery(vararg arg: String): String = "$query()"
    },

    /** 数値関数(random) */
    RANDOM("random", false, Long::class.qualifiedName!!),

    /** 判定関数(ifnull) */
    IFNULL("ifnull", false) {
        override fun getReturnType(argTypes: List<String>) =
            // 引数の型に基づいて戻り値の型を決定する
            argTypes.reduceOrNull(::widerType) ?: returnType
    },

    /** 判定関数(coalesce) */
    COALESCE("coalesce", false) {
        override fun getReturnType(argTypes: List<String>) =
            // 引数の型に基づいて戻り値の型を決定する
            argTypes.reduceOrNull(::widerType) ?: returnType
    },

    /** 判定関数(nullif) */
    NULLIF("nullif", false) {
        override fun getReturnType(argTypes: List<String>) =
            // 引数の型に基づいて戻り値の型を決定する
            argTypes.reduceOrNull(::widerType) ?: returnType
    },

    /** カスタム関数 */
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

    /**
     * ## 戻り値の型取得
     * ### 関数の戻り値の型を取得する
     * @param argTypes 引数（必要に応じて型を決定するために使用されることがある）
     * @return 関数の戻り値の型、デフォルトでは Any クラス
     * @author Masahiro Inoue
     * @since 2026-04-30
     */
    open fun getReturnType(argTypes: List<String> = emptyList()) = returnType

    /**
     * ## 型の広い方を決定するメソッド
     * ### 2 つの型を比較し、より広い方の型を返すためのメソッド
     * ### 型の広さの順序を定義し、引数の型に基づいて適切な型を返すロジックを実装する
     * @param type1 比較対象の最初の型を表す TypeName オブジェクト
     * @param type2 比較対象の2番目の型を表す TypeName オブジェクト
     * @return 2 つの型のうち、より広い方の型を表す TypeName オブジェクト
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    fun widerType(type1: String?, type2: String?): String {
        // 型の広さの順序を定義し、引数の型に基づいて適切な型を返すロジックを実装する
        val result = when {
            type1 == type2 -> type1!!
            type1 == String::class.qualifiedName ||
                    type2 == String::class.qualifiedName
                -> String::class.qualifiedName!!

            type1 == Double::class.qualifiedName!! ||
                    type2 == Double::class.qualifiedName!! ||
                    type1 == Float::class.qualifiedName!! ||
                    type2 == Float::class.qualifiedName!!
                -> Double::class.qualifiedName!!

            type1 == Long::class.qualifiedName!! ||
                    type2 == Long::class.qualifiedName!! ||
                    type1 == Int::class.qualifiedName!! ||
                    type2 == Int::class.qualifiedName!!
                -> Long::class.qualifiedName!!

            else -> returnType
        }
        return result
    }

    /**
     * ## sum 関数の戻り値の型を解決するメソッド
     * ### sum 関数の引数の型に基づいて、sum 関数の戻り値の型を決定するためのメソッド
     * ### 引数がすべて INT または LONG の場合は LONG を返し、引数のいずれかが DOUBLE または FLOAT の場合は DOUBLE を返し、そうでない場合は ANY を返すロジックを実装する
     * @param argTypes sum 関数の引数の型を表す TypeName オブジェクトのリスト
     * @return sum 関数の戻り値の型を表す TypeName オブジェクト
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    fun resolveSumReturnType(argTypes: List<String>): String =
        when {
            // 引数がすべて INT または LONG の場合は LONG を返す
            argTypes.all { it == Int::class.qualifiedName!! || it == Long::class.qualifiedName!! }
                -> Long::class.qualifiedName!!
            // 引数のいずれかが DOUBLE または FLOAT の場合は DOUBLE を返す
            argTypes.any { it == Double::class.qualifiedName!! || it == Float::class.qualifiedName!! }
                -> Double::class.qualifiedName!!
            // デフォルトの returnType を返す
            else -> returnType
        }
}