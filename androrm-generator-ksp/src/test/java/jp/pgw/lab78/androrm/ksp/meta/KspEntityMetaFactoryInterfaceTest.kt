package jp.pgw.lab78.androrm.ksp.meta

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import jp.pgw.lab78.androrm.common.Constants.DIRECTORY_DELIMITER
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger
import jp.pgw.lab78.androrm.ksp.projectoin.ProjectionDefinition
import jp.pgw.lab78.androrm.ksp.resolver.KspColumnAnnotationResolver
import jp.pgw.lab78.androrm.ksp.testsupport.KspSymbolMockFactory.classDeclarationOf
import jp.pgw.lab78.androrm.ksp.testsupport.KspSymbolMockFactory.columnAnnotationOf
import jp.pgw.lab78.androrm.ksp.testsupport.KspSymbolMockFactory.propertyDeclarationOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * ## KSP Entityメタ情報のインターフェースColumn対応テスト
 * @author Masahiro Inoue
 * @since 2026-07-31
 */
class KspEntityMetaFactoryInterfaceTest {

    private lateinit var target: KspEntityMetaFactory
    private lateinit var columnAnnotationResolver:
        KspColumnAnnotationResolver

    /** テスト前処理 */
    @BeforeEach
    fun setUp() {
        val environment = mock<SymbolProcessorEnvironment>()
        val kspLogger = mock<KSPLogger>()

        whenever(environment.logger).thenReturn(kspLogger)
        whenever(environment.options).thenReturn(
            mapOf(
                "androrm.moduleDir" to
                    "build${DIRECTORY_DELIMITER}test-module",
            ),
        )

        CreateLogger.initialize(environment)
        columnAnnotationResolver = mock()
        target = KspEntityMetaFactory(
            columnAnnotationResolver = columnAnnotationResolver,
        )
    }

    /**
     * ## 継承Columnメタ情報生成テスト
     * ### name・alias・hasColumnAnnotationへ解決結果を反映する
     */
    @Test
    fun create_shouldUseColumnAnnotationReturnedByResolver() {
        val property = propertyDeclarationOf(
            propertyName = "createdAt",
            typeName = "java.time.LocalDateTime",
        )
        val classDeclaration = classDeclarationOf(
            qualifiedName = "test.Product",
            allProperties = listOf(property),
        )
        val resolvedAnnotation = columnAnnotationOf(
            name = "CREATED_AT",
            alias = "CREATED_AT_ALIAS",
            defaultValue = "CURRENT_TIMESTAMP_ISO",
        )
        whenever(
            columnAnnotationResolver.find(
                ownerClass = classDeclaration,
                property = property,
            ),
        ).thenReturn(resolvedAnnotation)
        val definition = ProjectionDefinition(
            entityNameExtend = "Select",
            properties = listOf(
                ColumnProjection(
                    property = "createdAt",
                    hideFromSelect = false,
                ),
            ),
        )

        val actual = target.create(
            classDecl = classDeclaration,
            definition = definition,
        ).properties.single()

        assertEquals("createdAt", actual.propertyName)
        assertEquals("CREATED_AT", actual.columnName)
        assertEquals("CREATED_AT_ALIAS", actual.aliasName)
        assertTrue(actual.hasColumnAnnotation)
        assertFalse(actual.hasFunctionAnnotation)
        assertFalse(actual.hideFromSelect)
        verify(columnAnnotationResolver).find(
            ownerClass = classDeclaration,
            property = property,
        )
    }

    /**
     * ## Column未定義フォールバックテスト
     * ### Resolverがnullの場合はプロパティ名のスネークケースを使用する
     */
    @Test
    fun create_shouldUseSnakeCase_whenResolverReturnsNull() {
        val property = propertyDeclarationOf(
            propertyName = "createdAt",
            typeName = "java.time.LocalDateTime",
        )
        val classDeclaration = classDeclarationOf(
            qualifiedName = "test.Product",
            allProperties = listOf(property),
        )
        whenever(
            columnAnnotationResolver.find(
                ownerClass = classDeclaration,
                property = property,
            ),
        ).thenReturn(null)
        val definition = ProjectionDefinition(
            entityNameExtend = "Select",
            properties = listOf(
                ColumnProjection(property = "createdAt"),
            ),
        )

        val actual = target.create(
            classDecl = classDeclaration,
            definition = definition,
        ).properties.single()

        assertEquals("CREATED_AT", actual.columnName)
        assertEquals("CREATED_AT", actual.aliasName)
        assertFalse(actual.hasColumnAnnotation)
    }
}
