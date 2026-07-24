package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.MessageConstants.AE00002
import jp.pgw.lab78.androrm.common.MessageConstants.AE00003
import jp.pgw.lab78.androrm.common.MessageConstants.AE00004
import jp.pgw.lab78.androrm.common.MessageConstants.AE00005
import jp.pgw.lab78.androrm.common.MessageConstants.AE00010
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Function
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction.MAX
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.database.entities.RuntimeEmployeeEntity as EmployeeEntity
import jp.pgw.lab78.androrm.database.entities.RuntimeEmployeeEntityIdSelection as EmployeeEntityIdSelection
import jp.pgw.lab78.androrm.database.entities.RuntimeSalaryEntitySelective as SalaryEntitySelective
import jp.pgw.lab78.androrm.database.interfaces.plus
import jp.pgw.lab78.androrm.database.queryparts.JoinType.*
import jp.pgw.lab78.androrm.database.reference.TableRef
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * ## Select テスト
 * ### Select 文生成、bindValues、JOIN / WHERE / HAVING / ORDER / LIMIT / OFFSET を確認する
 * @author Masahiro Inoue
 * @since 2026-06-09
 */
class SelectTest {

    /**
     * 「testTableColumns_buildsSelectForTableTransfer」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testTableColumns_buildsSelectForTableTransfer() {
        val actual = Select.tableColumns(
            tableName = "SOURCE_TABLE",
            columnList = listOf("ID", "NAME", "AGE"),
        )

        assertEquals(
            "select ID,NAME,AGE from SOURCE_TABLE",
            actual,
        )
    }

    /**
     * 「testBuild_withoutCondition_buildsBasicSelect」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testBuild_withoutCondition_buildsBasicSelect() {
        val select = Select(EmployeeEntity::class)

        assertAll(
            {
                assertEquals(
                    "select EMP.EMPLOYEE_ID as EMP_EMPLOYEE_ID, " +
                            "EMP.NAME as EMP_NAME, " +
                            "EMP.ADDRESS as EMP_ADDRESS, " +
                            "EMP.GENDER as EMP_GENDER, " +
                            "EMP.POSITION as EMP_POSITION " +
                            "from EMPLOYEE EMP",
                    select.build(),
                )
            },
            { assertEquals(emptyList<Any?>(), select.bindValues) },
            { assertFalse(select.build().contains("EMPLOYEE_SUB_ID")) },
            { assertFalse(select.build().contains("EMP_EMPLOYEE_SUB_ID")) },
        )
    }

    /**
     * 「testBuild_withDistinct_buildsDistinctSelect」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testBuild_withDistinct_buildsDistinctSelect() {
        val select = Select(EmployeeEntityIdSelection::class, isDistinct = true)

        assertAll(
            {
                assertEquals(
                    "select distinct EMP_ID.EMPLOYEE_ID as EMP_ID_EMPLOYEE_ID " +
                            "from EMPLOYEE EMP_ID",
                    select.build(),
                )
            },
            { assertEquals(emptyList<Any?>(), select.bindValues) },
        )
    }

    /**
     * 「testBuild_withFunctionProjection_buildsFunctionColumns」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testBuild_withFunctionProjection_buildsFunctionColumns() {
        val select = Select(SalaryEntitySelective::class)

        assertAll(
            {
                assertEquals(
                    "select SAL.EMPLOYEE_ID as SAL_EMPLOYEE_ID, " +
                            "SAL.PAY_MONTH as SAL_PAY_MONTH, " +
                            "SAL.GROSS as SAL_GROSS, " +
                            "sum(SAL.GROSS) as SAL_TOTAL_GROSS, " +
                            "max(SAL.GROSS) as SAL_MAX_GROSS, " +
                            "avg(SAL.GROSS) as SAL_AVG_GROSS, " +
                            "max(deduction) as SAL_MAX_DEDUCTION, " +
                            "avg(deduction) as SAL_AVG_DEDUCTION " +
                            "from SALARY SAL",
                    select.build(),
                )
            },
            { assertEquals(emptyList<Any?>(), select.bindValues) },
        )
    }

    /**
     * 「testBuild_withWhereLogicalConditions_buildsWhereAndBindValues」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testBuild_withWhereLogicalConditions_buildsWhereAndBindValues() {
        val select = Select(EmployeeEntity::class)
            .where {
                and {
                    EmployeeEntity::name eq "川流"
                    EmployeeEntity::address like "%Shinjuku%"
                }
                or {
                    EmployeeEntity::gender eq "MALE"
                    EmployeeEntity::position eq "PG"
                }
            }

        assertAll(
            {
                assertEquals(
                    "select EMP.EMPLOYEE_ID as EMP_EMPLOYEE_ID, " +
                            "EMP.NAME as EMP_NAME, " +
                            "EMP.ADDRESS as EMP_ADDRESS, " +
                            "EMP.GENDER as EMP_GENDER, " +
                            "EMP.POSITION as EMP_POSITION " +
                            "from EMPLOYEE EMP " +
                            "where (EMP.NAME = ? and EMP.ADDRESS like ?) " +
                            "and (EMP.GENDER = ? or EMP.POSITION = ?)",
                    select.build(),
                )
            },
            { assertEquals(listOf("川流", "%Shinjuku%", "MALE", "PG"), select.bindValues) },
        )
    }

    /**
     * 「testBuild_withRawCondition_buildsWhereAndBindValues」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testBuild_withRawCondition_buildsWhereAndBindValues() {
        val select = Select(EmployeeEntity::class)
            .where {
                condition(
                    "EMP.NAME like ? OR EMP.ADDRESS like ?",
                    "川流%",
                    "%東京%",
                )
            }

        assertAll(
            {
                assertEquals(
                    "select EMP.EMPLOYEE_ID as EMP_EMPLOYEE_ID, " +
                            "EMP.NAME as EMP_NAME, " +
                            "EMP.ADDRESS as EMP_ADDRESS, " +
                            "EMP.GENDER as EMP_GENDER, " +
                            "EMP.POSITION as EMP_POSITION " +
                            "from EMPLOYEE EMP " +
                            "where EMP.NAME like ? OR EMP.ADDRESS like ?",
                    select.build(),
                )
            },
            { assertEquals(listOf("川流%", "%東京%"), select.bindValues) },
        )
    }

    /**
     * 「testBuild_withInListAndBetween_buildsWhereAndBindValues」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testBuild_withInListAndBetween_buildsWhereAndBindValues() {
        val select = Select(EmployeeEntityIdSelection::class)
            .where {
                EmployeeEntityIdSelection::employeeId inList listOf("EMP001", "EMP002")
                EmployeeEntityIdSelection::employeeId between ("EMP001" to "EMP999")
            }

        assertAll(
            {
                assertEquals(
                    "select EMP_ID.EMPLOYEE_ID as EMP_ID_EMPLOYEE_ID " +
                            "from EMPLOYEE EMP_ID " +
                            "where EMP_ID.EMPLOYEE_ID in (?, ?) " +
                            "and EMP_ID.EMPLOYEE_ID between ? and ?",
                    select.build(),
                )
            },
            { assertEquals(listOf("EMP001", "EMP002", "EMP001", "EMP999"), select.bindValues) },
        )
    }

    /**
     * 「testBuild_withJoinOnBlock_buildsJoinSelect」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testBuild_withJoinOnBlock_buildsJoinSelect() {
        val select = Select(EmployeeEntityIdSelection::class)
            .join(LEFT, EmployeeEntity::class) {
                EmployeeEntityIdSelection::employeeId eq EmployeeEntity::employeeId
            }

        assertAll(
            {
                assertEquals(
                    "select EMP_ID.EMPLOYEE_ID as EMP_ID_EMPLOYEE_ID, " +
                            "EMP.EMPLOYEE_ID as EMP_EMPLOYEE_ID, " +
                            "EMP.NAME as EMP_NAME, " +
                            "EMP.ADDRESS as EMP_ADDRESS, " +
                            "EMP.GENDER as EMP_GENDER, " +
                            "EMP.POSITION as EMP_POSITION " +
                            "from EMPLOYEE EMP_ID " +
                            "left join EMPLOYEE EMP on EMP_ID.EMPLOYEE_ID = EMP.EMPLOYEE_ID",
                    select.build(),
                )
            },
            { assertEquals(emptyList<Any?>(), select.bindValues) },
            { assertEquals(listOf("EMP_ID", "EMP"), select.usedEntityClasses.map { it.alias }) },
        )
    }

    /**
     * 「testBuild_withJoinConditionOn_buildsInnerJoinSelect」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testBuild_withJoinConditionOn_buildsInnerJoinSelect() {
        val select = Select(EmployeeEntityIdSelection::class)
            .join(INNER, EmployeeEntity::class)
            .on {
                EmployeeEntityIdSelection::employeeId eq EmployeeEntity::employeeId
                EmployeeEntity::name like "川流%"
            }

        assertAll(
            {
                assertEquals(
                    "select EMP_ID.EMPLOYEE_ID as EMP_ID_EMPLOYEE_ID, " +
                            "EMP.EMPLOYEE_ID as EMP_EMPLOYEE_ID, " +
                            "EMP.NAME as EMP_NAME, " +
                            "EMP.ADDRESS as EMP_ADDRESS, " +
                            "EMP.GENDER as EMP_GENDER, " +
                            "EMP.POSITION as EMP_POSITION " +
                            "from EMPLOYEE EMP_ID " +
                            "inner join EMPLOYEE EMP on EMP_ID.EMPLOYEE_ID = EMP.EMPLOYEE_ID " +
                            "AND EMP.NAME like ?",
                    select.build(),
                )
            },
            { assertEquals(listOf("川流%"), select.bindValues) },
        )
    }

    /**
     * 「testBuild_withJoinConditionOn_buildsLeftJoinSelect」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testBuild_withJoinConditionOn_buildsLeftJoinSelect() {
        val select = Select(EmployeeEntityIdSelection::class)
            .join(LEFT, EmployeeEntity::class)
            .on {
                EmployeeEntityIdSelection::employeeId eq EmployeeEntity::employeeId
                EmployeeEntity::name like "川流%"
            }

        assertAll(
            {
                assertEquals(
                    "select EMP_ID.EMPLOYEE_ID as EMP_ID_EMPLOYEE_ID, " +
                            "EMP.EMPLOYEE_ID as EMP_EMPLOYEE_ID, " +
                            "EMP.NAME as EMP_NAME, " +
                            "EMP.ADDRESS as EMP_ADDRESS, " +
                            "EMP.GENDER as EMP_GENDER, " +
                            "EMP.POSITION as EMP_POSITION " +
                            "from EMPLOYEE EMP_ID " +
                            "left join EMPLOYEE EMP on EMP_ID.EMPLOYEE_ID = EMP.EMPLOYEE_ID " +
                            "AND EMP.NAME like ?",
                    select.build(),
                )
            },
            { assertEquals(listOf("川流%"), select.bindValues) },
        )
    }

    /**
     * 「testBuild_withTableRefSelfJoin_buildsJoinWithCustomAlias」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testBuild_withTableRefSelfJoin_buildsJoinWithCustomAlias() {
        val mainTable = TableRef(EmployeeEntity::class, "M")
        val joinedTable = TableRef(EmployeeEntity::class, "S")
        val select = Select(mainTable)
            .join(LEFT, joinedTable) {
                mainTable[EmployeeEntity::employeeId] eq joinedTable[EmployeeEntity::employeeId]
            }
            .where {
                mainTable[EmployeeEntity::name] like "川流%"
            }
            .limit(10)
            .offset(5)

        assertAll(
            {
                assertEquals(
                    "select M.EMPLOYEE_ID as M_EMPLOYEE_ID, " +
                            "M.NAME as M_NAME, " +
                            "M.ADDRESS as M_ADDRESS, " +
                            "M.GENDER as M_GENDER, " +
                            "M.POSITION as M_POSITION, " +
                            "S.EMPLOYEE_ID as S_EMPLOYEE_ID, " +
                            "S.NAME as S_NAME, " +
                            "S.ADDRESS as S_ADDRESS, " +
                            "S.GENDER as S_GENDER, " +
                            "S.POSITION as S_POSITION " +
                            "from EMPLOYEE M " +
                            "left join EMPLOYEE S on M.EMPLOYEE_ID = S.EMPLOYEE_ID " +
                            "where M.NAME like ? limit ? offset ?",
                    select.build(),
                )
            },
            { assertEquals(listOf("川流%", 10, 5), select.bindValues) },
            { assertEquals(listOf("M", "S"), select.usedEntityClasses.map { it.alias }) },
        )
    }

    /**
     * 「testBuild_withHaving_buildsGroupByAndHaving」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testBuild_withHaving_buildsGroupByAndHaving() {
        val select = Select(SalaryEntitySelective::class)
            .having {
                SalaryEntitySelective::totalGross gt 100_000
            }

        assertAll(
            {
                assertEquals(
                    "select SAL.EMPLOYEE_ID as SAL_EMPLOYEE_ID, " +
                            "SAL.PAY_MONTH as SAL_PAY_MONTH, " +
                            "SAL.GROSS as SAL_GROSS, " +
                            "sum(SAL.GROSS) as SAL_TOTAL_GROSS, " +
                            "max(SAL.GROSS) as SAL_MAX_GROSS, " +
                            "avg(SAL.GROSS) as SAL_AVG_GROSS, " +
                            "max(deduction) as SAL_MAX_DEDUCTION, " +
                            "avg(deduction) as SAL_AVG_DEDUCTION " +
                            "from SALARY SAL " +
                            "group by SAL.EMPLOYEE_ID, SAL.PAY_MONTH, SAL.GROSS " +
                            "having SAL.TOTAL_GROSS > ?",
                    select.build(),
                )
            },
            { assertEquals(listOf(100_000), select.bindValues) },
        )
    }

    /**
     * 「testBuild_withOrder_buildsOrderBy」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testBuild_withOrder_buildsOrderBy() {
        val select = Select(EmployeeEntity::class)
            .order {
                EmployeeEntity::employeeId.asc
                EmployeeEntity::name.desc.nullsLast
                EmployeeEntity::address.nullsFirst
            }

        assertAll(
            {
                assertEquals(
                    "select EMP.EMPLOYEE_ID as EMP_EMPLOYEE_ID, " +
                            "EMP.NAME as EMP_NAME, " +
                            "EMP.ADDRESS as EMP_ADDRESS, " +
                            "EMP.GENDER as EMP_GENDER, " +
                            "EMP.POSITION as EMP_POSITION " +
                            "from EMPLOYEE EMP " +
                            "order by EMP.EMPLOYEE_ID asc nulls first, " +
                            "EMP.NAME desc nulls last, " +
                            "EMP.ADDRESS asc nulls first",
                    select.build(),
                )
            },
            { assertEquals(emptyList<Any?>(), select.bindValues) },
        )
    }

    /**
     * 「testBuild_withLimitOnly_buildsLimitAndBindValues」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testBuild_withLimitOnly_buildsLimitAndBindValues() {
        val select = Select(EmployeeEntityIdSelection::class)
            .limit(20)

        assertAll(
            {
                assertEquals(
                    "select EMP_ID.EMPLOYEE_ID as EMP_ID_EMPLOYEE_ID " +
                            "from EMPLOYEE EMP_ID limit ?",
                    select.build(),
                )
            },
            { assertEquals(listOf(20), select.bindValues) },
        )
    }

    /**
     * 「testBuild_withDefaultLimitAndDefaultOffset_buildsLimitOffsetAndBindValues」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testBuild_withDefaultLimitAndDefaultOffset_buildsLimitOffsetAndBindValues() {
        val select = Select(EmployeeEntityIdSelection::class)
            .limit()
            .offset()

        assertAll(
            {
                assertEquals(
                    "select EMP_ID.EMPLOYEE_ID as EMP_ID_EMPLOYEE_ID " +
                            "from EMPLOYEE EMP_ID limit ? offset ?",
                    select.build(),
                )
            },
            { assertEquals(listOf(10, 0), select.bindValues) },
        )
    }

    /**
     * 「testBuild_afterAlreadyBuiltAndWhereAdded_rebuildsSql」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testBuild_afterAlreadyBuiltAndWhereAdded_rebuildsSql() {
        val select = Select(EmployeeEntityIdSelection::class)
        val before = select.build()

        select.where {
            EmployeeEntityIdSelection::employeeId eq "EMP001"
        }
        val after = select.build()

        assertAll(
            {
                assertEquals(
                    "select EMP_ID.EMPLOYEE_ID as EMP_ID_EMPLOYEE_ID from EMPLOYEE EMP_ID",
                    before,
                )
            },
            {
                assertEquals(
                    "select EMP_ID.EMPLOYEE_ID as EMP_ID_EMPLOYEE_ID " +
                            "from EMPLOYEE EMP_ID where EMP_ID.EMPLOYEE_ID = ?",
                    after,
                )
            },
            { assertEquals(listOf("EMP001"), select.bindValues) },
        )
    }

    /**
     * 「testBuild_withExistsSubQuery_buildsExistsCondition」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testBuild_withExistsSubQuery_buildsExistsCondition() {
        val subQuery = Select(EmployeeEntityIdSelection::class)
            .where {
                EmployeeEntityIdSelection::employeeId eq EmployeeEntity::employeeId
            }
        val select = Select(EmployeeEntity::class)
            .where {
                exists(subQuery)
            }

        assertAll(
            {
                assertEquals(
                    "select EMP.EMPLOYEE_ID as EMP_EMPLOYEE_ID, " +
                            "EMP.NAME as EMP_NAME, " +
                            "EMP.ADDRESS as EMP_ADDRESS, " +
                            "EMP.GENDER as EMP_GENDER, " +
                            "EMP.POSITION as EMP_POSITION " +
                            "from EMPLOYEE EMP " +
                            "where exists (select EMP_ID.EMPLOYEE_ID as EMP_ID_EMPLOYEE_ID " +
                            "from EMPLOYEE EMP_ID where EMP_ID.EMPLOYEE_ID = EMP.EMPLOYEE_ID)",
                    select.build(),
                )
            },
            { assertEquals(emptyList<Any?>(), select.bindValues) },
        )
    }

    /**
     * 「testWhere_withRawConditionPlaceholderMismatch_throwsIllegalArgumentException」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testWhere_withRawConditionPlaceholderMismatch_throwsIllegalArgumentException() {
        val actual = assertThrows<IllegalArgumentException> {
            Select(EmployeeEntity::class)
                .where {
                    condition("EMP.NAME = ? AND EMP.ADDRESS = ?", "川流")
                }
        }

        assertEquals(
            AE00002.format("EMP.NAME = ? AND EMP.ADDRESS = ?", 2, 1),
            actual.message,
        )
    }

    /**
     * 「testJoin_withDuplicateAlias_throwsIllegalArgumentException」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testJoin_withDuplicateAlias_throwsIllegalArgumentException() {
        val mainTable = TableRef(EmployeeEntity::class, "EMP")
        val joinedTable = TableRef(EmployeeEntity::class, "EMP")

        val actual = assertThrows<IllegalArgumentException> {
            Select(mainTable)
                .join(LEFT, joinedTable) {
                    mainTable[EmployeeEntity::employeeId] eq joinedTable[EmployeeEntity::employeeId]
                }
        }

        assertEquals(
            AE00003.format("EMP", "EMPLOYEE"),
            actual.message,
        )
    }

    /**
     * 「testWhere_calledTwice_throwsIllegalStateException」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testWhere_calledTwice_throwsIllegalStateException() {
        val select = Select(EmployeeEntity::class)
            .where {
                EmployeeEntity::name eq "川流"
            }

        val actual = assertThrows<IllegalStateException> {
            select.where {
                EmployeeEntity::address like "%東京%"
            }
        }

        assertEquals(AE00010.format("Select", "where"), actual.message)
    }

    /**
     * 「testHaving_calledTwice_throwsIllegalStateException」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testHaving_calledTwice_throwsIllegalStateException() {
        val select = Select(SalaryEntitySelective::class)
            .having {
                SalaryEntitySelective::totalGross gt 100_000
            }

        val actual = assertThrows<IllegalStateException> {
            select.having {
                SalaryEntitySelective::maxGross gt 100_000
            }
        }

        assertEquals(AE00010.format("Select", "having"), actual.message)
    }

    /**
     * 「testOrder_calledTwice_throwsIllegalStateException」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testOrder_calledTwice_throwsIllegalStateException() {
        val select = Select(EmployeeEntity::class)
            .order {
                EmployeeEntity::employeeId.asc
            }

        val actual = assertThrows<IllegalStateException> {
            select.order {
                EmployeeEntity::name.desc
            }
        }

        assertEquals(AE00010.format("Select", "order"), actual.message)
    }

    /**
     * 「testLimit_calledTwice_throwsIllegalStateException」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testLimit_calledTwice_throwsIllegalStateException() {
        val select = Select(EmployeeEntity::class)
        select.limit(10)

        val actual = assertThrows<IllegalStateException> {
            select.limit(20)
        }

        assertEquals(AE00010.format("Select", "limit"), actual.message)
    }

    /**
     * 「testOffset_calledTwice_throwsIllegalStateException」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testOffset_calledTwice_throwsIllegalStateException() {
        val limitClause = Select(EmployeeEntity::class).limit(10)
        limitClause.offset(5)

        val actual = assertThrows<IllegalStateException> {
            limitClause.offset(10)
        }

        assertEquals(AE00010.format("Select", "offset"), actual.message)
    }

    /**
     * 「testLimit_withNegativeValue_throwsIllegalArgumentException」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testLimit_withNegativeValue_throwsIllegalArgumentException() {
        val actual = assertThrows<IllegalArgumentException> {
            Select(EmployeeEntity::class).limit(-1)
        }

        assertEquals(AE00004, actual.message)
    }

    /**
     * 「testOffset_withNegativeValue_throwsIllegalArgumentException」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testOffset_withNegativeValue_throwsIllegalArgumentException() {
        val actual = assertThrows<IllegalArgumentException> {
            Select(EmployeeEntity::class).limit(10).offset(-1)
        }

        assertEquals(AE00005, actual.message)
    }

    /**
     * 「testConstructor_withDualAnnotationEntity_throwsIllegalArgumentException」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testConstructor_withDualAnnotationEntity_throwsIllegalArgumentException() {
        val actual = assertThrows<IllegalArgumentException> {
            Select(InvalidDualAnnotationEntity::class)
        }
        assertTrue(
            actual.message.orEmpty().contains(
                "Property 'id' in entity 'InvalidDualAnnotationEntity' cannot have both @Column and @Function."
            )
        )
    }

    /**
     * 「testConstructor_withAllHiddenEntity_throwsIllegalArgumentException」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testConstructor_withAllHiddenEntity_throwsIllegalArgumentException() {
        val actual = assertThrows<IllegalArgumentException> {
            Select(InvalidAllHiddenEntity::class)
        }

        assertTrue(
            actual.message.orEmpty().contains(
                "Entity 'InvalidAllHiddenEntity' derived from "
            )
        )
        assertTrue(
            actual.message.orEmpty().contains(
                "has no selectable properties. All properties are hidden from SELECT."
            )
        )
    }

    /**
     * 「testConstructor_withDuplicateAliasEntity_throwsIllegalArgumentException」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testConstructor_withDuplicateAliasEntity_throwsIllegalArgumentException() {
        val actual = assertThrows<IllegalArgumentException> {
            Select(InvalidDuplicateAliasEntity::class)
        }

        assertTrue(
            actual.message.orEmpty().contains(
                "Duplicate alias 'DUPLICATE_ALIAS' in entity 'InvalidDuplicateAliasEntity'"
            )
        )
    }

    /**
     * 「testConstructor_withDuplicateColumnNameEntity_throwsIllegalArgumentException」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testConstructor_withDuplicateColumnNameEntity_throwsIllegalArgumentException() {
        val actual = assertThrows<IllegalArgumentException> {
            Select(InvalidDuplicateAliasEntity::class)
        }
        assertTrue(
            actual.message.orEmpty().contains(
                "Duplicate alias 'DUPLICATE_ALIAS' in entity 'InvalidDuplicateAliasEntity'"
            )
        )
    }

    /**
     * 「testBuild_withWhereSqlExpression_buildsWhereExpressionAndBindValues」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Test
    fun testBuild_withWhereSqlExpression_buildsWhereExpressionAndBindValues() {
        val salaryTable = TableRef(SalaryEntitySelective::class, "SAL")

        val select = Select(SalaryEntitySelective::class)
            .where {
                SalaryEntitySelective::gross eq (salaryTable[SalaryEntitySelective::gross] + 10)
            }

        assertAll(
            {
                assertEquals(
                    "select SAL.EMPLOYEE_ID as SAL_EMPLOYEE_ID, " +
                            "SAL.PAY_MONTH as SAL_PAY_MONTH, " +
                            "SAL.GROSS as SAL_GROSS, " +
                            "sum(SAL.GROSS) as SAL_TOTAL_GROSS, " +
                            "max(SAL.GROSS) as SAL_MAX_GROSS, " +
                            "avg(SAL.GROSS) as SAL_AVG_GROSS, " +
                            "max(deduction) as SAL_MAX_DEDUCTION, " +
                            "avg(deduction) as SAL_AVG_DEDUCTION " +
                            "from SALARY SAL " +
                            "where SAL.GROSS = (SAL.GROSS + ?)",
                    select.build(),
                )
            },
            { assertEquals(listOf(10), select.bindValues) },
        )
    }

    /**
     * Entityメタ情報またはSQL生成の検証に使用するテスト用InvalidDualAnnotationEntity。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Table(name = "INVALID_DUAL_ANNOTATION", alias = "IDA")
    private data class InvalidDualAnnotationEntity(
        @Column(name = "ID")
        @Function(columnFunction = MAX, alias = "MAX_ID", args = ["id"])
        val id: Int,
    ) : SelectEntity

    /**
     * Entityメタ情報またはSQL生成の検証に使用するテスト用InvalidAllHiddenEntity。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Table(name = "INVALID_ALL_HIDDEN", alias = "IAH")
    private data class InvalidAllHiddenEntity(
        @Column(name = "ID", hideFromSelect = true)
        val id: Int,
    ) : SelectEntity

    /**
     * Entityメタ情報またはSQL生成の検証に使用するテスト用InvalidDuplicateAliasEntity。
     * @author Masahiro Inoue
     * @since 2026-06-09
     */
    @Table(name = "INVALID_DUPLICATE_ALIAS", alias = "IDA2")
    private data class InvalidDuplicateAliasEntity(
        @Column(name = "ID", alias = "DUPLICATE_ALIAS")
        val id: Int,
        @Column(name = "NAME", alias = "DUPLICATE_ALIAS")
        val name: String,
    ) : SelectEntity

}
