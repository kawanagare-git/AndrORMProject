package jp.pgw.lab78.androrm.common.database.function

import jp.pgw.lab78.androrm.common.dml.interfaces.SqlFunction
import jp.pgw.lab78.androrm.common.dml.interfaces.SqlFunction.ArgumentArity
import jp.pgw.lab78.androrm.common.dml.interfaces.SqlFunction.ArgumentArity.*
import java.util.Locale

/**
 * ## 関数列挙型
 * ### 列に使用する関数定義
 * @param argumentArity 引数の持ち方
 * @param returnType 戻り値型
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
enum class ColumnFunction(
    override val argumentArity: ArgumentArity,
    val returnType: String = Any::class.qualifiedName!!
) : SqlFunction<ColumnFunction> {
    /** 集約関数(count) */
    COUNT(SINGLE, Long::class.qualifiedName!!) {
        override fun build(vararg args: String): String {
            require(args.isNotEmpty()) { "Aggregate functions require a column argument." }
            return super.build(*args)
        }
    },

    /** 集約関数(count:全行)  */
    COUNT_ALL(NONE, Long::class.qualifiedName!!) {
        override val functionName: String
            get() = COUNT.name.lowercase()

        override fun build(vararg args: String) = "$functionName(*)"
    },

    /** 集約関数(sum) */
    SUM(SINGLE) {
        override fun getReturnType(argTypes: List<String>) = resolveSumReturnType(argTypes)
        override fun build(vararg args: String): String {
            require(args.isNotEmpty()) { "Aggregate functions require a column argument." }
            return super.build(*args)
        }
    },

    /** 集約関数(avg) */
    AVG(SINGLE, Double::class.qualifiedName!!) {
        override fun getReturnType(argTypes: List<String>) = returnType
        override fun build(vararg args: String): String {
            require(args.isNotEmpty()) { "Aggregate functions require a column argument." }
            return super.build(*args)
        }
    },

    /** 集約関数(max) */
    MAX(SINGLE) {
        override fun getReturnType(argTypes: List<String>) =
            // 引数の型に基づいて戻り値の型を決定する
            argTypes.reduceOrNull(::widerType) ?: returnType

        override fun build(vararg args: String): String {
            require(args.isNotEmpty()) { "Aggregate functions require a column argument." }
            return super.build(*args)
        }
    },

    /** 集約関数(min) */
    MIN(SINGLE) {
        override fun getReturnType(argTypes: List<String>) =
            // 引数の型に基づいて戻り値の型を決定する
            argTypes.reduceOrNull(::widerType) ?: returnType

        override fun build(vararg args: String): String {
            require(args.isNotEmpty()) { "Aggregate functions require a column argument." }
            return super.build(*args)
        }
    },

    /** 集約関数(total) */
    TOTAL(SINGLE, Double::class.qualifiedName!!) {
        override fun build(vararg args: String): String {
            require(args.isNotEmpty()) { "Aggregate functions require a column argument." }
            return super.build(*args)
        }
    },

    /** 集約関数(group_concat) */
    GROUP_CONCAT(MULTI, String::class.qualifiedName!!) {
        override fun build(vararg args: String): String {
            require(args.size in 1..2) {
                "Invalid number of arguments. Expected 1 or 2 arguments."
            }
            return "${GROUP_CONCAT.functionName}(${args.joinToString(",")})"
        }
    },

    /** スカラー関数(max) */
    SCALAR_MAX(MULTI) {
        override val functionName: String
            get() = MAX.functionName

        override fun build(vararg args: String): String =
            args.joinToString(",", "${functionName}(", ")")
    },

    /** スカラー関数(min) */
    SCALAR_MIN(MULTI) {
        override val functionName: String
            get() = MIN.functionName

        override fun build(vararg args: String): String =
            args.joinToString(",", "${functionName}(", ")")
    },

    /** 文字列関数(length) */
    LENGTH(SINGLE, Int::class.qualifiedName!!) {
        override fun build(vararg args: String): String {
            require(args.size == 1) {
                "This function requires at least one argument."
            }
            return super.build(args.first())
        }
    },

    /** 文字列関数(lower) */
    LOWER(SINGLE, String::class.qualifiedName!!) {
        override fun build(vararg args: String): String {
            require(args.size == 1) {
                "This function requires at least one argument."
            }
            return super.build(args.first())
        }
    },

    /** 文字列関数(upper) */
    UPPER(SINGLE, String::class.qualifiedName!!) {
        override fun build(vararg args: String): String {
            require(args.size == 1) {
                "This function requires at least one argument."
            }
            return super.build(args.first())
        }
    },

    /** 文字列関数(replace) */
    REPLACE(MULTI, String::class.qualifiedName!!) {
        override fun build(vararg args: String): String {
            require(args.size == 3) {
                "Invalid number of arguments. Expected 3 arguments."
            }
            return "$functionName(${args[0]},${args[1]},${args[2]})"
        }
    },

    /** 文字列関数(substr) */
    SUBSTR(MULTI, String::class.qualifiedName!!),

    /** 文字列関数(concat) */
    CONCAT(MULTI, String::class.qualifiedName!!) {
        override fun build(vararg args: String): String {
            require(args.size >= 2) {
                "Invalid number of arguments. Expected 2 or more arguments."
            }
            return args.joinToString(" || ")
        }
    },

    /** 文字列関数(trim) */
    TRIM(MULTI, String::class.qualifiedName!!),

    /** 文字列関数(ltrim) */
    LTRIM(MULTI, String::class.qualifiedName!!),

    /** 文字列関数(rtrim) */
    RTRIM(MULTI, String::class.qualifiedName!!),

    /** 日付関数(date) */
    DATE(MULTI, String::class.qualifiedName!!),

    /** 日付関数(time) */
    TIME(MULTI, String::class.qualifiedName!!),

    /** 日付関数(datetime) */
    DATETIME(MULTI, String::class.qualifiedName!!),

    /** 日付関数(strftime) */
    STRFTIME(MULTI, String::class.qualifiedName!!),

    /** 日付関数(julianday) */
    JULIANDAY(MULTI, Double::class.qualifiedName!!),

    /** 数値関数(ABS) */
    ABS(MULTI) {
        override fun getReturnType(argTypes: List<String>) =
            // 引数の型に基づいて戻り値の型を決定する
            argTypes.reduceOrNull(::widerType) ?: returnType

        override fun build(vararg args: String): String {
            require(args.size == 1) {
                "This function requires at least one argument."
            }
            return super.build(args.first())
        }
    },

    /** 数値関数(random) */
    RANDOM(NONE, Long::class.qualifiedName!!),

    /** 数値関数(ROUND) */
    ROUND(MULTI, Double::class.qualifiedName!!) {
        override fun build(vararg args: String): String {
            require(args.size in 1..2) {
                "Invalid number of arguments. Expected 1 or 2 arguments."
            }
            return super.build(*args)
        }
    },

    /** 判定関数(ifnull) */
    IFNULL(MULTI) {
        override fun getReturnType(argTypes: List<String>) =
            // 引数の型に基づいて戻り値の型を決定する
            argTypes.reduceOrNull(::widerType) ?: returnType

        override fun build(vararg args: String): String {
            require(args.size == 2) {
                "Invalid number of arguments. Expected 2 arguments."
            }
            return "$functionName(${args[0]},${args[1]})"
        }
    },

    /** 判定関数(coalesce) */
    COALESCE(MULTI) {
        override fun getReturnType(argTypes: List<String>) =
            // 引数の型に基づいて戻り値の型を決定する
            argTypes.reduceOrNull(::widerType) ?: returnType

        override fun build(vararg args: String): String {
            require(args.size >= 2) {
                "Invalid number of arguments. Expected 2 or more arguments."
            }
            return "$functionName(${args.joinToString(",")})"
        }
    },

    /** 判定関数(nullif) */
    NULLIF(MULTI) {
        override fun getReturnType(argTypes: List<String>) =
            // 引数の型に基づいて戻り値の型を決定する
            argTypes.reduceOrNull(::widerType) ?: returnType
    },

    /** 型変換関数 */
    CAST(MULTI) {
        override fun build(vararg args: String): String {
            require(args.size == 2) {
                "Invalid number of arguments. Expected 2 arguments."
            }
            return "$functionName(${args[0]} as ${args[1]})"
        }
    },

    /** カスタム関数 */
    CUSTOM(SPECIAL) {
        override val functionName: String
            get() = ""

        override fun build(vararg args: String): String = run {
            // カスタム関数は少なくとも1つの引数が必要：空なら例外 IllegalArgumentException() をスロー
            require(args.isNotEmpty()) { "CUSTOM requires at least one argument." }
            args.first()
        }
    }, ;

    /** 関数名 */
    override val functionName: String
        get() = name.lowercase(Locale.ROOT)

    /**
     * ## 戻り値の型取得
     * ### 関数の戻り値の型を取得する
     * @param argTypes 引数（必要に応じて型を決定するために使用されることがある）
     * @return 関数の戻り値の型、デフォルトでは Any クラス
     * @author Masahiro Inoue
     * @since 2026-04-30
     */
    override fun getReturnType(argTypes: List<String>): String = returnType

    /**
     * ## 関数文字列生成
     * ### 関数クエリを生成する
     * @param args 引数
     * @return 関数名文字列
     * @author Masahiro Inoue
     * @since 2024-10-30
     */
    override fun build(vararg args: String): String =
        when (this.argumentArity) {
            NONE -> "$functionName()"
            SINGLE -> "$functionName(${args.first()})"
            MULTI -> "$functionName(${args.joinToString(",")})"
            else -> args.joinToString(",")
        }

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
    fun widerType(type1: String?, type2: String?): String =
        // 型の広さの順序を定義し、引数の型に基づいて適切な型を返すロジックを実装する
        when {
            type1 == type2 -> type1!!
            type1 == String::class.qualifiedName
                    || type2 == String::class.qualifiedName
                -> String::class.qualifiedName!!

            type1 == Double::class.qualifiedName!!
                    || type2 == Double::class.qualifiedName!!
                    || type1 == Float::class.qualifiedName!!
                    || type2 == Float::class.qualifiedName!!
                -> Double::class.qualifiedName!!

            type1 == Long::class.qualifiedName!!
                    || type2 == Long::class.qualifiedName!!
                    || type1 == Int::class.qualifiedName!!
                    || type2 == Int::class.qualifiedName!!
                -> Long::class.qualifiedName!!

            else -> returnType
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