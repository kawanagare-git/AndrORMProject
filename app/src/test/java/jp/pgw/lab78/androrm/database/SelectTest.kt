package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.database.Select.JoinType.*
import jp.pgw.lab78.androrm.database.entities.define.DepartmentEntity
import jp.pgw.lab78.androrm.database.entities.define.EmployeeEntity
import jp.pgw.lab78.androrm.database.entities.define.TestAllEntity
import jp.pgw.lab78.androrm.database.entities.select.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
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
                EmployeeEntityIdSelection::employeeId.asc.nullsFirst
                EmployeeEntityJoined::name.desc
            }
        println(SELECT.build())
        SELECT = Select(DepartmentEntityInfo::class)
            .order {
                DepartmentEntityInfo::employeeId.asc
                DepartmentEntityInfo::section.asc.nullsLast
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
            .order { DepartmentEntityInfo::department.asc }
        println(SELECT.build())
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(INNER, DepartmentEntityInfo::class, {
                EmployeeEntityIdSelection::employeeId eq DepartmentEntityInfo::employeeId
            })
            .where({
                EmployeeEntityIdSelection::employeeId eq EmployeeEntity::employeeId
            })
            .order { DepartmentEntityInfo::department.desc }
        println(SELECT.build())
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(INNER, DepartmentEntityInfo::class, {
                EmployeeEntityIdSelection::employeeId eq DepartmentEntityInfo::employeeId
            })
            .where({
                EmployeeEntityIdSelection::employeeId eq DepartmentEntityInfo::employeeId
            })
            .order { DepartmentEntityInfo::department.asc }
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
            .order { DepartmentEntityInfo::department.asc }
        println(SELECT.build())
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(INNER, DepartmentEntityInfo::class) {
                EmployeeEntityIdSelection::employeeId eq DepartmentEntity::employeeId
            }
            .join(LEFT, SalaryEntitySelective::class) {
                EmployeeEntityIdSelection::employeeId eq SalaryEntitySelective::employeeId
            }
            .where({
                DepartmentEntityInfo::department eq DepartmentEntity::department
                or {
                    DepartmentEntityInfo::section eq "10"
                    DepartmentEntityInfo::section eq "20"
                    EmployeeEntityIdSelection::employeeId between "1000" to "2000"
                }
            })
            .having {
                SalaryEntitySelective::maxGross gt 100000
            }
            .order {
                DepartmentEntityInfo::department.nullsFirst
                EmployeeEntityIdSelection::employeeId.nullsLast
            }
        println(SELECT.build())
    }

    @Test
    fun build5() {
        SELECT = Select(TestSelectEntityWithAlias::class)
            .join(LEFT, TestSelectEntity::class) {
                TestSelectEntityWithAlias::id eq TestSelectEntity::id
                TestSelectEntity::name like "%kawanagare"
            }
            .where {
                or {
                    TestSelectEntity::name like "%kawanagare"
                    TestSelectEntityWithAlias::address like "%Shinjuku%"
                    DepartmentEntityInfo::section eq "20"
                }
            }
            .order { TestSelectEntity::address.nullsLast }
        println(SELECT.build())
    }

    @Test
    fun build6() {
        val fromTable: KClass<out SelectEntity> = TestSelectEntityWithAlias::class
        val joinTable: KClass<out SelectEntity> = TestSelectEntity::class
        SELECT = Select(fromTable)
            .join(
                LEFT, joinTable, on = {
                    fromTable.prop("id") eq TestSelectEntity::id
                    joinTable.prop("name") like "%kawanagare"
                })
            .where {
                or {
                    joinTable.prop("name") like "%kawanagare"
                    fromTable.prop("address") like "%Shinjuku%"
                }
            }
            .order { joinTable.prop("address").nullsLast }
        println(SELECT.build())
    }

    @Test
    fun build7() {
        SELECT = Select(TestSelectEntityWithAlias::class)
            .join(INNER, TestSelectEntity::class, on = {
                and {
                    TestSelectEntityWithAlias::id eq TestSelectEntity::id
                    TestSelectEntityWithAlias::name eq TestSelectEntity::name
                }
            })
            .join(LEFT, TestAllEntity::class).on {
                TestSelectEntityWithAlias::birthday ge TestAllEntity::birthday
            }
            .where {
                and {
                    TestSelectEntityWithAlias::name eq "川流"
                    TestSelectEntity::address like "%Shinjuku%"
                    or {
                        TestAllEntity::insertDateTime gt
                                LocalDateTime.parse("2025-01-01T00:00:00.000")
                        TestAllEntity::updateDate lt LocalDate.parse("2025-07-01")
                    }
                    TestSelectEntityWithAlias::id between (100) and (200)
                }
            }
        println(SELECT.build())
    }

    private fun KClass<out SelectEntity>.prop(name: String): KProperty1<out SelectEntity, *> =
        this.memberProperties
            .firstOrNull { it.name == name } as KProperty1<out SelectEntity, *>
}
