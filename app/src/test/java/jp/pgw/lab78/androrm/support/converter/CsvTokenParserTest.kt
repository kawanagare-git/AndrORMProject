package jp.pgw.lab78.androrm.support.converter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * ## CsvTokenParser テスト
 * ### CSV 用テストデータ内の List / 2次元 List / 特殊値変換を検証する
 */
class CsvTokenParserTest {

    @Test
    @DisplayName("parseList は ':' 区切りの文字列を List に変換できる")
    fun parseList_basic() {
        val actual = CsvTokenParser.parseList("A:B:C")

        assertEquals(
            listOf("A", "B", "C"),
            actual,
        )
    }

    @Test
    @DisplayName("parseList は '\\:' を値としての ':' に変換できる")
    fun parseList_escapeColon() {
        val actual = CsvTokenParser.parseList("""12\:30:http\://example.com:jp\:pgw\:lab78""")

        assertEquals(
            listOf("12:30", "http://example.com", "jp:pgw:lab78"),
            actual,
        )
    }

    @Test
    @DisplayName("parseList は '\\^' を値としての '^' に変換できる")
    fun parseList_escapeRowDelimiter() {
        val actual = CsvTokenParser.parseList("""A\^B:C""")

        assertEquals(
            listOf("A^B", "C"),
            actual,
        )
    }

    @Test
    @DisplayName("parseList は '\\\\' を値としての '\\' に変換できる")
    fun parseList_escapeBackslash() {
        val actual = CsvTokenParser.parseList("""C\\work:C\\temp""")

        assertEquals(
            listOf("""C\work""", """C\temp"""),
            actual,
        )
    }

    @Test
    @DisplayName("parse2dList は '^' で行、':' で列を分割できる")
    fun parse2dList_basic() {
        val actual = CsvTokenParser.parse2dList("A:B^C:D")

        assertEquals(
            listOf(
                listOf("A", "B"),
                listOf("C", "D"),
            ),
            actual,
        )
    }

    @Test
    @DisplayName("parse2dList はエスケープされた ':' と '^' を値として扱える")
    fun parse2dList_escapeDelimiter() {
        val actual = CsvTokenParser.parse2dList("""A\:1:B^C:D\^E""")

        assertEquals(
            listOf(
                listOf("A:1", "B"),
                listOf("C", "D^E")
            ),
            actual,
        )
    }

    @Test
    @DisplayName("toStringValue は '<null>' を null に変換できる")
    fun toStringValue_nullToken() {
        val actual = CsvTokenParser.toStringValue("<null>")

        assertEquals(null, actual)
    }

    @Test
    @DisplayName("toStringValue は '<empty>' を空文字に変換できる")
    fun toStringValue_emptyToken() {
        val actual = CsvTokenParser.toStringValue("<empty>")

        assertEquals("", actual)
    }

    @Test
    @DisplayName("toAnyValue は整数文字列を Int に変換できる")
    fun toAnyValue_int() {
        val actual = CsvTokenParser.toAnyValue("123")

        assertEquals(123, actual)
    }

    @Test
    @DisplayName("toAnyValue は小数文字列を Double に変換できる")
    fun toAnyValue_double() {
        val actual = CsvTokenParser.toAnyValue("12.34")

        assertEquals(12.34, actual)
    }

    @Test
    @DisplayName("toAnyValue は true / false を Boolean に変換できる")
    fun toAnyValue_boolean() {
        assertEquals(true, CsvTokenParser.toAnyValue("true"))
        assertEquals(false, CsvTokenParser.toAnyValue("false"))
    }

    @Test
    @DisplayName("toAnyValue は '<null>' を null に変換できる")
    fun toAnyValue_nullToken() {
        val actual = CsvTokenParser.toAnyValue("<null>")

        assertEquals(null, actual)
    }

    @Test
    @DisplayName("toAnyValue は '<empty>' を空文字に変換できる")
    fun toAnyValue_emptyToken() {
        val actual = CsvTokenParser.toAnyValue("<empty>")

        assertEquals("", actual)
    }

    @Test
    @DisplayName("parseList の終端文字を'\\'を設定 List に変換できる")
    fun parseList_LastSlash() {
        val actual = CsvTokenParser.parseList("""A:B:C:\""")

        assertEquals(listOf("A", "B", "C", "\\"), actual)
    }

    @Test
    @DisplayName("parse2dList は行分割時に列区切り用エスケープを温存できる")
    fun parse2dList_keepListDelimiterEscapeWhenSplittingRows() {
        val actual = CsvTokenParser.parse2dList("""A\:1:B^C:D""")

        assertEquals(
            listOf(
                listOf("A:1", "B"),
                listOf("C", "D"),
            ),
            actual,
        )
    }

    @Test
    @DisplayName("parse2dList は '\\\\' を値としての '\\' に変換できる")
    fun parse2dList_escapeBackslash() {
        val actual = CsvTokenParser.parse2dList("""C\\work:D^E:F\\temp""")

        assertEquals(
            listOf(
                listOf("""C\work""", "D"),
                listOf("E", """F\temp"""),
            ),
            actual,
        )
    }
}