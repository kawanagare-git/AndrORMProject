package jp.pgw.lab78.androrm.ksp.validator

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.*
import jp.pgw.lab78.androrm.common.Constants.DIRECTORY_DELIMITER
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_DEFAULT_VALUE
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger
import jp.pgw.lab78.androrm.ksp.projectoin.ProjectionDefinition
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.*
import java.util.stream.Stream

/**
 * ## カラム既定値検証テストクラス
 * ### Kotlin型とnull許容性に応じた既定値検証結果を確認する
 * @author Masahiro Inoue
 * @since 2026-06-13
 */
class ColumnDefaultValueValidatorTest {

    private lateinit var target: ColumnDefaultValueValidator
    private lateinit var mockKspLogger: KSPLogger

    /**
     * ## テストケース提供オブジェクト
     * ### 有効および無効な既定値のパラメータ化テストデータを提供する
     * @author Masahiro Inoue
     * @since 2026-06-13
     */
    companion object {
        private const val PROPERTY_NAME = "target"
        private const val TEST_CLASS_NAME =
            "jp.pgw.lab78.androrm.ksp.validator.DefaultValueTestEntity"

        private const val TYPE_INT = "kotlin.Int"
        private const val TYPE_LONG = "kotlin.Long"
        private const val TYPE_FLOAT = "kotlin.Float"
        private const val TYPE_DOUBLE = "kotlin.Double"
        private const val TYPE_BOOLEAN = "kotlin.Boolean"
        private const val TYPE_STRING = "kotlin.String"
        private const val TYPE_BYTE_ARRAY = "kotlin.ByteArray"
        private const val TYPE_LOCAL_DATE = "java.time.LocalDate"
        private const val TYPE_LOCAL_TIME = "java.time.LocalTime"
        private const val TYPE_LOCAL_DATE_TIME = "java.time.LocalDateTime"

        /**
         * ## 有効な既定値ケース生成
         * @return 有効な型、null許容性および既定値の組み合わせ
         * @author Masahiro Inoue
         * @since 2026-06-13
         */
        @JvmStatic
        fun validDefaultValueCases(): Stream<Arguments> = Stream.of(
            arguments("defaultValue 未指定", TYPE_STRING, false, ""),

            arguments("Int / 整数", TYPE_INT, false, "0"),
            arguments("Long / 負数", TYPE_LONG, false, "-100"),
            arguments("Int? / NULL", TYPE_INT, true, "NULL"),

            arguments("Float / 小数", TYPE_FLOAT, false, "1.25"),
            arguments("Double / 負小数", TYPE_DOUBLE, false, "-1.25"),
            arguments("Double? / NULL", TYPE_DOUBLE, true, "NULL"),

            arguments("Boolean / false", TYPE_BOOLEAN, false, "0"),
            arguments("Boolean / true", TYPE_BOOLEAN, false, "1"),
            arguments("Boolean? / NULL", TYPE_BOOLEAN, true, "NULL"),

            arguments("String / SQL文字列", TYPE_STRING, false, "'abc'"),
            arguments("String / SQL文字列内シングルクォート", TYPE_STRING, false, "'it''s'"),
            arguments("String? / NULL", TYPE_STRING, true, "NULL"),

            arguments("LocalDate / SQL文字列", TYPE_LOCAL_DATE, false, "'2026-06-13'"),
            arguments("LocalDate / CURRENT_DATE", TYPE_LOCAL_DATE, false, "CURRENT_DATE"),
            arguments("LocalDate? / NULL", TYPE_LOCAL_DATE, true, "NULL"),

            arguments("LocalTime / SQL文字列", TYPE_LOCAL_TIME, false, "'12:30:00'"),
            arguments("LocalTime / CURRENT_TIME", TYPE_LOCAL_TIME, false, "CURRENT_TIME"),
            arguments("LocalTime? / NULL", TYPE_LOCAL_TIME, true, "NULL"),

            arguments(
                "LocalDateTime / T区切り",
                TYPE_LOCAL_DATE_TIME,
                false,
                "'2026-06-13T12:30:00'"
            ),
            arguments(
                "LocalDateTime / 空白区切り",
                TYPE_LOCAL_DATE_TIME,
                false,
                "'2026-06-13 12:30:00'"
            ),
            arguments(
                "LocalDateTime / CURRENT_TIMESTAMP",
                TYPE_LOCAL_DATE_TIME,
                false,
                "CURRENT_TIMESTAMP"
            ),
            arguments(
                "LocalDateTime / CURRENT_TIMESTAMP_ISO",
                TYPE_LOCAL_DATE_TIME,
                false,
                "CURRENT_TIMESTAMP_ISO"
            ),
            arguments("LocalDateTime? / NULL", TYPE_LOCAL_DATE_TIME, true, "NULL"),

            arguments("ByteArray / defaultValue 未指定", TYPE_BYTE_ARRAY, false, ""),
            arguments("ByteArray? / NULL", TYPE_BYTE_ARRAY, true, "NULL"),
        )

        /**
         * ## 無効な既定値ケース生成
         * @return 無効な型、null許容性および既定値の組み合わせ
         * @author Masahiro Inoue
         * @since 2026-06-13
         */
        @JvmStatic
        fun invalidDefaultValueCases(): Stream<Arguments> = Stream.of(
            arguments("Int / SQL文字列", TYPE_INT, false, "'0'"),
            arguments("Int / NULL 非nullable", TYPE_INT, false, "NULL"),

            arguments("Float / SQL文字列", TYPE_FLOAT, false, "'1.25'"),
            arguments("Double / 数値以外", TYPE_DOUBLE, false, "abc"),

            arguments("Boolean / true 文字列", TYPE_BOOLEAN, false, "true"),
            arguments("Boolean / false 文字列", TYPE_BOOLEAN, false, "false"),
            arguments("Boolean / SQL文字列", TYPE_BOOLEAN, false, "'1'"),
            arguments("Boolean / NULL 非nullable", TYPE_BOOLEAN, false, "NULL"),

            arguments("String / クォートなし", TYPE_STRING, false, "abc"),
            arguments("String / NULL 非nullable", TYPE_STRING, false, "NULL"),

            arguments("LocalDate / 存在しない日付", TYPE_LOCAL_DATE, false, "'2026-02-30'"),
            arguments("LocalDate / CURRENT_TIME", TYPE_LOCAL_DATE, false, "CURRENT_TIME"),
            arguments("LocalDate / NULL 非nullable", TYPE_LOCAL_DATE, false, "NULL"),

            arguments("LocalTime / 存在しない時刻", TYPE_LOCAL_TIME, false, "'25:30:00'"),
            arguments("LocalTime / CURRENT_DATE", TYPE_LOCAL_TIME, false, "CURRENT_DATE"),
            arguments("LocalTime / NULL 非nullable", TYPE_LOCAL_TIME, false, "NULL"),

            arguments("LocalDateTime / 日付のみ", TYPE_LOCAL_DATE_TIME, false, "'2026-06-13'"),
            arguments(
                "LocalDateTime / 存在しない日時",
                TYPE_LOCAL_DATE_TIME,
                false,
                "'2026-02-30T12:30:00'"
            ),
            arguments(
                "LocalDateTime / 空白区切り存在しない日時",
                TYPE_LOCAL_DATE_TIME,
                false,
                "'2026-02-30 12:30:00'"
            ),
            arguments("LocalDateTime / CURRENT_DATE", TYPE_LOCAL_DATE_TIME, false, "CURRENT_DATE"),
            arguments("LocalDateTime / NULL 非nullable", TYPE_LOCAL_DATE_TIME, false, "NULL"),

            arguments("ByteArray / NULL 非nullable", TYPE_BYTE_ARRAY, false, "NULL"),
            arguments("ByteArray? / BLOB リテラル", TYPE_BYTE_ARRAY, true, "X'00'"),
        )
    }

    /**
     * ## テスト前処理
     * ### KSPロガーと検証対象を初期化する
     * @author Masahiro Inoue
     * @since 2026-06-13
     */
    @BeforeEach
    fun setUp() {
        val mockProcessor = mock<SymbolProcessorEnvironment>()
        mockKspLogger = mock<KSPLogger>()

        whenever(mockProcessor.logger).thenReturn(mockKspLogger)
        whenever(mockProcessor.options).thenReturn(
            mapOf("androrm.moduleDir" to "build${DIRECTORY_DELIMITER}test-module")
        )

        CreateLogger.initialize(mockProcessor)
        target = ColumnDefaultValueValidator()
    }

    /**
     * ## 有効な既定値の検証
     * @param caseName テストケース名
     * @param typeName プロパティの完全修飾型名
     * @param nullable null許容の場合true
     * @param defaultValue 検証する既定値
     * @author Masahiro Inoue
     * @since 2026-06-13
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("validDefaultValueCases")
    fun validate_shouldReturnTrue_whenDefaultValueIsValid(
        caseName: String,
        typeName: String,
        nullable: Boolean,
        defaultValue: String,
    ) {
        val classDecl = classDeclarationOf(
            propertyDeclarationOf(
                propertyName = PROPERTY_NAME,
                typeName = typeName,
                nullable = nullable,
                defaultValue = defaultValue,
            )
        )
        val definition = projectionDefinitionOf()
        val result = target.validate(classDecl, definition)
        assertTrue(result, caseName)
        verify(mockKspLogger, never()).error(any<String>(), anyOrNull())
    }

    /**
     * ## 無効な既定値の検証
     * @param caseName テストケース名
     * @param typeName プロパティの完全修飾型名
     * @param nullable null許容の場合true
     * @param defaultValue 検証する既定値
     * @author Masahiro Inoue
     * @since 2026-06-13
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidDefaultValueCases")
    fun validate_shouldReturnFalse_whenDefaultValueIsInvalid(
        caseName: String,
        typeName: String,
        nullable: Boolean,
        defaultValue: String,
    ) {
        val classDecl = classDeclarationOf(
            propertyDeclarationOf(
                propertyName = PROPERTY_NAME,
                typeName = typeName,
                nullable = nullable,
                defaultValue = defaultValue,
            )
        )
        val definition = projectionDefinitionOf()

        val result = target.validate(classDecl, definition)
        assertFalse(result, caseName)
        verify(mockKspLogger, atLeastOnce()).error(
            check<String> { message ->
                assertTrue(message.contains("Invalid @Column(defaultValue"), caseName)
                assertTrue(message.contains(PROPERTY_NAME), caseName)
                assertTrue(message.contains(typeName), caseName)
                assertTrue(message.contains(defaultValue), caseName)
            },
            anyOrNull(),
        )
    }

    /**
     * ## テスト用Projection定義生成
     * @return 既定値検証に使用するProjection定義
     * @author Masahiro Inoue
     * @since 2026-06-13
     */
    private fun projectionDefinitionOf(): ProjectionDefinition =
        ProjectionDefinition(
            entityNameExtend = "DefaultValueTest",
            properties = listOf(
                ColumnProjection(
                    property = PROPERTY_NAME,
                )
            ),
        )

    /**
     * ## テスト用クラス宣言生成
     * @param property クラスに含めるプロパティ宣言
     * @return モック化したクラス宣言
     * @author Masahiro Inoue
     * @since 2026-06-13
     */
    private fun classDeclarationOf(
        property: KSPropertyDeclaration,
    ): KSClassDeclaration {
        val classDecl = mock<KSClassDeclaration>()
        val qualifiedName = nameOf(TEST_CLASS_NAME)

        whenever(classDecl.qualifiedName).thenReturn(qualifiedName)
        whenever(classDecl.getAllProperties()).thenReturn(sequenceOf(property))

        return classDecl
    }

    /**
     * ## テスト用プロパティ宣言生成
     * @param propertyName プロパティ名
     * @param typeName 完全修飾型名
     * @param nullable null許容の場合true
     * @param defaultValue カラム既定値
     * @return モック化したプロパティ宣言
     * @author Masahiro Inoue
     * @since 2026-06-13
     */
    private fun propertyDeclarationOf(
        propertyName: String,
        typeName: String,
        nullable: Boolean,
        defaultValue: String,
    ): KSPropertyDeclaration {
        val property = mock<KSPropertyDeclaration>()
        val simpleName = nameOf(propertyName)
        val typeReference = typeReferenceOf(typeName, nullable)
        val columnAnnotation = columnAnnotationOf(defaultValue)

        whenever(property.simpleName).thenReturn(simpleName)
        whenever(property.type).thenReturn(typeReference)
        whenever(property.annotations).thenReturn(
            sequenceOf(columnAnnotation)
        )

        return property
    }

    /**
     * ## テスト用Columnアノテーション生成
     * @param defaultValue カラム既定値
     * @return モック化したColumnアノテーション
     * @author Masahiro Inoue
     * @since 2026-06-13
     */
    private fun columnAnnotationOf(
        defaultValue: String,
    ): KSAnnotation {
        val annotation = mock<KSAnnotation>()
        val shortName = nameOf(Column::class.simpleName!!)
        val annotationType = typeReferenceOf(Column::class.qualifiedName!!)
        val arguments = listOf(
            valueArgumentOf(
                name = COLUMN_DEFAULT_VALUE,
                value = defaultValue,
            )
        )

        whenever(annotation.shortName).thenReturn(shortName)
        whenever(annotation.annotationType).thenReturn(annotationType)
        whenever(annotation.arguments).thenReturn(arguments)

        return annotation
    }

    /**
     * ## テスト用アノテーション引数生成
     * @param name 引数名
     * @param value 引数値
     * @return モック化したアノテーション引数
     * @author Masahiro Inoue
     * @since 2026-06-13
     */
    private fun valueArgumentOf(
        name: String,
        value: String,
    ): KSValueArgument {
        val argument = mock<KSValueArgument>()
        val argumentName = nameOf(name)

        whenever(argument.name).thenReturn(argumentName)
        whenever(argument.value).thenReturn(value)

        return argument
    }

    /**
     * ## テスト用型参照生成
     * @param qualifiedName 完全修飾型名
     * @param nullable null許容の場合true
     * @return モック化した型参照
     * @author Masahiro Inoue
     * @since 2026-06-13
     */
    private fun typeReferenceOf(
        qualifiedName: String,
        nullable: Boolean = false,
    ): KSTypeReference {
        val typeReference = mock<KSTypeReference>()
        val type = mock<KSType>()
        val declaration = mock<KSDeclaration>()
        val declarationQualifiedName = nameOf(qualifiedName)
        val declarationSimpleName = nameOf(qualifiedName.substringAfterLast('.'))

        whenever(typeReference.resolve()).thenReturn(type)
        whenever(type.isMarkedNullable).thenReturn(nullable)
        whenever(type.declaration).thenReturn(declaration)
        whenever(declaration.qualifiedName).thenReturn(declarationQualifiedName)
        whenever(declaration.simpleName).thenReturn(declarationSimpleName)

        return typeReference
    }

    /**
     * ## テスト用KSP名生成
     * @param value 名前の文字列表現
     * @return モック化したKSP名
     * @author Masahiro Inoue
     * @since 2026-06-13
     */
    private fun nameOf(
        value: String,
    ): KSName {
        val name = mock<KSName>()

        whenever(name.asString()).thenReturn(value)
        whenever(name.getShortName()).thenReturn(value.substringAfterLast('.'))
        whenever(name.getQualifier()).thenReturn(value.substringBeforeLast('.', ""))

        return name
    }
}
