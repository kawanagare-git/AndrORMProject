package jp.pgw.lab78.androrm.database.validation

import android.database.Cursor
import android.database.sqlite.SQLiteQuery
import jp.pgw.lab78.androrm.common.MessageConstants.AE00009
import jp.pgw.lab78.androrm.common.MessageConstants.AE00030
import jp.pgw.lab78.androrm.common.MessageConstants.AE00044
import jp.pgw.lab78.androrm.common.MessageConstants.AE00045
import jp.pgw.lab78.androrm.common.database.columns.controller.SqlValueType
import jp.pgw.lab78.androrm.common.database.columns.interfaces.ColumnControllerModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * ## SQL既定値の対応型
 * ### AndrORMがDEFAULT値として検証できるKotlin型を表す
 * @author Masahiro Inoue
 * @since 2026-07-18
 */
enum class ValueFromCursor(
    val reference: SqlValueType,
    val cursorType: Int,
    val columnType: String
) :
    ColumnControllerModel {
    /** Int / INTEGER */
    INT(SqlValueType.INT, Cursor.FIELD_TYPE_INTEGER, "INTEGER") {
        override fun getValueFromCursor(cursor: Cursor, columnIndex: Int) =
            run {
                typeChecker(cursor, columnIndex)
                cursor.getInt(columnIndex)
            }

        override fun <Long> toColumnValue(value: Any): Long = SqlValueType.INT.toColumnValue(value)
        override fun setBind(query: SQLiteQuery, index: Int, value: Any) {
            query.bindLong(index, reference.toColumnValue(value))
        }
    },

    /** Long / INTEGER */
    LONG(SqlValueType.LONG, Cursor.FIELD_TYPE_INTEGER, "INTEGER") {
        override fun getValueFromCursor(cursor: Cursor, columnIndex: Int) =
            run {
                typeChecker(cursor, columnIndex)
                cursor.getLong(columnIndex)
            }

        override fun <Long> toColumnValue(value: Any): Long = SqlValueType.LONG.toColumnValue(value)
        override fun setBind(query: SQLiteQuery, index: Int, value: Any) {
            query.bindLong(index, reference.toColumnValue(value))
        }
    },

    /** Float / REAL */
    FLOAT(SqlValueType.FLOAT, Cursor.FIELD_TYPE_FLOAT, "REAL") {
        override fun getValueFromCursor(cursor: Cursor, columnIndex: Int) =
            run {
                typeChecker(cursor, columnIndex)
                cursor.getFloat(columnIndex)
            }

        override fun <Float> toColumnValue(value: Any): Float =
            SqlValueType.FLOAT.toColumnValue(value)

        override fun setBind(query: SQLiteQuery, index: Int, value: Any) {
            query.bindDouble(index, reference.toColumnValue(value))
        }
    },

    /** Double / REAL */
    DOUBLE(SqlValueType.DOUBLE, Cursor.FIELD_TYPE_FLOAT, "REAL") {
        override fun getValueFromCursor(cursor: Cursor, columnIndex: Int) =
            run {
                typeChecker(cursor, columnIndex)
                cursor.getDouble(columnIndex)
            }

        override fun <Double> toColumnValue(value: Any): Double =
            SqlValueType.DOUBLE.toColumnValue(value)

        override fun setBind(query: SQLiteQuery, index: Int, value: Any) {
            query.bindDouble(index, reference.toColumnValue(value))
        }
    },

    /** Boolean / INTEGER */
    BOOLEAN(SqlValueType.BOOLEAN, Cursor.FIELD_TYPE_INTEGER, "INTEGER") {
        override fun getValueFromCursor(cursor: Cursor, columnIndex: Int) =
            run {
                typeChecker(cursor, columnIndex)
                val parseValue = cursor.getInt(columnIndex)
                require(parseValue in 0..1) {
                    AE00045.format(cursor.getColumnName(columnIndex), parseValue)
                }
                // true / false に変換
                parseValue == 1
            }

        override fun <Long> toColumnValue(value: Any): Long =
            SqlValueType.BOOLEAN.toColumnValue(value)

        override fun setBind(query: SQLiteQuery, index: Int, value: Any) {
            query.bindLong(index, reference.toColumnValue(value))
        }
    },

    /** String / TEXT */
    STRING(SqlValueType.STRING, Cursor.FIELD_TYPE_STRING, "STRING") {
        override fun getValueFromCursor(cursor: Cursor, columnIndex: Int): Any =
            run {
                typeChecker(cursor, columnIndex)
                cursor.getString(columnIndex)
            }

        override fun <String> toColumnValue(value: Any): String =
            SqlValueType.STRING.toColumnValue(value)
    },

    /** LocalDate / DATETIME */
    LOCAL_DATE(SqlValueType.LOCAL_DATE, Cursor.FIELD_TYPE_STRING, "STRING") {
        override fun getValueFromCursor(cursor: Cursor, columnIndex: Int) =
            run {
                typeChecker(cursor, columnIndex)
                LocalDate.parse(cursor.getString(columnIndex))!!
            }

        override fun <String> toColumnValue(value: Any): String =
            SqlValueType.LOCAL_DATE.toColumnValue(value)
    },

    /** LocalTime / DATETIME */
    LOCAL_TIME(SqlValueType.LOCAL_TIME, Cursor.FIELD_TYPE_STRING, "STRING") {
        override fun getValueFromCursor(cursor: Cursor, columnIndex: Int) =
            run {
                typeChecker(cursor, columnIndex)
                LocalTime.parse(cursor.getString(columnIndex))!!
            }

        override fun <String> toColumnValue(value: Any): String =
            SqlValueType.LOCAL_TIME.toColumnValue(value)
    },

    /** LocalDateTime / DATETIME */
    LOCAL_DATE_TIME(SqlValueType.LOCAL_DATE_TIME, Cursor.FIELD_TYPE_STRING, "STRING") {
        override fun getValueFromCursor(cursor: Cursor, columnIndex: Int) =
            run {
                typeChecker(cursor, columnIndex)
                LocalDateTime.parse(cursor.getString(columnIndex))!!
            }

        override fun <LocalDateTime> toColumnValue(value: Any): LocalDateTime =
            SqlValueType.LOCAL_DATE_TIME.toColumnValue(value)
    },

    /** ByteArray / BLOB */
    BYTE_ARRAY(SqlValueType.BYTE_ARRAY, Cursor.FIELD_TYPE_BLOB, "BLOB") {
        override fun getValueFromCursor(cursor: Cursor, columnIndex: Int) =
            run {
                typeChecker(cursor, columnIndex)
                requireNotNull(cursor.getBlob(columnIndex)) { AE00030.format(ByteArray::class.qualifiedName) }
            }

        override fun <ByteArray> toColumnValue(value: Any): ByteArray =
            SqlValueType.BYTE_ARRAY.toColumnValue(value)

        override fun setBind(query: SQLiteQuery, index: Int, value: Any) {
            query.bindBlob(index, reference.toColumnValue(value))
        }
    },
    ;

    companion object {
        /**
         * ## 完全修飾型名から対応型を取得
         * @param value KotlinまたはJava上の完全修飾型名
         * @return 対応型。未対応型の場合は null
         * @author Masahiro Inoue
         * @since 2026-07-18
         */
        fun identifyType(value: Any): ValueFromCursor =
            requireNotNull(ValueFromCursor.entries.firstOrNull {
                it.reference.valueType.type?.isInstance(value) == true
            }) {
                AE00009.format(value::class.qualifiedName)
            }

        /**
         * ## カラム型識別子からカラム型を取得
         * @param value カラム識別子（Cursor.FIELD_TYPE_STRINGなど）
         * @return カラム型名
         * @author Masahiro Inoue
         * @since 2026-07-18
         */
        fun pickColumnType(value: Int) =
            requireNotNull(ValueFromCursor.entries.firstOrNull { it.cursorType == value }) {
                AE00009.format("Cursor.FIELD_TYPE_? $value")
            }.columnType
    }

    /**
     * ## カラムデータ取得
     * ### カーソルを使用して値を取得する
     * @param cursor データ読み出し用のカーソル
     * @param columnIndex カラムの順番
     * @return 読み出した値
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    abstract fun getValueFromCursor(cursor: Cursor, columnIndex: Int): Any

    /**
     * ## バインド変数設定
     * ### クエリと値を受け取り、バインド値として組み込む
     * @param query バインド変数を組み込むクエリ
     * @param index カラムの番号
     * @param value バインド値
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    open fun setBind(query: SQLiteQuery, index: Int, value: Any) {
        query.bindString(index, reference.toColumnValue(value))
    }

    /**
     * ## データベース型判定
     * ### データベースから取得した型と、転送先の型を比較する
     * @param cursor カーソル情報
     * @param index 型判定する番号
     */
    fun typeChecker(cursor: Cursor, index: Int) {
        val type = cursor.getType(index)
        if (type != cursorType) {
            throw ColumnTypeMismatchException(
                AE00044.format(
                    cursor.getColumnName(index),
                    pickColumnType(type),
                    pickColumnType(cursorType),
                )
            )
        }
    }
}

class ColumnTypeMismatchException(message: String) : ClassCastException(message)