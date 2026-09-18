package jp.pgw.lab78.shared.library.csv

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.StringWriter

/**
 * ## CsvTableWriter 単体テスト
 * @author Masahiro Inoue
 * @since 2026-09-17
 */
class CsvTableWriterTest {
    /**
     * ## Map一覧CSV出力テスト
     * ### 先頭Mapのキー順をヘッダーとしてCSV出力されることを確認する
     */
    @Test
    fun write_withMapRows_writesCsv() {
        val writer = StringWriter()
        val rows = listOf(
            linkedMapOf("ID" to "1", "NAME" to "Alice"),
            linkedMapOf("ID" to "2", "NAME" to "Bob"),
        )

        CsvTableWriter.write(writer, rows)

        assertEquals(
            "ID,NAME\n" +
                "1,Alice\n" +
                "2,Bob\n",
            writer.toString(),
        )
    }

    /**
     * ## CSVエスケープテスト
     * ### カンマ、ダブルクォート、改行を含む値がクォートされることを確認する
     */
    @Test
    fun write_withSpecialCharacters_escapesValues() {
        val writer = StringWriter()
        val rows = listOf(
            linkedMapOf(
                "ID" to "1",
                "NAME" to "A,B",
                "MEMO" to "line1\nline2 \"quoted\"",
            )
        )

        CsvTableWriter.write(writer, rows)

        assertEquals(
            "ID,NAME,MEMO\n" +
                "1,\"A,B\",\"line1\nline2 \"\"quoted\"\"\"\n",
            writer.toString(),
        )
    }

    /**
     * ## ヘッダーのみCSV出力テスト
     * ### データ行が0件でも指定ヘッダーを出力できることを確認する
     */
    @Test
    fun write_withHeaderAndNoRows_writesHeaderOnly() {
        val writer = StringWriter()

        CsvTableWriter.write(
            writer = writer,
            header = listOf("ID", "NAME"),
            rows = emptyList(),
        )

        assertEquals("ID,NAME\n", writer.toString())
    }

    /**
     * ## 列構成不一致テスト
     * ### Mapの列構成がヘッダーと一致しない場合に失敗することを確認する
     */
    @Test
    fun write_withMismatchedColumns_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            CsvTableWriter.write(
                writer = StringWriter(),
                header = listOf("ID", "NAME"),
                rows = listOf(linkedMapOf("ID" to "1")),
            )
        }
    }
}
