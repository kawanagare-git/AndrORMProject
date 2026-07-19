package jp.pgw.lab78.androrm.database.support

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import jp.pgw.lab78.androrm.common.Constants.COMMA
import jp.pgw.lab78.androrm.common.Constants.D_QUOTE
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import java.io.OutputStreamWriter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * ## AndroidTest CSV 出力
 * ### androidTest 実行中の DB 状態を Download 配下へ CSV 出力する
 * @author Masahiro Inoue
 * @since 2026-06-16
 */
object AndroidTestCsvExporter {

    /**
     * ## 全テーブル CSV 出力
     * ### MediaStore 経由で Download/AndrORM/entitiesCsv/stepName 配下へ CSV を出力する
     * @param db SQLiteDatabase
     * @param context Context
     * @param stepName 実行済みテストメソッド名
     * @return 出力先 Uri リスト
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    fun exportAllTablesToDownload(
        db: SQLiteDatabase,
        context: Context,
        stepName: String,
    ): List<Uri> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            error("MediaStore Downloads export requires Android 10 or higher.")
        }

        val tableNames = findUserTableNames(db)
        val outputUris = mutableListOf<Uri>()

        tableNames.forEach { tableName ->
            val uri = createCsvUri(
                context = context,
                stepName = stepName,
                tableName = tableName,
            )

            exportTableToCsv(
                db = db,
                context = context,
                tableName = tableName,
                uri = uri,
            )

            outputUris += uri
        }

        return outputUris
    }

    /**
     * ## ユーザーテーブル名取得
     * ### SQLite 管理テーブルを除外して出力対象テーブル名を取得する
     * @param db SQLiteDatabase
     * @return テーブル名リスト
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun findUserTableNames(
        db: SQLiteDatabase,
    ): List<String> {
        val tableNames = mutableListOf<String>()

        db.rawQuery(
            """
                select name
                from sqlite_master
                where type = 'table'
                  and name not like 'sqlite_%'
                  and name <> 'android_metadata'
                order by name
            """.trimIndent(),
            emptyArray<String>(),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                tableNames += cursor.getString(0)
            }
        }

        return tableNames
    }

    /**
     * ## CSV Uri 作成
     * ### Download/AndrORM/entitiesCsv/stepName 配下に CSV 出力先を作成する
     * @param context Context
     * @param stepName 実行済みテストメソッド名
     * @param tableName テーブル名
     * @return CSV 出力先 Uri
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun createCsvUri(
        context: Context,
        stepName: String,
        tableName: String,
    ): Uri {
        val resolver = context.contentResolver
        val fileName = "$tableName.csv"
        val relativePath = "Download/AndrORM/entitiesCsv/$stepName/"

        resolver.delete(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            "${MediaStore.MediaColumns.DISPLAY_NAME} = ? and ${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
            arrayOf(fileName, relativePath),
        )

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }

        return resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            values,
        ) ?: error("Failed to create CSV file. tableName=$tableName")
    }

    /**
     * ## テーブル CSV 出力
     * ### 指定テーブルの全件を CSV へ出力する
     * @param db SQLiteDatabase
     * @param context Context
     * @param tableName テーブル名
     * @param uri CSV 出力先 Uri
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun exportTableToCsv(
        db: SQLiteDatabase,
        context: Context,
        tableName: String,
        uri: Uri,
    ) {
        db.rawQuery(
            "select * from ${quoteIdentifier(tableName)}",
            emptyArray<String>(),
        ).use { cursor ->
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: error("Failed to open CSV output stream. tableName=$tableName")

            OutputStreamWriter(outputStream, Charsets.UTF_8).buffered().use { writer ->
                writer.appendLine(
                    cursor.columnNames.joinToString(COMMA) { columnName ->
                        escapeCsv(columnName)
                    }
                )

                while (cursor.moveToNext()) {
                    writer.appendLine(
                        createCsvRecord(cursor)
                    )
                }
            }
        }
    }

    /**
     * ## SELECT Entity 結果 CSV 出力
     * ### executeSelectAsEntityList の結果を CSV へ出力する
     * @param context Context
     * @param stepName 実行済みテストメソッド名
     * @param resultName 出力ファイル名。拡張子なし
     * @param rows SELECT 結果
     * @return 出力先 Uri
     * @author Masahiro Inoue
     * @since 2026-07-05
     */
    fun exportSelectEntityResultToDownload(
        context: Context,
        stepName: String,
        resultName: String,
        rows: List<Map<String, SelectEntity?>>,
    ): Uri {
        val uri = createCsvUri(
            context = context,
            stepName = stepName,
            tableName = resultName,
        )

        exportSelectEntityResultToCsv(
            context = context,
            rows = rows,
            uri = uri,
        )

        return uri
    }

    /**
     * ## SELECT Entity 結果 CSV 出力
     * ### alias.propertyName 形式で CSV 化する
     * @param context Context
     * @param rows SELECT 結果
     * @param uri CSV 出力先 Uri
     * @author Masahiro Inoue
     * @since 2026-07-05
     */
    private fun exportSelectEntityResultToCsv(
        context: Context,
        rows: List<Map<String, SelectEntity?>>,
        uri: Uri,
    ) {
        val columns = createSelectEntityCsvColumns(rows)
        val outputStream = context.contentResolver.openOutputStream(uri)
            ?: error("Failed to open SELECT result CSV output stream.")

        OutputStreamWriter(outputStream, Charsets.UTF_8).buffered().use { writer ->
            writer.appendLine(
                columns.joinToString(COMMA) { column ->
                    escapeCsv(column.header)
                }
            )

            rows.forEach { row ->
                writer.appendLine(
                    columns.joinToString(COMMA) { column ->
                        escapeCsv(
                            column.valueToString(row[column.alias])
                        )
                    }
                )
            }
        }
    }

    /**
     * ## SELECT Entity CSV カラム定義生成
     * ### result の alias と Entity プロパティから CSV カラム定義を生成する
     * @param rows SELECT 結果
     * @return CSV カラム定義
     * @author Masahiro Inoue
     * @since 2026-07-05
     */
    private fun createSelectEntityCsvColumns(
        rows: List<Map<String, SelectEntity?>>,
    ): List<SelectEntityCsvColumn> {
        val aliases = rows
            .flatMap { row -> row.keys }
            .distinct()

        return aliases.flatMap { alias ->
            val sampleEntity = rows
                .asSequence()
                .mapNotNull { row -> row[alias] }
                .firstOrNull()

            if (sampleEntity == null) {
                emptyList()
            } else {
                createSelectEntityCsvColumns(
                    alias = alias,
                    sampleEntity = sampleEntity,
                )
            }
        }
    }

    /**
     * ## SELECT Entity CSV カラム定義生成
     * ### Entity の primary constructor 順に CSV カラムを生成する
     * @param alias SELECT 結果 Map の alias
     * @param sampleEntity サンプル Entity
     * @return CSV カラム定義
     * @author Masahiro Inoue
     * @since 2026-07-05
     */
    private fun createSelectEntityCsvColumns(
        alias: String,
        sampleEntity: SelectEntity,
    ): List<SelectEntityCsvColumn> {
        val propertyMap = sampleEntity::class.memberProperties.associateBy { property ->
            property.name
        }

        val propertyNames = sampleEntity::class.primaryConstructor
            ?.parameters
            ?.mapNotNull { parameter -> parameter.name }
            ?: propertyMap.keys.sorted()

        return propertyNames.mapNotNull { propertyName ->
            val property = propertyMap[propertyName] ?: return@mapNotNull null

            @Suppress("UNCHECKED_CAST")
            val typedProperty = property as KProperty1<SelectEntity, *>

            SelectEntityCsvColumn(
                alias = alias,
                propertyName = propertyName,
                valueGetter = { entity ->
                    entity?.let { typedProperty.get(it) }
                },
            )
        }
    }

    /**
     * ## SELECT Entity CSV カラム定義
     * @param alias SELECT 結果 Map の alias
     * @param propertyName Entity プロパティ名
     * @param valueGetter 値取得処理
     * @author Masahiro Inoue
     * @since 2026-07-05
     */
    private data class SelectEntityCsvColumn(
        val alias: String,
        val propertyName: String,
        val valueGetter: (SelectEntity?) -> Any?,
    ) {
        /** CSV ヘッダー */
        val header: String = "$alias.$propertyName"

        /**
         * ## CSV 値文字列化
         * @param entity 対象 Entity
         * @return CSV 出力値
         * @author Masahiro Inoue
         * @since 2026-07-05
         */
        fun valueToString(entity: SelectEntity?): String {
            val value = valueGetter(entity)

            return when (value) {
                null -> ""
                is ByteArray -> value.toHexString()
                else -> value.toString()
            }
        }
    }

    /**
     * ## CSV レコード生成
     * ### Cursor の現在行から CSV 1行分を生成する
     * @param cursor Cursor
     * @return CSV 1行分
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun createCsvRecord(
        cursor: Cursor,
    ): String =
        (0 until cursor.columnCount)
            .joinToString(COMMA) { index ->
                escapeCsv(
                    cursorValueToString(
                        cursor = cursor,
                        index = index,
                    )
                )
            }

    /**
     * ## Cursor 値文字列化
     * ### Cursor の型に応じて CSV 出力用文字列へ変換する
     * @param cursor Cursor
     * @param index カラム位置
     * @return CSV 出力用文字列
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun cursorValueToString(
        cursor: Cursor,
        index: Int,
    ): String {
        val converters: Map<Int, (Cursor, Int) -> String> = mapOf(
            Cursor.FIELD_TYPE_NULL to { _, _ -> "" },
            Cursor.FIELD_TYPE_INTEGER to { source, position ->
                source.getLong(position).toString()
            },
            Cursor.FIELD_TYPE_FLOAT to { source, position ->
                source.getDouble(position).toString()
            },
            Cursor.FIELD_TYPE_STRING to { source, position ->
                source.getString(position).orEmpty()
            },
            Cursor.FIELD_TYPE_BLOB to { source, position ->
                source.getBlob(position).toHexString()
            },
        )

        return converters[cursor.getType(index)]?.invoke(cursor, index)
            ?: cursor.getString(index).orEmpty()
    }

    /**
     * ## BLOB 文字列化
     * ### ByteArray を 16進数文字列へ変換する
     * @receiver ByteArray?
     * @return 16進数文字列
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun ByteArray?.toHexString(): String =
        this
            ?.joinToString("") { byte ->
                "%02X".format(byte.toInt() and 0xFF)
            }
            .orEmpty()

    /**
     * ## CSV エスケープ
     * ### カンマ、ダブルクォート、改行を含む値を CSV 形式にエスケープする
     * @param value 値
     * @return CSV 出力値
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun escapeCsv(
        value: String,
    ): String {
        val requiresQuote =
            value.contains(COMMA) ||
                    value.contains(D_QUOTE) ||
                    value.contains("\n") ||
                    value.contains("\r")

        if (requiresQuote) {
            return D_QUOTE + value.replace(D_QUOTE, D_QUOTE + D_QUOTE) + D_QUOTE
        }

        return value
    }

    /**
     * ## SQL 識別子クォート
     * ### SQLite 用に識別子をダブルクォートで囲む
     * @param identifier 識別子
     * @return クォート済み識別子
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun quoteIdentifier(
        identifier: String,
    ): String =
        D_QUOTE + identifier.replace(D_QUOTE, D_QUOTE + D_QUOTE) + D_QUOTE
}