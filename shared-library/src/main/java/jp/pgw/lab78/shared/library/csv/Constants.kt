package jp.pgw.lab78.shared.library.csv

object Constants {
    /** UTF-8テキスト先頭に含まれ得るBOM。 */
    internal const val BYTE_ORDER_MARK = '\uFEFF'

    /** CSV区切り文字。 */
    internal const val COMMA = ","
    internal val COMMA_CHAR = COMMA.first()

    /** CSVクォート文字。 */
    internal const val DOUBLE_QUOTE = "\""
    internal val DOUBLE_QUOTE_CHAR = DOUBLE_QUOTE.first()

    /** キャリッジリターン。 */
    internal const val CARRIAGE_RETURN = '\r'

    /** ラインフィード。 */
    internal const val LINE_FEED = '\n'
}