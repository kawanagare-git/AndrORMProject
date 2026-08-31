package jp.pgw.lab78.androrm.database.queryparts

import jp.pgw.lab78.androrm.common.MessageConstants.AE00010
import jp.pgw.lab78.androrm.database.entities.delete.TestAllEntityDelete
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * WhereClauseDelegateの動作を検証するテストクラス。
 * @author Masahiro Inoue
 * @since 2026-05-27
 */
class WhereClauseDelegateTest {

    /**
     * 句デリゲートのテストで所有者として使用する補助クラス。
     * @author Masahiro Inoue
     * @since 2026-05-27
     */
    private class TestOwner

    /**
     * 「testBuildClause_empty」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-27
     */
    @Test
    fun testBuildClause_empty() {
        val owner = TestOwner()
        val target = WhereClauseDelegate(
            owner = owner,
            ownerName = "TestOwner",
        )

        assertFalse(target.hasCondition)
        assertEquals(emptyList<Any?>(), target.bindValues)
        assertEquals("", target.buildCondition())
        assertEquals("", target.buildClause())
    }

    /**
     * 「testWhere_buildConditionAndBindValues」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-27
     */
    @Test
    fun testWhere_buildConditionAndBindValues() {
        var changed = false
        val owner = TestOwner()
        val target = WhereClauseDelegate(
            owner = owner,
            ownerName = "TestOwner",
        ) {
            changed = true
        }

        val actualOwner = target.where {
            TestAllEntityDelete::address like "長野県%"
        }

        assertSame(owner, actualOwner)
        assertTrue(changed)
        assertTrue(target.hasCondition)
        assertEquals(1, target.conditions.size)
        assertEquals(
            "TEST_ALL_ENTITY.ADDRESS like ?",
            target.buildCondition(),
        )
        assertEquals(
            "where TEST_ALL_ENTITY.ADDRESS like ?",
            target.buildClause(),
        )
        assertEquals(
            listOf("長野県%"),
            target.bindValues,
        )
    }

    /**
     * 「testWhere_multiConditions」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-27
     */
    @Test
    fun testWhere_multiConditions() {
        val owner = TestOwner()
        val target = WhereClauseDelegate(
            owner = owner,
            ownerName = "TestOwner",
        )

        target.where {
            TestAllEntityDelete::id eq 10
            TestAllEntityDelete::address like "愛知県%"
        }

        assertEquals(
            "TEST_ALL_ENTITY.ID = ? and TEST_ALL_ENTITY.ADDRESS like ?",
            target.buildCondition(),
        )
        assertEquals(
            "where TEST_ALL_ENTITY.ID = ? and TEST_ALL_ENTITY.ADDRESS like ?",
            target.buildClause(),
        )
        assertEquals(
            listOf(10, "愛知県%"),
            target.bindValues,
        )
    }

    /**
     * 「testWhere_twice_throwsIllegalStateException」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-27
     */
    @Test
    fun testWhere_twice_throwsIllegalStateException() {
        val owner = TestOwner()
        val target = WhereClauseDelegate(
            owner = owner,
            ownerName = "TestOwner",
        )

        target.where {
            TestAllEntityDelete::id eq 10
        }

        val actual = assertThrows(IllegalStateException::class.java) {
            target.where {
                TestAllEntityDelete::address like "愛知県%"
            }
        }

        assertEquals(
            AE00010.format("TestOwner", "where"),
            actual.message,
        )
    }
}