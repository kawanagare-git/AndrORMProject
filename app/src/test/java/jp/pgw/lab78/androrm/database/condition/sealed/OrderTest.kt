package jp.pgw.lab78.androrm.database.condition.sealed

import jp.pgw.lab78.androrm.database.entities.select.EmployeeEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class OrderTest {

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