package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.MessageConstants.AE00016
import jp.pgw.lab78.androrm.common.MessageConstants.AE00017
import jp.pgw.lab78.androrm.common.MessageConstants.AE00018
import jp.pgw.lab78.androrm.database.entities.RuntimeSalaryEntityUpsert
import jp.pgw.lab78.androrm.database.entities.RuntimeTestAllEntityComprehensive
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Upsertの動作を検証するテストクラス。
 * @author Masahiro Inoue
 * @since 2026-05-27
 */
class UpsertTest {

    /**
     * 「testSingleLineBuild」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-27
     */
    @Test
    fun testSingleLineBuild() {
        val birthday = LocalDate.of(1968, 1, 7)
        val updateDate = LocalDateTime.of(2026, 5, 27, 12, 1, 0)
        val insertDateTime = LocalDateTime.of(2026, 5, 27, 12, 2, 0)

        val entity = RuntimeTestAllEntityComprehensive(
            id = 1,
            name = "川流",
            address = "愛知県豊田市",
            birthday = birthday,
            updateDate = updateDate,
            insertDateTime = insertDateTime,
        )
        val updateData = LocalDateTime.now().plusMonths(1)
        val upsert = Upsert(RuntimeTestAllEntityComprehensive::class)
            .onConflict { column(RuntimeTestAllEntityComprehensive::id) }
            .set {
                RuntimeTestAllEntityComprehensive::name assign excluded(RuntimeTestAllEntityComprehensive::name)
                RuntimeTestAllEntityComprehensive::address assign excluded(RuntimeTestAllEntityComprehensive::address)
                RuntimeTestAllEntityComprehensive::updateDate becomes updateData
            }
        val query = upsert.addEntity(entity).build()
        val values = upsert.bindValues
        assertEquals(
            "insert into TEST_ALL_ENTITY " +
                    "(ID, NAME, ADDRESS, BIRTHDAY, UPDATE_DATE, INSERT_DATE_TIME) " +
                    "values(?, ?, ?, ?, ?, ?) " +
                    "on conflict(ID) do update set " +
                    "NAME = excluded.NAME, ADDRESS = excluded.ADDRESS, UPDATE_DATE = ?",
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
                updateData
            ),
            values
        )
    }

    /**
     * 「testMultiLineBuild」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-27
     */
    @Test
    fun testMultiLineBuild() {
        val birthday1 = LocalDate.of(1968, 1, 7)
        val updateDate1 = LocalDateTime.of(2026, 5, 27, 12, 1, 0)
        val insertDateTime1 = LocalDateTime.of(2026, 5, 27, 12, 2, 0)

        val birthday2 = LocalDate.of(1978, 6, 17)
        val updateDate2 = LocalDateTime.of(2026, 5, 27, 13, 1, 0)
        val insertDateTime2 = LocalDateTime.of(2026, 5, 27, 13, 2, 0)

        val entityList = listOf(
            RuntimeTestAllEntityComprehensive(
                id = 1,
                name = "川流",
                address = "愛知県豊田市",
                birthday = birthday1,
                updateDate = updateDate1,
                insertDateTime = insertDateTime1,
            ),
            RuntimeTestAllEntityComprehensive(
                id = 2,
                name = "kawanagare",
                address = "宮城県仙台市",
                birthday = birthday2,
                updateDate = updateDate2,
                insertDateTime = insertDateTime2,
            )
        )
        val upsert = Upsert(RuntimeTestAllEntityComprehensive::class)
            .onConflict { column(RuntimeTestAllEntityComprehensive::id) }
            .set {
                RuntimeTestAllEntityComprehensive::name assign excluded(RuntimeTestAllEntityComprehensive::name)
                RuntimeTestAllEntityComprehensive::address assign excluded(RuntimeTestAllEntityComprehensive::address)
                RuntimeTestAllEntityComprehensive::updateDate becomes excluded(RuntimeTestAllEntityComprehensive::updateDate)
            }
        val query = upsert.addEntities(entityList).build()
        val values = upsert.bindValues
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

    /**
     * 「testAddEntityBuild」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-27
     */
    @Test
    fun testAddEntityBuild() {
        val createdAt = LocalDateTime.of(2026, 5, 27, 12, 1, 0)

        val upsert = Upsert(RuntimeSalaryEntityUpsert::class)
            .onConflict {
                column(RuntimeSalaryEntityUpsert::employeeId)
                column(RuntimeSalaryEntityUpsert::payMonth)
            }
            .set { RuntimeSalaryEntityUpsert::createdAt assign excluded(RuntimeSalaryEntityUpsert::createdAt) }
        upsert.addEntity(
            RuntimeSalaryEntityUpsert(
                employeeId = "00010",
                payMonth = "202605",
                createdAt = createdAt,
            )
        )

        val query = upsert.build()
        val values = upsert.bindValues

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

    /**
     * 「testBuildEmptyList」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-27
     */
    @Test
    fun testBuildEmptyList() {
        val upsert = Upsert(RuntimeTestAllEntityComprehensive::class)
            .onConflict { column(RuntimeTestAllEntityComprehensive::id) }
            .set { RuntimeTestAllEntityComprehensive::name becomes RuntimeTestAllEntityComprehensive::name }
        val actual = assertThrows(IllegalArgumentException::class.java) {
            upsert.build()
        }
        assertEquals(AE00016, actual.message)
    }

    /**
     * 「testBuildWithoutConflictColumns」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-27
     */
    @Test
    fun testBuildWithoutConflictColumns() {
        val birthday = LocalDate.of(1968, 1, 7)
        val updateDate = LocalDateTime.of(2026, 5, 27, 12, 1, 0)
        val insertDateTime = LocalDateTime.of(2026, 5, 27, 12, 2, 0)

        val entityList = listOf(
            RuntimeTestAllEntityComprehensive(
                id = 1,
                name = "川流",
                address = "愛知県豊田市",
                birthday = birthday,
                updateDate = updateDate,
                insertDateTime = insertDateTime,
            )
        )

        val upsert = Upsert(RuntimeTestAllEntityComprehensive::class)
            .addEntities(entityList)
            .set { RuntimeTestAllEntityComprehensive::name assign "Test name" }

        val actual = assertThrows(IllegalArgumentException::class.java) {
            upsert.build()
        }
        assertEquals(AE00017, actual.message)
    }

    /**
     * 「testBuildWithoutUpdateColumns」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-27
     */
    @Test
    fun testBuildWithoutUpdateColumns() {
        val birthday = LocalDate.of(1968, 1, 7)
        val updateDate = LocalDateTime.of(2026, 5, 27, 12, 1, 0)
        val insertDateTime = LocalDateTime.of(2026, 5, 27, 12, 2, 0)

        val entityList = listOf(
            RuntimeTestAllEntityComprehensive(
                id = 1,
                name = "川流",
                address = "愛知県豊田市",
                birthday = birthday,
                updateDate = updateDate,
                insertDateTime = insertDateTime,
            )
        )
        val upsert = Upsert(RuntimeTestAllEntityComprehensive::class)
            .onConflict { column(RuntimeTestAllEntityComprehensive::id) }.addEntities(entityList)
        val actual = assertThrows(IllegalArgumentException::class.java) {
            upsert.build()
        }
        assertEquals(AE00018, actual.message)
    }

    /**
     * 「testBuildWithWhereBuildsDoUpdateWhereClause」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-27
     */
    @Test
    fun testBuildWithWhereBuildsDoUpdateWhereClause() {
        val createdAt = LocalDateTime.of(2026, 7, 2, 21, 30, 0)

        val upsert = Upsert(RuntimeSalaryEntityUpsert::class)
            .addEntity(
                RuntimeSalaryEntityUpsert(
                    employeeId = "00010",
                    payMonth = "202607",
                    createdAt = createdAt,
                )
            )
            .onConflict {
                column(RuntimeSalaryEntityUpsert::employeeId)
                column(RuntimeSalaryEntityUpsert::payMonth)
            }
            .set {
                RuntimeSalaryEntityUpsert::createdAt assign
                        excluded(RuntimeSalaryEntityUpsert::createdAt)
            }
            .where {
                condition("excluded.PAY_MONTH = ?", "202607")
            }

        val actualQuery = upsert.build()
        val actualValues = upsert.bindValues

        assertEquals(
            "insert into SALARY " +
                    "(EMPLOYEE_ID, PAY_MONTH, CREATE_DATE_TIME) " +
                    "values(?, ?, ?) " +
                    "on conflict(EMPLOYEE_ID, PAY_MONTH) do update set " +
                    "CREATE_DATE_TIME = excluded.CREATE_DATE_TIME " +
                    "where excluded.PAY_MONTH = ?",
            actualQuery,
        )

        assertEquals(
            listOf(
                "00010",
                "202607",
                createdAt,
                "202607",
            ),
            actualValues,
        )
    }

    /**
     * 「testOnConflictCalledTwiceReplacesConflictColumns」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-27
     */
    @Test
    fun testOnConflictCalledTwiceReplacesConflictColumns() {
        val createdAt = LocalDateTime.of(2026, 7, 2, 21, 30, 0)

        val upsert = Upsert(RuntimeSalaryEntityUpsert::class)
            .addEntity(
                RuntimeSalaryEntityUpsert(
                    employeeId = "00010",
                    payMonth = "202607",
                    createdAt = createdAt,
                )
            )
            .onConflict {
                column(RuntimeSalaryEntityUpsert::employeeId)
            }
            .onConflict {
                key(RuntimeSalaryEntityUpsert::employeeId)
                key(RuntimeSalaryEntityUpsert::payMonth)
            }
            .set {
                RuntimeSalaryEntityUpsert::createdAt assign
                        excluded(RuntimeSalaryEntityUpsert::createdAt)
            }

        assertEquals(
            "insert into SALARY " +
                    "(EMPLOYEE_ID, PAY_MONTH, CREATE_DATE_TIME) " +
                    "values(?, ?, ?) " +
                    "on conflict(EMPLOYEE_ID, PAY_MONTH) do update set " +
                    "CREATE_DATE_TIME = excluded.CREATE_DATE_TIME",
            upsert.build(),
        )
    }
}