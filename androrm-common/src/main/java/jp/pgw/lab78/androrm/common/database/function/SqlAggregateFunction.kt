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
enum class SqlAggregateFunction(override val argType: ArgType) : SqlFunction {
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
        override fun build(vararg arg: String): String =
            "${COUNT.getFunctionName()}(*)"
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