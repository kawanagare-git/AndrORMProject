package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.database.entities.define.TestAllEntity
import jp.pgw.lab78.androrm.database.entities.insert.TestInsertEntity
import jp.pgw.lab78.androrm.database.entities.select.TestSelectEntity
import jp.pgw.lab78.androrm.database.entities.select.TestSelectEntityWithAlias
import jp.pgw.lab78.androrm.database.utility.EntityManager.extractClassFromProperty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class EntityTest {

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
        val actual = property.extractClassFromProperty()

        assertEquals(expectedClass, actual)
    }
}
