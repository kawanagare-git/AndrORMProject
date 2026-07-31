package jp.pgw.lab78.androrm.ksp.resolver

import com.google.devtools.ksp.symbol.ClassKind
import jp.pgw.lab78.androrm.ksp.testsupport.KspSymbolMockFactory.classDeclarationOf
import jp.pgw.lab78.androrm.ksp.testsupport.KspSymbolMockFactory.columnAnnotationOf
import jp.pgw.lab78.androrm.ksp.testsupport.KspSymbolMockFactory.propertyDeclarationOf
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * ## KSP用Columnアノテーション解決テスト
 * ### 実装先優先、インターフェース継承、多段継承、競合を検証する
 * @author Masahiro Inoue
 * @since 2026-07-31
 */
class KspColumnAnnotationResolverTest {

    private val target = KspColumnAnnotationResolver()

    /**
     * ## 実装先Column優先テスト
     * ### 実装先にColumnがある場合はインターフェース側を使用しない
     */
    @Test
    fun find_shouldPrioritizeImplementationAnnotation() {
        val implementationAnnotation = columnAnnotationOf(
            name = "IMPLEMENTATION_COLUMN",
            defaultValue = "0",
        )
        val interfaceAnnotation = columnAnnotationOf(
            name = "INTERFACE_COLUMN",
            defaultValue = "1",
        )
        val implementationProperty = propertyDeclarationOf(
            propertyName = "enabled",
            columnAnnotation = implementationAnnotation,
        )
        val interfaceProperty = propertyDeclarationOf(
            propertyName = "enabled",
            columnAnnotation = interfaceAnnotation,
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

        val actual = target.find(
            ownerClass = product,
            property = implementationProperty,
        )

        assertSame(implementationAnnotation, actual)
    }

    /**
     * ## インターフェースColumn取得テスト
     * ### 実装先にColumnがない場合はインターフェース側を取得する
     */
    @Test
    fun find_shouldReturnInterfaceAnnotation_whenImplementationHasNoAnnotation() {
        val interfaceAnnotation = columnAnnotationOf(
            name = "CREATED_AT",
            defaultValue = "CURRENT_TIMESTAMP_ISO",
        )
        val implementationProperty = propertyDeclarationOf(
            propertyName = "createdAt",
        )
        val interfaceProperty = propertyDeclarationOf(
            propertyName = "createdAt",
            columnAnnotation = interfaceAnnotation,
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

        val actual = target.find(
            ownerClass = product,
            property = implementationProperty,
        )

        assertSame(interfaceAnnotation, actual)
    }

    /**
     * ## 多段インターフェースColumn取得テスト
     * ### 直接実装したインターフェースにColumnがない場合は上位を検索する
     */
    @Test
    fun find_shouldReturnParentInterfaceAnnotation() {
        val parentAnnotation = columnAnnotationOf(
            name = "UPDATED_AT",
            defaultValue = "CURRENT_TIMESTAMP_ISO",
        )
        val implementationProperty = propertyDeclarationOf(
            propertyName = "updatedAt",
        )
        val parentProperty = propertyDeclarationOf(
            propertyName = "updatedAt",
            columnAnnotation = parentAnnotation,
        )
        val parentInterface = classDeclarationOf(
            qualifiedName = "test.ManagedEntity",
            classKind = ClassKind.INTERFACE,
            declaredProperties = listOf(parentProperty),
        )
        val childInterface = classDeclarationOf(
            qualifiedName = "test.ProductManagedEntity",
            classKind = ClassKind.INTERFACE,
            superClasses = listOf(parentInterface),
        )
        val product = classDeclarationOf(
            qualifiedName = "test.Product",
            superClasses = listOf(childInterface),
            allProperties = listOf(implementationProperty),
        )

        val actual = target.find(
            ownerClass = product,
            property = implementationProperty,
        )

        assertSame(parentAnnotation, actual)
    }

    /**
     * ## 同一定義重複テスト
     * ### 複数インターフェースのColumn設定が同じ場合は正常終了する
     */
    @Test
    fun find_shouldAcceptSameDefinitionsFromMultipleInterfaces() {
        val firstAnnotation = columnAnnotationOf(
            name = "ENABLED",
            defaultValue = "1",
        )
        val secondAnnotation = columnAnnotationOf(
            name = "ENABLED",
            defaultValue = "1",
        )
        val implementationProperty = propertyDeclarationOf(
            propertyName = "enabled",
        )
        val firstInterface = classDeclarationOf(
            qualifiedName = "test.FirstManagedEntity",
            classKind = ClassKind.INTERFACE,
            declaredProperties = listOf(
                propertyDeclarationOf(
                    propertyName = "enabled",
                    columnAnnotation = firstAnnotation,
                ),
            ),
        )
        val secondInterface = classDeclarationOf(
            qualifiedName = "test.SecondManagedEntity",
            classKind = ClassKind.INTERFACE,
            declaredProperties = listOf(
                propertyDeclarationOf(
                    propertyName = "enabled",
                    columnAnnotation = secondAnnotation,
                ),
            ),
        )
        val product = classDeclarationOf(
            qualifiedName = "test.Product",
            superClasses = listOf(
                firstInterface,
                secondInterface,
            ),
            allProperties = listOf(implementationProperty),
        )
        val actual = target.find(ownerClass = product, property = implementationProperty)
        assertTrue(actual === firstAnnotation || actual === secondAnnotation)
    }

    /**
     * ## 異なる定義競合テスト
     * ### 複数インターフェースのColumn設定が異なる場合はCE00016を送出する
     */
    @Test
    fun find_shouldThrow_whenDefinitionsConflict() {
        val implementationProperty = propertyDeclarationOf(
            propertyName = "enabled",
        )
        val firstInterface = classDeclarationOf(
            qualifiedName = "test.FirstManagedEntity",
            classKind = ClassKind.INTERFACE,
            declaredProperties = listOf(
                propertyDeclarationOf(
                    propertyName = "enabled",
                    columnAnnotation = columnAnnotationOf(
                        name = "ENABLED",
                        defaultValue = "1",
                    ),
                ),
            ),
        )
        val secondInterface = classDeclarationOf(
            qualifiedName = "test.SecondManagedEntity",
            classKind = ClassKind.INTERFACE,
            declaredProperties = listOf(
                propertyDeclarationOf(
                    propertyName = "enabled",
                    columnAnnotation = columnAnnotationOf(
                        name = "IS_ENABLED",
                        defaultValue = "0",
                    ),
                ),
            ),
        )
        val product = classDeclarationOf(
            qualifiedName = "test.Product",
            superClasses = listOf(
                firstInterface,
                secondInterface,
            ),
            allProperties = listOf(implementationProperty),
        )

        val actual = assertThrows(IllegalStateException::class.java) {
            target.find(
                ownerClass = product,
                property = implementationProperty,
            )
        }

        assertTrue(
            actual.message?.contains(
                "Conflicting @Column annotations",
            ) == true,
        )
        assertTrue(actual.message?.contains("enabled") == true)
        assertTrue(actual.message?.contains("test.Product") == true)
        assertTrue(
            actual.message?.contains("test.FirstManagedEntity") == true,
        )
        assertTrue(
            actual.message?.contains("test.SecondManagedEntity") == true,
        )
    }
}
