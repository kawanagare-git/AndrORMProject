package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator.EQUALS
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator.GREATER_THAN
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator.GREATER_THAN_OR_EQUALS
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator.LESS_THAN
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator.LIKE
import jp.pgw.lab78.androrm.database.entities.condition.DepartmentEntityCondition
import jp.pgw.lab78.androrm.database.entities.condition.EmployeeEntityCondition
import jp.pgw.lab78.androrm.database.entities.define.EmployeeEntity
import jp.pgw.lab78.androrm.database.entities.define.TestAllEntity
import jp.pgw.lab78.androrm.database.entities.select.DepartmentEntityInfo
import jp.pgw.lab78.androrm.database.entities.select.EmployeeEntityIdSelection
import jp.pgw.lab78.androrm.database.entities.select.TestSelectEntity
import jp.pgw.lab78.androrm.database.entities.select.TestSelectEntityWithAlias
import jp.pgw.lab78.androrm.database.function.AggregateFunction
import jp.pgw.lab78.androrm.log.AopLogger
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import jp.pgw.lab78.androrm.database.entities.select.EmployeeEntity as EmployeeEntityJoined

class SelectTest {

    companion object {
        @JvmStatic
        lateinit var SELECT: Select<*>

        val aop = AopLogger()
        @JvmStatic
        @BeforeAll
        fun setup(): Unit {

            // 内部ストレージの /files/logs ディレクトリを取得
            val logDir = File("AndrORM", "logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            // logback 用の LOG_DIR プロパティを設定
            System.setProperty("LOG_DIR", logDir.absolutePath)
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
                Select.JoinType.LEFT
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
            .where {condition(TestSelectEntity::name, LIKE,"%kawanagare")}
            .join(Select.JoinType.LEFT, TestSelectEntity::class,{
                and{
                    condition(TestSelectEntityWithAlias::id, EQUALS, TestSelectEntity::id)
                    condition(TestSelectEntityWithAlias::name, EQUALS, TestSelectEntity::name)
                }
            })
        println(SELECT.build())
        SELECT = Select(TestSelectEntityWithAlias::class)
            .where {
                and {
                    condition(TestSelectEntityWithAlias::name, EQUALS, "川流")
                    condition(TestSelectEntityWithAlias::address, LIKE, "%Shinjuku%")
                }
                or {
                    condition(TestSelectEntityWithAlias::id, GREATER_THAN, 100)
                    condition(TestSelectEntityWithAlias::id, LESS_THAN, 10)
                }
            }
        println(SELECT.build())
        SELECT = Select(TestSelectEntityWithAlias::class)
            .join(Select.JoinType.INNER, TestSelectEntity::class,{
                and{
                    condition(TestSelectEntityWithAlias::id, EQUALS, TestSelectEntity::id)
                    condition(TestSelectEntityWithAlias::name, EQUALS, TestSelectEntity::name)
                }
            })
            .join(Select.JoinType.LEFT, TestAllEntity::class,{
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
            .join(Select.JoinType.INNER, TestSelectEntity::class,{
                and{
                    condition(TestSelectEntityWithAlias::id, EQUALS, TestSelectEntity::id)
                    condition(TestSelectEntityWithAlias::name, EQUALS, TestSelectEntity::name)
                }
            })
            .join(Select.JoinType.LEFT, TestAllEntity::class,{
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
                    AggregateFunction.SUM, TestSelectEntity::birthday
                    , LESS_THAN, LocalDate.parse("2025-07-01"))
            }
        println(SELECT.build())
        // 新エンティティクラス
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(Select.JoinType.LEFT, EmployeeEntityJoined::class,
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
            .join(Select.JoinType.INNER, DepartmentEntityInfo::class,{
                condition(EmployeeEntityIdSelection::employeeId, EQUALS, DepartmentEntityInfo::employeeId)
            })
            .order{column(DepartmentEntityInfo::department)}
        println(SELECT.build())
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(Select.JoinType.INNER, DepartmentEntityInfo::class,{
                condition(EmployeeEntityIdSelection::employeeId, EQUALS, DepartmentEntityInfo::employeeId)
            })
            .where({condition(EmployeeEntityIdSelection::employeeId,EQUALS,EmployeeEntityCondition::employeeId)})
            .order{column(DepartmentEntityInfo::department)}
        println(SELECT.build())
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(Select.JoinType.INNER, DepartmentEntityInfo::class,{
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
            .join(Select.JoinType.INNER, DepartmentEntityInfo::class,{
                condition(EmployeeEntityIdSelection::employeeId, EQUALS, DepartmentEntityInfo::employeeId)
                condition(EmployeeEntityIdSelection::employeeId, EQUALS, DepartmentEntityInfo::employeeId)
            })
            .order{column(DepartmentEntityInfo::department)}
        println(SELECT.build())
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(Select.JoinType.INNER, DepartmentEntityInfo::class,{
                condition(EmployeeEntityIdSelection::employeeId, EQUALS, DepartmentEntityInfo::employeeId)
                condition(EmployeeEntityIdSelection::employeeId, EQUALS, DepartmentEntityCondition::employeeId)
            })
            .where({
                condition(DepartmentEntityInfo::department,EQUALS, DepartmentEntityCondition::department)
                    or{
                        condition(DepartmentEntityInfo::section,EQUALS, "10")
                        condition(DepartmentEntityInfo::section,EQUALS, "20")
                    }
            })
            .order{column(DepartmentEntityInfo::department)}
        println(SELECT.build())
    }
}
