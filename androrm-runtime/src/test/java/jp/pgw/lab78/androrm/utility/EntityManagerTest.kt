package jp.pgw.lab78.androrm.utility

import jp.pgw.lab78.androrm.common.MessageConstants.AE00009
import jp.pgw.lab78.androrm.common.database.SupportFunction.toSnakeCase
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.database.utility.EntityManager.mapKotlinTypeToSqlType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvFileSource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.reflect.full.createType

/**
 * EntityManagerの動作を検証するテストクラス。
 * @author Masahiro Inoue
 * @since 2025-01-25
 */
class EntityManagerTest {

    @ParameterizedTest(name = "No{index} (testData,expected) -> ({arguments})")
    @CsvFileSource(resources = ["/TestToSnakeCaseData.csv"], numLinesToSkip = 1)
    @DisplayName("toSnakeCase 関数のテスト")
            /**
             * mapKotlinTypeToSqlType テストメソッド
             * @param testData CSV ファイルから取り込んだテストデータ
             * @param expected CSV ファイルから取り込んだ期待値
              * @author Masahiro Inoue
              * @since 2025-01-25
             */
    fun toSnakeCaseTest(testData: String, expected: String) {
        assertEquals(expected, testData.toSnakeCase())
    }

    @ParameterizedTest(name = "No{index} (testData,castType,expected) -> ({arguments})")
    @CsvFileSource(resources = ["/TestMapKotlinTypeToSqlTypeData.csv"], numLinesToSkip = 1)
    @DisplayName("mapKotlinTypeToSqlTypeTest 関数のテスト")
            /**
             * toSnakeCase テストメソッド
             * @param testData CSV ファイルから取り込んだテストデータ
             * @param castType CSV ファイルから取り込んだテストデータのキャストタイプ
             * @param expected CSV ファイルから取り込んだ期待値
              * @author Masahiro Inoue
              * @since 2025-01-25
             */
    fun mapKotlinTypeToSqlTypeTest(testData: Any, castType: String, expected: String) {
        val actual = castToType(testData.toString(), castType)
        assertEquals(expected, mapKotlinTypeToSqlType(actual))
        assertEquals(expected, mapKotlinTypeToSqlType(actual::class.createType()))
    }

    /**
     * ## 未対応値のSQLite型変換テスト
     * ### Mapに登録されていない値の型を意図した検証例外で拒否することを確認する
     * @author Masahiro Inoue
     * @since 2026-07-19
     */
    @Test
    fun mapKotlinTypeToSqlType_withUnsupportedValue_throwsIllegalArgumentException() {
        val actual = assertThrows<IllegalArgumentException> {
            mapKotlinTypeToSqlType(BigDecimal.ONE)
        }

        assertEquals(AE00009.format(BigDecimal::class), actual.message)
    }

    /**
     * ## 未対応KTypeのSQLite型変換テスト
     * ### Mapに登録されていないKTypeを意図した検証例外で拒否することを確認する
     * @author Masahiro Inoue
     * @since 2026-07-19
     */
    @Test
    fun mapKotlinTypeToSqlType_withUnsupportedKType_throwsIllegalArgumentException() {
        val actual = assertThrows<IllegalArgumentException> {
            mapKotlinTypeToSqlType(BigDecimal::class.createType())
        }

        assertEquals(AE00009.format(BigDecimal::class), actual.message)
    }

    /**
     * ParameterizedTest 用動的キャスト関数
     * @param value キャスト対象の値
     * @param targetType キャストタイプ
     * @return 指定された型へ変換した値
     * @author Masahiro Inoue
     * @since 2025-01-25
     */
    private fun castToType(value: String, targetType: String): Any {
        return when (targetType) {
            "Int::class" -> value.toInt()
            "Long::class" -> value.toLong()
            "Float::class" -> value.toFloat()
            "Double::class" -> value.toDouble()
            "Boolean::class" -> value.toBoolean()
            "String::class" -> value
            "LocalDate::class" -> LocalDate.parse(value)
            "LocalTime::class" -> LocalTime.parse(value)
            "LocalDateTime::class" -> LocalDateTime.parse(value)
            else -> require(false) { AE00009.format(targetType) }
        }
    }

    /**
     * Entityメタ情報またはSQL生成の検証に使用するテスト用TestDuplicateColumnAliasEntity。
     * @author Masahiro Inoue
     * @since 2025-01-25
     */
    @Table(name = "TEST_DUPLICATE_COLUMN_ALIAS_ENTITY", alias = "TDCAE")
    private data class TestDuplicateColumnAliasEntity(
        @Column(name = "DUPLICATE_COLUMN")
        val first: String,
        @Column(name = "DUPLICATE_COLUMN")
        val second: String,
    ) : SelectEntity

}
