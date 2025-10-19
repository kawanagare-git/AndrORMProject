package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.database.Select.JoinType.*
import jp.pgw.lab78.androrm.database.entities.define.DepartmentEntity
import jp.pgw.lab78.androrm.database.entities.define.EmployeeEntity
import jp.pgw.lab78.androrm.database.entities.define.TestAllEntity
import jp.pgw.lab78.androrm.database.entities.select.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import jp.pgw.lab78.androrm.database.entities.select.EmployeeEntity as EmployeeEntityJoined

class SelectTest {

    companion object {
        @JvmStatic
        lateinit var SELECT: Select<*>

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
                LEFT, TestSelectEntity::class, {
                    TestSelectEntityWithAlias::id eq TestSelectEntity::id
                })
            .where {
                or {
                    TestSelectEntity::name like "kawanagare%"
                    condition("name like '川流%'")
                }
            }
        println(SELECT.build())
        SELECT = Select(TestSelectEntityWithAlias::class)
            .where {
                and {
                    TestSelectEntityWithAlias::name eq "川流"
                    TestSelectEntityWithAlias::address like "%Shinjuku%"
                }
                or {
                    TestSelectEntityWithAlias::id gt 100
                    TestSelectEntityWithAlias::id le 10
                }
            }
        println(SELECT.build())
        SELECT = Select(TestSelectEntityWithAlias::class)
            .join(INNER, TestSelectEntity::class, {
                and {
                    TestSelectEntityWithAlias::id eq TestSelectEntity::id
                    TestSelectEntityWithAlias::name eq TestSelectEntity::name
                }
            })
            .join(LEFT, TestAllEntity::class, {
                TestSelectEntityWithAlias::birthday ge TestAllEntity::birthday
            })
            .where {
                and {
                    TestSelectEntityWithAlias::name eq "川流"
                    TestSelectEntity::address like "%Shinjuku%"
                }
                or {
                    TestAllEntity::insertDateTime gt
                            LocalDateTime.parse("2025-01-01T00:00:00.000")
                    TestAllEntity::updateDate lt LocalDate.parse("2025-07-01")
                }
            }
        println(SELECT.build())
    }

    @Test
    fun build2() {
        // 最終TEST
        SELECT = Select(TestSelectEntityWithAlias::class)
            .join(INNER, TestSelectEntity::class, {
                and {
                    TestSelectEntityWithAlias::id eq TestSelectEntity::id
                    TestSelectEntityWithAlias::name eq TestSelectEntity::name
                }
            })
            .join(LEFT, TestAllEntity::class, {
                TestSelectEntityWithAlias::birthday ge TestAllEntity::birthday
            })
            .where {
                and {
                    TestSelectEntityWithAlias::name eq "川流"
                    TestSelectEntity::address like "%Shinjuku%"
                }
                or {
                    TestAllEntity::insertDateTime gt LocalDateTime.parse("2025-01-01T00:00:00.000")
                    TestAllEntity::updateDate lt LocalDate.parse("2025-07-01")
                }
            }
            .having {
                TestSelectEntity::newestBirthday lt LocalDate.parse("2020-01-01")
            }
        println(SELECT.build())
        // 新エンティティクラス
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(
                LEFT, EmployeeEntityJoined::class,
                {
                    EmployeeEntityIdSelection::employeeId eq EmployeeEntityJoined::employeeId
                })
            .order {
                column(EmployeeEntityIdSelection::employeeId, true)
                column(EmployeeEntity::name)
            }
        println(SELECT.build())
        SELECT = Select(DepartmentEntityInfo::class)
            .order {
                column(DepartmentEntityInfo::employeeId, true)
                column(DepartmentEntityInfo::section)
            }
        println(SELECT.build())
    }

    @Test
    fun build3() {
        // 最終TEST
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(INNER, DepartmentEntityInfo::class, {
                EmployeeEntityIdSelection::employeeId eq DepartmentEntityInfo::employeeId
            })
            .order { column(DepartmentEntityInfo::department) }
        println(SELECT.build())
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(INNER, DepartmentEntityInfo::class, {
                EmployeeEntityIdSelection::employeeId eq DepartmentEntityInfo::employeeId
            })
            .where({
                EmployeeEntityIdSelection::employeeId eq EmployeeEntity::employeeId
            })
            .order { column(DepartmentEntityInfo::department) }
        println(SELECT.build())
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(INNER, DepartmentEntityInfo::class, {
                EmployeeEntityIdSelection::employeeId eq DepartmentEntityInfo::employeeId
            })
            .where({
                EmployeeEntityIdSelection::employeeId eq DepartmentEntityInfo::employeeId
            })
            .order { column(DepartmentEntityInfo::department) }
        println(SELECT.build())
    }

    @Test
    fun build4() {
        // 最終TEST
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(INNER, DepartmentEntityInfo::class, {
                EmployeeEntityIdSelection::employeeId eq DepartmentEntityInfo::employeeId
                EmployeeEntityIdSelection::employeeId eq DepartmentEntityInfo::employeeId
            })
            .order { column(DepartmentEntityInfo::department) }
        println(SELECT.build())
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(INNER, DepartmentEntityInfo::class, {
                EmployeeEntityIdSelection::employeeId eq DepartmentEntity::employeeId
            })
            .join(LEFT, SalaryEntitySelective::class, {
                EmployeeEntityIdSelection::employeeId eq SalaryEntitySelective::employeeId
            })
            .where({
                DepartmentEntityInfo::department eq DepartmentEntity::department
                or {
                    DepartmentEntityInfo::section eq "10"
                    DepartmentEntityInfo::section eq "20"
                }
            })
            .having {
                SalaryEntitySelective::maxGross gt 100000
            }
            .order { column(DepartmentEntityInfo::department) }
        println(SELECT.build())
    }

    @Test
    fun build5() {
        SELECT = Select(TestSelectEntityWithAlias::class)
            .join(
                LEFT, TestSelectEntity::class, {
                    TestSelectEntityWithAlias::id eq TestSelectEntity::id
                    TestSelectEntity::name like "%kawanagare"
                })
            .where {
                or {
                    TestSelectEntity::name like "%kawanagare"
                    TestSelectEntityWithAlias::address like "%Shinjuku%"
                }
            }
        println(SELECT.build())
    }
}
