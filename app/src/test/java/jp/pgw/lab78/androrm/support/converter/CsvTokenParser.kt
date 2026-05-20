package jp.pgw.lab78.androrm.support.converter

/**
 * ## CSV トークン解析ユーティリティ
 * ### @CsvSource 内で List / 2次元 List を表現するための簡易パーサ
 * @author Masahiro Inoue
 * @since 2026-05-20
 */
object CsvTokenParser {

    /** リスト・配列要素の区切り文字 */
    private const val LIST_DELIMITER = ':'

    /** リスト・配列の行の区切り文字 */
    private const val ROW_DELIMITER = '^'

    /** エスケープ文字 */
    private const val ESCAPE_CHAR = '\\'

    /** null 表現 */
    private const val NULL_TOKEN = "<null>"

    /** 空要素表現 */
    private const val EMPTY_TOKEN = "<empty>"

    /**
     * ## 1次元 List 解析
     * ### ":" 区切り。ただし "\:" は値としての ":" として扱う
     * @param source 解析対象文字列
     * @return 解析結果のリスト
     * @author Masahiro Inoue
     * @since 2026-05-20
     */
    fun parseList(source: String): List<String> =
        splitEscaped(source, LIST_DELIMITER)

    /**
     * ## 2次元 List 解析
     * ### "^" で行分割、":" で列分割。ただし "\^" / "\:" は値として扱う
     * @param source 解析対象文字列
     * @return 解析結果のリスト（2次元）
     * @author Masahiro Inoue
     * @since 2026-05-20
     */
    fun parse2dList(source: String): List<List<String>> =
        splitEscaped(source, ROW_DELIMITER, mutableSetOf(LIST_DELIMITER, ESCAPE_CHAR))
            .map { row -> splitEscaped(row, LIST_DELIMITER) }

    /**
     * ## String トークン変換
     * ### トークンを文字列か null に変換
     * @param token 変換対象文字列
     * @return 文字列から変換した表現
     * @author Masahiro Inoue
     * @since 2026-05-20
     */
    fun toStringValue(token: String): String? =
        when (token.trim()) {
            NULL_TOKEN -> null
            EMPTY_TOKEN -> ""
            else -> token
        }

    /**
     * ## Any トークン変換
     * ### bindValues 向けに String / Int / Long / Double / Boolean / null を推定変換する
     * @param token 変換対象文字列
     * @return 変換した値
     * @author Masahiro Inoue
     * @since 2026-05-20
     */
    fun toAnyValue(token: String): Any? {
        val value = token.trim()
        // token の値から、適切な値に予測変換する
        return when {
            value == NULL_TOKEN -> null
            value == EMPTY_TOKEN -> ""
            value.equals("true", ignoreCase = true) -> true
            value.equals("false", ignoreCase = true) -> false
            value.matches(Regex("""-?\d+""")) -> value.toIntOrNull() ?: value.toLong()
            value.matches(Regex("""-?\d+\.\d+""")) -> value.toDouble()
            else -> token
        }
    }

    /**
     * ## エスケープ対応 split
     * ### delimiter の直前に "\" がある場合は区切りではなく値として扱う
     * @param source 加工元文字列
     * @param delimiter 区切り文字
     * @param preserveEscapedDelimiters 後段解析用にエスケープ状態を温存する区切り文字
     * @return 区切り後のリスト
     * @author Masahiro Inoue
     * @since 2026-05-20
     */
    @Suppress("MoveVariableDeclarationIntoWhen")
    private fun splitEscaped(
        source: String,
        delimiter: Char,
        preserveEscapedDelimiters: Set<Char> = emptySet(),
    ): List<String> {
        val result = mutableListOf<String>()
        // 文字列生成用インスタンス
        val buffer = StringBuilder()
        var skipNextChar = false
        // 文字列長に達するまで繰り返し
        for (index in source.indices) {
            if (skipNextChar) {
                skipNextChar = false
                continue
            }
            // 文字の取得
            val char = source[index]
            // 現在の文字がエスケープ文字
            if (char == ESCAPE_CHAR) {
                // 次の文字列が取得できるか判定
                if (index + 1 < source.length) {
                    // 次の文字を取得
                    val nextChar = source[index + 1]
                    // 次の文字を判定
                    when (nextChar) {
                        // 後段解析用にエスケープ状態を温存する区切り文字
                        in preserveEscapedDelimiters -> {
                            buffer.append(char)
                            buffer.append(nextChar)
                        }

                        else -> {
                            // 次の文字は値として扱う
                            buffer.append(nextChar)
                        }
                    }
                    skipNextChar = true
                } else {
                    // 現在の文字は終端なので、無条件に文字列領域へ設定
                    buffer.append(char)
                }
            } else {
                // 区切り文字化判定
                if (char == delimiter) {
                    // 文字列を結果リストに設定
                    result += buffer.toString()
                    // 文字列領域をクリア
                    buffer.clear()
                } else {
                    // 現在の delimiter とは無関係なので文字列領域へ設定
                    buffer.append(char)
                }
            }
        }
        result += buffer.toString()
        return result
    }
}