package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.MessageConstants.AE00016
import jp.pgw.lab78.androrm.common.MessageConstants.AE00017
import jp.pgw.lab78.androrm.database.entities.upsert.SalaryEntityUpsert
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AbsertTest {

    @Test
    fun testSingleLineBuild() {
        val createdAt = LocalDateTime.of(2026, 7, 2, 21, 30, 0)

        val absert = Absert(SalaryEntityUpsert::class)
            .addEntity(
                SalaryEntityUpsert(
                    employeeId = "00010",
                    payMonth = "202607",
                    createdAt = createdAt,
                )
            )
            .onConflict {
                column(SalaryEntityUpsert::employeeId)
                column(SalaryEntityUpsert::payMonth)
            }

        val actualQuery = absert.build()
        val actualValues = absert.bindValues

        assertEquals(
            "insert into SALARY " +
                    "(EMPLOYEE_ID, PAY_MONTH, CREATE_DATE_TIME) " +
                    "values(?, ?, ?) " +
                    "on conflict(EMPLOYEE_ID, PAY_MONTH) do nothing",
            actualQuery,
        )

        assertEquals(
            listOf(
                "00010",
                "202607",
                createdAt,
            ),
            actualValues,
        )
    }

    @Test
    fun testMultiLineBuild() {
        val createdAt1 = LocalDateTime.of(2026, 7, 2, 21, 30, 0)
        val createdAt2 = LocalDateTime.of(2026, 7, 2, 21, 31, 0)

        val absert = Absert(SalaryEntityUpsert::class)
            .addEntities(
                listOf(
                    SalaryEntityUpsert(
                        employeeId = "00010",
                        payMonth = "202607",
                        createdAt = createdAt1,
                    ),
                    SalaryEntityUpsert(
                        employeeId = "00011",
                        payMonth = "202607",
                        createdAt = createdAt2,
                    ),
                )
            )
            .onConflict {
                column(SalaryEntityUpsert::employeeId)
                column(SalaryEntityUpsert::payMonth)
            }

        val actualQuery = absert.build()
        val actualValues = absert.bindValues

        assertEquals(
            "insert into SALARY " +
                    "(EMPLOYEE_ID, PAY_MONTH, CREATE_DATE_TIME) " +
                    "values(?, ?, ?), (?, ?, ?) " +
                    "on conflict(EMPLOYEE_ID, PAY_MONTH) do nothing",
            actualQuery,
        )

        assertEquals(
            listOf(
                "00010",
                "202607",
                createdAt1,
                "00011",
                "202607",
                createdAt2,
            ),
            actualValues,
        )
    }

    @Test
    fun testBuildEmptyList() {
        val absert = Absert(SalaryEntityUpsert::class)
            .onConflict {
                column(SalaryEntityUpsert::employeeId)
                column(SalaryEntityUpsert::payMonth)
            }

        val actual = assertThrows(IllegalArgumentException::class.java) {
            absert.build()
        }

        assertEquals(AE00016, actual.message)
    }

    @Test
    fun testBuildWithoutConflictColumns() {
        val createdAt = LocalDateTime.of(2026, 7, 2, 21, 30, 0)

        val absert = Absert(SalaryEntityUpsert::class)
            .addEntity(
                SalaryEntityUpsert(
                    employeeId = "00010",
                    payMonth = "202607",
                    createdAt = createdAt,
                )
            )

        val actual = assertThrows(IllegalArgumentException::class.java) {
            absert.build()
        }

        assertEquals(AE00017, actual.message)
    }

    @Test
    fun testOnConflictCalledTwiceReplacesConflictColumns() {
        val createdAt = LocalDateTime.of(2026, 7, 2, 21, 30, 0)

        val absert = Absert(SalaryEntityUpsert::class)
            .addEntity(
                SalaryEntityUpsert(
                    employeeId = "00010",
                    payMonth = "202607",
                    createdAt = createdAt,
                )
            )
            .onConflict {
                column(SalaryEntityUpsert::employeeId)
            }
            .onConflict {
                key(SalaryEntityUpsert::employeeId)
                key(SalaryEntityUpsert::payMonth)
            }

        assertEquals(
            "insert into SALARY " +
                    "(EMPLOYEE_ID, PAY_MONTH, CREATE_DATE_TIME) " +
                    "values(?, ?, ?) " +
                    "on conflict(EMPLOYEE_ID, PAY_MONTH) do nothing",
            absert.build(),
        )
    }
}