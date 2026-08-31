package jp.pgw.lab78.androrm.common.database

import jp.pgw.lab78.androrm.common.database.SupportFunction.getRelationAlias
import jp.pgw.lab78.androrm.common.database.SupportFunction.getRelationName
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.annotation.View
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * ## SELECT 元名称解決テスト
 * @author Masahiro Inoue
 * @since 2026-08-31
 */
class ViewSupportFunctionTest {
    /** @View の name と alias を解決できることを確認する。 */
    @Test
    fun resolvesViewRelationNameAndAlias() {
        assertEquals("ACTIVE_EMPLOYEE", ViewEntity::class.getRelationName())
        assertEquals("AV", ViewEntity::class.getRelationAlias())
    }

    /** @Table の既存名称解決を維持することを確認する。 */
    @Test
    fun resolvesTableRelationNameAndAlias() {
        assertEquals("EMPLOYEE", TableEntity::class.getRelationName())
        assertEquals("E", TableEntity::class.getRelationAlias())
    }

    /** @Table と @View の併用を拒否する。 */
    @Test
    fun rejectsTableAndViewCombination() {
        assertThrows<IllegalArgumentException> { InvalidEntity::class.getRelationName() }
    }

    @View(name = "ACTIVE_EMPLOYEE", alias = "AV")
    private class ViewEntity : Entity

    @Table(name = "EMPLOYEE", alias = "E")
    private class TableEntity : Entity

    @Table(name = "INVALID")
    @View(name = "INVALID")
    private class InvalidEntity : Entity
}
