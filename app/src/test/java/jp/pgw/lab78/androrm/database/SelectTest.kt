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
        SELECT = Select(TestSelectEntityWithAlias::class)
                    .join(Select.JoinType.LEFT, TestSelectEntity::class
                            ,TestSelectEntityWithAlias::id,ComparisonOperator.EQUALS,TestSelectEntity::id)
                    .where {
                        and{
                            condition(TestSelectEntity::name, ComparisonOperator.LIKE, "kawanagare%")
                            condition(TestSelectEntity::id, ComparisonOperator.EQUALS, "0078" )
                        }
                    }
        println(SELECT.build())
        SELECT = Select(TestSelectEntityWithAlias::class)
                    .where {condition(TestSelectEntity::name,ComparisonOperator.LIKE,"%kawanagare")}
                    .join(Select.JoinType.LEFT, TestSelectEntity::class
                            ,TestSelectEntityWithAlias::id,ComparisonOperator.EQUALS,TestSelectEntity::id)
        println(SELECT.build())
        SELECT = Select(TestSelectEntityWithAlias::class)
            .where {
                and {
                    condition(TestSelectEntityWithAlias::name, ComparisonOperator.EQUALS, "川流")
                    condition(TestSelectEntityWithAlias::address, ComparisonOperator.LIKE, "%Shinjuku%")
                }
                or {
                    condition(TestSelectEntityWithAlias::id, ComparisonOperator.GREATER_THAN, 100)
                    condition(TestSelectEntityWithAlias::id, ComparisonOperator.LESS_THAN, 10)
                }
            }
        println(SELECT.build())

    }
}