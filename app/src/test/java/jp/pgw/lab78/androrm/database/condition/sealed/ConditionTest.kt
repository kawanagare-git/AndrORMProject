package jp.pgw.lab78.androrm.database.condition.sealed

import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator.*
import jp.pgw.lab78.androrm.database.entities.select.EmployeeEntity
import jp.pgw.lab78.androrm.database.reference.ColumnRef
import jp.pgw.lab78.androrm.database.reference.TableRef

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * ## Condition テスト
 * ### Condition#build が期待する SQL 断片を生成することを確認する
 */
class ConditionTest {

    companion object {

        @JvmStatic
        fun valueConditionCases(): Stream<Arguments> =
            Stream.of(
                Arguments.of(EQ, "?", "EMP.EMPLOYEE_ID = ?"),
                Arguments.of(NE, "?", "EMP.EMPLOYEE_ID <> ?"),
                Arguments.of(GT, "?", "EMP.EMPLOYEE_ID > ?"),
                Arguments.of(GE, "?", "EMP.EMPLOYEE_ID >= ?"),
                Arguments.of(LT, "?", "EMP.EMPLOYEE_ID < ?"),
                Arguments.of(LE, "?", "EMP.EMPLOYEE_ID <= ?"),
                Arguments.of(LIKE, "?", "EMP.EMPLOYEE_ID like ?"),
                Arguments.of(NOT_LIKE, "?", "EMP.EMPLOYEE_ID not like ?"),
                Arguments.of(GLOB, "?", "EMP.EMPLOYEE_ID glob ?"),
                Arguments.of(NOT_GLOB, "?", "EMP.EMPLOYEE_ID not glob ?"),
            )

        @JvmStatic
        fun inConditionCases(): Stream<Arguments> =
            Stream.of(
                Arguments.of(IN, listOf("?", "?", "?"), "EMP.EMPLOYEE_ID in (?, ?, ?)"),
                Arguments.of(NOT_IN, listOf("?", "?", "?"), "EMP.EMPLOYEE_ID not in (?, ?, ?)"),
            )
    }

    @ParameterizedTest
    @MethodSource("valueConditionCases")
    fun value_shouldBuildExpectedSql(
        operator: ComparisonOperator,
        value: Any,
        expected: String,
    ) {
        val condition = Compare.Value(
            lhsProperty = EmployeeEntity::employeeId,
            operator = operator,
            value = value,
        )

        assertEquals(expected, condition.build())
    }

    @ParameterizedTest
    @MethodSource("inConditionCases")
    fun value_shouldBuildExpectedSql_whenValueIsList(
        operator: ComparisonOperator,
        value: Any,
        expected: String,
    ) {
        val condition = Compare.Value(
            lhsProperty = EmployeeEntity::employeeId,
            operator = operator,
            value = value,
        )

        assertEquals(expected, condition.build())
    }

    @Test
    fun between_shouldBuildExpectedSql() {
        val tabRef = TableRef(EmployeeEntity::class, "EMP")
        val colRef = ColumnRef(tabRef, EmployeeEntity::employeeId)
        val condition = Compare.ColumnBetween(
            column = colRef,
            start = "?",
            end = "?",
        )

        assertEquals(
            "EMP.EMPLOYEE_ID between ? and ?",
            condition.build()
        )
    }

    @Test
    fun isNull_shouldBuildExpectedSql() {
        val condition = Compare.IsNull(
            lhsProperty = EmployeeEntity::employeeSubId,
        )

        assertEquals(
            "EMP.EMPLOYEE_SUB_ID is null",
            condition.build()
        )
    }

    @Test
    fun isNotNull_shouldBuildExpectedSql() {
        val condition = Compare.IsNotNull(
            lhsProperty = EmployeeEntity::employeeSubId,
        )

        assertEquals(
            "EMP.EMPLOYEE_SUB_ID is not null",
            condition.build()
        )
    }

    @Test
    fun freeText_shouldReturnOriginalText() {
        val condition = FreeText("EMP.EMPLOYEE_ID = ?")

        assertEquals(
            "EMP.EMPLOYEE_ID = ?",
            condition.build()
        )
    }
}