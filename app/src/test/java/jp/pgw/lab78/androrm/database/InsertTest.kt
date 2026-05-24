package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.MessageConstants.AE00011
import jp.pgw.lab78.androrm.database.entities.insert.SalaryEntityInsert
import jp.pgw.lab78.androrm.database.entities.insert.TestInsertEntity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class InsertTest {
    @Test
    fun testSingleLineBuild() {
        val updateDate = LocalDate.of(2026, 5, 23)
        val insertDate = LocalDate.of(2026, 5, 23)
        val testInsertEntityList = listOf(
            TestInsertEntity(
                "川流", "愛知県豊田市",
                LocalDate.of(1968, 1, 7),
                updateDate,
                insertDate,
            ),
        )
        val insert = Insert(TestInsertEntity::class)
        val (query, values) = insert.build(testInsertEntityList)
        println("$query / values = $values")
        assertEquals(
            "insert into TEST_INSERT_ENTITY (NAME, ADDRESS, BIRTHDAY, UPDATE_DATE, INSERT_DATE_TIME) values(?, ?, ?, ?, ?)",
            query
        )
        assertEquals(
            listOf(
                "川流", "愛知県豊田市", LocalDate.of(1968, 1, 7), updateDate, insertDate
            ),
            values
        )
    }

    @Test
    fun testMultiLineBuild() {
        val updateAt = LocalDateTime.of(2026, 5, 23, 0, 10, 59)
        val testInsertEntityList = listOf(
            SalaryEntityInsert(
                "00010", "1",
                278900,
                updateAt,
                "KW001",
            ),
            SalaryEntityInsert(
                "00010", "2",
                286500,
                updateAt,
                "KW002",
            )
        )
        val insert = Insert(SalaryEntityInsert::class)
        val (query, values) = insert.build(testInsertEntityList)
        println("$query / values = $values")
        assertEquals(
            "insert into SALARY (EMPLOYEE_ID, PAY_MONTH, GROSS, UPDATE_DATE_TIME, UPDATE_BY_ID) values(?, ?, ?, ?, ?), (?, ?, ?, ?, ?)",
            query
        )
        assertEquals(
            listOf(
                "00010", "1", 278900, updateAt, "KW001", "00010", "2", 286500, updateAt, "KW002",
            ),
            values
        )
    }

    @Test
    fun testBuildEmptyList() {
        val insert = Insert(TestInsertEntity::class)
        val actual = assertThrows(IllegalArgumentException::class.java) {
            insert.build(emptyList<TestInsertEntity>())
        }
        assertEquals(AE00011, actual.message)
    }
}