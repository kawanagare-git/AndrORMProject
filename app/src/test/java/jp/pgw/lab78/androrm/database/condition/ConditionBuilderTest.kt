package jp.pgw.lab78.androrm.database.condition

import jp.pgw.lab78.androrm.database.entities.select.EmployeeEntity
import jp.pgw.lab78.androrm.support.createConditionBuilderTestState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * ## ConditionBuilder テスト
 * ### BaseConditionBuilder の DSL public メソッドを ConditionBuilder 経由で検証する
 */
class ConditionBuilderTest {

    @DisplayName("論理条件 DSL を検証する")
    @ParameterizedTest(name = "[{index}] operator={0}, expected={1}")
    @CsvSource(
        "'AND','(EMP.EMPLOYEE_ID = ? and EMP.NAME = ?)'",
        "'OR','(EMP.EMPLOYEE_ID = ? or EMP.NAME = ?)'",
    )
    fun testLogicalDsl(
        logicalOperator: String,
        expected: String,
    ) {
        val state = createConditionBuilderTestState()

        with(state.builder) {
            when (logicalOperator) {
                "AND" -> and {
                    EmployeeEntity::employeeId eq 10
                    EmployeeEntity::name eq "TARO"
                }

                "OR" -> or {
                    EmployeeEntity::employeeId eq 10
                    EmployeeEntity::name eq "TARO"
                }

                else -> error("Unsupported logicalOperator: $logicalOperator")
            }
        }

        assertEquals(expected, state.singleSql())
        assertEquals(listOf(10, "TARO"), state.valueHolder.bindValues)
    }
}