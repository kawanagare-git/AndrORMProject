package jp.pgw.lab78.androrm.ksp.testsupport

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_ALIAS
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_DEFAULT_VALUE
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_HIDE_FROM_SELECT
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_NAME
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * ## KSPテスト用モック生成
 * ### KSPシンボルを使用するUnit Testで共通利用する
 * @author Masahiro Inoue
 * @since 2026-07-31
 */
internal object KspSymbolMockFactory {

    /** KSP名生成 */
    fun nameOf(value: String): KSName {
        val result = mock<KSName>()

        whenever(result.asString()).thenReturn(value)
        whenever(result.getShortName()).thenReturn(
            value.substringAfterLast('.'),
        )
        whenever(result.getQualifier()).thenReturn(
            value.substringBeforeLast('.', ""),
        )

        return result
    }

    /** アノテーション引数生成 */
    fun valueArgumentOf(
        name: String,
        value: Any,
    ): KSValueArgument {
        val result = mock<KSValueArgument>()
        val argumentName = nameOf(name)

        whenever(result.name).thenReturn(argumentName)
        whenever(result.value).thenReturn(value)

        return result
    }

    /** Columnアノテーション生成 */
    fun columnAnnotationOf(
        name: String = "",
        alias: String = "",
        hideFromSelect: Boolean = false,
        defaultValue: String = "",
    ): KSAnnotation {
        val result = mock<KSAnnotation>()
        val shortName = nameOf(Column::class.simpleName!!)
        val annotationType =
            typeReferenceOf(Column::class.qualifiedName!!)
        val arguments =
            listOf(
                valueArgumentOf(COLUMN_NAME, name),
                valueArgumentOf(COLUMN_ALIAS, alias),
                valueArgumentOf(
                    COLUMN_HIDE_FROM_SELECT,
                    hideFromSelect,
                ),
                valueArgumentOf(
                    COLUMN_DEFAULT_VALUE,
                    defaultValue,
                ),
            )

        whenever(result.shortName).thenReturn(shortName)
        whenever(result.annotationType).thenReturn(annotationType)
        whenever(result.arguments).thenReturn(arguments)

        return result
    }

    /** プロパティ宣言生成 */
    fun propertyDeclarationOf(
        propertyName: String,
        typeName: String = "kotlin.String",
        nullable: Boolean = false,
        columnAnnotation: KSAnnotation? = null,
    ): KSPropertyDeclaration {
        val result = mock<KSPropertyDeclaration>()
        val simpleName = nameOf(propertyName)
        val typeReference =
            typeReferenceOf(
                qualifiedName = typeName,
                nullable = nullable,
            )
        val annotations = listOfNotNull(columnAnnotation)

        whenever(result.simpleName).thenReturn(simpleName)
        whenever(result.type).thenReturn(typeReference)
        whenever(result.annotations).thenAnswer {
            annotations.asSequence()
        }

        return result
    }

    /** クラス・インターフェース宣言生成 */
    fun classDeclarationOf(
        qualifiedName: String,
        classKind: ClassKind = ClassKind.CLASS,
        declaredProperties: List<KSPropertyDeclaration> = emptyList(),
        superClasses: List<KSClassDeclaration> = emptyList(),
        allProperties: List<KSPropertyDeclaration> = declaredProperties,
    ): KSClassDeclaration {
        val result = mock<KSClassDeclaration>()
        val qualifiedKsName = nameOf(qualifiedName)
        val simpleKsName =
            nameOf(qualifiedName.substringAfterLast('.'))
        val superTypeReferences =
            superClasses.map(::typeReferenceOf)

        whenever(result.qualifiedName).thenReturn(qualifiedKsName)
        whenever(result.simpleName).thenReturn(simpleKsName)
        whenever(result.classKind).thenReturn(classKind)
        whenever(result.annotations).thenAnswer { emptySequence<KSAnnotation>() }
        whenever(result.declarations).thenAnswer { declaredProperties.asSequence() }
        whenever(result.superTypes).thenAnswer { superTypeReferences.asSequence() }
        whenever(result.getAllProperties()).thenAnswer { allProperties.asSequence() }

        return result
    }

    /** 宣言を返す型参照生成 */
    private fun typeReferenceOf(
        declaration: KSDeclaration,
    ): KSTypeReference {
        val result = mock<KSTypeReference>()
        val type = mock<KSType>()

        whenever(result.resolve()).thenReturn(type)
        whenever(type.declaration).thenReturn(declaration)
        whenever(type.isMarkedNullable).thenReturn(false)

        return result
    }

    /** 完全修飾型名を持つ型参照生成 */
    private fun typeReferenceOf(
        qualifiedName: String,
        nullable: Boolean = false,
    ): KSTypeReference {
        val result = mock<KSTypeReference>()
        val type = mock<KSType>()
        val declaration = mock<KSDeclaration>()
        val qualifiedKsName = nameOf(qualifiedName)
        val simpleKsName =
            nameOf(qualifiedName.substringAfterLast('.'))

        whenever(result.resolve()).thenReturn(type)
        whenever(type.declaration).thenReturn(declaration)
        whenever(type.isMarkedNullable).thenReturn(nullable)
        whenever(declaration.qualifiedName).thenReturn(qualifiedKsName)
        whenever(declaration.simpleName).thenReturn(simpleKsName)

        return result
    }
}
