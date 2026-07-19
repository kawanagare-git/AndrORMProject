package jp.pgw.lab78.androrm.common.meta

import jp.pgw.lab78.androrm.support.converter.AnyListConverter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.converter.ConvertWith
import org.junit.jupiter.params.provider.CsvSource

/**
 * ## EntityMetaValidationResult テスト
 * ### エラー・警告リストと判定プロパティの整合性を検証する
 * @author Masahiro Inoue
 * @since 2026-05-21
 */
class EntityMetaValidationResultTest {

    /**
     * ## エラー・警告有無判定の検証
     * ### errorsとwarningsの内容に応じてhasErrorsとhasWarningsが正しく判定されることを検証する
     * @param errors エラーメッセージのテストデータ
     * @param warnings 警告メッセージのテストデータ
     * @param expectedHasErrors エラー有無の期待値
     * @param expectedHasWarnings 警告有無の期待値
     * @author Masahiro Inoue
     * @since 2026-05-21
     */
    @DisplayName("errors と warnings の有無に応じて hasErrors / hasWarnings を判定できる")
    @ParameterizedTest(
        name = "[{index}] errors={0}, warnings={1}, hasErrors={2}, hasWarnings={3}",
    )
    @CsvSource(
        value = [
            "<emptyList>, <emptyList>, false, false",
            "ERROR_001, <emptyList>, true, false",
            "<emptyList>, WARNING_001, false, true",
            "ERROR_001:ERROR_002, WARNING_001:WARNING_002, true, true",
            """ERROR\:TIME, WARNING\^ROW, true, true""",
            """C\\work, http\://example.com, true, true""",
        ],
        nullValues = ["<emptyList>"],
    )
    fun testHasErrorsAndHasWarnings(
        @ConvertWith(AnyListConverter::class)
        errors: List<Any?>,

        @ConvertWith(AnyListConverter::class)
        warnings: List<Any?>,

        expectedHasErrors: Boolean,
        expectedHasWarnings: Boolean,
    ) {
        val target = EntityMetaValidationResult(
            errors = errors.toStringList(),
            warnings = warnings.toStringList(),
        )

        assertEquals(expectedHasErrors, target.hasErrors)
        assertEquals(expectedHasWarnings, target.hasWarnings)
    }

    /**
     * ## エラー・警告内容保持の検証
     * ### 指定したerrorsとwarningsが順序および内容を維持して保持されることを検証する
     * @param errors エラーメッセージのテストデータ
     * @param warnings 警告メッセージのテストデータ
     * @author Masahiro Inoue
     * @since 2026-05-21
     */
    @DisplayName("errors と warnings に指定した内容を保持できる")
    @ParameterizedTest(
        name = "[{index}] errors={0}, warnings={1}",
    )
    @CsvSource(
        value = [
            "<emptyList>, <emptyList>",
            "ERROR_001, <emptyList>",
            "<emptyList>, WARNING_001",
            "ERROR_001:ERROR_002, WARNING_001:WARNING_002",
            """ERROR\:TIME:C\\work, WARNING\^ROW:http\://example.com""",
        ],
        nullValues = ["<emptyList>"],
    )
    fun testErrorsAndWarningsValues(
        @ConvertWith(AnyListConverter::class)
        errors: List<Any?>,

        @ConvertWith(AnyListConverter::class)
        warnings: List<Any?>,
    ) {
        val expectedErrors = errors.toStringList()
        val expectedWarnings = warnings.toStringList()

        val target = EntityMetaValidationResult(
            errors = expectedErrors,
            warnings = expectedWarnings,
        )

        assertEquals(expectedErrors, target.errors)
        assertEquals(expectedWarnings, target.warnings)
    }

    /**
     * ## String List 変換
     * ### AnyListConverter の戻り値を EntityMetaValidationResult 用の List<String> に変換する
     * @receiver 変換対象のエラーまたは警告リスト
     * @return 各要素を文字列化したリスト
     * @throws IllegalArgumentException リストにnull要素が含まれる場合
     * @author Masahiro Inoue
     * @since 2026-05-21
     */
    private fun List<Any?>.toStringList(): List<String> =
        map { value ->
            requireNotNull(value) {
                "EntityMetaValidationResult errors/warnings do not allow null elements."
            }.toString()
        }
}