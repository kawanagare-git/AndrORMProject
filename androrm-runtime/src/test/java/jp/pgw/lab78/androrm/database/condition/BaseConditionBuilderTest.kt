package jp.pgw.lab78.androrm.database.condition

import jp.pgw.lab78.androrm.database.Select
import jp.pgw.lab78.androrm.database.condition.base.BaseConditionBuilder.NullMarker
import jp.pgw.lab78.androrm.database.reference.ColumnRef
import jp.pgw.lab78.androrm.database.reference.TableRef
import jp.pgw.lab78.androrm.database.support.SupportOperation.changeColumnRef
import jp.pgw.lab78.androrm.database.support.SupportOperation.changeProperty
import jp.pgw.lab78.androrm.database.support.createConditionBuilderTestState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import jp.pgw.lab78.androrm.database.entities.RuntimeEmployeeEntity as EmployeeEntity
import jp.pgw.lab78.androrm.database.entities.RuntimeEmployeeEntityIdSelection as EmployeeEntityIdSelection

/**
 * ## ConditionBuilder テスト
 * ### BaseConditionBuilder の DSL public メソッドを ConditionBuilder 経由で検証する
  * @author Masahiro Inoue
  * @since 2026-05-18
 */
class BaseConditionBuilderTest {

    /**
     * 「testKPropertyValueDsl」の条件における期待動作を検証する。
     * @param methodName methodNameとして使用する値。
     * @param value 追加する値。
     * @param expected 期待値。
     * @author Masahiro Inoue
     * @since 2026-05-18
     */
    @DisplayName("KProperty1 の値比較 DSL を検証する")
    @ParameterizedTest(name = "[{index}] method={0}, value={1}, expected={2}")
    @CsvSource(
        "'eq',10,'EMP.EMPLOYEE_ID = ?'",
        "'equal',10,'EMP.EMPLOYEE_ID = ?'",
        "'ne',10,'EMP.EMPLOYEE_ID <> ?'",
        "'notEqual',10,'EMP.EMPLOYEE_ID <> ?'",
        "'gt',10,'EMP.EMPLOYEE_ID > ?'",
        "'graterThan',10,'EMP.EMPLOYEE_ID > ?'",
        "'ge',10,'EMP.EMPLOYEE_ID >= ?'",
        "'graterEqual',10,'EMP.EMPLOYEE_ID >= ?'",
        "'lt',10,'EMP.EMPLOYEE_ID < ?'",
        "'lesserThan',10,'EMP.EMPLOYEE_ID < ?'",
        "'le',10,'EMP.EMPLOYEE_ID <= ?'",
        "'lessEqual',10,'EMP.EMPLOYEE_ID <= ?'",
        "'like',ABC%,'EMP.EMPLOYEE_ID like ?'",
        "'notLike',ABC%,'EMP.EMPLOYEE_ID not like ?'",
        "'glob',ABC*,'EMP.EMPLOYEE_ID glob ?'",
        "'notGlob',ABC*,'EMP.EMPLOYEE_ID not glob ?'",
    )
    fun testKPropertyValueDsl(
        methodName: String,
        value: String,
        expected: String,
    ) {
        val state = createConditionBuilderTestState()

        with(state.builder) {
            when (methodName) {
                "eq" -> EmployeeEntity::employeeId eq value
                "equal" -> EmployeeEntity::employeeId equal value
                "ne" -> EmployeeEntity::employeeId ne value
                "notEqual" -> EmployeeEntity::employeeId notEqual value
                "gt" -> EmployeeEntity::employeeId gt value
                "graterThan" -> EmployeeEntity::employeeId graterThan value
                "ge" -> EmployeeEntity::employeeId ge value
                "graterEqual" -> EmployeeEntity::employeeId graterEqual value
                "lt" -> EmployeeEntity::employeeId lt value
                "lesserThan" -> EmployeeEntity::employeeId lesserThan value
                "le" -> EmployeeEntity::employeeId le value
                "lessEqual" -> EmployeeEntity::employeeId lessEqual value
                "like" -> EmployeeEntity::employeeId like value
                "notLike" -> EmployeeEntity::employeeId notLike value
                "glob" -> EmployeeEntity::employeeId glob value
                "notGlob" -> EmployeeEntity::employeeId notGlob value
                else -> error("Unsupported methodName: $methodName")
            }
        }

        assertEquals(expected, state.singleSql())
        assertEquals(listOf(value), state.valueHolder.bindValues)
    }

    /**
     * 「testColumnRefValueDsl」の条件における期待動作を検証する。
     * @param methodName methodNameとして使用する値。
     * @param refString 列参照を表す文字列。
     * @param value 追加する値。
     * @param expected 期待値。
     * @author Masahiro Inoue
     * @since 2026-05-18
     */
    @DisplayName("ColumnRef の値比較 DSL を検証する")
    @ParameterizedTest(name = "[{index}] method={0}, ref={1}, value={2}, expected={3}")
    @CsvSource(
        "'eq','EmployeeEntity::employeeId',10,'EMP.EMPLOYEE_ID = ?'",
        "'equal','EmployeeEntity::employeeId',10,'EMP.EMPLOYEE_ID = ?'",
        "'ne','EmployeeEntity::employeeId',10,'EMP.EMPLOYEE_ID <> ?'",
        "'notEqual','EmployeeEntity::employeeId',10,'EMP.EMPLOYEE_ID <> ?'",
        "'gt','EmployeeEntity::employeeId',10,'EMP.EMPLOYEE_ID > ?'",
        "'graterThan','EmployeeEntity::employeeId',10,'EMP.EMPLOYEE_ID > ?'",
        "'ge','EmployeeEntity::employeeId',10,'EMP.EMPLOYEE_ID >= ?'",
        "'graterEqual','EmployeeEntity::employeeId',10,'EMP.EMPLOYEE_ID >= ?'",
        "'lt','EmployeeEntity::employeeId',10,'EMP.EMPLOYEE_ID < ?'",
        "'lesserThan','EmployeeEntity::employeeId',10,'EMP.EMPLOYEE_ID < ?'",
        "'le','EmployeeEntity::employeeId',10,'EMP.EMPLOYEE_ID <= ?'",
        "'lessEqual','EmployeeEntity::employeeId',10,'EMP.EMPLOYEE_ID <= ?'",
        "'like','EmployeeEntity::employeeId',ABC%,'EMP.EMPLOYEE_ID like ?'",
        "'notLike','EmployeeEntity::employeeId',ABC%,'EMP.EMPLOYEE_ID not like ?'",
        "'glob','EmployeeEntity::employeeId',ABC*,'EMP.EMPLOYEE_ID glob ?'",
        "'notGlob','EmployeeEntity::employeeId',ABC*,'EMP.EMPLOYEE_ID not glob ?'",
    )
    fun testColumnRefValueDsl(
        methodName: String,
        refString: String,
        value: String,
        expected: String,
    ) {
        val columnRef = changeColumnRef(refString)
        val state = createConditionBuilderTestState()

        with(state.builder) {
            when (methodName) {
                "eq" -> columnRef eq value
                "equal" -> columnRef equal value
                "ne" -> columnRef ne value
                "notEqual" -> columnRef notEqual value
                "gt" -> columnRef gt value
                "graterThan" -> columnRef graterThan value
                "ge" -> columnRef ge value
                "graterEqual" -> columnRef graterEqual value
                "lt" -> columnRef lt value
                "lesserThan" -> columnRef lesserThan value
                "le" -> columnRef le value
                "lessEqual" -> columnRef lessEqual value
                "like" -> columnRef like value
                "notLike" -> columnRef notLike value
                "glob" -> columnRef glob value
                "notGlob" -> columnRef notGlob value
                else -> error("Unsupported methodName: $methodName")
            }
        }

        assertEquals(expected, state.singleSql())
        assertEquals(listOf(value), state.valueHolder.bindValues)
    }

    /**
     * 「testColumnRefColumnDsl」の条件における期待動作を検証する。
     * @param methodName methodNameとして使用する値。
     * @param lhsRefString lhsRefStringとして使用する値。
     * @param rhsRefString rhsRefStringとして使用する値。
     * @param expected 期待値。
     * @author Masahiro Inoue
     * @since 2026-05-18
     */
    @DisplayName("ColumnRef 同士の比較 DSL を検証する")
    @ParameterizedTest(name = "[{index}] method={0}, lhs={1}, rhs={2}, expected={3}")
    @CsvSource(
        "'eq','EmployeeEntity::employeeId','EmployeeEntity::employeeId','EMP.EMPLOYEE_ID = EMP.EMPLOYEE_ID'",
        "'equal','EmployeeEntity::employeeId','EmployeeEntity::employeeId','EMP.EMPLOYEE_ID = EMP.EMPLOYEE_ID'",
        "'ne','EmployeeEntity::employeeId','EmployeeEntity::employeeId','EMP.EMPLOYEE_ID <> EMP.EMPLOYEE_ID'",
        "'notEqual','EmployeeEntity::employeeId','EmployeeEntity::employeeId','EMP.EMPLOYEE_ID <> EMP.EMPLOYEE_ID'",
        "'gt','EmployeeEntity::employeeId','EmployeeEntity::employeeId','EMP.EMPLOYEE_ID > EMP.EMPLOYEE_ID'",
        "'graterThan','EmployeeEntity::employeeId','EmployeeEntity::employeeId','EMP.EMPLOYEE_ID > EMP.EMPLOYEE_ID'",
        "'ge','EmployeeEntity::employeeId','EmployeeEntity::employeeId','EMP.EMPLOYEE_ID >= EMP.EMPLOYEE_ID'",
        "'graterEqual','EmployeeEntity::employeeId','EmployeeEntity::employeeId','EMP.EMPLOYEE_ID >= EMP.EMPLOYEE_ID'",
        "'lt','EmployeeEntity::employeeId','EmployeeEntity::employeeId','EMP.EMPLOYEE_ID < EMP.EMPLOYEE_ID'",
        "'lesserThan','EmployeeEntity::employeeId','EmployeeEntity::employeeId','EMP.EMPLOYEE_ID < EMP.EMPLOYEE_ID'",
        "'le','EmployeeEntity::employeeId','EmployeeEntity::employeeId','EMP.EMPLOYEE_ID <= EMP.EMPLOYEE_ID'",
        "'lessEqual','EmployeeEntity::employeeId','EmployeeEntity::employeeId','EMP.EMPLOYEE_ID <= EMP.EMPLOYEE_ID'",
    )
    fun testColumnRefColumnDsl(
        methodName: String,
        lhsRefString: String,
        rhsRefString: String,
        expected: String,
    ) {
        val lhs = changeColumnRef(lhsRefString)
        val rhs = changeColumnRef(rhsRefString)
        val state = createConditionBuilderTestState()

        with(state.builder) {
            when (methodName) {
                "eq" -> lhs eq rhs
                "equal" -> lhs equal rhs
                "ne" -> lhs ne rhs
                "notEqual" -> lhs notEqual rhs
                "gt" -> lhs gt rhs
                "graterThan" -> lhs graterThan rhs
                "ge" -> lhs ge rhs
                "graterEqual" -> lhs graterEqual rhs
                "lt" -> lhs lt rhs
                "lesserThan" -> lhs lesserThan rhs
                "le" -> lhs le rhs
                "lessEqual" -> lhs lessEqual rhs
                else -> error("Unsupported methodName: $methodName")
            }
        }
        assertEquals(expected, state.singleSql())
    }

    /**
     * 「testKPropertyListDsl」の条件における期待動作を検証する。
     * @param methodName methodNameとして使用する値。
     * @param valueCount valueCountとして使用する値。
     * @param expected 期待値。
     * @author Masahiro Inoue
     * @since 2026-05-18
     */
    @DisplayName("KProperty1 の IN / NOT IN DSL を検証する")
    @ParameterizedTest(name = "[{index}] method={0}, count={1}, expected={2}")
    @CsvSource(
        "'inList',3,'EMP.EMPLOYEE_ID in (?, ?, ?)'",
        "'notInList',5,'EMP.EMPLOYEE_ID not in (?, ?, ?, ?, ?)'",
    )
    fun testKPropertyListDsl(
        methodName: String,
        valueCount: Int,
        expected: String,
    ) {
        val values = List(valueCount) { it + 1 }
        val state = createConditionBuilderTestState()

        with(state.builder) {
            when (methodName) {
                "inList" -> EmployeeEntity::employeeId inList values
                "notInList" -> EmployeeEntity::employeeId notInList values
                else -> error("Unsupported methodName: $methodName")
            }
        }

        assertEquals(expected, state.singleSql())
        assertEquals(values, state.valueHolder.bindValues)
    }

    /**
     * 「testColumnRefListDsl」の条件における期待動作を検証する。
     * @param refString 列参照を表す文字列。
     * @param valueCount valueCountとして使用する値。
     * @param expected 期待値。
     * @author Masahiro Inoue
     * @since 2026-05-18
     */
    @DisplayName("ColumnRef の IN DSL を検証する")
    @ParameterizedTest(name = "[{index}] ref={0}, count={1}, expected={2}")
    @CsvSource(
        "'EmployeeEntity::employeeId',3,'EMP.EMPLOYEE_ID in (?, ?, ?)'",
    )
    fun testColumnRefListDsl(
        refString: String,
        valueCount: Int,
        expected: String,
    ) {
        @Suppress("UNCHECKED_CAST")
        val columnRef: ColumnRef<EmployeeEntity, Int> =
            changeColumnRef(refString) as ColumnRef<EmployeeEntity, Int>
        val values = List(valueCount) { it + 1 }
        val state = createConditionBuilderTestState()

        with(state.builder) {
            columnRef inList values
        }

        assertEquals(expected, state.singleSql())
        assertEquals(values, state.valueHolder.bindValues)
    }

    /**
     * 「testKPropertyBetweenDsl」の条件における期待動作を検証する。
     * @param methodName methodNameとして使用する値。
     * @param start startとして使用する値。
     * @param end endとして使用する値。
     * @param expected 期待値。
     * @author Masahiro Inoue
     * @since 2026-05-18
     */
    @DisplayName("KProperty1 の BETWEEN DSL を検証する")
    @ParameterizedTest(name = "[{index}] method={0}, start={1}, end={2}, expected={3}")
    @CsvSource(
        "'betweenAnd',10,50,'EMP.EMPLOYEE_ID between ? and ?'",
        "'betweenPair',10,50,'EMP.EMPLOYEE_ID between ? and ?'",
    )
    fun testKPropertyBetweenDsl(
        methodName: String,
        start: Int,
        end: Int,
        expected: String,
    ) {
        val state = createConditionBuilderTestState()

        with(state.builder) {
            when (methodName) {
                "betweenAnd" -> EmployeeEntity::employeeId between start and end
                "betweenPair" -> EmployeeEntity::employeeId between Pair(start, end)
                else -> error("Unsupported methodName: $methodName")
            }
        }

        assertEquals(expected, state.singleSql())
        assertEquals(listOf(start, end), state.valueHolder.bindValues)
    }

    /**
     * 「testColumnRefBetweenDsl」の条件における期待動作を検証する。
     * @param methodName methodNameとして使用する値。
     * @param refString 列参照を表す文字列。
     * @param start startとして使用する値。
     * @param end endとして使用する値。
     * @param expected 期待値。
     * @author Masahiro Inoue
     * @since 2026-05-18
     */
    @DisplayName("ColumnRef の BETWEEN DSL を検証する")
    @ParameterizedTest(name = "[{index}] method={0}, ref={1}, start={2}, end={3}, expected={4}")
    @CsvSource(
        "'betweenAnd','EmployeeEntity::employeeId',10,50,'EMP.EMPLOYEE_ID between ? and ?'",
        "'betweenPair','EmployeeEntity::employeeId',10,50,'EMP.EMPLOYEE_ID between ? and ?'",
    )
    fun testColumnRefBetweenDsl(
        methodName: String,
        refString: String,
        start: Int,
        end: Int,
        expected: String,
    ) {
        val columnRef = changeColumnRef(refString)
        val state = createConditionBuilderTestState()

        with(state.builder) {
            when (methodName) {
                "betweenAnd" -> columnRef between start and end
                "betweenPair" -> columnRef between Pair(start, end)
                else -> error("Unsupported methodName: $methodName")
            }
        }

        assertEquals(expected, state.singleSql())
        assertEquals(listOf(start, end), state.valueHolder.bindValues)
    }

    /**
     * 「testKPropertyNullDsl」の条件における期待動作を検証する。
     * @param methodName methodNameとして使用する値。
     * @param propertyString プロパティ参照を表す文字列。
     * @param expected 期待値。
     * @author Masahiro Inoue
     * @since 2026-05-18
     */
    @DisplayName("KProperty1 の NULL 判定 DSL を検証する")
    @ParameterizedTest(name = "[{index}] method={0}, property={1}, expected={2}")
    @CsvSource(
        "'isNull','EmployeeEntity::employeeSubId','EMP.EMPLOYEE_SUB_ID is null'",
        "'isNotNull','EmployeeEntity::employeeSubId','EMP.EMPLOYEE_SUB_ID is not null'",
        "'checkForNullIsNull','EmployeeEntity::employeeSubId','EMP.EMPLOYEE_SUB_ID is null'",
        "'checkForNullIsNotNull','EmployeeEntity::employeeSubId','EMP.EMPLOYEE_SUB_ID is not null'",
    )
    fun testKPropertyNullDsl(
        methodName: String,
        propertyString: String,
        expected: String,
    ) {
        val property = changeProperty(propertyString)
        val state = createConditionBuilderTestState()

        with(state.builder) {
            when (methodName) {
                "isNull" -> property isNull Unit
                "isNotNull" -> property isNotNull Unit
                "checkForNullIsNull" -> property checkForNull NullMarker.IS_NULL
                "checkForNullIsNotNull" -> property checkForNull NullMarker.IS_NOT_NULL
                else -> error("Unsupported methodName: $methodName")
            }
        }

        assertEquals(expected, state.singleSql())
        assertEquals(emptyList<Any?>(), state.valueHolder.bindValues)
    }

    /**
     * 「testColumnRefNullDsl」の条件における期待動作を検証する。
     * @param methodName methodNameとして使用する値。
     * @param refString 列参照を表す文字列。
     * @param expected 期待値。
     * @author Masahiro Inoue
     * @since 2026-05-18
     */
    @DisplayName("ColumnRef の NULL 判定 DSL を検証する")
    @ParameterizedTest(name = "[{index}] method={0}, ref={1}, expected={2}")
    @CsvSource(
        "'isNull','EmployeeEntity::employeeId','EMP.EMPLOYEE_ID is null'",
        "'isNotNull','EmployeeEntity::employeeId','EMP.EMPLOYEE_ID is not null'",
        "'checkForNullIsNull','EmployeeEntity::employeeId','EMP.EMPLOYEE_ID is null'",
        "'checkForNullIsNotNull','EmployeeEntity::employeeId','EMP.EMPLOYEE_ID is not null'",
    )
    fun testColumnRefNullDsl(
        methodName: String,
        refString: String,
        expected: String,
    ) {
        val columnRef = changeColumnRef(refString)
        val state = createConditionBuilderTestState()

        with(state.builder) {
            when (methodName) {
                "isNull" -> columnRef isNull Unit
                "isNotNull" -> columnRef isNotNull Unit
                "checkForNullIsNull" -> columnRef checkForNull NullMarker.IS_NULL
                "checkForNullIsNotNull" -> columnRef checkForNull NullMarker.IS_NOT_NULL
                else -> error("Unsupported methodName: $methodName")
            }
        }

        assertEquals(expected, state.singleSql())
        assertEquals(emptyList<Any?>(), state.valueHolder.bindValues)
    }

    /**
     * 「testFreeTextDsl」の条件における期待動作を検証する。
     * @param conditionText conditionTextとして使用する値。
     * @param value 追加する値。
     * @param expected 期待値。
     * @author Masahiro Inoue
     * @since 2026-05-18
     */
    @DisplayName("自由条件 DSL を検証する")
    @ParameterizedTest(name = "[{index}] condition={0}, value={1}, expected={2}")
    @CsvSource(
        "'EMP.EMPLOYEE_ID = ?','10','EMP.EMPLOYEE_ID = ?'",
    )
    fun testFreeTextDsl(
        conditionText: String,
        value: String,
        expected: String,
    ) {
        val state = createConditionBuilderTestState()

        state.builder.condition(conditionText, value)

        assertEquals(expected, state.singleSql())
        assertEquals(listOf(value), state.valueHolder.bindValues)
    }

    /**
     * 「testFreeTextErrorDsl」の条件における期待動作を検証する。
     * @param conditionText conditionTextとして使用する値。
     * @param value 追加する値。
     * @author Masahiro Inoue
     * @since 2026-05-18
     */
    @DisplayName("自由条件 DSL のプレースホルダー数不一致を検証する")
    @ParameterizedTest(name = "[{index}] condition={0}, value={1}")
    @CsvSource(
        "'EMP.EMPLOYEE_ID = ? AND EMP.NAME = ?',10",
    )
    fun testFreeTextErrorDsl(
        conditionText: String,
        value: String,
    ) {
        val state = createConditionBuilderTestState()

        assertThrows(IllegalArgumentException::class.java) {
            state.builder.condition(conditionText, value)
        }
    }

    /**
     * 「testExistsDsl」の条件における期待動作を検証する。
     * @param methodName methodNameとして使用する値。
     * @param expectedOperator expectedOperatorとして使用する値。
     * @author Masahiro Inoue
     * @since 2026-05-18
     */
    @DisplayName("EXISTS / NOT EXISTS DSL を検証する")
    @ParameterizedTest(name = "[{index}] method={0}, operator={1}")
    @CsvSource(
        "'exists','exists'",
        "'notExists','not exists'",
    )
    fun testExistsDsl(
        methodName: String,
        expectedOperator: String,
    ) {
        val subQuery = Select(EmployeeEntity::class)
        val state = createConditionBuilderTestState()

        with(state.builder) {
            when (methodName) {
                "exists" -> exists(subQuery)
                "notExists" -> notExists(subQuery)
                else -> error("Unsupported methodName: $methodName")
            }
        }

        val expected = "$expectedOperator (${subQuery.build()})"

        assertEquals(expected, state.singleSql())
        assertEquals(emptyList<Any?>(), state.valueHolder.bindValues)
    }

    /**
     * 「testExistsDsl_withSubQueryBindValues」の条件における期待動作を検証する。
     * @param methodName methodNameとして使用する値。
     * @param expectedOperator expectedOperatorとして使用する値。
     * @author Masahiro Inoue
     * @since 2026-05-18
     */
    @DisplayName("EXISTS / NOT EXISTS DSL はサブクエリの bindValues を引き継ぐ")
    @ParameterizedTest(name = "[{index}] method={0}, operator={1}")
    @CsvSource(
        "'exists','exists'",
        "'notExists','not exists'",
    )
    fun testExistsDsl_withSubQueryBindValues(
        methodName: String,
        expectedOperator: String,
    ) {
        val subQuery = createEmployeeIdSubQuery()
        val state = createConditionBuilderTestState()

        val operations = mapOf<String, ConditionBuilder.() -> Unit>(
            "exists" to {
                exists(subQuery)
            },
            "notExists" to {
                notExists(subQuery)
            },
        )

        with(state.builder) {
            operations.getValue(methodName).invoke(this)
        }

        assertEquals(
            "$expectedOperator (${subQuery.build()})",
            state.singleSql(),
        )
        assertEquals(
            listOf("EMP001"),
            state.valueHolder.bindValues,
        )
    }

    /**
     * 「testKPropertyInSelectDsl_withSubQueryBindValues」の条件における期待動作を検証する。
     * @param methodName methodNameとして使用する値。
     * @param expectedOperator expectedOperatorとして使用する値。
     * @author Masahiro Inoue
     * @since 2026-05-18
     */
    @DisplayName("KProperty1 の IN SELECT / NOT IN SELECT DSL を検証する")
    @ParameterizedTest(name = "[{index}] method={0}, operator={1}")
    @CsvSource(
        "'inSelect','in'",
        "'notInSelect','not in'",
    )
    fun testKPropertyInSelectDsl_withSubQueryBindValues(
        methodName: String,
        expectedOperator: String,
    ) {
        val subQuery = createEmployeeIdSubQuery()
        val state = createConditionBuilderTestState()

        val operations = mapOf<String, ConditionBuilder.() -> Unit>(
            "inSelect" to {
                EmployeeEntity::employeeId inSelect subQuery
            },
            "notInSelect" to {
                EmployeeEntity::employeeId notInSelect subQuery
            },
        )

        with(state.builder) {
            operations.getValue(methodName).invoke(this)
        }

        assertEquals(
            "EMP.EMPLOYEE_ID $expectedOperator (${subQuery.build()})",
            state.singleSql(),
        )
        assertEquals(
            listOf("EMP001"),
            state.valueHolder.bindValues,
        )
    }

    /**
     * 「testColumnRefInSelectDsl_withSubQueryBindValues」の条件における期待動作を検証する。
     * @param methodName methodNameとして使用する値。
     * @param expectedOperator expectedOperatorとして使用する値。
     * @author Masahiro Inoue
     * @since 2026-05-18
     */
    @DisplayName("ColumnRef の IN SELECT / NOT IN SELECT DSL を検証する")
    @ParameterizedTest(name = "[{index}] method={0}, operator={1}")
    @CsvSource(
        "'inSelect','in'",
        "'notInSelect','not in'",
    )
    fun testColumnRefInSelectDsl_withSubQueryBindValues(
        methodName: String,
        expectedOperator: String,
    ) {
        val employeeTable = TableRef(EmployeeEntity::class, "EMP_MAIN")
        val subQuery = createEmployeeIdSubQuery()
        val state = createConditionBuilderTestState()

        val operations = mapOf<String, ConditionBuilder.() -> Unit>(
            "inSelect" to {
                employeeTable[EmployeeEntity::employeeId] inSelect subQuery
            },
            "notInSelect" to {
                employeeTable[EmployeeEntity::employeeId] notInSelect subQuery
            },
        )

        with(state.builder) {
            operations.getValue(methodName).invoke(this)
        }

        assertEquals(
            "EMP_MAIN.EMPLOYEE_ID $expectedOperator (${subQuery.build()})",
            state.singleSql(),
        )
        assertEquals(
            listOf("EMP001"),
            state.valueHolder.bindValues,
        )
    }

    /**
     * 「testSubQueryDsl_addsBindValuesInConditionOrder」の条件における期待動作を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-18
     */
    @Test
    fun testSubQueryDsl_addsBindValuesInConditionOrder() {
        val subQuery = createEmployeeIdSubQuery()
        val state = createConditionBuilderTestState()

        with(state.builder) {
            EmployeeEntity::name eq "TARO"
            EmployeeEntity::employeeId inSelect subQuery
            EmployeeEntity::position eq "PG"
        }

        val actualSql = state.builder
            .buildList()
            .joinToString(" and ") { condition ->
                condition.build()
            }

        assertEquals(
            "EMP.NAME = ? " +
                    "and EMP.EMPLOYEE_ID in (${subQuery.build()}) " +
                    "and EMP.POSITION = ?",
            actualSql,
        )
        assertEquals(
            listOf("TARO", "EMP001", "PG"),
            state.valueHolder.bindValues,
        )
    }

    /**
     * 従業員IDを抽出するサブクエリを生成する。
     * @return 処理結果。
     * @author Masahiro Inoue
     * @since 2026-05-18
     */
    private fun createEmployeeIdSubQuery(): Select<EmployeeEntityIdSelection> =
        Select(EmployeeEntityIdSelection::class)
            .where {
                EmployeeEntityIdSelection::employeeId eq "EMP001"
            }
}