package jp.pgw.lab78.androrm.common

/**
 * ## 定数オブジェクト
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
object Constants {
    /** 空文字列 */
    const val EMPTY_STRING = ""

    /** 不明 */
    const val UNKNOWN = "Unknown"

    /** 空白 */
    const val SPACE = " "

    /** カンマ */
    const val COMMA = ","

    /**
     * ## 論理演算子
     * @param query クエリ文字列
     */
    enum class LogicalOperator(val query: String) {
        AND(" and "),
        OR(" or "),
    }
}