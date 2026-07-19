package jp.pgw.lab78.androrm.common.database.function

import jp.pgw.lab78.androrm.common.Constants.D_QUOTE_CHAR
import jp.pgw.lab78.androrm.common.dml.interfaces.SqlFunction
import jp.pgw.lab78.androrm.support.converter.AnyListConverter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.converter.ConvertWith
import org.junit.jupiter.params.provider.CsvSource

/**
 * ## ColumnFunction テスト
 * ### ColumnFunction のプロパティ・クエリ生成・戻り値型推論を検証する
 * @author Masahiro Inoue
 * @since 2026-05-22
 */
class ColumnFunctionTest {

    /**
     * ## ColumnFunction は定義された functionName / argumentArity / returnType を保持できる
     * ### ColumnFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("ColumnFunction は定義された functionName / argumentArity / returnType を保持できる")
    @ParameterizedTest(name = "[{index}] function={0}")
    @CsvSource(
        // 集約関数
        "COUNT, count, SINGLE, kotlin.Long",
        "COUNT_ALL, count, NONE, kotlin.Long",
        "SUM, sum, SINGLE, kotlin.Any",
        "AVG, avg, SINGLE, kotlin.Double",
        "MAX, max, SINGLE, kotlin.Any",
        "MIN, min, SINGLE, kotlin.Any",
        "GROUP_CONCAT, group_concat, MULTI, kotlin.String",

        // 文字列関数
        "LENGTH, length, SINGLE, kotlin.Int",
        "LOWER, lower, SINGLE, kotlin.String",
        "UPPER, upper, SINGLE, kotlin.String",
        "REPLACE, replace, MULTI, kotlin.String",
        "SUBSTR, substr, MULTI, kotlin.String",
        "CONCAT, concat, MULTI, kotlin.String",
        "TRIM, trim, MULTI, kotlin.String",
        "LTRIM, ltrim, MULTI, kotlin.String",
        "RTRIM, rtrim, MULTI, kotlin.String",

        // 日付関数
        "DATE, date, MULTI, kotlin.String",
        "TIME, time, MULTI, kotlin.String",
        "DATETIME, datetime, MULTI, kotlin.String",
        "STRFTIME, strftime, MULTI, kotlin.String",
        "JULIANDAY, julianday, MULTI, kotlin.Double",

        // 数値・判定・カスタム関数
        "ABS, abs, MULTI, kotlin.Any",
        "ROUND, round, MULTI, kotlin.Double",
        "RANDOM, random, NONE, kotlin.Long",
        "IFNULL, ifnull, MULTI, kotlin.Any",
        "COALESCE, coalesce, MULTI, kotlin.Any",
        "NULLIF, nullif, MULTI, kotlin.Any",
        "CUSTOM, '', SPECIAL, kotlin.Any",
    )
    fun testProperties(
        function: ColumnFunction,
        expectedQuery: String,
        expectedArgumentArity: SqlFunction.ArgumentArity,
        expectedReturnType: String,
    ) {
        assertEquals(expectedQuery, function.functionName)
        assertEquals(expectedArgumentArity, function.argumentArity)
        assertEquals(expectedReturnType, function.returnType)
    }

    /**
     * ## createQuery は単一引数関数のクエリを生成できる
     * ### ColumnFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("createQuery は単一引数関数のクエリを生成できる")
    @ParameterizedTest(name = "[{index}] function={0}, args={1}")
    @CsvSource(
        "COUNT, employee_id, count(employee_id)",
        "SUM, gross, sum(gross)",
        "AVG, gross, avg(gross)",
        "MAX, gross, max(gross)",
        "MIN, gross, min(gross)",
        "GROUP_CONCAT, name, group_concat(name)",
        "LENGTH, name, length(name)",
        "LOWER, name, lower(name)",
        "UPPER, name, upper(name)",
    )
    fun testCreateQuerySingleArgument(
        function: ColumnFunction,
        @ConvertWith(AnyListConverter::class)
        args: List<Any?>,
        expected: String,
    ) {
        val actual = function.build(*args.toStringArray())
        assertEquals(expected, actual)
    }

    /**
     * ## createQuery は単一引数関数に複数引数を渡した場合、先頭引数だけを使用する
     * ### ColumnFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("createQuery は単一引数関数に複数引数を渡した場合、先頭引数だけを使用する")
    @ParameterizedTest(name = "[{index}] function={0}, args={1}")
    @CsvSource(
        "COUNT, employee_id:name, count(employee_id)",
        "SUM, gross:deduction, sum(gross)",
        "AVG, gross:deduction, avg(gross)",
        "MAX, gross:deduction, max(gross)",
        "MIN, gross:deduction, min(gross)",
        quoteCharacter = D_QUOTE_CHAR,
    )
    fun testCreateQuerySingleArgumentUsesFirstArgumentOnly(
        function: ColumnFunction,
        @ConvertWith(AnyListConverter::class)
        args: List<Any?>,
        expected: String,
    ) {
        val actual = function.build(*args.toStringArray())
        assertEquals(expected, actual)
    }

    /**
     * ## createQuery は複数引数関数のクエリを生成できる
     * ### ColumnFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("createQuery は複数引数関数のクエリを生成できる")
    @ParameterizedTest(name = "[{index}] function={0}, args={1}")
    @CsvSource(
        value = [
            """GROUP_CONCAT, "name:','", "group_concat(name,',')"""",
            """REPLACE, "name:'old':'new'", "replace(name,'old','new')"""",
            """SUBSTR, "name:1:3", "substr(name,1,3)"""",
            """TRIM, "name:' '", "trim(name,' ')"""",
            """LTRIM, "name:' '", "ltrim(name,' ')"""",
            """RTRIM, "name:' '", "rtrim(name,' ')"""",
            """DATE, "created_at:'localtime'", "date(created_at,'localtime')"""",
            """TIME, "created_at:'localtime'", "time(created_at,'localtime')"""",
            """DATETIME, "created_at:'localtime'", "datetime(created_at,'localtime')"""",
            """STRFTIME, "'%Y-%m-%d':created_at", "strftime('%Y-%m-%d',created_at)"""",
            """JULIANDAY, "created_at:'localtime'", "julianday(created_at,'localtime')"""",
            "ABS, gross, abs(gross)",
            """IFNULL, "name:'unknown'", "ifnull(name,'unknown')"""",
            """COALESCE, "name:nickname:'unknown'", "coalesce(name,nickname,'unknown')"""",
            """NULLIF, "gross:0", "nullif(gross,0)"""",
        ],
        quoteCharacter = D_QUOTE_CHAR,
    )
    fun testCreateQueryMultiArgument(
        function: ColumnFunction,
        @ConvertWith(AnyListConverter::class)
        args: List<Any?>,
        expected: String,
    ) {
        val actual = function.build(*args.toStringArray())
        assertEquals(expected, actual)
    }

    /**
     * ## COUNT_ALL は引数に関係なく count(*) を生成できる
     * ### ColumnFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("COUNT_ALL は引数に関係なく count(*) を生成できる")
    @ParameterizedTest(name = "[{index}] args={0}")
    @CsvSource(
        "<emptyList>, count(*)",
        "employee_id, count(*)",
        "employee_id:name, count(*)",
    )
    fun testCreateQueryCountAll(
        @ConvertWith(AnyListConverter::class)
        args: List<Any?>,
        expected: String,
    ) {
        val actual = ColumnFunction.COUNT_ALL.build(*args.toStringArray())

        assertEquals(expected, actual)
    }

    /**
     * ## CONCAT は引数を SQLite の文字列連結演算子で結合できる
     * ### ColumnFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("CONCAT は引数を SQLite の文字列連結演算子で結合できる")
    @ParameterizedTest(name = "[{index}] args={0}")
    @CsvSource(
        "first_name:last_name, first_name || last_name",
        "first_name:' ':last_name, first_name || ' ' || last_name",
    )
    fun testCreateQueryConcat(
        @ConvertWith(AnyListConverter::class)
        args: List<Any?>,
        expected: String,
    ) {
        val actual = ColumnFunction.CONCAT.build(*args.toStringArray())
        assertEquals(expected, actual)
    }

    /**
     * ## ROUND は現在の実装どおり round() を生成する
     * ### ColumnFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("ROUND は現在の実装どおり round() を生成する")
    @ParameterizedTest(name = "[{index}] args={0}")
    @CsvSource(
        value = [
            "gross| round(gross)",
            "gross:2| round(gross,2)",
        ], delimiter = '|'
    )
    fun testCreateQueryRound(
        @ConvertWith(AnyListConverter::class)
        args: List<Any?>,
        expected: String,
    ) {
        val actual = ColumnFunction.ROUND.build(*args.toStringArray())
        assertEquals(expected, actual)
    }

    /**
     * ## RANDOM は引数なしで random() を生成できる
     * ### ColumnFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("RANDOM は引数なしで random() を生成できる")
    @ParameterizedTest(name = "[{index}] args={0}")
    @CsvSource(
        "<emptyList>, random()",
    )
    fun testCreateQueryRandom(
        @ConvertWith(AnyListConverter::class)
        args: List<Any?>,
        expected: String,
    ) {
        val actual = ColumnFunction.RANDOM.build(*args.toStringArray())
        assertEquals(expected, actual)
    }

    /**
     * ## CUSTOM は現在の実装どおり空の関数名でクエリを生成する
     * ### ColumnFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("CUSTOM は現在の実装どおり空の関数名でクエリを生成する")
    @ParameterizedTest(name = "[{index}] args={0}")
    @CsvSource(
        "RAW_SQL, RAW_SQL",
    )
    fun testCreateQueryCustom(
        @ConvertWith(AnyListConverter::class)
        args: List<Any?>,
        expected: String,
    ) {
        val actual = ColumnFunction.CUSTOM.build(*args.toStringArray())
        assertEquals(expected, actual)
    }

    /**
     * ## getReturnType は固定戻り値型を返せる
     * ### ColumnFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("getReturnType は固定戻り値型を返せる")
    @ParameterizedTest(name = "[{index}] function={0}")
    @CsvSource(
        "COUNT, kotlin.Long",
        "COUNT_ALL, kotlin.Long",
        "AVG, kotlin.Double",
        "GROUP_CONCAT, kotlin.String",
        "LENGTH, kotlin.Int",
        "LOWER, kotlin.String",
        "UPPER, kotlin.String",
        "REPLACE, kotlin.String",
        "SUBSTR, kotlin.String",
        "CONCAT, kotlin.String",
        "TRIM, kotlin.String",
        "LTRIM, kotlin.String",
        "RTRIM, kotlin.String",
        "DATE, kotlin.String",
        "TIME, kotlin.String",
        "DATETIME, kotlin.String",
        "STRFTIME, kotlin.String",
        "JULIANDAY, kotlin.Double",
        "ROUND, kotlin.Double",
        "RANDOM, kotlin.Long",
        "CUSTOM, kotlin.Any",
    )
    fun testGetReturnTypeFixed(
        function: ColumnFunction,
        expected: String,
    ) {
        val actual = function.getReturnType()
        assertEquals(expected, actual)
    }

    /**
     * ## SUM の getReturnType は引数型から戻り値型を推論できる
     * ### ColumnFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("SUM の getReturnType は引数型から戻り値型を推論できる")
    @ParameterizedTest(name = "[{index}] argTypes={0}")
    @CsvSource(
        "kotlin.Int, kotlin.Long",
        "kotlin.Long, kotlin.Long",
        "kotlin.Int:kotlin.Long, kotlin.Long",
        "kotlin.Double, kotlin.Double",
        "kotlin.Float, kotlin.Double",
        "kotlin.Int:kotlin.Double, kotlin.Double",
        "kotlin.String, kotlin.Any",
    )
    fun testGetReturnTypeSum(
        @ConvertWith(AnyListConverter::class)
        argTypes: List<Any?>,
        expected: String,
    ) {
        val actual = ColumnFunction.SUM.getReturnType(argTypes.toStringList())
        assertEquals(expected, actual)
    }

    /**
     * ## MAX / MIN の getReturnType は引数型の広い方を返せる
     * ### ColumnFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("MAX / MIN の getReturnType は引数型の広い方を返せる")
    @ParameterizedTest(name = "[{index}] function={0}, argTypes={1}")
    @CsvSource(
        "MAX, kotlin.Int:kotlin.Long, kotlin.Long",
        "MAX, kotlin.Int:kotlin.Double, kotlin.Double",
        "MAX, kotlin.Float:kotlin.Int, kotlin.Double",
        "MAX, kotlin.String:kotlin.Int, kotlin.String",
        "MIN, kotlin.Int:kotlin.Long, kotlin.Long",
        "MIN, kotlin.Int:kotlin.Double, kotlin.Double",
        "MIN, kotlin.Float:kotlin.Int, kotlin.Double",
        "MIN, kotlin.String:kotlin.Int, kotlin.String",
    )
    fun testGetReturnTypeMaxMin(
        function: ColumnFunction,
        @ConvertWith(AnyListConverter::class)
        argTypes: List<Any?>,
        expected: String,
    ) {
        val actual = function.getReturnType(argTypes.toStringList())
        assertEquals(expected, actual)
    }

    /**
     * ## ABS / IFNULL / COALESCE / NULLIF の getReturnType は引数型の広い方を返せる
     * ### ColumnFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("ABS / IFNULL / COALESCE / NULLIF の getReturnType は引数型の広い方を返せる")
    @ParameterizedTest(name = "[{index}] function={0}, argTypes={1}")
    @CsvSource(
        "ABS, kotlin.Int:kotlin.Long, kotlin.Long",
        "ABS, kotlin.Int:kotlin.Double, kotlin.Double",
        "ABS, kotlin.String:kotlin.Int, kotlin.String",
        "IFNULL, kotlin.Int:kotlin.Long, kotlin.Long",
        "IFNULL, kotlin.Int:kotlin.Double, kotlin.Double",
        "IFNULL, kotlin.String:kotlin.Int, kotlin.String",
        "COALESCE, kotlin.Int:kotlin.Long, kotlin.Long",
        "COALESCE, kotlin.Int:kotlin.Double, kotlin.Double",
        "COALESCE, kotlin.String:kotlin.Int, kotlin.String",
        "NULLIF, kotlin.Int:kotlin.Long, kotlin.Long",
        "NULLIF, kotlin.Int:kotlin.Double, kotlin.Double",
        "NULLIF, kotlin.String:kotlin.Int, kotlin.String",
    )
    fun testGetReturnTypeByWiderType(
        function: ColumnFunction,
        @ConvertWith(AnyListConverter::class)
        argTypes: List<Any?>,
        expected: String,
    ) {
        val actual = function.getReturnType(argTypes.toStringList())
        assertEquals(expected, actual)
    }

    /**
     * ## widerType は2つの型から広い型を返せる
     * ### ColumnFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("widerType は2つの型から広い型を返せる")
    @ParameterizedTest(name = "[{index}] type1={0}, type2={1}")
    @CsvSource(
        "kotlin.Int, kotlin.Int, kotlin.Int",
        "kotlin.Int, kotlin.Long, kotlin.Long",
        "kotlin.Long, kotlin.Int, kotlin.Long",
        "kotlin.Int, kotlin.Double, kotlin.Double",
        "kotlin.Float, kotlin.Int, kotlin.Double",
        "kotlin.String, kotlin.Int, kotlin.String",
        "java.time.LocalDateTime, java.math.BigDecimal, kotlin.Any",
    )
    fun testWiderType(
        type1: String,
        type2: String,
        expected: String,
    ) {
        val actual = ColumnFunction.SUM.widerType(type1, type2)
        assertEquals(expected, actual)
    }

    /**
     * ## resolveSumReturnType は SUM の戻り値型を推論できる
     * ### ColumnFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("resolveSumReturnType は SUM の戻り値型を推論できる")
    @ParameterizedTest(name = "[{index}] argTypes={0}")
    @CsvSource(
        "<emptyList>, kotlin.Long",
        "kotlin.Int, kotlin.Long",
        "kotlin.Long, kotlin.Long",
        "kotlin.Int:kotlin.Long, kotlin.Long",
        "kotlin.Double, kotlin.Double",
        "kotlin.Float, kotlin.Double",
        "kotlin.Int:kotlin.Float, kotlin.Double",
        "kotlin.String, kotlin.Any",
    )
    fun testResolveSumReturnType(
        @ConvertWith(AnyListConverter::class)
        argTypes: List<Any?>,
        expected: String,
    ) {
        val actual = ColumnFunction.SUM.resolveSumReturnType(argTypes.toStringList())
        assertEquals(expected, actual)
    }

    /**
     * ## ColumnFunction引数配列変換
     * ### テストデータの各要素を文字列へ変換し、可変長引数へ渡す配列を生成する
     * @receiver 変換対象の引数リスト
     * @return nullを含まない文字列配列
     * @throws IllegalArgumentException リストにnull要素が含まれる場合
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    private fun List<Any?>.toStringArray(): Array<String> =
        map { value ->
            requireNotNull(value) {
                "ColumnFunction arguments do not allow null elements."
            }.toString()
        }.toTypedArray()

    /**
     * ## ColumnFunction引数型リスト変換
     * ### テストデータの各要素を型名文字列へ変換する
     * @receiver 変換対象の型情報リスト
     * @return nullを含まない型名文字列リスト
     * @throws IllegalArgumentException リストにnull要素が含まれる場合
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    private fun List<Any?>.toStringList(): List<String> =
        map { value ->
            requireNotNull(value) {
                "ColumnFunction type arguments do not allow null elements."
            }.toString()
        }
}