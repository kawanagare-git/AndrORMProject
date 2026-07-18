package jp.pgw.lab78.androrm.common.database.validation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * ## SQL既定値検証テスト
 * ### ColumnとMigrationDefaultで共有する型別規則を要求仕様から検証する
 * @author Masahiro Inoue
 * @since 2026-07-18
 */
class SqlDefaultValueValidatorTest {

    companion object {
        /** 妥当な型別SQL既定値 */
        @JvmStatic
        fun validValues(): Stream<Arguments> = Stream.of(
            Arguments.of(SqlDefaultValueType.INT, false, "-1"),
            Arguments.of(SqlDefaultValueType.LONG, false, "1"),
            Arguments.of(SqlDefaultValueType.FLOAT, false, "1.5"),
            Arguments.of(SqlDefaultValueType.DOUBLE, false, "-1.5"),
            Arguments.of(SqlDefaultValueType.BOOLEAN, false, "0"),
            Arguments.of(SqlDefaultValueType.BOOLEAN, false, "1"),
            Arguments.of(SqlDefaultValueType.STRING, false, "'it''s ready'"),
            Arguments.of(SqlDefaultValueType.LOCAL_DATE, false, "'2026-07-18'"),
            Arguments.of(SqlDefaultValueType.LOCAL_DATE, false, "CURRENT_DATE"),
            Arguments.of(SqlDefaultValueType.LOCAL_TIME, false, "'12:34:56'"),
            Arguments.of(SqlDefaultValueType.LOCAL_TIME, false, "CURRENT_TIME"),
            Arguments.of(SqlDefaultValueType.LOCAL_DATE_TIME, false, "'2026-07-18T12:34:56'"),
            Arguments.of(SqlDefaultValueType.LOCAL_DATE_TIME, false, "'2026-07-18 12:34:56'"),
            Arguments.of(SqlDefaultValueType.LOCAL_DATE_TIME, false, "CURRENT_TIMESTAMP"),
            Arguments.of(SqlDefaultValueType.LOCAL_DATE_TIME, false, "CURRENT_TIMESTAMP_ISO"),
            Arguments.of(SqlDefaultValueType.BYTE_ARRAY, true, "NULL"),
        )

        /** 不正な型別SQL既定値 */
        @JvmStatic
        fun invalidValues(): Stream<Arguments> = Stream.of(
            Arguments.of(SqlDefaultValueType.INT, false, "'1'"),
            Arguments.of(SqlDefaultValueType.LONG, false, "1.5"),
            Arguments.of(SqlDefaultValueType.FLOAT, false, "number"),
            Arguments.of(SqlDefaultValueType.DOUBLE, false, "'1.5'"),
            Arguments.of(SqlDefaultValueType.BOOLEAN, false, "true"),
            Arguments.of(SqlDefaultValueType.BOOLEAN, false, "2"),
            Arguments.of(SqlDefaultValueType.STRING, false, "unquoted"),
            Arguments.of(SqlDefaultValueType.LOCAL_DATE, false, "'2026-02-30'"),
            Arguments.of(SqlDefaultValueType.LOCAL_TIME, false, "'25:00:00'"),
            Arguments.of(SqlDefaultValueType.LOCAL_DATE_TIME, false, "'2026-07-18'"),
            Arguments.of(SqlDefaultValueType.BYTE_ARRAY, false, "X'00'"),
            Arguments.of(SqlDefaultValueType.BYTE_ARRAY, false, "NULL"),
        )
    }

    /** 妥当な値を許可することを検証する */
    @ParameterizedTest
    @MethodSource("validValues")
    fun testIsValid_withValidValue_returnsTrue(
        type: SqlDefaultValueType,
        nullable: Boolean,
        value: String,
    ) {
        assertTrue(SqlDefaultValueValidator.isValid(type, nullable, value, allowBlank = false))
    }

    /** 不正な値を拒否することを検証する */
    @ParameterizedTest
    @MethodSource("invalidValues")
    fun testIsValid_withInvalidValue_returnsFalse(
        type: SqlDefaultValueType,
        nullable: Boolean,
        value: String,
    ) {
        assertFalse(SqlDefaultValueValidator.isValid(type, nullable, value, allowBlank = false))
    }

    /** Columnだけが未指定値を許可できることを検証する */
    @org.junit.jupiter.api.Test
    fun testIsValid_withBlankValue_respectsAllowBlank() {
        assertTrue(
            SqlDefaultValueValidator.isValid(
                SqlDefaultValueType.INT, false, "", allowBlank = true
            )
        )
        assertFalse(
            SqlDefaultValueValidator.isValid(
                SqlDefaultValueType.INT, false, "", allowBlank = false
            )
        )
    }

    /** AndrORM固有日時予約値をSQLite式へ変換することを検証する */
    @org.junit.jupiter.api.Test
    fun testNormalize_withCurrentTimestampIso_returnsSqliteExpression() {
        assertEquals(
            "(strftime('%Y-%m-%dT%H:%M:%f', 'now', 'localtime'))",
            SqlDefaultValueValidator.normalize("CURRENT_TIMESTAMP_ISO"),
        )
    }
}
