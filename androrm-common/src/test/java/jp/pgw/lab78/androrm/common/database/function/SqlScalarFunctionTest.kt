package jp.pgw.lab78.androrm.common.database.function

import jp.pgw.lab78.androrm.common.Constants.D_QUOTE_CHAR
import jp.pgw.lab78.androrm.common.MessageConstants
import jp.pgw.lab78.androrm.common.dml.interfaces.SqlFunction.ArgumentArity
import jp.pgw.lab78.androrm.support.converter.AnyListConverter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.converter.ConvertWith
import org.junit.jupiter.params.provider.CsvSource

/**
 * ## SqlScalarFunction テスト
 * ### スカラー関数の関数名・引数タイプ・SQL 生成を検証する
 * @author Masahiro Inoue
 * @since 2026-05-22
 */
class SqlScalarFunctionTest {

    /**
     * ## getFunctionName は enum 名を小文字の SQL 関数名として返せる
     * ### SqlScalarFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("getFunctionName は enum 名を小文字の SQL 関数名として返せる")
    @ParameterizedTest(name = "[{index}] function={0}, expected={1}")
    @CsvSource(
        "ABS, abs",
        "LENGTH, length",
        "LOWER, lower",
        "UPPER, upper",
        "REPLACE, replace",
        "ROUND, round",
        "COALESCE, coalesce",
        "IFNULL, ifnull",
        "CAST, cast",
        "CONCAT, concat",
        "SUBSTR, substr",
        "TRIM, trim",
        "LTRIM, ltrim",
        "RTRIM, rtrim",
        "DATE, date",
        "TIME, time",
        "DATETIME, datetime",
        "STRFTIME, strftime",
        "JULIANDAY, julianday",
        "RANDOM, random",
        "SCALAR_MAX, max",
        "SCALAR_MIN, min",
        "CUSTOM, ''",
    )
    fun testGetFunctionName(
        function: SqlScalarFunction,
        expected: String,
    ) {
        assertEquals(expected, function.functionName)
    }

    /**
     * ## argType は関数ごとの引数タイプを返せる
     * ### SqlScalarFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("argType は関数ごとの引数タイプを返せる")
    @ParameterizedTest(name = "[{index}] function={0}, expected={1}")
    @CsvSource(
        "ABS, MULTI",
        "LENGTH, SINGLE",
        "LOWER, SINGLE",
        "UPPER, SINGLE",
        "REPLACE, MULTI",
        "ROUND, MULTI",
        "COALESCE, MULTI",
        "IFNULL, MULTI",
        "CAST, MULTI",
        "CONCAT, MULTI",
        "SUBSTR, MULTI",
        "TRIM, MULTI",
        "LTRIM, MULTI",
        "RTRIM, MULTI",
        "DATE, MULTI",
        "TIME, MULTI",
        "DATETIME, MULTI",
        "STRFTIME, MULTI",
        "JULIANDAY, MULTI",
        "RANDOM, NONE",
        "SCALAR_MAX, MULTI",
        "SCALAR_MIN, MULTI",
        "CUSTOM, SPECIAL",
    )
    fun testArgType(
        function: SqlScalarFunction,
        expected: ArgumentArity,
    ) {
        assertEquals(expected, function.argumentArity)
    }

    /**
     * ## build は単一引数のスカラー関数 SQL を生成できる
     * ### SqlScalarFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("build は単一引数のスカラー関数 SQL を生成できる")
    @ParameterizedTest(name = "[{index}] function={0}, args={1}, expected={2}")
    @CsvSource(
        "ABS, gross, abs(gross)",
        "LENGTH, name, length(name)",
        "LOWER, name, lower(name)",
        "UPPER, name, upper(name)",
        "SCALAR_MAX, contents:container, 'max(contents,container)'",
        "SCALAR_MIN, total:net, 'min(total,net)'",
    )
    fun testBuildSingleArgument(
        function: SqlScalarFunction,
        @ConvertWith(AnyListConverter::class)
        args: List<Any?>,
        expected: String,
    ) {
        val actual = function.build(*args.toStringArray())
        assertEquals(expected, actual)
    }

    /**
     * ## build は引数なしのスカラー関数 SQL を生成できる
     * ### SqlScalarFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("build は引数なしのスカラー関数 SQL を生成できる")
    @ParameterizedTest(name = "[{index}] function={0}, expected={1}")
    @CsvSource(
        "RANDOM, random()",
    )
    fun testBuildNoneArgument(
        function: SqlScalarFunction,
        expected: String,
    ) {
        val actual = function.build()
        assertEquals(expected, actual)
    }

    /**
     * ## build は複数引数のスカラー関数 SQL を生成できる
     * ### SqlScalarFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("build は複数引数のスカラー関数 SQL を生成できる")
    @ParameterizedTest(name = "[{index}] function={0}, args={1}, expected={2}")
    @CsvSource(
        value = [
            """SCALAR_MAX, gross:net, "max(gross,net)"""",
            """SCALAR_MIN, gross:net, "min(gross,net)"""",
            """REPLACE, "name:'old':'new'", "replace(name,'old','new')"""",
            """COALESCE, "name:nickname:'unknown'", "coalesce(name,nickname,'unknown')"""",
            """IFNULL, "name:'unknown'", "ifnull(name,'unknown')"""",
            """CONCAT, "first_name:last_name", "first_name || last_name"""",
            """SUBSTR, "name:1:3", "substr(name,1,3)"""",
            """TRIM, "name:' '" , "trim(name,' ')"""",
            """LTRIM, "name:' '" , "ltrim(name,' ')"""",
            """RTRIM, "name:' '" , "rtrim(name,' ')"""",
            """DATE, "created_at:'localtime'", "date(created_at,'localtime')"""",
            """TIME, "created_at:'localtime'", "time(created_at,'localtime')"""",
            """DATETIME, "created_at:'localtime'", "datetime(created_at,'localtime')"""",
            """STRFTIME, "'%Y-%m-%d':created_at", "strftime('%Y-%m-%d',created_at)"""",
            """JULIANDAY, "created_at:'localtime'", "julianday(created_at,'localtime')"""",
            """CAST, "age:text", "cast(age as text)"""",
        ],
        quoteCharacter = D_QUOTE_CHAR,
    )
    fun testBuildMultiArgument(
        function: SqlScalarFunction,
        @ConvertWith(AnyListConverter::class)
        args: List<Any?>,
        expected: String,
    ) {
        val actual = function.build(*args.toStringArray())
        assertEquals(expected, actual)
    }

    /**
     * ## ROUND は引数1件または2件の SQL を生成できる
     * ### SqlScalarFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("ROUND は引数1件または2件の SQL を生成できる")
    @ParameterizedTest(name = "[{index}] args={0}, expected={1}")
    @CsvSource(
        value = [
            "gross, round(gross)",
            """gross:2, "round(gross,2)"""",
        ],
        quoteCharacter = D_QUOTE_CHAR,
    )
    fun testBuildRound(
        @ConvertWith(AnyListConverter::class)
        args: List<Any?>,
        expected: String,
    ) {
        val actual = SqlScalarFunction.ROUND.build(*args.toStringArray())
        assertEquals(expected, actual)
    }

    /**
     * ## CAST は expression と type から SQL を生成できる
     * ### SqlScalarFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("CAST は expression と type から SQL を生成できる")
    @ParameterizedTest(name = "[{index}] args={0}, expected={1}")
    @CsvSource(
        value = [
            """gross:TEXT, "cast(gross as TEXT)"""",
        ],
        quoteCharacter = D_QUOTE_CHAR,
    )
    fun testBuildCast(
        @ConvertWith(AnyListConverter::class)
        args: List<Any?>,
        expected: String,
    ) {
        val actual = SqlScalarFunction.CAST.build(*args.toStringArray())
        assertEquals(expected, actual)
    }

    /**
     * ## CUSTOM は第1引数をそのまま SQL として返せる
     * ### SqlScalarFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("CUSTOM は第1引数をそのまま SQL として返せる")
    @ParameterizedTest(name = "[{index}] args={0}, expected={1}")
    @CsvSource(
        value = [
            "RAW_SQL, RAW_SQL",
            """"CASE WHEN gross > 0 THEN 1 ELSE 0 END", "CASE WHEN gross > 0 THEN 1 ELSE 0 END"""",
        ],
        quoteCharacter = D_QUOTE_CHAR,
    )
    fun testBuildCustom(
        @ConvertWith(AnyListConverter::class)
        args: List<Any?>,
        expected: String,
    ) {
        val actual = SqlScalarFunction.CUSTOM.build(*args.toStringArray())
        assertEquals(expected, actual)
    }

    /**
     * ## 単一引数関数は引数なしの場合に例外を投げる
     * ### SqlScalarFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("単一引数関数は引数なしの場合に例外を投げる")
    @ParameterizedTest(name = "[{index}] function={0}")
    @CsvSource(
        "ABS,'${MessageConstants.CE00002}'",
        "LENGTH,'${MessageConstants.CE00002}'",
        "LOWER,'${MessageConstants.CE00002}'",
        "UPPER,'${MessageConstants.CE00002}'",
    )
    fun testBuildSingleArgumentRequiresAtLeastOneArgument(
        function: SqlScalarFunction,
        expected: String,
    ) {
        val actual = assertThrows(IllegalArgumentException::class.java) {
            function.build()
        }
        assertEquals(expected, actual.message)
    }

    /**
     * ## 複数引数関数は引数が不足している場合に例外を投げる
     * ### SqlScalarFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("複数引数関数は引数が不足している場合に例外を投げる")
    @ParameterizedTest(name = "[{index}] function={0}, args={1}")
    @CsvSource(
        "REPLACE, <emptyList>, '${MessageConstants.CE00006}'",
        "REPLACE, name, '${MessageConstants.CE00006}'",
        "COALESCE, <emptyList> ,'${MessageConstants.CE00005}'",
        "COALESCE, name, '${MessageConstants.CE00005}'",
        "IFNULL, <emptyList>, '${MessageConstants.CE00004}'",
        "IFNULL, name, '${MessageConstants.CE00004}'",
        "CONCAT, <emptyList>, '${MessageConstants.CE00005}'",
        "CONCAT, name, '${MessageConstants.CE00005}'",
    )
    fun testBuildMultiArgumentRequiresAtLeastTwoArguments(
        function: SqlScalarFunction,
        @ConvertWith(AnyListConverter::class)
        args: List<Any?>,
        expected: String
    ) {
        val actual = assertThrows(IllegalArgumentException::class.java) {
            function.build(*args.toStringArray())
        }
        assertEquals(
            expected,
            actual.message,
        )
    }

    /**
     * ## ROUND は引数不適札の場合に例外を投げる
     * ### SqlScalarFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("ROUND は引数不適札の場合に例外を投げる")
    @ParameterizedTest(name = "[{index}] args={0}")
    @CsvSource(
        "<emptyList>, '${MessageConstants.CE00003}'",
        "gross:2:999, '${MessageConstants.CE00003}'",
    )
    fun testBuildRoundRequiresAtLeastOneArgument(
        @ConvertWith(AnyListConverter::class)
        args: List<Any?>,
        expected: String
    ) {
        val actual = assertThrows(IllegalArgumentException::class.java) {
            SqlScalarFunction.ROUND.build(*args.toStringArray())
        }
        assertEquals(
            expected,
            actual.message,
        )
    }

    /**
     * ## CAST は引数が不適切な場合に例外を投げる
     * ### SqlScalarFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("CAST は引数が不適切な場合に例外を投げる")
    @ParameterizedTest(name = "[{index}] args={0}")
    @CsvSource(
        "<emptyList>, '${MessageConstants.CE00004}'",
        "gross, '${MessageConstants.CE00004}'",
        "gross:INTEGER:ignored, '${MessageConstants.CE00004}'",
    )
    fun testBuildCastRequiresExpressionAndType(
        @ConvertWith(AnyListConverter::class)
        args: List<Any?>,
        expected: String
    ) {
        val actual = assertThrows(IllegalArgumentException::class.java) {
            SqlScalarFunction.CAST.build(*args.toStringArray())
        }
        assertEquals(
            expected,
            actual.message,
        )
    }

    /**
     * ## CUSTOM は引数なしの場合に例外を投げる
     * ### SqlScalarFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("CUSTOM は引数なしの場合に例外を投げる")
    @ParameterizedTest(name = "[{index}] args={0}")
    @CsvSource("<emptyList>")
    fun testBuildCustomRequiresAtLeastOneArgument(
        @ConvertWith(AnyListConverter::class)
        args: List<Any?>,
    ) {
        val actual = assertThrows(IllegalArgumentException::class.java) {
            SqlScalarFunction.CUSTOM.build(*args.toStringArray())
        }
        assertEquals(MessageConstants.CE00007, actual.message)
    }

    /**
     * ## String 配列変換
     * ### AnyListConverter の戻り値を SqlScalarFunction.build 用の String 配列へ変換する
     * @receiver 変換対象の引数リスト
     * @return 各要素を文字列化した配列
     * @throws IllegalArgumentException リストにnull要素が含まれる場合
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    private fun List<Any?>.toStringArray(): Array<String> =
        map { value ->
            requireNotNull(value) {
                "SqlScalarFunction arguments do not allow null elements."
            }.toString()
        }.toTypedArray()
}