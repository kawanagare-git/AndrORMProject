package jp.pgw.lab78.androrm.common.database.function

import jp.pgw.lab78.androrm.common.database.function.SqlAggregateFunction.*
import jp.pgw.lab78.androrm.common.dml.interfaces.SqlFunction
import jp.pgw.lab78.androrm.common.dml.interfaces.SqlFunction.ArgType
import jp.pgw.lab78.androrm.common.dml.interfaces.SqlFunction.ArgType.*
import java.util.Locale

/**
 * ## スカラー関数 (Scalar Function)
 * GROUP BY が不要な関数。
 * WHERE 句や ORDER 句で使用可能。
 */
enum class SqlScalarFunction(override val argType: ArgType) : SqlFunction {
    ABS(SINGLE),
    LENGTH(SINGLE),
    LOWER(SINGLE),
    UPPER(SINGLE),
    REPLACE(MULTI),
    ROUND(SINGLE) {
        /**
         * ## クエリ生成（round 専用）
         * ### 関数クエリを生成する
         * @param arg 引数
         * @return 生成されたクエリ文字列
         * @author Masahiro Inoue
         * @since 2024-10-30
         */
        override fun build(vararg arg: String): String = run {
            require(arg.isNotEmpty()) {
                "ROUND requires at least one argument. The second argument (precision) is optional." +
                        " Additional arguments are ignored."
            }
            if (arg.size == 1) {
                "${getFunctionName()}(${arg[0]})"
            } else {
                "${getFunctionName()}(${arg[0]}, ${arg[1]})"
            }
        }
    },
    COALESCE(MULTI),
    IFNULL(MULTI),
    CAST(SPECIAL) {
        /**
         * ## クエリ生成（cast 専用）
         * ### 関数クエリを生成する
         * @param arg 引数
         * @return 生成されたクエリ文字列
         * @author Masahiro Inoue
         * @since 2024-10-30
         */
        override fun build(vararg arg: String): String {
            require(arg.size >= 2) { "CAST requires at least two arguments: expression and type. Additional arguments are ignored." }
            return "${getFunctionName()}(${arg[0]} AS ${arg[1]})"
        }
    },
    CONCAT(MULTI),
    SUBSTR(MULTI),
    TRIM(MULTI),
    LTRIM(MULTI),
    RTRIM(MULTI),
    DATE(MULTI),
    TIME(MULTI),
    DATETIME(MULTI),
    STRFTIME(MULTI),
    JULIANDAY(MULTI),
    RANDOM(NONE),
    SCALAR_MAX(SINGLE) {
        /**
         * ## 関数名取得
         * @return 関数名文字列
         * @author Masahiro Inoue
         * @since 2024-10-30
         */
        override fun getFunctionName(): String = MAX.getFunctionName()

        /**
         * ## クエリ生成（random 専用）
         * ### 関数クエリを生成する
         * @param arg 引数
         * @return 生成されたクエリ文字列
         * @author Masahiro Inoue
         * @since 2024-10-30
         */
        override fun build(vararg arg: String): String = MAX.build(arg.first())
    },
    SCALAR_MIN(SINGLE) {
        /**
         * ## 関数名取得
         * @return 関数名文字列
         * @author Masahiro Inoue
         * @since 2024-10-30
         */
        override fun getFunctionName(): String = MIN.getFunctionName()

        /**
         * ## クエリ生成（random 専用）
         * ### 関数クエリを生成する
         * @param arg 引数
         * @return 生成されたクエリ文字列
         * @author Masahiro Inoue
         * @since 2024-10-30
         */
        override fun build(vararg arg: String): String = MIN.build(arg.first())
    },
    CUSTOM(SPECIAL) {
        /**
         * ## 関数名取得
         * @return 関数名文字列
         * @author Masahiro Inoue
         * @since 2024-10-30
         */
        override fun getFunctionName(): String = ""

        /**
         * ## クエリ生成（custom 専用）
         * ### 関数クエリを生成する
         * @param arg 引数
         * @return 生成されたクエリ文字列
         * @author Masahiro Inoue
         * @since 2025-10-30
         */
        override fun build(vararg arg: String): String = run {
            // カスタム関数は少なくとも1つの引数が必要：空なら例外 IllegalArgumentException() をスロー
            require(arg.isNotEmpty()) { "CUSTOM requires at least one argument." }
            arg.first()
        }

    }, ;

    /**
     * ## 関数名取得
     * @return 関数名文字列
     * @author Masahiro Inoue
     * @since 2024-10-30
     */
    override fun getFunctionName(): String = name.lowercase(Locale.getDefault())

    /**
     * ## 関数生成
     * ### 関数クエリを生成する
     * @param arg 引数
     * @return 関数名文字列
     * @author Masahiro Inoue
     * @since 2024-10-30
     */
    override fun build(vararg arg: String): String = buildDefault(argType, *arg)
}
