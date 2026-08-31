package jp.pgw.lab78.androrm.ksp.factory

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.asClassName
import jp.pgw.lab78.androrm.common.database.annotation.View
import jp.pgw.lab78.androrm.ksp.Constants.TABLE_ALIAS
import jp.pgw.lab78.androrm.ksp.Constants.TABLE_NAME
import jp.pgw.lab78.androrm.ksp.testsupport.KspSymbolMockFactory.nameOf
import jp.pgw.lab78.androrm.ksp.testsupport.KspSymbolMockFactory.valueArgumentOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * ## ViewAnnotationFactory テスト
 * @author Masahiro Inoue
 * @since 2026-08-31
 */
class ViewAnnotationFactoryTest {
    /** VIEW 名を維持し、aliasExtend をエイリアスへ付加することを確認する。 */
    @Test
    fun createPropagatesViewNameAndExtendsAlias() {
        val viewAnnotation = mock<KSAnnotation>()
        val viewShortName = nameOf(View::class.simpleName!!)
        val classSimpleName = nameOf("ActiveEmployeeViewDefinition")
        val viewArguments = listOf(
            valueArgumentOf(TABLE_NAME, "ACTIVE_EMPLOYEE"),
            valueArgumentOf(TABLE_ALIAS, "AV"),
        )
        whenever(viewAnnotation.shortName).thenReturn(viewShortName)
        whenever(viewAnnotation.arguments).thenReturn(viewArguments)
        val declaration = mock<KSClassDeclaration>()
        whenever(declaration.simpleName).thenReturn(classSimpleName)
        whenever(declaration.annotations).thenAnswer { sequenceOf(viewAnnotation) }

        val actual = ViewAnnotationFactory().create(declaration, "DETAIL")

        assertEquals(View::class.asClassName(), actual.typeName)
        assertTrue(actual.toString().contains("name = \"ACTIVE_EMPLOYEE\""))
        assertTrue(actual.toString().contains("alias = \"AV_DETAIL\""))
    }
}
