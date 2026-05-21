package jp.pgw.lab78.androrm.database.condition

import jp.pgw.lab78.androrm.database.Select
import jp.pgw.lab78.androrm.database.condition.base.BaseConditionBuilder.NullMarker
import jp.pgw.lab78.androrm.database.entities.select.EmployeeEntity
import jp.pgw.lab78.androrm.database.reference.ColumnRef
import jp.pgw.lab78.androrm.database.support.SupportOperation.changeColumnRef
import jp.pgw.lab78.androrm.database.support.SupportOperation.changeProperty
import jp.pgw.lab78.androrm.database.support.createConditionBuilderTestState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * ## ConditionBuilder テスト
 * ### BaseConditionBuilder の DSL public メソッドを ConditionBuilder 経由で検証する
 */
class BaseConditionBuilderTest {

    @DisplayName("KProperty1 の値比較 DSL を検証する")
    @ParameterizedTest(name = "[{index}] method={0}, value={1}, expected={2}")
    @CsvSource(
        "'eq',10,'EMP.EMPLOYEE_ID = ?'",
        "'equal',10,'EMP.EMPLOYEE_ID = ?'",
        "'ne',10,'EMP.EMPLOYEE_ID <> ?'",
        "'norEqual',10,'EMP.EMPLOYEE_ID <> ?'",
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
                "norEqual" -> EmployeeEntity::employeeId norEqual value
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

    @DisplayName("ColumnRef の値比較 DSL を検証する")
    @ParameterizedTest(name = "[{index}] method={0}, ref={1}, value={2}, expected={3}")
    @CsvSource(
        "'eq','EmployeeEntity::employeeId',10,'EMP.EMPLOYEE_ID = ?'",
        "'equal','EmployeeEntity::employeeId',10,'EMP.EMPLOYEE_ID = ?'",
        "'ne','EmployeeEntity::employeeId',10,'EMP.EMPLOYEE_ID <> ?'",
        "'norEqual','EmployeeEntity::employeeId',10,'EMP.EMPLOYEE_ID <> ?'",
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
                "norEqual" -> columnRef norEqual value
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

    @DisplayName("ColumnRef 同士の比較 DSL を検証する")
    @ParameterizedTest(name = "[{index}] method={0}, lhs={1}, rhs={2}, expected={3}")
    @CsvSource(
        "'eq','EmployeeEntity::employeeId','EmployeeEntity::employeeId','EMP.EMPLOYEE_ID = EMP.EMPLOYEE_ID'",
        "'equal','EmployeeEntity::employeeId','EmployeeEntity::employeeId','EMP.EMPLOYEE_ID = EMP.EMPLOYEE_ID'",
        "'ne','EmployeeEntity::employeeId','EmployeeEntity::employeeId','EMP.EMPLOYEE_ID <> EMP.EMPLOYEE_ID'",
        "'norEqual','EmployeeEntity::employeeId','EmployeeEntity::employeeId','EMP.EMPLOYEE_ID <> EMP.EMPLOYEE_ID'",
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
                "norEqual" -> lhs norEqual rhs
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
}