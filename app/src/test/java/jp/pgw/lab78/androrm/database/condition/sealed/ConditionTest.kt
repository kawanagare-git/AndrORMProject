package jp.pgw.lab78.androrm.database.condition.sealed

import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator
import jp.pgw.lab78.androrm.database.entities.select.EmployeeEntity
import jp.pgw.lab78.androrm.database.support.SupportOperation.changeColumnRef
import jp.pgw.lab78.androrm.database.support.SupportOperation.changeProperty
import jp.pgw.lab78.androrm.database.utility.EntityManager.toColumnString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * ## Condition テスト
 * ### Condition#build が期待する SQL 断片を生成することを確認する
 */
class ConditionTest {

    @ParameterizedTest
    @CsvSource(
        "'EQ','EMP.EMPLOYEE_ID = ?'",
        "'NE','EMP.EMPLOYEE_ID <> ?'",
        "'GT','EMP.EMPLOYEE_ID > ?'",
        "'GE','EMP.EMPLOYEE_ID >= ?'",
        "'LT','EMP.EMPLOYEE_ID < ?'",
        "'LE','EMP.EMPLOYEE_ID <= ?'",
        "'LIKE','EMP.EMPLOYEE_ID like ?'",
        "'NOT_LIKE','EMP.EMPLOYEE_ID not like ?'",
        "'GLOB','EMP.EMPLOYEE_ID glob ?'",
        "'NOT_GLOB','EMP.EMPLOYEE_ID not glob ?'"
    )
    fun value_shouldBuildExpectedSql(
        operator: ComparisonOperator,
        expected: String,
    ) {
        val condition = Compare.Value(
            lhsProperty = EmployeeEntity::employeeId.toColumnString(),
            operator = operator,
            value = "?",
        )
        assertEquals(expected, condition.build())
    }

    @ParameterizedTest
    @CsvSource(
        "'IN',3,'EMP.EMPLOYEE_ID in (?, ?, ?)'",
        "'NOT_IN',5,'EMP.EMPLOYEE_ID not in (?, ?, ?, ?, ?)'"
    )
    fun value_shouldBuildExpectedSql_whenValueIsList(
        operator: ComparisonOperator,
        value: Int,
        expected: String,
    ) {
        val valueList = mutableListOf<Any>()
        for (i in 1..value) {
            valueList += "?"
        }
        val condition = Compare.Value(
            lhsProperty = EmployeeEntity::employeeId.toColumnString(),
            operator = operator,
            value = valueList,
        )
        assertEquals(expected, condition.build())
    }

    @ParameterizedTest
    @CsvSource("'EmployeeEntity::employeeId', 'EMP.EMPLOYEE_ID between ? and ?'")
    fun between_shouldBuildExpectedSql(refString: String, expected: String) {
        val ref = changeColumnRef(refString)
        val condition = Compare.Between(lhsProperty = ref.build(), start = "?", end = "?")
        assertEquals(expected, condition.build())
    }

    @ParameterizedTest
    @CsvSource("'EmployeeEntity::employeeSubId', 'EMP.EMPLOYEE_SUB_ID is null'")
    fun isNull_shouldBuildExpectedSql(propertyString: String, expected: String) {
        val property = changeProperty(propertyString)
        val condition = Compare.IsNull(lhsProperty = property.toColumnString())
        assertEquals(expected, condition.build())
    }

    @ParameterizedTest
    @CsvSource("'EmployeeEntity::employeeSubId', 'EMP.EMPLOYEE_SUB_ID is not null'")
    fun isNotNull_shouldBuildExpectedSql(
        propertyString: String, expected: String
    ) {
        val property = changeProperty(propertyString)
        val condition = Compare.IsNotNull(lhsProperty = property.toColumnString())
        assertEquals(expected, condition.build())
    }

    @ParameterizedTest
    @CsvSource("'EMP.EMPLOYEE_ID = ?','EMP.EMPLOYEE_ID = ?'")
    fun freeText_shouldReturnOriginalText(actualBase: String, expected: String) {
        val condition = FreeText(actualBase)
        assertEquals(expected, condition.build())
    }
}