package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.database.entities.select.TestSelectEntity
import jp.pgw.lab78.androrm.database.entities.select.TestSelectEntityWithAlias
import org.junit.jupiter.api.Test

class SelectTest {

    companion object {
        @JvmStatic
        lateinit var SELECT: Select<*>

    }

    @Test
    fun build() {
        SELECT = Select(TestSelectEntity::class)
        println(SELECT.build())
        SELECT = Select(TestSelectEntityWithAlias::class)
        println(SELECT.build())
        SELECT = Select(TestSelectEntity::class, isDistinct = true)
        println(SELECT.build())
        SELECT = Select(TestSelectEntityWithAlias::class).join(Select.JoinType.LEFT, TestSelectEntity::class,TestSelectEntityWithAlias::id,
            ComparisonOperator.EQUALS,TestSelectEntity::id)
        println(SELECT.build())
    }
}