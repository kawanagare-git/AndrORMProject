package jp.pgw.lab78.androrm.database.condition.interfaces

import jp.pgw.lab78.androrm.support.converter.AnyListConverter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.converter.ConvertWith
import org.junit.jupiter.params.provider.CsvSource

/**
 * ## QueryWithBindValues テスト
 * ### バインド値の追加・複数追加・クリア・追加バインド値合成を検証する
 * @author Masahiro Inoue
 * @since 2026-05-21
 */
class QueryWithBindValuesTest {

    /**
     * ## QueryWithBindValues テスト用具象クラス
     * ### abstract class をテストするための最小実装
     */
    private class TestQueryWithBindValues(
        private val additionalValues: List<Any?> = emptyList(),
    ) : QueryWithBindValues() {

        /**
         * ## バインド値追加
         * ### internal メソッドをテストから扱いやすくするためのラッパー
         */
        fun addValue(value: Any?) {
            addBindValue(value)
        }

        /**
         * ## バインド値複数追加
         * ### Iterator の中身が展開されることを検証するためのラッパー
         */
        fun addValues(values: List<Any?>) {
            addBindValues(values.iterator())
        }

        /**
         * ## バインド値クリア
         * ### protected メソッドをテストから扱いやすくするためのラッパー
         */
        fun clearValues() {
            clearBindValues()
        }

        /**
         * ## 追加バインド値取得
         * ### 後置追加したいバインド値を返す
         */
        override fun additionalBindValues(): List<Any?> = additionalValues
    }

    @DisplayName("addBindValue で値を1件追加できる")
    @ParameterizedTest(name = "[{index}] value={0}, expected={1}")
    @CsvSource(
        "ABC, ABC",
        "100, 100",
        "true, true",
        "false, false",
        "<null>, <null>",
        "<empty>, <empty>",
        """12\:30, 12\:30""",
        """http\://example.com, http\://example.com""",
        """jp\:pgw\:lab78, jp\:pgw\:lab78""",
    )
    fun testAddBindValue(
        @ConvertWith(AnyListConverter::class)
        valueList: List<Any?>,

        @ConvertWith(AnyListConverter::class)
        expected: List<Any?>,
    ) {
        val target = TestQueryWithBindValues()

        target.addValue(valueList.firstOrNull())

        assertEquals(expected, target.bindValues)
    }

    @DisplayName("addBindValues で Iterator の中身を展開して追加できる")
    @ParameterizedTest(name = "[{index}] values={0}, expected={1}")
    @CsvSource(
        "A:B:C, A:B:C",
        "1:2:3, 1:2:3",
        "A:<null>:3, A:<null>:3",
        "A:<empty>:3, A:<empty>:3",
        "true:false:<null>, true:false:<null>",
        """12\:30:http\://example.com:jp\:pgw\:lab78, 12\:30:http\://example.com:jp\:pgw\:lab78""",
        """C\\work:D, C\\work:D""",
    )
    fun testAddBindValues(
        @ConvertWith(AnyListConverter::class)
        values: List<Any?>,

        @ConvertWith(AnyListConverter::class)
        expected: List<Any?>,
    ) {
        val target = TestQueryWithBindValues()

        target.addValues(values)

        assertEquals(expected, target.bindValues)
    }

    @DisplayName("addBindValue と addBindValues を呼び出し順に保持できる")
    @ParameterizedTest(name = "[{index}] first={0}, values={1}, last={2}, expected={3}")
    @CsvSource(
        "FIRST, SECOND:100:<null>, LAST, FIRST:SECOND:100:<null>:LAST",
        "1, 2:3, 4, 1:2:3:4",
        "<empty>, A:B, <null>, <empty>:A:B:<null>",
        """12\:30, http\://example.com:jp\:pgw\:lab78, C\\work, 12\:30:http\://example.com:jp\:pgw\:lab78:C\\work""",
    )
    fun testAddBindValueAndAddBindValues(
        @ConvertWith(AnyListConverter::class)
        firstValueList: List<Any?>,

        @ConvertWith(AnyListConverter::class)
        values: List<Any?>,

        @ConvertWith(AnyListConverter::class)
        lastValueList: List<Any?>,

        @ConvertWith(AnyListConverter::class)
        expected: List<Any?>,
    ) {
        val target = TestQueryWithBindValues()

        target.addValue(firstValueList.firstOrNull())
        target.addValues(values)
        target.addValue(lastValueList.firstOrNull())

        assertEquals(expected, target.bindValues)
    }

    @DisplayName("additionalBindValues の値を bindValues の末尾に合成できる")
    @ParameterizedTest(name = "[{index}] values={0}, additional={1}, expected={2}")
    @CsvSource(
        "WHERE_VALUE, 10:20, WHERE_VALUE:10:20",
        "A:B:C, 1:2:3, A:B:C:1:2:3",
        "A:<null>:C, 10:<empty>:true, A:<null>:C:10:<empty>:true",
        """12\:30, http\://example.com:C\\work, 12\:30:http\://example.com:C\\work""",
    )
    fun testAdditionalBindValues(
        @ConvertWith(AnyListConverter::class)
        values: List<Any?>,

        @ConvertWith(AnyListConverter::class)
        additionalValues: List<Any?>,

        @ConvertWith(AnyListConverter::class)
        expected: List<Any?>,
    ) {
        val target = TestQueryWithBindValues(
            additionalValues = additionalValues,
        )

        target.addValues(values)

        assertEquals(expected, target.bindValues)
    }

    @DisplayName("clearBindValues で内部保持しているバインド値をクリアできる")
    @ParameterizedTest(name = "[{index}] values={0}, additional={1}")
    @CsvSource(
        "ABC:100, 10:20",
        "A:<null>:C, <empty>:true",
        """12\:30:C\\work, http\://example.com""",
    )
    fun testClearBindValues(
        @ConvertWith(AnyListConverter::class)
        values: List<Any?>,

        @ConvertWith(AnyListConverter::class)
        additionalValues: List<Any?>,
    ) {
        val target = TestQueryWithBindValues(
            additionalValues = additionalValues,
        )

        target.addValues(values)
        target.clearValues()

        assertEquals(additionalValues, target.bindValues)
    }

    @DisplayName("clearBindValues では additionalBindValues の値は消えない")
    @Test
    fun testClearBindValuesDoesNotClearAdditionalBindValues() {
        val additionalValues = listOf(10, 20)
        val target = TestQueryWithBindValues(
            additionalValues = additionalValues,
        )

        target.addValue("WHERE_VALUE")
        target.clearValues()

        assertEquals(additionalValues, target.bindValues)
    }
}