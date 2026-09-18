package jp.pgw.lab78.shared.library.csv

import jp.pgw.lab78.shared.library.csv.Constants.CARRIAGE_RETURN
import jp.pgw.lab78.shared.library.csv.Constants.COMMA
import jp.pgw.lab78.shared.library.csv.Constants.DOUBLE_QUOTE
import jp.pgw.lab78.shared.library.csv.Constants.LINE_FEED
import java.io.Writer

/**
 * ## CSV表出力管理
 * ### `List<Map<String, String>>`をRFC 4180相当のCSVへ出力する軽量ライター
 * ### AndrORM固有機能には依存せず、一般的なKotlin/JVMコードから利用できる
 * @author Masahiro Inoue
 * @since 2026-09-17
 */
object CsvTableWriter {
    /**
     * ## CSV出力処理
     * ### 先頭Mapのキー順をヘッダー順としてCSVを出力する
     * ### 行が0件の場合は何も出力しない
     * @param writer CSV出力先Writer
     * @param rows CSV出力するデータ行一覧
     * @author Masahiro Inoue
     * @since 2026-09-17
     */
    fun write(
        writer: Writer,
        rows: List<Map<String, String>>,
    ) {
        if (rows.isEmpty()) {
            return
        }

        write(
            writer = writer,
            header = rows.first().keys.toList(),
            rows = rows,
        )
    }

    /**
     * ## ヘッダー指定CSV出力処理
     * ### 指定したヘッダー順でCSVを出力する
     * ### データ行が0件でもヘッダーを出力できる
     * @param writer CSV出力先Writer
     * @param header CSVヘッダー。出力順を兼ねる
     * @param rows CSV出力するデータ行一覧
     * @author Masahiro Inoue
     * @since 2026-09-17
     */
    fun write(
        writer: Writer,
        header: List<String>,
        rows: List<Map<String, String>>,
    ) {
        require(header.none(String::isBlank)) { "CSVヘッダーに空の列名があります。" }
        require(header.distinct().size == header.size) { "CSVヘッダーに重複があります。" }

        if (header.isEmpty()) {
            require(rows.isEmpty()) { "CSVヘッダーが空のためデータ行を出力できません。" }
            return
        }

        writer.appendLine(
            header.joinToString(COMMA) { columnName ->
                escape(columnName)
            }
        )

        rows.forEachIndexed { index, row ->
            val recordNumber = index + 2
            require(row.keys.size == header.size && header.all(row::containsKey)) {
                "CSV ${recordNumber}レコード目の列構成がヘッダーと一致しません。"
            }

            writer.appendLine(
                header.joinToString(COMMA) { columnName ->
                    escape(row.getValue(columnName))
                }
            )
        }
    }

    /**
     * ## CSVエスケープ処理
     * ### カンマ、ダブルクォート、改行を含む値をCSV形式へエスケープする
     * @param value CSVへ出力する値
     * @return CSV形式へエスケープした値
     * @author Masahiro Inoue
     * @since 2026-09-17
     */
    private fun escape(value: String): String {
        val requiresQuote =
            value.contains(COMMA) ||
                value.contains(DOUBLE_QUOTE) ||
                value.contains(LINE_FEED) ||
                value.contains(CARRIAGE_RETURN)

        if (!requiresQuote) {
            return value
        }

        return DOUBLE_QUOTE +
            value.replace(DOUBLE_QUOTE, DOUBLE_QUOTE + DOUBLE_QUOTE) +
            DOUBLE_QUOTE
    }
}
