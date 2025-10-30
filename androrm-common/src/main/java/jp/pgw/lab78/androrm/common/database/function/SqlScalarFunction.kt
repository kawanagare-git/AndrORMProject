package jp.pgw.lab78.androrm.common.database.function

import jp.pgw.lab78.androrm.common.database.function.AggregateFunction.*
import jp.pgw.lab78.androrm.common.dml.interfaces.SqlFunction
import jp.pgw.lab78.androrm.common.dml.interfaces.SqlFunction.ArgType.*
import java.util.Locale

/**
 * ## スカラー関数 (Scalar Function)
 * GROUP BY が不要な関数。
 * WHERE 句や ORDER 句で使用可能。
 */
enum class ScalarFunction(private val argType: SqlFunction.ArgType) : SqlFunction {
    ABS(MULTI),
    LENGTH(SINGLE),
    LOWER(SINGLE),
    UPPER(SINGLE),
    REPLACE(MULTI),
    ROUND(MULTI),
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
        override fun build(vararg arg: String): String =
            "${this.getFunctionName()}(${arg.first()} AS ${arg[1]})"
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
        override fun build(vararg arg: String): String = arg.first()
    }, ;

    /**
     * ## 関数名取得
     * @return 関数名文字列
     * @author Masahiro Inoue
     * @since 2024-10-30
     */
    override fun getFunctionName(): String = this.name.lowercase(Locale.getDefault())

    /**
     * ## クエリ生成
     * ### 関数クエリを生成する
     * @param arg 引数
     * @return 生成されたクエリ文字列
     * @author Masahiro Inoue
     * @since 2024-10-30
     */
    override fun build(vararg arg: String): String =
        when (argType) {
            NONE -> "${this.getFunctionName()}()"
            SINGLE -> "${this.getFunctionName()}(${arg.first()})"
            SPECIAL -> throw IllegalArgumentException("SPECIAL argType requires custom build implementation.")
            MULTI -> {
                if (arg.isEmpty()) {
                    throw IllegalArgumentException("At least one argument is required for MULTI argType.")
                }
                "${this.getFunctionName()}(${arg.joinToString(",")})"
            }
        }
}
