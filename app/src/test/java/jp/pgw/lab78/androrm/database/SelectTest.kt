package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.database.entities.define.DepartmentEntity
import jp.pgw.lab78.androrm.database.entities.define.TestAllEntity
import jp.pgw.lab78.androrm.database.entities.select.*
import jp.pgw.lab78.androrm.database.queryparts.JoinType.*
import jp.pgw.lab78.androrm.database.reference.Support.tableRef
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
    fun build00() {
        SELECT = Select(TestSelectEntity::class)
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
        SELECT = Select(TestSelectEntityWithAlias::class)
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
        SELECT = Select(TestSelectEntity::class, isDistinct = true)
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
    }

    @Test
    fun build01() {
        SELECT = Select(TestSelectEntityWithAlias::class)
            .join(
                LEFT, TestSelectEntity::class, {
                    TestSelectEntityWithAlias::id eq TestSelectEntity::id
                })
            .where {
                or {
                    TestSelectEntity::name like "kawanagare%"
                    condition("name like ?", "川流%")
                }
            }
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
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
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
    }

    @Test
    fun build02() {
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
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
        // 新エンティティクラス
        SELECT = Select(DepartmentEntityInfo::class)
            .order {
                DepartmentEntityInfo::employeeId.asc
                DepartmentEntityInfo::section.asc.nullsLast
            }
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
    }

    @Test
    fun build03() {
        // 最終TEST
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(INNER, DepartmentEntityInfo::class, {
                EmployeeEntityIdSelection::employeeId eq DepartmentEntityInfo::employeeId
            })
            .order { DepartmentEntityInfo::department.asc }
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(INNER, DepartmentEntityInfo::class, {
                EmployeeEntityIdSelection::employeeId eq DepartmentEntityInfo::employeeId
            })
            .where({
                EmployeeEntityIdSelection::employeeId eq DepartmentEntityInfo::employeeId
            })
            .order { DepartmentEntityInfo::department.desc }
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(INNER, DepartmentEntityInfo::class, {
                EmployeeEntityIdSelection::employeeId eq DepartmentEntityInfo::employeeId
            })
            .where({
                EmployeeEntityIdSelection::employeeId eq DepartmentEntityInfo::employeeId
            })
            .order { DepartmentEntityInfo::department.asc }
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
    }

    @Test
    fun build04() {
        // 最終TEST
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(INNER, DepartmentEntityInfo::class, {
                EmployeeEntityIdSelection::employeeId eq DepartmentEntityInfo::employeeId
                EmployeeEntityIdSelection::employeeId eq DepartmentEntityInfo::employeeId
            })
            .order { DepartmentEntityInfo::department.asc }
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
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
                    EmployeeEntityIdSelection::employeeId between ("1000" to "2000")
                }
            })
            .having {
                SalaryEntitySelective::maxGross gt 100000
            }
            .order {
                DepartmentEntityInfo::department.nullsFirst
                EmployeeEntityIdSelection::employeeId.nullsLast
            }
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
    }

    @Test
    fun build05() {
        SELECT = Select(TestSelectEntityWithAlias::class)
            .join(LEFT, TestSelectEntity::class).on {
                TestSelectEntityWithAlias::id eq DepartmentEntityInfo::employeeId
                TestSelectEntity::name like "%kawanagare"
            }
            .where {
                or {
                    TestSelectEntity::name like "%kawanagare"
                    TestSelectEntityWithAlias::address like "%Shinjuku%"
                    DepartmentEntityInfo::section eq "20"
                    DepartmentEntityInfo::department between "10" and "50"
                }
            }
            .order { TestSelectEntity::address.nullsLast }
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
    }

    @Test
    fun build06() {
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
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
    }

    @Test
    fun build07() {
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
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
    }

    @Test
    fun build08() {
        SELECT = Select(TestSelectEntity::class)
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
        SELECT.where {
            TestSelectEntity::id inList listOf(1, 2, 3, 4, 5)
        }
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
        SELECT = Select(TestSelectEntity::class)
        SELECT.where {
            TestSelectEntity::name like "川流%"
        }
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
        SELECT.order {
            TestSelectEntity::id.asc
        }
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
    }

    @Test
    fun build09() {
        val bindValues = TestAllEntity(
            id = 10,
            name = "川流%",
            address = "新宿区",
            birthday = LocalDate.parse("1990-01-01"),
        )
        SELECT = Select(TestSelectEntity::class)
            .join(LEFT, TestAllEntity::class) {
                TestSelectEntity::id eq TestAllEntity::id
                TestAllEntity::birthday eq "2026-01-15"
            }
            .where {
                TestSelectEntity::name like bindValues.name
                TestSelectEntity::address eq bindValues.address
                TestAllEntity::subId eq TestAllEntity::id
                TestAllEntity::id ge "20"
            }
            .order { TestSelectEntity::id.asc }
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
    }

    @Test
    fun build10() {
        SELECT = Select(EmployeeEntityIdSelection::class)
            .join(
                LEFT, EmployeeEntityJoined::class
            ).on {
                EmployeeEntityIdSelection::employeeId eq EmployeeEntityJoined::employeeId
            }
            .order {
                EmployeeEntityIdSelection::employeeId.asc.nullsFirst
                EmployeeEntityJoined::name.desc
            }
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
    }

    @Test
    fun build11() {
        SELECT = Select(DepartmentEntityInfo::class)
            .where {
                DepartmentEntityInfo::employeeId between ("1000" to "2000")
            }
            .having {
                DepartmentEntityInfo::allLine gt 100000
            }
            .order {
                DepartmentEntityInfo::employeeId.nullsFirst
            }
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
    }

    @Test
    fun build12() {
        SELECT = Select(EmployeeEntityIdSelection::class)
            .where { EmployeeEntityIdSelection::employeeId inList (listOf(10, 20, 30)) }
            .order {
                EmployeeEntityIdSelection::employeeId.asc.nullsFirst
            }
        println("${SELECT.build()}; values = ${SELECT.bindValues}")
    }

    private fun KClass<out SelectEntity>.prop(name: String): KProperty1<out SelectEntity, *> =
        this.memberProperties
            .firstOrNull { it.name == name } as KProperty1<out SelectEntity, *>

    @Test
    fun build13() {
        val fromTable = tableRef(TestSelectEntity::class, "FR")
        val join1Table = tableRef(TestSelectEntity::class, "J1")
        val join2Table = tableRef(TestSelectEntity::class, "J2")
        val select = Select(fromTable).join(
            LEFT,
            join1Table,
            { fromTable[TestSelectEntity::id] eq join1Table[TestSelectEntity::id] })
            .join(
                INNER,
                join2Table,
                { join1Table[TestSelectEntity::name] eq join2Table[TestSelectEntity::name] })
            .where { fromTable[TestSelectEntity::id] like "0010%" }
            .limit(10).offset(5)
        println("${select.build()}; values = ${select.bindValues}")
    }
}
