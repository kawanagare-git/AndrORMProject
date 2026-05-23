package jp.pgw.lab78.androrm.utility

import jp.pgw.lab78.androrm.common.MessageConstants.AE00009
import jp.pgw.lab78.androrm.common.database.SupportFunction.toSnakeCase
import jp.pgw.lab78.androrm.database.utility.EntityManager.mapKotlinTypeToSqlType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvFileSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.reflect.full.createType

class EntityManagerTest {

    @ParameterizedTest(name = "No{index} (testData,expected) -> ({arguments})")
    @CsvFileSource(resources = ["/TestToSnakeCaseData.csv"], numLinesToSkip = 1)
    @DisplayName("toSnakeCase 関数のテスト")
            /**
             * toSnakeCase テストメソッド
             * @param testData CSV ファイルから取り込んだテストデータ
             * @param expected CSV ファイルから取り込んだ期待値
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
             */
    fun mapKotlinTypeToSqlTypeTest(testData: Any, castType: String, expected: String) {
        val actual = castToType(testData.toString(), castType)
        assertEquals(expected, mapKotlinTypeToSqlType(actual))
        assertEquals(expected, mapKotlinTypeToSqlType(actual::class.createType()))
    }

    /**
     * ParameterizedTest 用動的キャスト関数
     * @param value キャスト対象の値
     * @param targetType キャストタイプ
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

}
