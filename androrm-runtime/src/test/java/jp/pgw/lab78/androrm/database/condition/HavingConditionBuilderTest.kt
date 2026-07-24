package jp.pgw.lab78.androrm.database.condition

import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.database.function.AggregateFunction.MAX
import jp.pgw.lab78.androrm.database.support.createConditionBuilderTestState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.reflect.KProperty1
import jp.pgw.lab78.androrm.database.entities.RuntimeEmployeeEntity as EmployeeEntity

/**
 * ## HavingConditionBuilder テスト
 * ### HAVING 用の集計関数条件を検証する
  * @author Masahiro Inoue
  * @since 2026-05-18
 */
class HavingConditionBuilderTest {

    /**
     * 指定された列のMAX関数式を生成する。
     * @param column MAX関数を適用する列。
     * @return 処理結果。
     * @author Masahiro Inoue
     * @since 2026-05-18
     */
    private fun <T : Entity> max(column: KProperty1<T, *>): String =
        MAX.build(column)

    /**
     * 「testMaxFunctionDsl」の条件における期待動作を検証する。
     * @param methodName methodNameとして使用する値。
     * @param value 追加する値。
     * @param expected 期待値。
     * @author Masahiro Inoue
     * @since 2026-05-18
     */
    @DisplayName("HAVING の MAX 関数条件 DSL を検証する")
    @ParameterizedTest(name = "[{index}] method={0}, value={1}, expected={2}")
    @CsvSource(
        "'eq',10,'max(EMP.EMPLOYEE_ID) = ?'",
        "'ne',20,'max(EMP.EMPLOYEE_ID) <> ?'",
        "'gt',30,'max(EMP.EMPLOYEE_ID) > ?'",
        "'ge',40,'max(EMP.EMPLOYEE_ID) >= ?'",
        "'lt',50,'max(EMP.EMPLOYEE_ID) < ?'",
        "'le',60,'max(EMP.EMPLOYEE_ID) <= ?'",
    )
    fun testMaxFunctionDsl(
        methodName: String,
        value: Int,
        expected: String,
    ) {
        val maxEmployeeId = max(EmployeeEntity::employeeId)
        val state = createConditionBuilderTestState()

        with(state.builder) {
            when (methodName) {
                "eq" -> condition("$maxEmployeeId = ?", value)
                "ne" -> condition("$maxEmployeeId <> ?", value)
                "gt" -> condition("$maxEmployeeId > ?", value)
                "ge" -> condition("$maxEmployeeId >= ?", value)
                "lt" -> condition("$maxEmployeeId < ?", value)
                "le" -> condition("$maxEmployeeId <= ?", value)
                else -> error("Unsupported methodName: $methodName")
            }
        }

        assertEquals(expected, state.singleSql())
        assertEquals(listOf(value), state.valueHolder.bindValues)
    }

    /**
     * 「testMaxFunctionLogicalDsl」の条件における期待動作を検証する。
     * @param logicalOperator logicalOperatorとして使用する値。
     * @param expected 期待値。
     * @author Masahiro Inoue
     * @since 2026-05-18
     */
    @DisplayName("HAVING の MAX 関数論理条件 DSL を検証する")
    @ParameterizedTest(name = "[{index}] operator={0}, expected={1}")
    @CsvSource(
        "'AND','(max(EMP.EMPLOYEE_ID) >= ? and max(EMP.EMPLOYEE_ID) <= ?)'",
        "'OR','(max(EMP.EMPLOYEE_ID) < ? or max(EMP.EMPLOYEE_ID) > ?)'",
    )
    fun testMaxFunctionLogicalDsl(
        logicalOperator: String,
        expected: String,
    ) {
        val maxEmployeeId = max(EmployeeEntity::employeeId)
        val state = createConditionBuilderTestState()

        with(state.builder) {
            when (logicalOperator) {
                "AND" -> and {
                    condition("$maxEmployeeId >= ?", 10)
                    condition("$maxEmployeeId <= ?", 50)
                }

                "OR" -> or {
                    condition("$maxEmployeeId < ?", 10)
                    condition("$maxEmployeeId > ?", 50)
                }

                else -> error("Unsupported logicalOperator: $logicalOperator")
            }
        }

        assertEquals(expected, state.singleSql())
        assertEquals(listOf(10, 50), state.valueHolder.bindValues)
    }
}