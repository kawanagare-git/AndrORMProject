package jp.pgw.lab78.androrm.common.meta

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * ## EntityMetaValidator テスト
 * ### EntityMeta の共通検証ルールを検証する
 * @author Masahiro Inoue
 * @since 2026-05-21
 */
class EntityMetaValidatorTest {

    /**
     * ## EntityMeta検証結果の検証
     * ### テストケースごとのエラー・警告件数とメッセージ内容が期待どおりであることを検証する
     * @param testCase 検証対象と期待結果を保持するテストケース
     * @author Masahiro Inoue
     * @since 2026-05-21
     */
    @DisplayName("EntityMetaValidator は EntityMeta の検証結果を正しく返す")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validateData")
    fun testValidate(
        testCase: ValidateTestCase,
    ) {
        val target = EntityMetaValidator()

        val actual = target.validate(testCase.entityMeta)

        assertEquals(
            testCase.expectedErrorCount > 0,
            actual.hasErrors,
            "hasErrors が期待値と異なります。case=${testCase.caseName}",
        )

        assertEquals(
            testCase.expectedWarningCount > 0,
            actual.hasWarnings,
            "hasWarnings が期待値と異なります。case=${testCase.caseName}",
        )

        assertEquals(
            testCase.expectedErrorCount,
            actual.errors.size,
            "errors の件数が期待値と異なります。case=${testCase.caseName}, errors=${actual.errors}",
        )

        assertEquals(
            testCase.expectedWarningCount,
            actual.warnings.size,
            "warnings の件数が期待値と異なります。case=${testCase.caseName}, warnings=${actual.warnings}",
        )

        assertContainsFragments(
            actual = actual.errors,
            expectedFragments = testCase.expectedErrorFragments,
            label = "errors",
            caseName = testCase.caseName,
        )

        assertContainsFragments(
            actual = actual.warnings,
            expectedFragments = testCase.expectedWarningFragments,
            label = "warnings",
            caseName = testCase.caseName,
        )
    }

    /**
     * ## 正常なEntityMetaの検証
     * ### 妥当なEntityMetaでエラーと警告が発生しないことを検証する
     * @param testCase 正常なEntityMetaを保持するテストケース
     * @author Masahiro Inoue
     * @since 2026-05-21
     */
    @DisplayName("正常な EntityMeta はエラー・警告なしになる")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validData")
    fun testValidateValidEntityMeta(
        testCase: ValidTestCase,
    ) {
        val target = EntityMetaValidator()

        val actual = target.validate(testCase.entityMeta)

        assertFalse(actual.hasErrors, "hasErrors は false を期待します。case=${testCase.caseName}")
        assertFalse(
            actual.hasWarnings,
            "hasWarnings は false を期待します。case=${testCase.caseName}"
        )
        assertEquals(emptyList<String>(), actual.errors)
        assertEquals(emptyList<String>(), actual.warnings)
    }

    /**
     * ## メッセージ部分一致検証
     * ### 実際のメッセージ一覧に期待する文字列断片がすべて含まれることを検証する
     * @param actual 実際のメッセージ一覧
     * @param expectedFragments 期待する文字列断片
     * @param label 検証対象を表すラベル
     * @param caseName テストケース名
     * @author Masahiro Inoue
     * @since 2026-05-21
     */
    private fun assertContainsFragments(
        actual: List<String>,
        expectedFragments: List<String>,
        label: String,
        caseName: String,
    ) {
        expectedFragments.forEach { expectedFragment ->
            assertTrue(
                actual.any { message -> message.contains(expectedFragment) },
                "$label に期待文字列が含まれていません。" +
                        " case=$caseName, expected=$expectedFragment, actual=$actual",
            )
        }
    }

    /**
     * ## 検証テストケース
     * ### EntityMetaValidator のエラー・警告検証用データ
     * @property caseName テストケース名
     * @property entityMeta 検証対象のEntityMeta
     * @property expectedErrorCount 期待するエラー件数
     * @property expectedWarningCount 期待する警告件数
     * @property expectedErrorFragments エラーメッセージに期待する文字列断片
     * @property expectedWarningFragments 警告メッセージに期待する文字列断片
     * @author Masahiro Inoue
     * @since 2026-05-21
     */
    data class ValidateTestCase(
        val caseName: String,
        val entityMeta: EntityMeta,
        val expectedErrorCount: Int,
        val expectedWarningCount: Int,
        val expectedErrorFragments: List<String>,
        val expectedWarningFragments: List<String>,
    ) {
        /**
         * ## テストケース名文字列化
         * @return テストケース名
         * @author Masahiro Inoue
         * @since 2026-05-21
         */
        override fun toString(): String = caseName
    }

    /**
     * ## 正常系テストケース
     * ### エラー・警告なしを期待する EntityMeta
     * @property caseName テストケース名
     * @property entityMeta 検証対象のEntityMeta
     * @author Masahiro Inoue
     * @since 2026-05-21
     */
    data class ValidTestCase(
        val caseName: String,
        val entityMeta: EntityMeta,
    ) {
        /**
         * ## テストケース名文字列化
         * @return テストケース名
         * @author Masahiro Inoue
         * @since 2026-05-21
         */
        override fun toString(): String = caseName
    }

    /**
     * ## EntityMetaValidatorテストデータ提供オブジェクト
     * ### 正常系およびエラー・警告検証用のMethodSourceを生成する
     * @author Masahiro Inoue
     * @since 2026-05-21
     */
    companion object {

        /**
         * ## 正常系テストデータ生成
         * ### エラーと警告が発生しないEntityMetaのテストケースを生成する
         * @return 正常系テスト引数
         * @author Masahiro Inoue
         * @since 2026-05-21
         */
        @JvmStatic
        fun validData(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    ValidTestCase(
                        caseName = "通常の表示対象プロパティ",
                        entityMeta = createEntityMeta(
                            entityName = "ValidEntity",
                            properties = listOf(
                                createPropertyMeta(
                                    propertyName = "employeeId",
                                    columnName = "EMPLOYEE_ID",
                                    aliasName = "EMPLOYEE_ID",
                                ),
                                createPropertyMeta(
                                    propertyName = "name",
                                    columnName = "NAME",
                                    aliasName = "NAME",
                                ),
                            ),
                        ),
                    ),
                ),
                Arguments.of(
                    ValidTestCase(
                        caseName = "空 alias は重複検証対象外",
                        entityMeta = createEntityMeta(
                            entityName = "BlankAliasEntity",
                            properties = listOf(
                                createPropertyMeta(
                                    propertyName = "employeeId",
                                    columnName = "EMPLOYEE_ID",
                                    aliasName = "",
                                ),
                                createPropertyMeta(
                                    propertyName = "name",
                                    columnName = "NAME",
                                    aliasName = "",
                                ),
                            ),
                        ),
                    ),
                ),
            )

        /**
         * ## 検証ルール別テストデータ生成
         * ### エラーまたは警告を期待するEntityMetaのテストケースを生成する
         * @return 検証ルール別テスト引数
         * @author Masahiro Inoue
         * @since 2026-05-21
         */
        @JvmStatic
        fun validateData(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    ValidateTestCase(
                        caseName = "@Column と @Function の併用は error",
                        entityMeta = createEntityMeta(
                            entityName = "DualAnnotationEntity",
                            properties = listOf(
                                createPropertyMeta(
                                    propertyName = "totalGross",
                                    columnName = "TOTAL_GROSS",
                                    aliasName = "TOTAL_GROSS",
                                    isFunction = true,
                                    hasColumnAnnotation = true,
                                    hasFunctionAnnotation = true,
                                ),
                                createPropertyMeta(
                                    propertyName = "employeeId",
                                    columnName = "EMPLOYEE_ID",
                                    aliasName = "EMPLOYEE_ID",
                                ),
                            ),
                        ),
                        expectedErrorCount = 1,
                        expectedWarningCount = 0,
                        expectedErrorFragments = listOf(
                            "cannot have both @Column and @Function",
                            "totalGross",
                            "DualAnnotationEntity",
                        ),
                        expectedWarningFragments = emptyList(),
                    ),
                ),
                Arguments.of(
                    ValidateTestCase(
                        caseName = "全プロパティ hideFromSelect=true は error",
                        entityMeta = createEntityMeta(
                            entityName = "AllHiddenEntity",
                            properties = listOf(
                                createPropertyMeta(
                                    propertyName = "employeeId",
                                    columnName = "EMPLOYEE_ID",
                                    aliasName = "EMPLOYEE_ID",
                                    hideFromSelect = true,
                                ),
                                createPropertyMeta(
                                    propertyName = "name",
                                    columnName = "NAME",
                                    aliasName = "NAME",
                                    hideFromSelect = true,
                                ),
                            ),
                        ),
                        expectedErrorCount = 1,
                        expectedWarningCount = 0,
                        expectedErrorFragments = listOf(
                            "has no selectable properties",
                            "All properties are hidden from SELECT",
                            "AllHiddenEntity",
                        ),
                        expectedWarningFragments = emptyList(),
                    ),
                ),
                Arguments.of(
                    ValidateTestCase(
                        caseName = "表示対象プロパティ同士の alias 重複は error",
                        entityMeta = createEntityMeta(
                            entityName = "DuplicateVisibleAliasEntity",
                            properties = listOf(
                                createPropertyMeta(
                                    propertyName = "employeeId",
                                    columnName = "EMPLOYEE_ID",
                                    aliasName = "DUP_ALIAS",
                                ),
                                createPropertyMeta(
                                    propertyName = "employeeSubId",
                                    columnName = "EMPLOYEE_SUB_ID",
                                    aliasName = "DUP_ALIAS",
                                ),
                            ),
                        ),
                        expectedErrorCount = 1,
                        expectedWarningCount = 0,
                        expectedErrorFragments = listOf(
                            "Duplicate alias 'DUP_ALIAS'",
                            "DuplicateVisibleAliasEntity",
                        ),
                        expectedWarningFragments = emptyList(),
                    ),
                ),
                Arguments.of(
                    ValidateTestCase(
                        caseName = "表示対象1件と非表示1件の alias 重複は warning",
                        entityMeta = createEntityMeta(
                            entityName = "DuplicateHiddenAliasEntity",
                            properties = listOf(
                                createPropertyMeta(
                                    propertyName = "employeeId",
                                    columnName = "EMPLOYEE_ID",
                                    aliasName = "DUP_ALIAS",
                                ),
                                createPropertyMeta(
                                    propertyName = "employeeSubId",
                                    columnName = "EMPLOYEE_SUB_ID",
                                    aliasName = "DUP_ALIAS",
                                    hideFromSelect = true,
                                ),
                            ),
                        ),
                        expectedErrorCount = 0,
                        expectedWarningCount = 1,
                        expectedErrorFragments = emptyList(),
                        expectedWarningFragments = listOf(
                            "Query construction is not affected",
                            "alias 'DUP_ALIAS'",
                            "DuplicateHiddenAliasEntity",
                        ),
                    ),
                ),
                Arguments.of(
                    ValidateTestCase(
                        caseName = "全非表示かつ alias 重複は error と warning",
                        entityMeta = createEntityMeta(
                            entityName = "AllHiddenDuplicateAliasEntity",
                            properties = listOf(
                                createPropertyMeta(
                                    propertyName = "employeeId",
                                    columnName = "EMPLOYEE_ID",
                                    aliasName = "DUP_ALIAS",
                                    hideFromSelect = true,
                                ),
                                createPropertyMeta(
                                    propertyName = "employeeSubId",
                                    columnName = "EMPLOYEE_SUB_ID",
                                    aliasName = "DUP_ALIAS",
                                    hideFromSelect = true,
                                ),
                            ),
                        ),
                        expectedErrorCount = 1,
                        expectedWarningCount = 1,
                        expectedErrorFragments = listOf(
                            "has no selectable properties",
                            "All properties are hidden from SELECT",
                            "AllHiddenDuplicateAliasEntity",
                        ),
                        expectedWarningFragments = listOf(
                            "Query construction is not affected",
                            "alias 'DUP_ALIAS'",
                            "AllHiddenDuplicateAliasEntity",
                        ),
                    ),
                ),
            )

        /**
         * ## EntityMeta生成
         * ### テスト用のEntity名とプロパティ一覧からEntityMetaを生成する
         * @param entityName Entity名
         * @param properties プロパティメタ情報一覧
         * @return テスト用EntityMeta
         * @author Masahiro Inoue
         * @since 2026-05-21
         */
        private fun createEntityMeta(
            entityName: String,
            properties: List<PropertyMeta>,
        ): EntityMeta =
            EntityMeta(
                defineEntityQualifiedName = "jp.pgw.lab78.androrm.test.$entityName",
                entityName = entityName,
                tableName = "TEST_TABLE",
                tableAlias = "TST",
                properties = properties,
            )

        /**
         * ## PropertyMeta生成
         * ### プロパティの定義情報からテスト用PropertyMetaを生成する
         * @param propertyName Kotlinプロパティ名
         * @param columnName DBカラム名
         * @param aliasName SELECT結果エイリアス
         * @param isFunction 関数列であるか
         * @param hideFromSelect SELECT対象から除外するか
         * @param hasColumnAnnotation Columnアノテーションがあるか
         * @param hasFunctionAnnotation Functionアノテーションがあるか
         * @return テスト用PropertyMeta
         * @author Masahiro Inoue
         * @since 2026-05-21
         */
        private fun createPropertyMeta(
            propertyName: String,
            columnName: String,
            aliasName: String,
            isFunction: Boolean = false,
            hideFromSelect: Boolean = false,
            hasColumnAnnotation: Boolean = true,
            hasFunctionAnnotation: Boolean = false,
        ): PropertyMeta =
            PropertyMeta(
                propertyName = propertyName,
                columnName = columnName,
                aliasName = aliasName,
                isFunction = isFunction,
                hideFromSelect = hideFromSelect,
                hasColumnAnnotation = hasColumnAnnotation,
                hasFunctionAnnotation = hasFunctionAnnotation,
            )
    }
}