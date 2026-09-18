package jp.pgw.lab78.shared.library.csv

import jp.pgw.lab78.shared.library.csv.Constants.BYTE_ORDER_MARK
import jp.pgw.lab78.shared.library.csv.Constants.CARRIAGE_RETURN
import jp.pgw.lab78.shared.library.csv.Constants.COMMA_CHAR
import jp.pgw.lab78.shared.library.csv.Constants.DOUBLE_QUOTE
import jp.pgw.lab78.shared.library.csv.Constants.DOUBLE_QUOTE_CHAR
import jp.pgw.lab78.shared.library.csv.Constants.LINE_FEED
import java.io.Reader

/**
 * ## CSV表読込管理
 * ### RFC 4180相当の基本的なCSVを`List<Map<String, String>>`へ読み込む軽量パーサー
 * ### UTF-8 BOM、ダブルクォート、クォート内のカンマ・改行、`""`によるエスケープ、空項目を扱う
 * ### AndrORM固有機能には依存せず、一般的なKotlin/JVMコードから利用できる
 * @author Masahiro Inoue
 * @since 2026-09-17
 */
object CsvTableReader {
    /**
     * ## CSV読込処理
     * ### 先頭レコードをヘッダーとして、各データレコードを列名と値のMapへ変換する
     * ### Mapのキー順はCSVヘッダー順を維持する
     * @param reader CSV文字列を供給するReader
     * @return ヘッダー名をキーとするCSVデータ行一覧
     * @author Masahiro Inoue
     * @since 2026-09-17
     */
    fun read(reader: Reader): List<Map<String, String>> {
        val records = parseRecords(reader.readText())
            .filterNot { record -> record.size == 1 && record.first().isBlank() }

        require(records.isNotEmpty()) { "CSVが空です。" }

        val header = records.first()
            .mapIndexed { index, value ->
                value.trim().let { field ->
                    if (index == 0) field.trimStart(BYTE_ORDER_MARK) else field
                }
            }

        require(header.none(String::isBlank)) { "CSVヘッダーに空の列名があります。" }
        require(header.distinct().size == header.size) { "CSVヘッダーに重複があります。" }

        return records.drop(1).mapIndexed { index, record ->
            val recordNumber = index + 2
            require(record.size == header.size) {
                "CSV ${recordNumber}レコード目の列数が不正です。" +
                    " expected=${header.size}, actual=${record.size}"
            }

            linkedMapOf<String, String>().apply {
                header.forEachIndexed { columnIndex, columnName ->
                    this[columnName] = record[columnIndex].trim()
                }
            }
        }
    }

    /**
     * ## レコード一覧解析処理
     * ### クォート、エスケープ、改行を考慮して文字列をレコードと項目へ分割する
     * @param text 処理対象となるCSV文字列
     * @return 解析したCSVレコード一覧
     * @author Masahiro Inoue
     * @since 2026-09-17
     */
    private fun parseRecords(text: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        val record = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var index = 0

        /** 現在の文字列バッファを1項目として処理中レコードへ追加する。 */
        fun finishField() {
            record += field.toString()
            field.setLength(0)
        }

        /** 現在項目を確定し、処理中レコードを解析結果へ追加する。 */
        fun finishRecord() {
            finishField()
            records += record.toList()
            record.clear()
        }

        while (index < text.length) {
            val char = text[index]
            when {
                char == DOUBLE_QUOTE_CHAR && inQuotes && index + 1 < text.length && text[index + 1] == DOUBLE_QUOTE_CHAR -> {
                    field.append(DOUBLE_QUOTE)
                    index += 2
                }

                char == DOUBLE_QUOTE_CHAR -> {
                    inQuotes = !inQuotes
                    index++
                }

                char == COMMA_CHAR && !inQuotes -> {
                    finishField()
                    index++
                }
                (char == CARRIAGE_RETURN || char == LINE_FEED) && !inQuotes -> {
                    if (char == CARRIAGE_RETURN && index + 1 < text.length && text[index + 1] == LINE_FEED) {
                        index++
                    }
                    finishRecord()
                    index++
                }
                char == CARRIAGE_RETURN -> {
                    if (index + 1 < text.length && text[index + 1] == LINE_FEED) {
                        index++
                    }
                    field.append(LINE_FEED)
                    index++
                }
                else -> {
                    field.append(char)
                    index++
                }
            }
        }
        require(!inQuotes) { "CSVのダブルクォートが閉じられていません。" }
        if (field.isNotEmpty() || record.isNotEmpty()) {
            finishRecord()
        }
        return records
    }
}
