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
     */
    data class ValidateTestCase(
        val caseName: String,
        val entityMeta: EntityMeta,
        val expectedErrorCount: Int,
        val expectedWarningCount: Int,
        val expectedErrorFragments: List<String>,
        val expectedWarningFragments: List<String>,
    ) {
        override fun toString(): String = caseName
    }

    /**
     * ## 正常系テストケース
     * ### エラー・警告なしを期待する EntityMeta
     */
    data class ValidTestCase(
        val caseName: String,
        val entityMeta: EntityMeta,
    ) {
        override fun toString(): String = caseName
    }

    companion object {

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