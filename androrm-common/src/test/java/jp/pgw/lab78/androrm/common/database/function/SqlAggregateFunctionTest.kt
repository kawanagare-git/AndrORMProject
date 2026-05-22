package jp.pgw.lab78.androrm.common.database.function

import jp.pgw.lab78.androrm.common.dml.interfaces.SqlFunction.ArgumentArity
import jp.pgw.lab78.androrm.support.converter.AnyListConverter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.converter.ConvertWith
import org.junit.jupiter.params.provider.CsvSource

/**
 * ## SqlAggregateFunction テスト
 * ### 集約関数の関数名・引数タイプ・SQL 生成を検証する
 * @author Masahiro Inoue
 * @since 2026-05-21
 */
class SqlAggregateFunctionTest {

    @DisplayName("getFunctionName は enum 名を小文字の SQL 関数名として返せる")
    @ParameterizedTest(name = "[{index}] function={0}, expected={1}")
    @CsvSource(
        "COUNT, count",
        "COUNT_ALL, count",
        "SUM, sum",
        "AVG, avg",
        "MAX, max",
        "MIN, min",
        "TOTAL, total",
        "GROUP_CONCAT, group_concat",
    )
    fun testGetFunctionName(
        function: SqlAggregateFunction,
        expected: String,
    ) {
        assertEquals(expected, function.functionName)
    }

    @DisplayName("argType は関数ごとの引数タイプを返せる")
    @ParameterizedTest(name = "[{index}] function={0}, expected={1}")
    @CsvSource(
        "COUNT, SINGLE",
        "COUNT_ALL, NONE",
        "SUM, SINGLE",
        "AVG, SINGLE",
        "MAX, SINGLE",
        "MIN, SINGLE",
        "TOTAL, SINGLE",
        "GROUP_CONCAT, MULTI",
    )
    fun testArgType(
        function: SqlAggregateFunction,
        expected: ArgumentArity,
    ) {
        assertEquals(expected, function.argumentArity)
    }

    @DisplayName("build は単一引数の集約関数 SQL を生成できる")
    @ParameterizedTest(name = "[{index}] function={0}, args={1}, expected={2}")
    @CsvSource(
        "COUNT, employee_id, count(employee_id)",
        "SUM, gross, sum(gross)",
        "AVG, gross, avg(gross)",
        "MAX, gross, max(gross)",
        "MIN, gross, min(gross)",
        "TOTAL, gross, total(gross)",
    )
    fun testBuildSingleArgument(
        function: SqlAggregateFunction,

        @ConvertWith(AnyListConverter::class)
        args: List<Any?>,

        expected: String,
    ) {
        val actual = function.build(*args.toStringArray())

        assertEquals(expected, actual)
    }

    @DisplayName("build は単一引数関数に複数引数を渡した場合、先頭引数だけを使用する")
    @ParameterizedTest(name = "[{index}] function={0}, args={1}, expected={2}")
    @CsvSource(
        "COUNT, employee_id:name, count(employee_id)",
        "SUM, gross:deduction, sum(gross)",
        "AVG, gross:deduction, avg(gross)",
        "MAX, gross:deduction, max(gross)",
        "MIN, gross:deduction, min(gross)",
        "TOTAL, gross:deduction, total(gross)",
    )
    fun testBuildSingleArgumentUsesFirstArgumentOnly(
        function: SqlAggregateFunction,

        @ConvertWith(AnyListConverter::class)
        args: List<Any?>,

        expected: String,
    ) {
        val actual = function.build(*args.toStringArray())

        assertEquals(expected, actual)
    }

    @DisplayName("build は COUNT_ALL を count(*) として生成できる")
    @ParameterizedTest(name = "[{index}] args={0}, expected={1}")
    @CsvSource(
        "'', count(*)",
        "ignored_column, count(*)",
        "employee_id:name, count(*)",
    )
    fun testBuildCountAll(
        @ConvertWith(AnyListConverter::class)
        args: List<Any?>,

        expected: String,
    ) {
        val actual = SqlAggregateFunction.COUNT_ALL.build(*args.toStringArray())

        assertEquals(expected, actual)
    }

    @DisplayName("build は複数引数の集約関数 SQL を生成できる")
    @ParameterizedTest(name = "[{index}] function={0}, args={1}, expected={2}")
    @CsvSource(
        value = [
            "GROUP_CONCAT, name, group_concat(name)",
            "GROUP_CONCAT, |employee_id:'\\,'|, |group_concat(employee_id,',')|",
        ],
        quoteCharacter = '|',
    )
    fun testBuildMultiArgument(
        function: SqlAggregateFunction,
        @ConvertWith(AnyListConverter::class)
        args: List<Any?>,
        expected: String,
    ) {
        val actual = function.build(*args.toStringArray())
        assertEquals(expected, actual)
    }

    @DisplayName("単一引数関数は引数なしの場合に例外を投げる")
    @ParameterizedTest(name = "[{index}] function={0}")
    @CsvSource(
        "COUNT",
        "SUM",
        "AVG",
        "MAX",
        "MIN",
        "TOTAL",
    )
    fun testBuildSingleArgumentRequiresAtLeastOneArgument(
        function: SqlAggregateFunction,
    ) {
        val actual = assertThrows(IllegalArgumentException::class.java) {
            function.build()
        }
        assertEquals(
            "Aggregate functions require a column argument.",
            actual.message,
        )
    }

    @DisplayName("複数引数関数は引数なしまたは1件のみの場合に例外を投げる")
    @ParameterizedTest(name = "[{index}] args={0}")
    @CsvSource(
        "''",
        "name:/:*",
    )
    fun testBuildMultiArgumentRequiresAtLeastTwoArguments(
        @ConvertWith(AnyListConverter::class)
        args: List<String>,
    ) {
        val actual = assertThrows(IllegalArgumentException::class.java) {
            SqlAggregateFunction.GROUP_CONCAT.build(*args.toStringArray())
        }
        assertEquals(
            "Invalid number of arguments. Expected 1 or 2 arguments.",
            actual.message,
        )
    }

    /**
     * ## String 配列変換
     * ### AnyListConverter の戻り値を SqlAggregateFunction.build 用の String 配列へ変換する
     * @return String 配列
     */
    private fun List<Any?>.toStringArray(): Array<String> =
        filterNot {
            // @CsvSource で空文字を渡した場合、空引数扱いにする
            it == ""
        }.map { value ->
            requireNotNull(value) {
                "SqlAggregateFunction arguments do not allow null elements."
            }.toString()
        }.toTypedArray()
}