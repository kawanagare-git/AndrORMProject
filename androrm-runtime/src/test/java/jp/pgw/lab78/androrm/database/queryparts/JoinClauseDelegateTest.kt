package jp.pgw.lab78.androrm.database.queryparts

import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.database.entities.select.TestSelectEntity
import jp.pgw.lab78.androrm.database.reference.TableRef
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * JoinClauseDelegateの動作を検証するテストクラス。
 * @author Masahiro Inoue
 * @since 2026-05-27
 */
class JoinClauseDelegateTest {

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
        val target = JoinClauseDelegate<TestOwner, Entity>(
            owner = owner,
        )

        assertEquals(emptyList<String>(), target.clauses)
        assertEquals(emptyList<Any?>(), target.bindValues)
        assertEquals("", target.buildClause())
    }

    /**
     * 「testJoin_tableRef_columnToColumn」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-27
     */
    @Test
    fun testJoin_tableRef_columnToColumn() {
        var changed = false
        val joinedTables = mutableListOf<TableRef<out Entity>>()

        val owner = TestOwner()
        val fromTable = TableRef(TestSelectEntity::class, "X")
        val joinTable = TableRef(TestSelectEntity::class, "Y")

        val target = JoinClauseDelegate<TestOwner, Entity>(
            owner = owner,
            onChanged = {
                changed = true
            },
            onTableJoined = { joinedTable ->
                joinedTables += joinedTable
            },
        )

        val actualOwner = target.join(
            joinType = JoinType.INNER,
            joinedTable = joinTable,
        ) {
            fromTable[TestSelectEntity::id] eq joinTable[TestSelectEntity::id]
        }

        assertSame(owner, actualOwner)
        assertTrue(changed)
        assertEquals(listOf(joinTable), joinedTables)
        assertEquals(
            listOf("inner join TEST_SELECT_ENTITY Y on X.ID = Y.ID"),
            target.clauses,
        )
        assertEquals(
            "inner join TEST_SELECT_ENTITY Y on X.ID = Y.ID",
            target.buildClause(),
        )
        assertEquals(emptyList<Any?>(), target.bindValues)
    }

    /**
     * 「testJoin_tableRef_columnToValue」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-27
     */
    @Test
    fun testJoin_tableRef_columnToValue() {
        val owner = TestOwner()
        val joinTable = TableRef(TestSelectEntity::class, "Y")

        val target = JoinClauseDelegate<TestOwner, Entity>(
            owner = owner,
        )

        val actualOwner = target.join(
            joinType = JoinType.LEFT,
            joinedTable = joinTable,
        ) {
            joinTable[TestSelectEntity::id] eq 100
        }

        assertSame(owner, actualOwner)
        assertEquals(
            "left join TEST_SELECT_ENTITY Y on Y.ID = ?",
            target.buildClause(),
        )
        assertEquals(
            listOf(100),
            target.bindValues,
        )
    }

    /**
     * 「testJoin_entityClass_usesDefaultAlias」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-27
     */
    @Test
    fun testJoin_entityClass_usesDefaultAlias() {
        val owner = TestOwner()

        val target = JoinClauseDelegate<TestOwner, Entity>(
            owner = owner,
        )

        val actualOwner = target.join(
            joinType = JoinType.INNER,
            joinedEntity = TestSelectEntity::class,
        ) {
            TestSelectEntity::id eq 100
        }

        assertSame(owner, actualOwner)
        assertEquals(
            "inner join TEST_SELECT_ENTITY TS on TS.ID = ?",
            target.buildClause(),
        )
        assertEquals(
            listOf(100),
            target.bindValues,
        )
    }

    /**
     * 「testJoin_onSyntax」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-27
     */
    @Test
    fun testJoin_onSyntax() {
        val owner = TestOwner()
        val fromTable = TableRef(TestSelectEntity::class, "X")
        val joinTable = TableRef(TestSelectEntity::class, "Y")

        val target = JoinClauseDelegate<TestOwner, Entity>(
            owner = owner,
        )

        val actualOwner = target
            .join(JoinType.LEFT, joinTable)
            .on {
                fromTable[TestSelectEntity::id] eq joinTable[TestSelectEntity::id]
            }

        assertSame(owner, actualOwner)
        assertEquals(
            "left join TEST_SELECT_ENTITY Y on X.ID = Y.ID",
            target.buildClause(),
        )
        assertEquals(emptyList<Any?>(), target.bindValues)
    }

    /**
     * 「testJoin_multiClauses」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-27
     */
    @Test
    fun testJoin_multiClauses() {
        val owner = TestOwner()
        val fromTable = TableRef(TestSelectEntity::class, "X")
        val joinTable1 = TableRef(TestSelectEntity::class, "Y")
        val joinTable2 = TableRef(TestSelectEntity::class, "Z")

        val target = JoinClauseDelegate<TestOwner, Entity>(
            owner = owner,
        )

        target.join(JoinType.INNER, joinTable1) {
            fromTable[TestSelectEntity::id] eq joinTable1[TestSelectEntity::id]
        }

        target.join(JoinType.LEFT, joinTable2) {
            joinTable1[TestSelectEntity::id] eq joinTable2[TestSelectEntity::id]
        }

        assertEquals(
            "inner join TEST_SELECT_ENTITY Y on X.ID = Y.ID " +
                    "left join TEST_SELECT_ENTITY Z on Y.ID = Z.ID",
            target.buildClause(),
        )
        assertEquals(emptyList<Any?>(), target.bindValues)
    }
}