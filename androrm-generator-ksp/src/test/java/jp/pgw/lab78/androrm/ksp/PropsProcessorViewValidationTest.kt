package jp.pgw.lab78.androrm.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.EntityPackageInfo
import jp.pgw.lab78.androrm.common.annotation.FunctionProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.database.annotation.Function
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.annotation.View
import jp.pgw.lab78.androrm.common.dml.interfaces.ViewDefinitionEntity
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger
import jp.pgw.lab78.androrm.ksp.testsupport.KspSymbolMockFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.ByteArrayOutputStream

/** PropsProcessorの入口からVIEW定義検証を実行するテスト。 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class PropsProcessorViewValidationTest {
    private lateinit var processor: PropsProcessor
    private lateinit var resolver: Resolver
    private lateinit var logger: KSPLogger
    private lateinit var codeGenerator: CodeGenerator

    /** 各テストでKSPロガーとProcessor入口を初期化する。 */
    @BeforeEach
    fun setUp() {
        val environment = mock<SymbolProcessorEnvironment>()
        logger = mock()
        whenever(environment.logger).thenReturn(logger)
        whenever(environment.options).thenReturn(
            mapOf("androrm.moduleDir" to "build/processor-view-validation"),
        )
        CreateLogger.initialize(environment)
        codeGenerator = mock()
        whenever(codeGenerator.createNewFile(any(), any(), any(), any())).thenReturn(ByteArrayOutputStream())
        processor = PropsProcessor(codeGenerator)
        resolver = mock()
    }

    /** @Tableと@Viewの併用をProcessor入口で拒否する。 */
    @Test
    @Order(1)
    fun tableAndViewAreRejectedByProcessor() {
        assertRejected(classDeclaration(extraAnnotations = listOf(annotation("Table", Table::class.qualifiedName!!))))
    }

    /** View定義元プロパティの@FunctionをProcessor入口で拒否する。 */
    @Test
    @Order(2)
    fun sourceFunctionIsRejectedByProcessor() {
        val property = KspSymbolMockFactory.propertyDeclarationOf(
            propertyName = "name",
            columnAnnotation = annotation("Function", Function::class.qualifiedName!!),
        )
        assertRejected(classDeclaration(properties = listOf(property)))
    }

    /** FunctionProjectionをProcessor入口で拒否する。 */
    @Test
    @Order(3)
    fun functionProjectionIsRejectedByProcessor() {
        assertRejected(
            classDeclaration(
                projectionFunctions = listOf(functionProjection()),
            ),
        )
    }

    /** View定義元の@Column(hideFromSelect=true)を拒否する。 */
    @Test
    @Order(4)
    fun sourceHiddenColumnIsRejectedByProcessor() {
        val property = KspSymbolMockFactory.propertyDeclarationOf(
            propertyName = "name",
            columnAnnotation = KspSymbolMockFactory.columnAnnotationOf(hideFromSelect = true),
        )
        assertRejected(classDeclaration(properties = listOf(property)))
    }

    /** ColumnProjectionのhideFromSelect=trueを拒否する。 */
    @Test
    @Order(5)
    fun hiddenColumnProjectionIsRejectedByProcessor() {
        assertRejected(
            classDeclaration(
                projectionProperties = listOf(columnProjection(hideFromSelect = true)),
            ),
        )
    }

    /** SELECTとNOT_USE以外の共通Interfaceをすべて拒否する。 */
    @ParameterizedTest
    @EnumSource(
        value = DMLInterfaceEnum::class,
        names = ["INSERT", "UPDATE", "DELETE", "UPSERT", "ABSERT"],
    )
    @Order(6)
    fun unsupportedCommonInterfaceIsRejected(interfaceType: DMLInterfaceEnum) {
        assertRejected(classDeclaration(commonInterfaces = listOf(interfaceType)))
    }

    /** SELECT Projectionが存在しない定義を拒否する。 */
    @Test
    @Order(7)
    fun noSelectProjectionIsRejected() {
        assertRejected(classDeclaration(commonInterfaces = listOf(DMLInterfaceEnum.NOT_USE)))
    }

    /** View本体の実効物理名重複をProcessor入口で拒否する。 */
    @Test
    @Order(8)
    fun duplicateExplicitPhysicalNamesAreRejected() {
        val properties = listOf(
            KspSymbolMockFactory.propertyDeclarationOf(
                "first",
                columnAnnotation = KspSymbolMockFactory.columnAnnotationOf(name = "EMPLOYEE_ID"),
            ),
            KspSymbolMockFactory.propertyDeclarationOf(
                "second",
                columnAnnotation = KspSymbolMockFactory.columnAnnotationOf(name = "employee_id"),
            ),
        )
        assertRejected(classDeclaration(properties = properties))
    }

    /** implicit snake_caseと明示@Column.nameの重複をProcessor入口で拒否する。 */
    @Test
    @Order(9)
    fun duplicateImplicitAndExplicitPhysicalNamesAreRejected() {
        val properties = listOf(
            KspSymbolMockFactory.propertyDeclarationOf("employeeId"),
            KspSymbolMockFactory.propertyDeclarationOf(
                "other",
                columnAnnotation = KspSymbolMockFactory.columnAnnotationOf(name = "EMPLOYEE_ID"),
            ),
        )
        assertRejected(classDeclaration(properties = properties))
    }

    /** SELECT Projectionだけなら検証を通過することを確認する。 */
    @Test
    @Order(10)
    fun selectProjectionIsAcceptedByValidation() {
        assertDoesNotThrow {
            invokeProcessor(classDeclaration(commonInterfaces = listOf(DMLInterfaceEnum.SELECT)))
        }
        verify(logger, never()).error(any(), anyOrNull())
    }

    /** SELECTとNOT_USEの組合せを検証対象として受け入れる。 */
    @Test
    @Order(11)
    fun selectAndNotUseProjectionsAreAcceptedByValidation() {
        assertDoesNotThrow {
            invokeProcessor(
                classDeclaration(
                    commonInterfaces = listOf(DMLInterfaceEnum.SELECT),
                    additionalProjectionInterfaces = listOf(DMLInterfaceEnum.NOT_USE),
                ),
            )
        }
        verify(logger, never()).error(any(), anyOrNull())
    }

    /** 複数Projectionが同一View列を参照しても受け入れる。 */
    @Test
    @Order(12)
    fun repeatedProjectionColumnIsAcceptedByValidation() {
        assertDoesNotThrow {
            invokeProcessor(
                classDeclaration(
                    commonInterfaces = listOf(DMLInterfaceEnum.SELECT),
                    additionalProjectionInterfaces = listOf(DMLInterfaceEnum.SELECT),
                ),
            )
        }
        verify(logger, never()).error(any(), anyOrNull())
    }

    /** PropsProcessor.processを通し、エラーログが出たことを異常系の失敗とする。 */
    private fun assertRejected(classDeclaration: KSClassDeclaration) {
        invokeProcessor(classDeclaration)
        verify(logger, atLeastOnce()).error(any(), anyOrNull())
    }

    /** モックResolverから対象Viewを返してProcessorを実行する。 */
    private fun invokeProcessor(classDeclaration: KSClassDeclaration) {
        whenever(
            resolver.getSymbolsWithAnnotation(
                Projection::class.qualifiedName!!,
                false,
            ),
        ).thenReturn(sequenceOf(classDeclaration))
        whenever(
            resolver.getSymbolsWithAnnotation(
                "jp.pgw.lab78.androrm.common.annotation.Projections",
                false,
            ),
        ).thenReturn(emptySequence())
        whenever(
            resolver.getSymbolsWithAnnotation(EntityPackageInfo::class.qualifiedName!!, false),
        ).thenReturn(emptySequence())
        processor.process(resolver)
    }

    /** テスト用VIEW定義クラスを生成する。 */
    private fun classDeclaration(
        properties: List<KSPropertyDeclaration> = listOf(stringProperty("name")),
        extraAnnotations: List<KSAnnotation> = emptyList(),
        projectionProperties: List<KSAnnotation> = listOf(columnProjection()),
        projectionFunctions: List<KSAnnotation> = emptyList(),
        commonInterfaces: List<DMLInterfaceEnum> = listOf(DMLInterfaceEnum.SELECT),
        additionalProjectionInterfaces: List<DMLInterfaceEnum> = emptyList(),
    ): KSClassDeclaration {
        val projectionAnnotations = listOf(
            projectionAnnotation(
                properties = projectionProperties,
                functions = projectionFunctions,
                commonInterfaces = commonInterfaces,
            ),
        ) + additionalProjectionInterfaces.map { interfaceType ->
            projectionAnnotation(
                properties = listOf(columnProjection()),
                commonInterfaces = listOf(interfaceType),
            )
        }
        val marker = KspSymbolMockFactory.classDeclarationOf(
            ViewDefinitionEntity::class.qualifiedName!!,
        )
        val result = KspSymbolMockFactory.classDeclarationOf(
            "test.SampleView",
            classKind = ClassKind.CLASS,
            declaredProperties = properties,
            allProperties = properties,
            superClasses = listOf(marker),
        )
        val annotations = projectionAnnotations +
                listOf(annotation("View", View::class.qualifiedName!!)) + extraAnnotations
        val packageName = KspSymbolMockFactory.nameOf("test")
        whenever(result.annotations).thenReturn(annotations.asSequence())
        whenever(result.packageName).thenReturn(packageName)
        return result
    }

    /** @Projectionモックを生成する。 */
    private fun projectionAnnotation(
        properties: List<KSAnnotation>,
        functions: List<KSAnnotation> = emptyList(),
        commonInterfaces: List<DMLInterfaceEnum>,
    ): KSAnnotation = annotation(
        "Projection",
        Projection::class.qualifiedName!!,
        listOf(
            valueArgument("entityNameExtend", "Select"),
            valueArgument("properties", properties),
            valueArgument("functions", functions),
            valueArgument("commonInterface", commonInterfaces),
            valueArgument("customInterface", listOf("")),
        ),
    )

    /** ColumnProjectionモックを生成する。 */
    private fun columnProjection(hideFromSelect: Boolean = false): KSAnnotation = annotation(
        "ColumnProjection",
        ColumnProjection::class.qualifiedName!!,
        listOf(
            valueArgument("property", "name"),
            valueArgument("hideFromSelect", hideFromSelect),
        ),
    )

    /** FunctionProjectionモックを生成する。 */
    private fun functionProjection(): KSAnnotation = annotation(
        "FunctionProjection",
        FunctionProjection::class.qualifiedName!!,
        listOf(
            valueArgument("function", "LENGTH"),
            valueArgument("args", listOf("name")),
            valueArgument("alias", "nameLength"),
            valueArgument("hideFromSelect", false),
            valueArgument("raw", ""),
        ),
    )

    /** KSPアノテーションモックを生成する。 */
    private fun annotation(
        shortName: String,
        qualifiedName: String,
        arguments: List<KSValueArgument> = emptyList(),
    ): KSAnnotation {
        val result = mock<KSAnnotation>()
        val type = mock<KSType>()
        val declaration = mock<KSDeclaration>()
        val typeReference = mock<KSTypeReference>()
        val qualifiedKsName = KspSymbolMockFactory.nameOf(qualifiedName)
        val shortKsName = KspSymbolMockFactory.nameOf(shortName)
        whenever(declaration.qualifiedName).thenReturn(qualifiedKsName)
        whenever(type.declaration).thenReturn(declaration)
        whenever(result.shortName).thenReturn(shortKsName)
        whenever(typeReference.resolve()).thenReturn(type)
        whenever(result.annotationType).thenReturn(typeReference)
        whenever(result.arguments).thenReturn(arguments)
        return result
    }

    /** KSPアノテーション引数モックを生成する。 */
    private fun valueArgument(name: String, value: Any): KSValueArgument =
        KspSymbolMockFactory.valueArgumentOf(name, value)

    /** 正常系の生成処理まで進めるため、KotlinPoetが解決できるString型プロパティを生成する。 */
    private fun stringProperty(propertyName: String): KSPropertyDeclaration {
        val property = mock<KSPropertyDeclaration>()
        val typeReference = mock<KSTypeReference>()
        val type = mock<KSType>()
        val declaration = KspSymbolMockFactory.classDeclarationOf("kotlin.String")
        val propertyKsName = KspSymbolMockFactory.nameOf(propertyName)
        val packageName = KspSymbolMockFactory.nameOf("kotlin")
        whenever(declaration.packageName).thenReturn(packageName)
        whenever(property.simpleName).thenReturn(propertyKsName)
        whenever(property.annotations).thenReturn(emptySequence())
        whenever(type.declaration).thenReturn(declaration)
        whenever(type.isMarkedNullable).thenReturn(false)
        whenever(typeReference.resolve()).thenReturn(type)
        whenever(property.type).thenReturn(typeReference)
        return property
    }
}
