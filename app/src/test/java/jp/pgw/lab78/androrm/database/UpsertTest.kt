package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.MessageConstants.AE00016
import jp.pgw.lab78.androrm.common.MessageConstants.AE00017
import jp.pgw.lab78.androrm.common.MessageConstants.AE00018
import jp.pgw.lab78.androrm.database.entities.insert.TestAllEntityComprehensive
import jp.pgw.lab78.androrm.database.entities.upsert.SalaryEntityUpsert
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class UpsertTest {

    @Test
    fun testSingleLineBuild() {
        val birthday = LocalDate.of(1968, 1, 7)
        val updateDate = LocalDateTime.of(2026, 5, 27, 12, 1, 0)
        val insertDateTime = LocalDateTime.of(2026, 5, 27, 12, 2, 0)

        val entityList = listOf(
            TestAllEntityComprehensive(
                id = 1,
                name = "川流",
                address = "愛知県豊田市",
                birthday = birthday,
                updateDate = updateDate,
                insertDateTime = insertDateTime,
            )
        )

        val upsert = Upsert(TestAllEntityComprehensive::class)
            .onConflict(TestAllEntityComprehensive::id)
            .updateColumns(
                TestAllEntityComprehensive::name,
                TestAllEntityComprehensive::address,
                TestAllEntityComprehensive::updateDate,
            )

        val (query, values) = upsert.build(entityList)

        println("$query / values = $values")

        assertEquals(
            "insert into TEST_ALL_ENTITY " +
                    "(ID, NAME, ADDRESS, BIRTHDAY, UPDATE_DATE, INSERT_DATE_TIME) " +
                    "values(?, ?, ?, ?, ?, ?) " +
                    "on conflict(ID) do update set " +
                    "NAME = excluded.NAME, ADDRESS = excluded.ADDRESS, UPDATE_DATE = excluded.UPDATE_DATE",
            query
        )

        assertEquals(
            listOf(
                1,
                "川流",
                "愛知県豊田市",
                birthday,
                updateDate,
                insertDateTime,
            ),
            values
        )
    }

    @Test
    fun testMultiLineBuild() {
        val birthday1 = LocalDate.of(1968, 1, 7)
        val updateDate1 = LocalDateTime.of(2026, 5, 27, 12, 1, 0)
        val insertDateTime1 = LocalDateTime.of(2026, 5, 27, 12, 2, 0)

        val birthday2 = LocalDate.of(1978, 6, 17)
        val updateDate2 = LocalDateTime.of(2026, 5, 27, 13, 1, 0)
        val insertDateTime2 = LocalDateTime.of(2026, 5, 27, 13, 2, 0)

        val entityList = listOf(
            TestAllEntityComprehensive(
                id = 1,
                name = "川流",
                address = "愛知県豊田市",
                birthday = birthday1,
                updateDate = updateDate1,
                insertDateTime = insertDateTime1,
            ),
            TestAllEntityComprehensive(
                id = 2,
                name = "kawanagare",
                address = "宮城県仙台市",
                birthday = birthday2,
                updateDate = updateDate2,
                insertDateTime = insertDateTime2,
            )
        )

        val upsert = Upsert(TestAllEntityComprehensive::class)
            .onConflict(TestAllEntityComprehensive::id)
            .updateColumns(
                TestAllEntityComprehensive::name,
                TestAllEntityComprehensive::address,
                TestAllEntityComprehensive::updateDate,
            )

        val (query, values) = upsert.build(entityList)

        println("$query / values = $values")

        assertEquals(
            "insert into TEST_ALL_ENTITY " +
                    "(ID, NAME, ADDRESS, BIRTHDAY, UPDATE_DATE, INSERT_DATE_TIME) " +
                    "values(?, ?, ?, ?, ?, ?), (?, ?, ?, ?, ?, ?) " +
                    "on conflict(ID) do update set " +
                    "NAME = excluded.NAME, ADDRESS = excluded.ADDRESS, UPDATE_DATE = excluded.UPDATE_DATE",
            query
        )

        assertEquals(
            listOf(
                1,
                "川流",
                "愛知県豊田市",
                birthday1,
                updateDate1,
                insertDateTime1,
                2,
                "kawanagare",
                "宮城県仙台市",
                birthday2,
                updateDate2,
                insertDateTime2,
            ),
            values
        )
    }

    @Test
    fun testAddEntityBuild() {
        val createdAt = LocalDateTime.of(2026, 5, 27, 12, 1, 0)

        val upsert = Upsert(SalaryEntityUpsert::class)
            .onConflict(
                SalaryEntityUpsert::employeeId,
                SalaryEntityUpsert::payMonth,
            )
            .updateColumns(
                SalaryEntityUpsert::createdAt,
            )

        upsert.addEntity(
            SalaryEntityUpsert(
                employeeId = "00010",
                payMonth = "202605",
                createdAt = createdAt,
            )
        )

        val query = upsert.build()
        val values = upsert.bindValues

        println("$query / values = $values")

        assertEquals(
            "insert into SALARY " +
                    "(EMPLOYEE_ID, PAY_MONTH, CREATE_DATE_TIME) " +
                    "values(?, ?, ?) " +
                    "on conflict(EMPLOYEE_ID, PAY_MONTH) do update set " +
                    "CREATE_DATE_TIME = excluded.CREATE_DATE_TIME",
            query
        )

        assertEquals(
            listOf(
                "00010",
                "202605",
                createdAt,
            ),
            values
        )
    }

    @Test
    fun testBuildEmptyList() {
        val upsert = Upsert(TestAllEntityComprehensive::class)
            .onConflict(TestAllEntityComprehensive::id)
            .updateColumns(TestAllEntityComprehensive::name)

        val actual = assertThrows(IllegalArgumentException::class.java) {
            upsert.build(emptyList<TestAllEntityComprehensive>())
        }

        assertEquals(AE00016, actual.message)
    }

    @Test
    fun testBuildWithoutConflictColumns() {
        val birthday = LocalDate.of(1968, 1, 7)
        val updateDate = LocalDateTime.of(2026, 5, 27, 12, 1, 0)
        val insertDateTime = LocalDateTime.of(2026, 5, 27, 12, 2, 0)

        val entityList = listOf(
            TestAllEntityComprehensive(
                id = 1,
                name = "川流",
                address = "愛知県豊田市",
                birthday = birthday,
                updateDate = updateDate,
                insertDateTime = insertDateTime,
            )
        )

        val upsert = Upsert(TestAllEntityComprehensive::class)
            .updateColumns(TestAllEntityComprehensive::name)

        val actual = assertThrows(IllegalArgumentException::class.java) {
            upsert.build(entityList)
        }

        assertEquals(AE00017, actual.message)
    }

    @Test
    fun testBuildWithoutUpdateColumns() {
        val birthday = LocalDate.of(1968, 1, 7)
        val updateDate = LocalDateTime.of(2026, 5, 27, 12, 1, 0)
        val insertDateTime = LocalDateTime.of(2026, 5, 27, 12, 2, 0)

        val entityList = listOf(
            TestAllEntityComprehensive(
                id = 1,
                name = "川流",
                address = "愛知県豊田市",
                birthday = birthday,
                updateDate = updateDate,
                insertDateTime = insertDateTime,
            )
        )

        val upsert = Upsert(TestAllEntityComprehensive::class)
            .onConflict(TestAllEntityComprehensive::id)

        val actual = assertThrows(IllegalArgumentException::class.java) {
            upsert.build(entityList)
        }

        assertEquals(AE00018, actual.message)
    }
}