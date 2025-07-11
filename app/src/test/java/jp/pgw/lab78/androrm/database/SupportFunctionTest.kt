package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.annotation.Table
import jp.pgw.lab78.androrm.database.interfaces.SelectEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

class SupportFunctionTest {

    @Test
    fun `splitTableNameAndAlias should split correctly`() {
        val (table, alias) = SupportFunction.splitTableNameAndAlias("users u")
        assertEquals("users", table)
        assertEquals("u", alias)
    }

    @Test
    fun `splitTableNameAndAlias should return empty alias when none`() {
        val (table, alias) = SupportFunction.splitTableNameAndAlias("products")
        assertEquals("products", table)
        assertEquals("", alias)
    }

    @Test
    fun `getAlias should return alias from annotation`() {
        // テスト用 Entity を登録
        val entityClass = AliasTestEntity::class
        SupportFunction.entityDefinitionMap[entityClass] =
            SupportFunction.EntityDefinitionManager("test_table t", mutableMapOf())

        val alias = SupportFunction.getAlias(entityClass)
        assertEquals("t", alias)
    }

    @Test
    fun `getAlias should fallback to class name when no alias`() {
        // テスト用 Entity を登録（alias なし）
        val entityClass = FallbackEntity::class
        SupportFunction.entityDefinitionMap[entityClass] =
            SupportFunction.EntityDefinitionManager("fallback_entity", mutableMapOf())

        val alias = SupportFunction.getAlias(entityClass)
        assertEquals("fallback_entity".uppercase(Locale.ROOT), alias)
    }

    // Dummy entity for alias test
    @Table(name = "test_table", alias = "t")
    data class AliasTestEntity(val id: Int) : SelectEntity

    // Dummy entity without alias
    @Table
    data class FallbackEntity(val id: Int) : SelectEntity
}
