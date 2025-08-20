package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.database.Select.JoinType.INNER
import jp.pgw.lab78.androrm.database.Select.JoinType.LEFT
import jp.pgw.lab78.androrm.database.condition.HavingConditionBuilder
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator.EQ
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator.EQUALS
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator.GREATER_THAN
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator.GREATER_THAN_OR_EQUALS
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator.GT
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator.LESS_THAN
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator.LIKE
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator.LT
import jp.pgw.lab78.androrm.database.entities.condition.DepartmentEntityCondition
import jp.pgw.lab78.androrm.database.entities.condition.EmployeeEntityCondition
import jp.pgw.lab78.androrm.database.entities.define.EmployeeEntity
import jp.pgw.lab78.androrm.database.entities.define.TestAllEntity
import jp.pgw.lab78.androrm.database.entities.select.DepartmentEntityInfo
import jp.pgw.lab78.androrm.database.entities.select.EmployeeEntityIdSelection
import jp.pgw.lab78.androrm.database.entities.select.SalaryEntitySelective
import jp.pgw.lab78.androrm.database.entities.select.TestSelectEntity
import jp.pgw.lab78.androrm.database.entities.select.TestSelectEntityWithAlias
import jp.pgw.lab78.androrm.database.function.AggregateFunction.MAX
import jp.pgw.lab78.androrm.database.function.AggregateFunction.SUM
import jp.pgw.lab78.androrm.log.AopLogger
import jp.pgw.lab78.androrm.log.LogInitializer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import jp.pgw.lab78.androrm.database.entities.select.EmployeeEntity as EmployeeEntityJoined

class SelectTest {

    companion object {
        @JvmStatic
        lateinit var SELECT: Select<*>
        @JvmStatic
        lateinit var aop: AopLogger

        @JvmStatic
        @BeforeAll
        fun setup() {
            LogInitializer.initLogDir()
            val aop = AopLogger()
        }
    }

    @Test
    fun build0() {
        SELECT = Select(TestSelectEntity::class)
        println(SELECT.build())
        SELECT = Select(TestSelectEntityWithAlias::class)
        println(SELECT.build())
        SELECT = Select(TestSelectEntity::class, isDistinct = true)
        println(SELECT.build())
    }

    @Test
    fun build1() {
        SELECT = Select(TestSelectEntityWithAlias::class)
            .join(
                LEFT
                , TestSelectEntity::class
                , {
                    condition(TestSelectEntityWithAlias::id, EQUALS, TestSelectEntity::id)
                })
            .where {
                or {
                    condition(TestSelectEntity::name, LIKE, "kawanagare%")
                    condition("name like '川流%'")
                }
            }
        println(SELECT.build())
        SELECT = Select(TestSelectEntityWithAlias::class)
            .where {
                and {
                    condition(TestSelectEntityWithAlias::name, EQUALS, "川流")
                    condition(TestSelectEntityWithAlias::address, LIKE, "%Shinjuku%")
                }
                or {
                    condition(TestSelectEntityWithAlias::id, GT, 100)
                    condition(TestSelectEntityWithAlias::id, LT, 10)
                }
            }
        println(SELECT.build())
        SELECT = Select(TestSelectEntityWithAlias::class)
            .join(INNER, TestSelectEntity::class,{
                and{
                    condition(TestSelectEntityWithAlias::id, EQUALS, TestSelectEntity::id)
                    condition(TestSelectEntityWithAlias::name, EQUALS, TestSelectEntity::name)
                }
            })
            .join(LEFT, TestAllEntity::class,{
                condition(TestSelectEntityWithAlias::birthday, GREATER_THAN_OR_EQUALS, TestAllEntity::birthday)
            })
            .where {
                and {
                    condition(TestSelectEntityWithAlias::name, EQUALS, "川流")
                    condition(TestSelectEntity::address, LIKE, "%Shinjuku%")
                }
                or {
                    condition(
                        TestAllEntity::insertDateTime
                        , GREATER_THAN
                        , LocalDateTime.parse("2025-01-01T00:00:00.000"))
                    condition(
                        TestAllEntity::updateDate
                        , LESS_THAN
                        , LocalDate.parse("2025-07-01"))
                }
            }
        println(SELECT.build())
    }

    @Test
    fun build2() {
        // 最終TEST
        SELECT = Select(TestSelectEntityWithAlias::class)
            .join(INNER, TestSelectEntity::class,{
                and{
                    condition(TestSelectEntityWithAlias::id, EQUALS, TestSelectEntity::id)
                    condition(TestSelectEntityWithAlias::name, EQUALS, TestSelectEntity::name)
                }
            })
            .join(LEFT, TestAllEntity::class,{
                condition(TestSelectEntityWithAlias::birthday, GREATER_THAN_OR_EQUALS, TestAllEntity::birthday)
            })
            .where {
                and {
                    condition(TestSelectEntityWithAlias::name, EQUALS, "川流")
                    condition(TestSelectEntity::address, LIKE, "%Shinjuku%")
                }
                or {
                    condition(
                        TestAllEntity::insertDateTime
                        , GREATER_THAN
                        , LocalDateTime.parse("2025-01-01T00:00:00.000"))
                    condition(
                        TestAllEntity::updateDate
                        , LESS_THAN
                        , LocalDate.parse("2025-07-01"))
                }
            }
            .having {
                condition(
                    MAX, TestSelectEntity::birthday
                    , LESS_THAN, LocalDate.parse("2025-07-01"))
            }
        println(SELECT.build())
        // 新エンティティクラス
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(
                LEFT, EmployeeEntityJoined::class,
                {condition(EmployeeEntityIdSelection::employeeId,
                            EQUALS,
                            EmployeeEntityJoined::employeeId)})
            .order{
                column(EmployeeEntityIdSelection::employeeId,true)
                column(EmployeeEntity::name)
            }
        println(SELECT.build())
        SELECT = Select(DepartmentEntityInfo::class)
            .order{
                column(DepartmentEntityInfo::employeeId,true)
                column(DepartmentEntityInfo::section)
            }
        println(SELECT.build())
    }

    @Test
    fun build3() {
        // 最終TEST
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(INNER, DepartmentEntityInfo::class,{
                condition(EmployeeEntityIdSelection::employeeId, EQUALS, DepartmentEntityInfo::employeeId)
            })
            .order{column(DepartmentEntityInfo::department)}
        println(SELECT.build())
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(INNER, DepartmentEntityInfo::class,{
                condition(EmployeeEntityIdSelection::employeeId, EQUALS, DepartmentEntityInfo::employeeId)
            })
            .where({condition(EmployeeEntityIdSelection::employeeId,EQUALS,EmployeeEntityCondition::employeeId)})
            .order{column(DepartmentEntityInfo::department)}
        println(SELECT.build())
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(INNER, DepartmentEntityInfo::class,{
                condition(EmployeeEntityIdSelection::employeeId, EQUALS, DepartmentEntityInfo::employeeId)
            })
            .where({condition(EmployeeEntityIdSelection::employeeId,
                EQUALS,
                DepartmentEntityInfo::employeeId)})
            .order{column(DepartmentEntityInfo::department)}
        println(SELECT.build())
    }

    @Test
    fun build4() {
        // 最終TEST
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(INNER, DepartmentEntityInfo::class,{
                condition(EmployeeEntityIdSelection::employeeId, EQ, DepartmentEntityInfo::employeeId)
                condition(EmployeeEntityIdSelection::employeeId, EQ, DepartmentEntityInfo::employeeId)
            })
            .order{column(DepartmentEntityInfo::department)}
        println(SELECT.build())
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(INNER, DepartmentEntityInfo::class,{
                condition(EmployeeEntityIdSelection::employeeId, EQ, DepartmentEntityCondition::employeeId)
            })
            .join(LEFT, SalaryEntitySelective::class,{
                condition(EmployeeEntityIdSelection::employeeId, EQ, SalaryEntitySelective::employeeId)
            })
            .where({
                condition(DepartmentEntityInfo::department,EQ, DepartmentEntityCondition::department)
                    or{
                        condition(DepartmentEntityInfo::section,EQ, "10")
                        condition(DepartmentEntityInfo::section,EQ, "20")
                    }
            })
            .having(HavingConditionBuilder().apply {
                condition(SUM,SalaryEntitySelective::gross,GT,100000)
            })
            .order{column(DepartmentEntityInfo::department)}
        println(SELECT.build())
    }

    @Test
    fun build5() {
        SELECT = Select(TestSelectEntityWithAlias::class)
            .join(
                LEFT
                , TestSelectEntity::class
                , {
                    condition(TestSelectEntityWithAlias::id, EQUALS, TestSelectEntity::id)
                    condition(TestSelectEntity::name, LIKE,"%kawanagare")
                })
            .where {
                or {
                    condition(TestSelectEntity::name, LIKE,"%kawanagare")
                    condition(TestSelectEntityWithAlias::address, LIKE, "%Shinjuku%")
                }
            }
        println(SELECT.build())
    }
}
