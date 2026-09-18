package jp.pgw.lab78.shared.library.csv

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.StringReader

/**
 * ## CsvTableReader 単体テスト
 * @author Masahiro Inoue
 * @since 2026-09-17
 */
class CsvTableReaderTest {
    /**
     * ## 基本CSV読込テスト
     * ### ヘッダー順を維持してMapへ変換されることを確認する
     */
    @Test
    fun read_withBasicCsv_returnsMapRows() {
        val actual = CsvTableReader.read(
            StringReader(
                """
                    ID,NAME,ACTIVE
                    1,Alice,true
                    2,Bob,false
                """.trimIndent()
            )
        )

        assertEquals(
            listOf(
                linkedMapOf("ID" to "1", "NAME" to "Alice", "ACTIVE" to "true"),
                linkedMapOf("ID" to "2", "NAME" to "Bob", "ACTIVE" to "false"),
            ),
            actual,
        )
        assertEquals(listOf("ID", "NAME", "ACTIVE"), actual.first().keys.toList())
    }

    /**
     * ## RFC 4180相当読込テスト
     * ### BOM、クォート、カンマ、ダブルクォート、改行、空項目を処理できることを確認する
     */
    @Test
    fun read_withQuotedValues_parsesSpecialCharacters() {
        val actual = CsvTableReader.read(
            StringReader(
                "\uFEFFID,NAME,MEMO,EMPTY\r\n" +
                    "1,\"A,B\",\"line1\r\nline2 \"\"quoted\"\"\",\r\n"
            )
        )

        assertEquals(
            listOf(
                linkedMapOf(
                    "ID" to "1",
                    "NAME" to "A,B",
                    "MEMO" to "line1\nline2 \"quoted\"",
                    "EMPTY" to "",
                )
            ),
            actual,
        )
    }

    /**
     * ## 不正列数テスト
     * ### ヘッダーとデータ行の列数が一致しない場合に失敗することを確認する
     */
    @Test
    fun read_withInvalidColumnCount_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            CsvTableReader.read(
                StringReader(
                    """
                        ID,NAME
                        1
                    """.trimIndent()
                )
            )
        }
    }

    /**
     * ## クォート未閉鎖テスト
     * ### ダブルクォートが閉じられていない場合に失敗することを確認する
     */
    @Test
    fun read_withUnclosedQuote_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            CsvTableReader.read(StringReader("ID,NAME\n1,\"Alice"))
        }
    }
}
