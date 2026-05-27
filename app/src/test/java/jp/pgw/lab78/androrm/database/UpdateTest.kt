package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.MessageConstants.AE00013
import jp.pgw.lab78.androrm.common.MessageConstants.AE00014
import jp.pgw.lab78.androrm.common.MessageConstants.AE00015
import jp.pgw.lab78.androrm.database.entities.select.TestSelectEntity
import jp.pgw.lab78.androrm.database.entities.update.TestAllEntityUpdate
import jp.pgw.lab78.androrm.database.queryparts.JoinType.INNER
import jp.pgw.lab78.androrm.database.utility.Support.tableRef
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class UpdateTest {

    @Test
    fun testBuild_setEntityAndWhere() {
        val updateDate = LocalDateTime.parse("2026-05-24T19:14:00")
        val entity =
            TestAllEntityUpdate(name = "川流", address = "愛知県豊田市", updateDate = updateDate)
        val update = Update(TestAllEntityUpdate::class)
            .set(entity)
            .where { TestAllEntityUpdate::name eq "更新前" }
        val actualQuery = update.build()
        val actualValues = update.bindValues
        assertEquals(
            "update TEST_ALL_ENTITY as TEST_ALL_ENTITY_UPDATE " +
                    "set NAME = ?, ADDRESS = ?, UPDATE_DATE = ? " +
                    "where TEST_ALL_ENTITY_UPDATE.NAME = ?",
            actualQuery,
        )
        assertEquals(
            listOf("川流", "愛知県豊田市", updateDate, "更新前"),
            actualValues,
        )
    }

    @Test
    fun testBuild_setDslFromJoinAndWhere() {
        val updateDate = LocalDateTime.parse("2026-05-24T19:14:00")
        val targetTable = tableRef(TestAllEntityUpdate::class, "A")
        val fromTable = tableRef(TestSelectEntity::class, "X")
        val joinTable = tableRef(TestSelectEntity::class, "Y")
        val update = Update(targetTable)
            .set {
                TestAllEntityUpdate::name setTo fromTable[TestSelectEntity::name]
                TestAllEntityUpdate::address setTo joinTable[TestSelectEntity::address]
                TestAllEntityUpdate::updateDate setTo updateDate
            }
            .from(fromTable)
            .join(INNER, joinTable) {
                fromTable[TestSelectEntity::id] eq joinTable[TestSelectEntity::id]
            }
            .where {
                fromTable[TestSelectEntity::id] eq 50
            }
        val actualQuery = update.build()
        val actualValues = update.bindValues
        assertEquals(
            "update TEST_ALL_ENTITY as A " +
                    "set NAME = X.NAME, ADDRESS = Y.ADDRESS, UPDATE_DATE = ? " +
                    "from TEST_SELECT_ENTITY X " +
                    "inner join TEST_SELECT_ENTITY Y on X.ID = Y.ID " +
                    "where X.ID = ?",
            actualQuery,
        )
        assertEquals(
            listOf(updateDate, 50),
            actualValues,
        )
    }

    @Test
    fun testBuild_withoutSet_throwsIllegalArgumentException() {
        val update = Update(TestAllEntityUpdate::class)
            .where {
                TestAllEntityUpdate::name eq "川流"
            }

        val actual = assertThrows(IllegalArgumentException::class.java) {
            update.build()
        }
        assertEquals(AE00014, actual.message)
    }

    @Test
    fun testBuild_withoutWhere_throwsIllegalArgumentException() {
        val updateDate = LocalDateTime.parse("2026-05-24T19:14:00")
        val entity =
            TestAllEntityUpdate(name = "川流", address = "愛知県豊田市", updateDate = updateDate)
        val update = Update(TestAllEntityUpdate::class)
            .set(entity)
        val actual = assertThrows(IllegalArgumentException::class.java) {
            update.build()
        }
        assertEquals(AE00013, actual.message)
    }

    @Test
    fun testJoin_beforeFrom_throwsIllegalStateException() {
        val targetTable = tableRef(TestAllEntityUpdate::class, "A")
        val joinTable = tableRef(TestSelectEntity::class, "Y")
        val update = Update(targetTable)
        val actual = assertThrows(IllegalStateException::class.java) {
            update.join(INNER, joinTable) {
                joinTable[TestSelectEntity::id] eq 50
            }
        }
        assertEquals(AE00015, actual.message)
    }
}