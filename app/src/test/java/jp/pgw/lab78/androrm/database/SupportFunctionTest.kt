package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.annotation.Table
import jp.pgw.lab78.androrm.database.entities.define.TestAllEntity
import jp.pgw.lab78.androrm.database.entities.insert.TestInsertEntity
import jp.pgw.lab78.androrm.database.entities.select.TestSelectEntity
import jp.pgw.lab78.androrm.database.entities.select.TestSelectEntityWithAlias
import jp.pgw.lab78.androrm.database.interfaces.orm.entity.query.Entity
import jp.pgw.lab78.androrm.database.interfaces.orm.entity.query.SelectEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

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

    companion object {
        @JvmStatic
        fun propertyProvider(): List<Pair<KProperty1<out Entity, *>, KClass<out Entity>>> = listOf(
            TestAllEntity::id to TestAllEntity::class,
            TestSelectEntity::name to TestSelectEntity::class,
            TestAllEntity::insertDateTime to TestAllEntity::class,
            TestInsertEntity::birthday to TestInsertEntity::class,
            TestSelectEntityWithAlias::address to TestSelectEntityWithAlias::class
        )
    }

    @ParameterizedTest
    @MethodSource("propertyProvider")
    fun `extractClassFromProperty should return correct KClass`(
        testData: Pair<KProperty1<out Entity, *>, KClass<out Entity>>
    ) {
        val (property, expectedClass) = testData

        // 安全キャストするために明示的な関数呼び出し（ジェネリクス警告防止）
        @Suppress("UNCHECKED_CAST")
        val actual = SupportFunction.extractClassFromProperty(property as KProperty1<Entity, *>)

        assertEquals(expectedClass, actual)
    }}
