package jp.pgw.lab78.androrm.common.database.function

import jp.pgw.lab78.androrm.common.dml.interfaces.SqlFunction
import jp.pgw.lab78.androrm.common.dml.interfaces.SqlFunction.ArgType
import jp.pgw.lab78.androrm.common.dml.interfaces.SqlFunction.ArgType.*
import java.util.Locale

/**
 * ## 集約関数 (Aggregate Function)
 * ### GROUP BY 句が必要な関数群を定義する列挙型クラス
 * ### HAVING 句や ORDER 句で使用可能。
 */
enum class AggregateFunction(private val argType: ArgType) : SqlFunction {
    COUNT(SINGLE),
    COUNT_ALL(NONE) {
        /**
         * ## クエリ生成
         * ### 関数クエリを生成する
         * @param arg 引数
         * @return 生成されたクエリ文字列
         * @author Masahiro Inoue
         * @since 2024-10-30
         */
        override fun build(vararg arg: String): String = "${COUNT.getFunctionName()}(*)"
    },
    SUM(SINGLE),
    AVG(SINGLE),
    MAX(SINGLE),
    MIN(SINGLE),
    TOTAL(SINGLE),
    GROUP_CONCAT(MULTI);

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
