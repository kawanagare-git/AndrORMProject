package jp.pgw.lab78.androrm.database.validation

import android.database.Cursor
import android.database.sqlite.SQLiteQuery
import jp.pgw.lab78.androrm.common.database.columns_controller.SqlValueType
import jp.pgw.lab78.androrm.common.database.columns_controller.interfaces.ColumnControllerModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * ## SQL既定値の対応型
 * ### AndrORMがDEFAULT値として検証できるKotlin型を表す
 * @author Masahiro Inoue
 * @since 2026-07-18
 */
enum class ValueFromCursor(val reference: SqlValueType) :ColumnControllerModel{
    /** Int / number */
    INT(SqlValueType.INT) {
        override fun getValueFromCursor(cursor: Cursor, columnIndex: Int) =
            cursor.getInt(columnIndex)
        override fun <Long> toColumnValue(value: Any):Long=SqlValueType.INT.toColumnValue(value)
    },

    /** Long / INTEGER */
    LONG(SqlValueType.LONG) {
        override fun getValueFromCursor(cursor: Cursor, columnIndex: Int) =
            cursor.getLong(columnIndex)
        override fun <Long> toColumnValue(value: Any):Long=SqlValueType.LONG.toColumnValue(value)
        override fun setBind(query: SQLiteQuery, index: Int, value: Any) {
            query.bindDouble(index,SqlValueType.LONG.toColumnValue( value))
        }
    },

    /** Float / REAL */
    FLOAT(SqlValueType.FLOAT) {
        override fun getValueFromCursor(cursor: Cursor, columnIndex: Int) =
            cursor.getFloat(columnIndex)
        override fun <Float> toColumnValue(value: Any):Float=SqlValueType.FLOAT.toColumnValue(value)
        override fun setBind(query: SQLiteQuery, index: Int, value: Any) {
            query.bindDouble(index,SqlValueType.FLOAT.toColumnValue( value))
        }
    },

    /** Double / REAL */
    DOUBLE(SqlValueType.DOUBLE) {
        override fun getValueFromCursor(cursor: Cursor, columnIndex: Int) =
            cursor.getDouble(columnIndex)
        override fun <Double> toColumnValue(value: Any):Double=SqlValueType.DOUBLE.toColumnValue(value)
    },

    /** Boolean / INTEGER */
    BOOLEAN(SqlValueType.BOOLEAN) {
        override fun getValueFromCursor(cursor: Cursor, columnIndex: Int) =
            cursor.getInt(columnIndex) == 1
        override fun <Long> toColumnValue(value: Any):Long=SqlValueType.BOOLEAN.toColumnValue(value)
    },

    /** String / TEXT */
    STRING(SqlValueType.STRING) {
        override fun getValueFromCursor(cursor: Cursor, columnIndex: Int): Any =
            cursor.getString(columnIndex)
        override fun <String> toColumnValue(value: Any):String=SqlValueType.STRING.toColumnValue(value)
    },

    /** LocalDate / DATETIME */
    LOCAL_DATE(SqlValueType.LOCAL_DATE) {
        override fun getValueFromCursor(cursor: Cursor, columnIndex: Int) =
            LocalDate.parse(cursor.getString(columnIndex))!!
        override fun <String> toColumnValue(value: Any):String=SqlValueType.LOCAL_DATE.toColumnValue(value)
    },

    /** LocalTime / DATETIME */
    LOCAL_TIME(SqlValueType.LOCAL_TIME) {
        override fun getValueFromCursor(cursor: Cursor, columnIndex: Int) =
            LocalTime.parse(cursor.getString(columnIndex))!!
        override fun <String> toColumnValue(value: Any):String=SqlValueType.LOCAL_TIME.toColumnValue(value)
    },

    /** LocalDateTime / DATETIME */
    LOCAL_DATE_TIME(SqlValueType.LOCAL_DATE_TIME) {
        override fun getValueFromCursor(cursor: Cursor, columnIndex: Int) =
            LocalDateTime.parse(cursor.getString(columnIndex))!!
        override fun <LocalDateTime> toColumnValue(value: Any):LocalDateTime=SqlValueType.LOCAL_DATE_TIME.toColumnValue(value)
    },

    /** ByteArray / BLOB */
    BYTE_ARRAY(SqlValueType.BYTE_ARRAY) {
        override fun getValueFromCursor(cursor: Cursor, columnIndex: Int) =
            cursor.getBlob(columnIndex) ?: "null"
        override fun <ByteArray> toColumnValue(value: Any): ByteArray =SqlValueType.BYTE_ARRAY.toColumnValue(value)
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
            ValueFromCursor.entries.first { it.reference.type.isInstance(value) }
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

    open fun setBind(query: SQLiteQuery, index: Int, value: Any) {
        query.bindDouble(index,reference.toColumnValue( value))
    }
}

