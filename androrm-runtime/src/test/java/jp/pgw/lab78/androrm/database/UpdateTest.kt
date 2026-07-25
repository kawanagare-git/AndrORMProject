package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.MessageConstants.AE00013
import jp.pgw.lab78.androrm.common.MessageConstants.AE00014
import jp.pgw.lab78.androrm.common.MessageConstants.AE00015
import jp.pgw.lab78.androrm.database.entities.RuntimeTestAllEntityUpdate
import jp.pgw.lab78.androrm.database.entities.select.TestSelectEntity
import jp.pgw.lab78.androrm.database.interfaces.plus
import jp.pgw.lab78.androrm.database.interfaces.rawExpression
import jp.pgw.lab78.androrm.database.queryparts.JoinType.INNER
import jp.pgw.lab78.androrm.database.reference.TableRef
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * Updateの動作を検証するテストクラス。
 * @author Masahiro Inoue
 * @since 2026-05-26
 */
class UpdateTest {

    /**
     * 「testBuild_setEntityAndWhere」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-26
     */
    @Test
    fun testBuild_setEntityAndWhere() {
        val updateDate = LocalDateTime.parse("2026-05-24T19:14:00")
        val entity =
            RuntimeTestAllEntityUpdate(
                name = "川流",
                address = "愛知県豊田市",
                updateDate = updateDate
            )
        val update = Update(RuntimeTestAllEntityUpdate::class)
            .set(entity)
            .where { RuntimeTestAllEntityUpdate::name eq "更新前" }
        val actualQuery = update.build()
        val actualValues = update.bindValues
        assertEquals(
            "update TEST_ALL_ENTITY as TEST_ALL_ENTITY " +
                    "set NAME = ?, ADDRESS = ?, UPDATE_DATE = ? " +
                    "where TEST_ALL_ENTITY.NAME = ?",
            actualQuery,
        )
        assertEquals(
            listOf("川流", "愛知県豊田市", updateDate, "更新前"),
            actualValues,
        )
    }

    /**
     * 「testBuild_setDslFromJoinAndWhere」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-26
     */
    @Test
    fun testBuild_setDslFromJoinAndWhere() {
        val updateDate = LocalDateTime.parse("2026-05-24T19:14:00")
        val targetTable = TableRef(RuntimeTestAllEntityUpdate::class, "A")
        val fromTable = TableRef(TestSelectEntity::class, "X")
        val joinTable = TableRef(TestSelectEntity::class, "Y")
        val update = Update(targetTable)
            .set {
                RuntimeTestAllEntityUpdate::name becomes fromTable[TestSelectEntity::name]
                RuntimeTestAllEntityUpdate::address becomes joinTable[TestSelectEntity::address]
                RuntimeTestAllEntityUpdate::updateDate assign updateDate
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

    /**
     * 「testBuild_withoutSet_throwsIllegalArgumentException」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-26
     */
    @Test
    fun testBuild_withoutSet_throwsIllegalArgumentException() {
        val update = Update(RuntimeTestAllEntityUpdate::class)
            .where {
                RuntimeTestAllEntityUpdate::name eq "川流"
            }

        val actual = assertThrows(IllegalArgumentException::class.java) {
            update.build()
        }
        assertEquals(AE00014, actual.message)
    }

    /**
     * 「testBuild_withoutWhere_throwsIllegalArgumentException」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-26
     */
    @Test
    fun testBuild_withoutWhere_throwsIllegalArgumentException() {
        val updateDate = LocalDateTime.parse("2026-05-24T19:14:00")
        val entity =
            RuntimeTestAllEntityUpdate(
                name = "川流",
                address = "愛知県豊田市",
                updateDate = updateDate
            )
        val update = Update(RuntimeTestAllEntityUpdate::class)
            .set(entity)
        val actual = assertThrows(IllegalArgumentException::class.java) {
            update.build()
        }
        assertEquals(AE00013, actual.message)
    }

    /**
     * 「testJoin_beforeFrom_throwsIllegalStateException」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-26
     */
    @Test
    fun testJoin_beforeFrom_throwsIllegalStateException() {
        val targetTable = TableRef(RuntimeTestAllEntityUpdate::class, "A")
        val joinTable = TableRef(TestSelectEntity::class, "Y")
        val update = Update(targetTable)
        val actual = assertThrows(IllegalStateException::class.java) {
            update.join(INNER, joinTable) {
                joinTable[TestSelectEntity::id] eq 50
            }
        }
        assertEquals(AE00015, actual.message)
    }

    /**
     * 「testBuild_setDslWithSqlExpression_buildsSetExpressionAndBindValues」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-26
     */
    @Test
    fun testBuild_setDslWithSqlExpression_buildsSetExpressionAndBindValues() {
        val targetTable = TableRef(RuntimeTestAllEntityUpdate::class, "A")

        val update = Update(targetTable)
            .set {
                RuntimeTestAllEntityUpdate::name becomes (targetTable[RuntimeTestAllEntityUpdate::name] + 10)
                RuntimeTestAllEntityUpdate::updateDate becomes rawExpression("CURRENT_TIMESTAMP")
            }
            .where {
                targetTable[RuntimeTestAllEntityUpdate::name] eq "更新前"
            }

        assertEquals(
            "update TEST_ALL_ENTITY as A " +
                    "set NAME = (A.NAME + ?), UPDATE_DATE = CURRENT_TIMESTAMP " +
                    "where A.NAME = ?",
            update.build(),
        )
        assertEquals(
            listOf(10, "更新前"),
            update.bindValues,
        )
    }

    /**
     * updateAllを指定した場合、WHERE句なしの全件更新SQLを生成することを検証する。
     * @author Masahiro Inoue
     * @since 2026-07-22
     */
    @Test
    fun testBuild_updateAll_buildsAllRecordsUpdate() {
        val updateDate = LocalDateTime.parse("2026-07-22T09:00:00")
        val entity =
            RuntimeTestAllEntityUpdate(
                name = "更新後",
                address = "愛知県豊田市",
                updateDate = updateDate,
            )
        val update = Update(RuntimeTestAllEntityUpdate::class)
            .set(entity)
            .updateAll()
        assertEquals(
            "update TEST_ALL_ENTITY as TEST_ALL_ENTITY " +
                    "set NAME = ?, ADDRESS = ?, UPDATE_DATE = ?",
            update.build(),
        )
        assertEquals(
            listOf("更新後", "愛知県豊田市", updateDate),
            update.bindValues,
        )
    }

    /**
     * WHERE付きSQLを生成した後にupdateAllを指定した場合、
     * WHERE句を除外したSQLへ再生成されることを検証する。
     * @author Masahiro Inoue
     * @since 2026-07-22
     */
    @Test
    fun testBuild_whereBuiltThenUpdateAll_rebuildsAllRecordsUpdate() {
        val updateDate = LocalDateTime.parse("2026-07-22T09:00:00")
        val entity =
            RuntimeTestAllEntityUpdate(
                name = "更新後",
                address = "愛知県豊田市",
                updateDate = updateDate,
            )
        val update = Update(RuntimeTestAllEntityUpdate::class)
            .set(entity)
            .where {
                RuntimeTestAllEntityUpdate::name eq "更新前"
            }
        assertEquals(
            "update TEST_ALL_ENTITY as TEST_ALL_ENTITY " +
                    "set NAME = ?, ADDRESS = ?, UPDATE_DATE = ? " +
                    "where TEST_ALL_ENTITY.NAME = ?",
            update.build(),
        )
        assertEquals(
            listOf("更新後", "愛知県豊田市", updateDate, "更新前"),
            update.bindValues,
        )
        update.updateAll()
        assertEquals(
            "update TEST_ALL_ENTITY as TEST_ALL_ENTITY " +
                    "set NAME = ?, ADDRESS = ?, UPDATE_DATE = ?",
            update.build(),
        )
        assertEquals(
            listOf("更新後", "愛知県豊田市", updateDate),
            update.bindValues,
        )
    }

    /**
     * WHERE付きSQLを生成した後にupdateAllを指定した場合、
     * WHERE句を除外したSQLへ再生成されることを検証する。
     * @author Masahiro Inoue
     * @since 2026-07-22
     */
    @Test
    fun testBuild_rebuildsAllRecordsUpdate_whereBuiltThenUpdateAll() {
        val updateDate = LocalDateTime.parse("2026-07-22T09:00:00")
        val entity =
            RuntimeTestAllEntityUpdate(
                name = "更新後",
                address = "愛知県豊田市",
                updateDate = updateDate,
            )
        val update = Update(RuntimeTestAllEntityUpdate::class)
            .set(entity)
            .updateAll()
        assertEquals(
            "update TEST_ALL_ENTITY as TEST_ALL_ENTITY " +
                    "set NAME = ?, ADDRESS = ?, UPDATE_DATE = ?",
            update.build(),
        )
        assertEquals(
            listOf("更新後", "愛知県豊田市", updateDate),
            update.bindValues,
        )
        update.set(entity)
            .where {
                RuntimeTestAllEntityUpdate::name eq "更新前"
            }
        assertEquals(
            "update TEST_ALL_ENTITY as TEST_ALL_ENTITY " +
                    "set NAME = ?, ADDRESS = ?, UPDATE_DATE = ? " +
                    "where TEST_ALL_ENTITY.NAME = ?",
            update.build(),
        )
        assertEquals(
            listOf("更新後", "愛知県豊田市", updateDate, "更新前"),
            update.bindValues,
        )
    }

    /**
     * updateAllによる全件更新SQLを生成した後にWHEREを指定した場合、
     * 条件付き更新SQLへ再生成されることを検証する。
     * @author Masahiro Inoue
     * @since 2026-07-22
     */
    @Test
    fun testBuild_updateAllBuiltThenWhere_rebuildsConditionalUpdate() {
        val updateDate = LocalDateTime.parse("2026-07-22T09:00:00")
        val entity =
            RuntimeTestAllEntityUpdate(
                name = "更新後",
                address = "愛知県豊田市",
                updateDate = updateDate,
            )
        val update = Update(RuntimeTestAllEntityUpdate::class)
            .set(entity)
            .updateAll()
        assertEquals(
            "update TEST_ALL_ENTITY as TEST_ALL_ENTITY " +
                    "set NAME = ?, ADDRESS = ?, UPDATE_DATE = ?",
            update.build(),
        )
        update.where {
            RuntimeTestAllEntityUpdate::name eq "更新前"
        }
        assertEquals(
            "update TEST_ALL_ENTITY as TEST_ALL_ENTITY " +
                    "set NAME = ?, ADDRESS = ?, UPDATE_DATE = ? " +
                    "where TEST_ALL_ENTITY.NAME = ?",
            update.build(),
        )
        assertEquals(
            listOf("更新後", "愛知県豊田市", updateDate, "更新前"),
            update.bindValues,
        )
    }

    /**
     * 同じUpdateを複数回buildしても、
     * SET用バインド値が重複しないことを検証する。
     * @author Masahiro Inoue
     * @since 2026-07-22
     */
    @Test
    fun testBuild_updateAll_buildTwice_doesNotDuplicateBindValues() {
        val updateDate = LocalDateTime.parse("2026-07-22T09:00:00")
        val entity =
            RuntimeTestAllEntityUpdate(
                name = "更新後",
                address = "愛知県豊田市",
                updateDate = updateDate,
            )
        val update = Update(RuntimeTestAllEntityUpdate::class)
            .set(entity)
            .updateAll()
        val firstQuery = update.build()
        val firstBindValues = update.bindValues
        val secondQuery = update.build()
        val secondBindValues = update.bindValues
        assertEquals(firstQuery, secondQuery)
        assertEquals(
            listOf("更新後", "愛知県豊田市", updateDate),
            firstBindValues,
        )
        assertEquals(
            firstBindValues,
            secondBindValues,
        )
    }

    /**
     * updateAll指定時もJOIN条件のバインド値は残し、
     * WHERE条件のバインド値だけ除外することを検証する。
     * @author Masahiro Inoue
     * @since 2026-07-22
     */
    @Test
    fun testBuild_updateAllWithJoin_keepsJoinBindValues() {
        val targetTable = TableRef(RuntimeTestAllEntityUpdate::class, "A")
        val fromTable = TableRef(TestSelectEntity::class, "X")
        val joinTable = TableRef(TestSelectEntity::class, "Y")
        val update = Update(targetTable)
            .set {
                RuntimeTestAllEntityUpdate::name assign "更新後"
            }
            .from(fromTable)
            .join(INNER, joinTable) {
                joinTable[TestSelectEntity::id] eq 50
            }
            .where {
                targetTable[RuntimeTestAllEntityUpdate::name] eq "更新前"
            }
            .updateAll()
        assertEquals(
            "update TEST_ALL_ENTITY as A " +
                    "set NAME = ? " +
                    "from TEST_SELECT_ENTITY X " +
                    "inner join TEST_SELECT_ENTITY Y on Y.ID = ?",
            update.build(),
        )
        assertEquals(
            listOf("更新後", 50),
            update.bindValues,
        )
    }

    /**
     * updateAllを指定しても、SET句が未指定なら例外になることを検証する。
     * @author Masahiro Inoue
     * @since 2026-07-22
     */
    @Test
    fun testBuild_updateAllWithoutSet_throwsIllegalArgumentException() {
        val update = Update(RuntimeTestAllEntityUpdate::class)
            .updateAll()
        val actual = assertThrows(IllegalArgumentException::class.java) {
            update.build()
        }
        assertEquals(AE00014, actual.message)
    }
}