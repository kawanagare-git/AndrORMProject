package jp.pgw.lab78.androrm.database.condition.sealed

import jp.pgw.lab78.androrm.database.entities.select.EmployeeEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Orderの動作を検証するテストクラス。
 * @author Masahiro Inoue
 * @since 2026-05-14
 */
class OrderTest {

    /**
     * 昇順指定とNULL配置指定からORDER BY句を構築できることを検証する。
     * @param ascending 昇順にする場合はtrue。
     * @param nullsLast NULLを末尾に配置する場合はtrue。
     * @param expected 期待値。
     * @author Masahiro Inoue
     * @since 2026-05-14
     */
    @ParameterizedTest
    @CsvSource(
        "'true','true','EMP.EMPLOYEE_ID asc nulls last'",
        "'true','false','EMP.EMPLOYEE_ID asc nulls first'",
        "'false','true','EMP.EMPLOYEE_ID desc nulls last'",
        "'false','false','EMP.EMPLOYEE_ID desc nulls first'",
    )
    fun build(ascending: Boolean, nullsLast: Boolean, expected: String) {
        val property = EmployeeEntity::employeeId
        val order = Order(
            property,
            ascending,
            nullsLast,
        )
        assertEquals(expected, order.build())
    }
}