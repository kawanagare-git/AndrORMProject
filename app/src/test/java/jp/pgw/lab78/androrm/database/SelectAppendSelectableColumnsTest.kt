package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.database.entities.select.EmployeeEntity
import jp.pgw.lab78.androrm.database.entities.select.EmployeeEntityIdSelection
import jp.pgw.lab78.androrm.database.queryparts.JoinType
import jp.pgw.lab78.androrm.database.reference.TableRef
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * ## Select#appendSelectableColumns テスト
 * ### SELECT 対象列追加時に hideFromSelect と join 先 alias が反映されることを確認する
 * @author Masahiro Inoue
 * @since 2026-05-10
 */
class SelectAppendSelectableColumnsTest {

    @Test
    fun appendSelectableColumns_shouldExcludeHiddenColumn_whenMainEntityContainsHiddenColumn() {
        val query = Select(EmployeeEntity::class).build()

        assertAll(
            {
                assertTrue(
                    query.contains("EMP.EMPLOYEE_ID as EMP_EMPLOYEE_ID"),
                    "visible column should be included."
                )
            },
            {
                assertTrue(
                    query.contains("EMP.NAME as EMP_NAME"),
                    "visible column should be included."
                )
            },
            {
                assertFalse(
                    query.contains("EMP.EMPLOYEE_SUB_ID"),
                    "hidden column should not be included."
                )
            },
            {
                assertFalse(
                    query.contains("EMP_EMPLOYEE_SUB_ID"),
                    "hidden column alias should not be included."
                )
            },
        )
    }

    @Test
    fun appendSelectableColumns_shouldUseJoinedTableAlias_whenJoinedEntityContainsSelectableColumns() {
        val query = Select(EmployeeEntityIdSelection::class)
            .join(JoinType.LEFT, EmployeeEntity::class)
            .on {
                condition("1 = 1")
            }
            .build()
        println(query)
        assertAll(
            {
                assertTrue(
                    query.contains("left join EMPLOYEE EMP on 1 = 1"),
                    "joined table alias should be EMP."
                )
            },
            {
                assertTrue(
                    query.contains("EMP_ID.EMPLOYEE_ID as EMP_ID_EMPLOYEE_ID"),
                    "main table column should use main table alias."
                )
            },
            {
                assertTrue(
                    query.contains("EMP.EMPLOYEE_ID as EMP_EMPLOYEE_ID"),
                    "joined table column should use joined table alias."
                )
            },
            {
                assertTrue(
                    query.contains("EMP.NAME as EMP_NAME"),
                    "joined table visible column should be included."
                )
            },
            {
                assertFalse(
                    query.contains("EMP.EMPLOYEE_SUB_ID"),
                    "joined table hidden column should not be included."
                )
            },
            {
                assertFalse(
                    query.contains("EMP_EMPLOYEE_SUB_ID"),
                    "joined table hidden column alias should not be included."
                )
            },
        )
    }

    @Test
    fun appendSelectableColumns_shouldUseNumberedAlias_whenSameEntityIsJoinedAgain() {
        val main = TableRef(EmployeeEntity::class, "M")
        val sub = TableRef(EmployeeEntity::class, "S")
        val query = Select(main)
            .join(JoinType.LEFT, sub)
            .on {
                condition("1 = 1")
            }
            .build()
        println(query)
        assertAll(
            {
                assertTrue(
                    query.contains("left join EMPLOYEE S on 1 = 1"),
                    "same table join should use numbered table alias."
                )
            },
            {
                assertTrue(
                    query.contains("S.EMPLOYEE_ID as S_EMPLOYEE_ID"),
                    "main table column should use original alias."
                )
            },
            {
                assertTrue(
                    query.contains("S.EMPLOYEE_ID as S_EMPLOYEE_ID"),
                    "joined same table column should use numbered alias."
                )
            },
            {
                assertTrue(
                    query.contains("S.NAME as S_NAME"),
                    "joined same table visible column should use numbered alias."
                )
            },
            {
                assertFalse(
                    query.contains("S.EMPLOYEE_SUB_ID"),
                    "joined same table hidden column should not be included."
                )
            },
            {
                assertFalse(
                    query.contains("S_EMPLOYEE_SUB_ID"),
                    "joined same table hidden column alias should not be included."
                )
            },
        )
    }
}