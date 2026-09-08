package jp.pgw.lab78.androrm.database.validation

import android.database.Cursor
import android.database.sqlite.SQLiteQuery
import jp.pgw.lab78.androrm.common.MessageConstants.AE00009
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.stream.Stream

/**
 * ## SELECT値制御テスト
 * ### Kotlin型ごとのSQLiteQueryバインドとCursor復元を要求仕様から検証する
 * @author Masahiro Inoue
 * @since 2026-09-03
 */
class ValueFromCursorTest {
    /** SELECTバインド値のパラメータ化テストデータを提供する。 */
    companion object {
        /**
         * ## INTEGERバインドケース生成
         * @return bindLongで検証する値と期待値
         * @author Masahiro Inoue
         * @since 2026-09-03
         */
        @JvmStatic
        fun longBindCases(): Stream<Arguments> = Stream.of(
            Arguments.of(10, 10L),
            Arguments.of(20L, 20L),
            Arguments.of(true, 1L),
            Arguments.of(false, 0L),
        )

        /**
         * ## REALバインドケース生成
         * @return bindDoubleで検証する値と期待値
         * @author Masahiro Inoue
         * @since 2026-09-03
         */
        @JvmStatic
        fun doubleBindCases(): Stream<Arguments> = Stream.of(
            Arguments.of(1.25F, 1.25),
            Arguments.of(2.5, 2.5),
        )

        /**
         * ## TEXTバインドケース生成
         * @return bindStringで検証する値と期待値
         * @author Masahiro Inoue
         * @since 2026-09-03
         */
        @JvmStatic
        fun stringBindCases(): Stream<Arguments> = Stream.of(
            Arguments.of("text", "text"),
            Arguments.of(LocalDate.of(2026, 9, 3), "2026-09-03"),
            Arguments.of(LocalTime.of(12, 34, 56), "12:34:56"),
            Arguments.of(
                LocalDateTime.of(2026, 9, 3, 12, 34, 56),
                "2026-09-03T12:34:56",
            ),
        )
    }

    /**
     * ## INTEGER型バインド検証
     * ### Int、Long、BooleanをbindLongへ変換することを検証する
     * @param value バインド値
     * @param expected 期待するINTEGER値
     * @author Masahiro Inoue
     * @since 2026-09-03
     */
    @ParameterizedTest
    @MethodSource("longBindCases")
    fun setBind_withIntegerStorageValue_usesBindLong(value: Any, expected: Long) {
        val query = Mockito.mock(SQLiteQuery::class.java)

        ValueFromCursor.identifyType(value).setBind(query, 1, value)

        Mockito.verify(query).bindLong(1, expected)
    }

    /**
     * ## REAL型バインド検証
     * ### FloatとDoubleをbindDoubleへ変換することを検証する
     * @param value バインド値
     * @param expected 期待するREAL値
     * @author Masahiro Inoue
     * @since 2026-09-03
     */
    @ParameterizedTest
    @MethodSource("doubleBindCases")
    fun setBind_withRealStorageValue_usesBindDouble(value: Any, expected: Double) {
        val query = Mockito.mock(SQLiteQuery::class.java)

        ValueFromCursor.identifyType(value).setBind(query, 1, value)

        Mockito.verify(query).bindDouble(1, expected)
    }

    /**
     * ## TEXT型バインド検証
     * ### Stringと日時型をbindStringへ変換することを検証する
     * @param value バインド値
     * @param expected 期待するTEXT値
     * @author Masahiro Inoue
     * @since 2026-09-03
     */
    @ParameterizedTest
    @MethodSource("stringBindCases")
    fun setBind_withTextStorageValue_usesBindString(value: Any, expected: String) {
        val query = Mockito.mock(SQLiteQuery::class.java)

        ValueFromCursor.identifyType(value).setBind(query, 1, value)

        Mockito.verify(query).bindString(1, expected)
    }

    /**
     * ## BLOB型バインド検証
     * ### ByteArrayを内容を変更せずbindBlobへ渡すことを検証する
     * @author Masahiro Inoue
     * @since 2026-09-03
     */
    @Test
    fun setBind_withByteArray_usesBindBlob() {
        val query = Mockito.mock(SQLiteQuery::class.java)
        val binary = byteArrayOf(0x00, 0x7F, 0x80.toByte(), 0xFF.toByte())

        ValueFromCursor.identifyType(binary).setBind(query, 2, binary)

        Mockito.verify(query).bindBlob(2, binary)
    }

    /**
     * ## バインド順検証
     * ### INTEGER、BLOB、TEXTをSQL出現順のindexへ設定することを検証する
     * @author Masahiro Inoue
     * @since 2026-09-03
     */
    @Test
    fun setBind_withMixedValues_preservesSqlOrder() {
        val query = Mockito.mock(SQLiteQuery::class.java)
        val binary = byteArrayOf(1, 2, 3)

        ValueFromCursor.identifyType(10L).setBind(query, 1, 10L)
        ValueFromCursor.identifyType(binary).setBind(query, 2, binary)
        ValueFromCursor.identifyType("last").setBind(query, 3, "last")

        val inOrder = Mockito.inOrder(query)
        inOrder.verify(query).bindLong(1, 10L)
        inOrder.verify(query).bindBlob(2, binary)
        inOrder.verify(query).bindString(3, "last")
    }

    /**
     * ## BLOB型取得検証
     * ### CursorのBLOB値をByteArrayとして取得することを検証する
     * @author Masahiro Inoue
     * @since 2026-09-03
     */
    @Test
    fun getValueFromCursor_withBlob_returnsByteArray() {
        val cursor = Mockito.mock(Cursor::class.java)
        val binary = byteArrayOf(1, 2, 3)
        Mockito.`when`(cursor.getColumnName(0)).thenReturn("BLOB_COLUMN")
        Mockito.`when`(cursor.getType(0)).thenReturn(Cursor.FIELD_TYPE_BLOB)
        Mockito.`when`(cursor.getBlob(0)).thenReturn(binary)

        val actual = ValueFromCursor.BYTE_ARRAY.getValueFromCursor(cursor, 0)

        assertArrayEquals(binary, actual as ByteArray)
    }
    /**
     * ## Boolean型取得検証
     * ### SQLite INTEGERの0以外をtrueとして復元することを検証する
     * @author Masahiro Inoue
     * @since 2026-09-03
     */
    @Test
    fun getValueFromCursor_withNonZeroInteger_throwsIllegalArgumentException() {
        val cursor = Mockito.mock(Cursor::class.java)
        Mockito.`when`(cursor.getColumnName(0)).thenReturn("BOOLEAN_COLUMN")
        Mockito.`when`(cursor.getType(0)).thenReturn(Cursor.FIELD_TYPE_INTEGER)
        Mockito.`when`(cursor.getInt(0)).thenReturn(2)

        assertThrows<IllegalArgumentException> {
            ValueFromCursor.BOOLEAN.getValueFromCursor(cursor, 0)
        }
    }

    /**
     * ## 未対応型検証
     * ### 未対応のバインド値をAE00009で拒否することを検証する
     * @author Masahiro Inoue
     * @since 2026-09-03
     */
    @Test
    fun identifyType_withUnsupportedType_throwsIllegalArgumentException() {
        val actual = assertThrows<IllegalArgumentException> {
            ValueFromCursor.identifyType(BigDecimal.ONE)
        }

        assertEquals(AE00009.format(BigDecimal::class.qualifiedName), actual.message)
    }
}
