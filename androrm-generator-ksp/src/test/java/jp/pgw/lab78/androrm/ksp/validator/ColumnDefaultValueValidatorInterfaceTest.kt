package jp.pgw.lab78.androrm.ksp.validator

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import jp.pgw.lab78.androrm.common.Constants.DIRECTORY_DELIMITER
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger
import jp.pgw.lab78.androrm.ksp.projectoin.ProjectionDefinition
import jp.pgw.lab78.androrm.ksp.resolver.KspColumnAnnotationResolver
import jp.pgw.lab78.androrm.ksp.testsupport.KspSymbolMockFactory.classDeclarationOf
import jp.pgw.lab78.androrm.ksp.testsupport.KspSymbolMockFactory.columnAnnotationOf
import jp.pgw.lab78.androrm.ksp.testsupport.KspSymbolMockFactory.propertyDeclarationOf
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * ## インターフェースColumn既定値検証テスト
 * ### 継承元defaultの検証と実装先優先を確認する
 * @author Masahiro Inoue
 * @since 2026-07-31
 */
class ColumnDefaultValueValidatorInterfaceTest {

    private lateinit var target: ColumnDefaultValueValidator
    private lateinit var kspLogger: KSPLogger

    /** テスト前処理 */
    @BeforeEach
    fun setUp() {
        val environment = mock<SymbolProcessorEnvironment>()
        kspLogger = mock()

        whenever(environment.logger).thenReturn(kspLogger)
        whenever(environment.options).thenReturn(
            mapOf(
                "androrm.moduleDir" to
                    "build${DIRECTORY_DELIMITER}test-module",
            ),
        )

        CreateLogger.initialize(environment)
        target = ColumnDefaultValueValidator(
            columnAnnotationResolver =
                KspColumnAnnotationResolver(),
        )
    }

    /**
     * ## インターフェース既定値正常テスト
     * ### 実装先にColumnがないBooleanへdefault=1を適用できる
     */
    @Test
    fun validate_shouldReturnTrue_whenInterfaceDefaultIsValid() {
        val implementationProperty = propertyDeclarationOf(
            propertyName = "enabled",
            typeName = "kotlin.Boolean",
        )
        val interfaceProperty = propertyDeclarationOf(
            propertyName = "enabled",
            typeName = "kotlin.Boolean",
            columnAnnotation = columnAnnotationOf(
                defaultValue = "1",
            ),
        )
        val managedEntity = classDeclarationOf(
            qualifiedName = "test.ManagedEntity",
            classKind = ClassKind.INTERFACE,
            declaredProperties = listOf(interfaceProperty),
        )
        val product = classDeclarationOf(
            qualifiedName = "test.Product",
            superClasses = listOf(managedEntity),
            allProperties = listOf(implementationProperty),
        )

        val actual = target.validate(
            classDecl = product,
            definition = projectionDefinitionOf("enabled"),
        )

        assertTrue(actual)
        verify(kspLogger, never()).error(
            any<String>(),
            anyOrNull(),
        )
    }

    /**
     * ## 実装先既定値優先テスト
     * ### インターフェースが正常でも実装先の不正defaultを検証対象にする
     */
    @Test
    fun validate_shouldPrioritizeImplementationDefault() {
        val implementationProperty = propertyDeclarationOf(
            propertyName = "enabled",
            typeName = "kotlin.Boolean",
            columnAnnotation = columnAnnotationOf(
                defaultValue = "true",
            ),
        )
        val interfaceProperty = propertyDeclarationOf(
            propertyName = "enabled",
            typeName = "kotlin.Boolean",
            columnAnnotation = columnAnnotationOf(
                defaultValue = "1",
            ),
        )
        val managedEntity = classDeclarationOf(
            qualifiedName = "test.ManagedEntity",
            classKind = ClassKind.INTERFACE,
            declaredProperties = listOf(interfaceProperty),
        )
        val product = classDeclarationOf(
            qualifiedName = "test.Product",
            superClasses = listOf(managedEntity),
            allProperties = listOf(implementationProperty),
        )

        val actual = target.validate(
            classDecl = product,
            definition = projectionDefinitionOf("enabled"),
        )

        assertFalse(actual)
        verify(kspLogger).error(
            org.mockito.kotlin.check<String> { message ->
                assertTrue(message.contains("enabled"))
                assertTrue(message.contains("kotlin.Boolean"))
                assertTrue(message.contains("true"))
            },
            anyOrNull(),
        )
    }

    /** Projection定義生成 */
    private fun projectionDefinitionOf(
        propertyName: String,
    ): ProjectionDefinition =
        ProjectionDefinition(
            entityNameExtend = "Test",
            properties = listOf(
                ColumnProjection(property = propertyName),
            ),
        )
}
