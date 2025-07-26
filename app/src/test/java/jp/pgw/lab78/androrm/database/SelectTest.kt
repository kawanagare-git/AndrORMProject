package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator
import jp.pgw.lab78.androrm.database.entities.define.TestAllEntity
import jp.pgw.lab78.androrm.database.entities.select.TestSelectEntity
import jp.pgw.lab78.androrm.database.entities.select.TestSelectEntityWithAlias
import jp.pgw.lab78.androrm.database.function.AggregateFunction
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

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
                    .join(
                        Select.JoinType.LEFT
                            , TestSelectEntity::class
                            , {
                                condition(TestSelectEntityWithAlias::id, ComparisonOperator.EQUALS, TestSelectEntity::id)
                            })
                    .where {
                            condition(TestSelectEntity::name, ComparisonOperator.LIKE, "kawanagare%")
                    }
        println(SELECT.build())
        SELECT = Select(TestSelectEntityWithAlias::class)
                    .where {condition(TestSelectEntity::name, ComparisonOperator.LIKE,"%kawanagare")}
                    .join(Select.JoinType.LEFT, TestSelectEntity::class,{
                            and{
                                condition(TestSelectEntityWithAlias::id, ComparisonOperator.EQUALS, TestSelectEntity::id)
                                condition(TestSelectEntityWithAlias::name, ComparisonOperator.EQUALS, TestSelectEntity::name)
                            }
                    })
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
        SELECT = Select(TestSelectEntityWithAlias::class)
            .join(Select.JoinType.INNER, TestSelectEntity::class,{
                and{
                    condition(TestSelectEntityWithAlias::id, ComparisonOperator.EQUALS, TestSelectEntity::id)
                    condition(TestSelectEntityWithAlias::name, ComparisonOperator.EQUALS, TestSelectEntity::name)
                }
            })
            .join(Select.JoinType.LEFT, TestAllEntity::class,{
                    condition(TestSelectEntityWithAlias::birthday, ComparisonOperator.GREATER_THAN_OR_EQUALS, TestAllEntity::birthday)
            })
            .where {
                and {
                    condition(TestSelectEntityWithAlias::name, ComparisonOperator.EQUALS, "川流")
                    condition(TestSelectEntity::address, ComparisonOperator.LIKE, "%Shinjuku%")
                }
                or {
                    condition(
                        TestAllEntity::insertDateTime
                                , ComparisonOperator.GREATER_THAN
                                , LocalDateTime.parse("2025-01-01T00:00:00.000"))
                    condition(
                        TestAllEntity::updateDate
                                , ComparisonOperator.LESS_THAN
                                , LocalDate.parse("2025-07-01"))
                }
            }
        println(SELECT.build())
        // 最終TEST
        SELECT = Select(TestSelectEntityWithAlias::class)
            .join(Select.JoinType.INNER, TestSelectEntity::class,{
                and{
                    condition(TestSelectEntityWithAlias::id, ComparisonOperator.EQUALS, TestSelectEntity::id)
                    condition(TestSelectEntityWithAlias::name, ComparisonOperator.EQUALS, TestSelectEntity::name)
                }
            })
            .join(Select.JoinType.LEFT, TestAllEntity::class,{
                condition(TestSelectEntityWithAlias::birthday, ComparisonOperator.GREATER_THAN_OR_EQUALS, TestAllEntity::birthday)
            })
            .where {
                and {
                    condition(TestSelectEntityWithAlias::name, ComparisonOperator.EQUALS, "川流")
                    condition(TestSelectEntity::address, ComparisonOperator.LIKE, "%Shinjuku%")
                }
                or {
                    condition(
                        TestAllEntity::insertDateTime
                        , ComparisonOperator.GREATER_THAN
                        , LocalDateTime.parse("2025-01-01T00:00:00.000"))
                    condition(
                        TestAllEntity::updateDate
                        , ComparisonOperator.LESS_THAN
                        , LocalDate.parse("2025-07-01"))
                }
            }
            .having {
                condition(
                    AggregateFunction.SUM, TestSelectEntity::birthday
                            , ComparisonOperator.LESS_THAN, LocalDate.parse("2025-07-01"))
            }
        println(SELECT.build())
        // 新エンティティクラス
//        SELECT = Select(EmployeeEntity::class)

    }
}