package jp.pgw.lab78.androrm.ksp.factory

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.Nullability
import com.squareup.kotlinpoet.BYTE_ARRAY
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger
import jp.pgw.lab78.androrm.ksp.resolver.KspColumnAnnotationResolver
import jp.pgw.lab78.androrm.ksp.testsupport.KspSymbolMockFactory.nameOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** 通常カラム生成がByteArrayの型とnull許容性を維持することを検証する。 */
class ColumnPropertyFactoryBlobTest {
    private lateinit var target: ColumnPropertyFactory

    /** KSPロガーと通常カラム生成器を初期化する。 */
    @BeforeEach
    fun setUp() {
        val environment = mock<SymbolProcessorEnvironment>()
        val logger = mock<KSPLogger>()
        whenever(environment.logger).thenReturn(logger)
        whenever(environment.options).thenReturn(mapOf("androrm.moduleDir" to "build/test-module"))
        CreateLogger.initialize(environment)
        target = ColumnPropertyFactory(mock<KspColumnAnnotationResolver>())
    }

    /** non-null ByteArrayをnullableや汎用配列へ変更せず生成する。 */
    @Test
    fun create_preservesNonNullByteArray() {
        val actual = target.create(mock<KSClassDeclaration>(), blobProperty(nullable = false))
        assertEquals(BYTE_ARRAY, actual.propertySpec.type)
        assertEquals("payload", actual.propertySpec.name)
    }

    /** ByteArrayのnull許容性を生成先へ引き継ぐ。 */
    @Test
    fun create_preservesNullableByteArray() {
        val actual = target.create(mock<KSClassDeclaration>(), blobProperty(nullable = true))
        assertEquals(BYTE_ARRAY.copy(nullable = true), actual.propertySpec.type)
        assertEquals("payload", actual.propertySpec.name)
    }

    /** KotlinPoetが解決するByteArrayのKSPシンボルを作成する。 */
    private fun blobProperty(nullable: Boolean): KSPropertyDeclaration {
        val declaration = mock<KSClassDeclaration>()
        val type = mock<KSType>()
        val reference = mock<KSTypeReference>()
        val property = mock<KSPropertyDeclaration>()
        val qualifiedName = nameOf("kotlin.ByteArray")
        val simpleName = nameOf("ByteArray")
        val packageName = nameOf("kotlin")
        val propertyName = nameOf("payload")
        whenever(declaration.qualifiedName).thenReturn(qualifiedName)
        whenever(declaration.simpleName).thenReturn(simpleName)
        whenever(declaration.packageName).thenReturn(packageName)
        whenever(declaration.classKind).thenReturn(ClassKind.CLASS)
        whenever(declaration.typeParameters).thenReturn(emptyList())
        whenever(type.declaration).thenReturn(declaration)
        whenever(type.arguments).thenReturn(emptyList<KSTypeArgument>())
        whenever(type.annotations).thenAnswer { emptySequence<KSAnnotation>() }
        whenever(type.isMarkedNullable).thenReturn(nullable)
        whenever(type.nullability).thenReturn(if (nullable) Nullability.NULLABLE else Nullability.NOT_NULL)
        whenever(reference.resolve()).thenReturn(type)
        whenever(property.type).thenReturn(reference)
        whenever(property.simpleName).thenReturn(propertyName)
        whenever(property.annotations).thenAnswer { emptySequence<KSAnnotation>() }
        return property
    }
}
