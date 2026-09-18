package jp.pgw.lab78.androrm.common.database.columns_controller

import jp.pgw.lab78.androrm.common.database.columns.controller.SqlValueType
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * ## SQL値型テスト
 * ### Kotlin型の識別とSQLite向け値変換を要求仕様から検証する
 * @author Masahiro Inoue
 * @since 2026-09-03
 */
class SqlValueTypeTest {
    /**
     * ## ByteArray型識別検証
     * ### ByteArrayの完全修飾型名をBLOB型として識別することを検証する
     * @author Masahiro Inoue
     * @since 2026-09-03
     */
    @Test
    fun fromQualifiedName_withByteArray_returnsByteArrayType() {
        val qualifiedName = requireNotNull(ByteArray::class.qualifiedName)

        assertEquals(SqlValueType.BYTE_ARRAY, SqlValueType.fromQualifiedName(qualifiedName))
    }

    /**
     * ## 未対応型識別検証
     * ### SQL内部制御用のAnyを通常の対応型として返さないことを検証する
     * @author Masahiro Inoue
     * @since 2026-09-03
     */
    @Test
    fun fromQualifiedName_withAny_returnsNull() {
        val qualifiedName = requireNotNull(Any::class.qualifiedName)

        assertNull(SqlValueType.fromQualifiedName(qualifiedName))
    }

    /**
     * ## ByteArray変換検証
     * ### BLOBへバインドするByteArrayの内容を変更せず返すことを検証する
     * @author Masahiro Inoue
     * @since 2026-09-03
     */
    @Test
    fun toColumnValue_withByteArray_preservesBinaryData() {
        val binary = byteArrayOf(0x00, 0x7F, 0x80.toByte(), 0xFF.toByte())

        val actual = SqlValueType.BYTE_ARRAY.toColumnValue<ByteArray>(binary)

        assertArrayEquals(binary, actual)
    }

    /**
     * ## ByteArray変換失敗検証
     * ### ByteArray以外の値を空配列へ置換せず拒否することを検証する
     * @author Masahiro Inoue
     * @since 2026-09-04
     */
    @Test
    fun toColumnValue_withNonByteArray_throwsIllegalArgumentException() {
        assertThrows<IllegalArgumentException> {
            SqlValueType.BYTE_ARRAY.toColumnValue<ByteArray>("X'00'")
        }
    }

    /** 大文字・小文字のBLOBリテラルと空BLOBを同じ形式として受理する。 */
    @ParameterizedTest
    @ValueSource(strings = ["X'01AF'", "X'01af'", "x'01AF'", "x'01af'", "x''"])
    fun byteArray_matchesCaseInsensitiveBlobLiteral(candidate: String) {
        assertTrue(SqlValueType.BYTE_ARRAY.matches(candidate))
    }

    /** 小文字BLOBリテラルをByteArrayへ変換する。 */
    @Test
    fun byteArray_toEntity_convertsLowercaseLiteral() {
        val actual = SqlValueType.BYTE_ARRAY.toEntity("x'00ff'") as ByteArray

        assertArrayEquals(byteArrayOf(0x00, 0xFF.toByte()), actual)
    }
}
