package jp.pgw.lab78.androrm.ksp.validator

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
import jp.pgw.lab78.androrm.common.Constants.DIRECTORY_DELIMITER
import jp.pgw.lab78.androrm.common.database.annotation.MigrationDefault
import jp.pgw.lab78.androrm.ksp.Constants.MIGRATION_DEFAULT_VALUE
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * ## MigrationDefault値KSP検証テスト
 * ### アノテーション値が生成前に共通規則で検証されることを確認する
 * @author Masahiro Inoue
 * @since 2026-07-18
 */
class MigrationDefaultValueValidatorTest {
    private lateinit var target: MigrationDefaultValueValidator
    private lateinit var logger: KSPLogger

    /**
     * ## テスト前処理
     * ### KSPロガーと検証対象を初期化する
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    @BeforeEach
    fun setUp() {
        val environment = mock<SymbolProcessorEnvironment>()
        logger = mock()
        whenever(environment.logger).thenReturn(logger)
        whenever(environment.options).thenReturn(
            mapOf("androrm.moduleDir" to "build${DIRECTORY_DELIMITER}test-module")
        )
        CreateLogger.initialize(environment)
        target = MigrationDefaultValueValidator()
    }

    /**
     * ## 有効なMigrationDefault値の検証
     * ### 正しい既定値が受理されエラーを出力しないことを確認する
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    @Test
    fun testValidate_withValidMigrationDefault_returnsTrue() {
        val entity = entityOf(propertyOf("status", "kotlin.Int", "0"))

        assertTrue(target.validate(entity))
        verify(logger, never()).error(any<String>(), anyOrNull())
    }

    /** ByteArray用BLOBリテラルのMigrationDefaultを受理する。 */
    @Test
    fun testValidate_withValidByteArrayMigrationDefault_returnsTrue() {
        val entity = entityOf(propertyOf("payload", "kotlin.ByteArray", "X'00017FFF'"))

        assertTrue(target.validate(entity))
        verify(logger, never()).error(any<String>(), anyOrNull())
    }

    /** 奇数桁BLOBリテラルのMigrationDefaultを拒否する。 */
    @Test
    fun testValidate_withOddLengthByteArrayMigrationDefault_returnsFalse() {
        val entity = entityOf(propertyOf("payload", "kotlin.ByteArray", "X'0'"))

        assertFalse(target.validate(entity))
    }

    /** 16進数以外を含むBLOBリテラルのMigrationDefaultを拒否する。 */
    @Test
    fun testValidate_withInvalidHexByteArrayMigrationDefault_returnsFalse() {
        val entity = entityOf(propertyOf("payload", "kotlin.ByteArray", "X'GG'"))

        assertFalse(target.validate(entity))
    }

    /**
     * ## 無効なBoolean既定値の検証
     * ### 検証失敗時にEntity名、プロパティ名および値がログへ出力されることを確認する
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    @Test
    fun testValidate_withInvalidBooleanMigrationDefault_returnsFalseAndLogsEntityProperty() {
        val entity = entityOf(propertyOf("enabled", "kotlin.Boolean", "true"))

        assertFalse(target.validate(entity))
        verify(logger).error(
            check<String> { message ->
                assertTrue(message.contains("MigrationEntity"))
                assertTrue(message.contains("enabled"))
                assertTrue(message.contains("true"))
            },
            anyOrNull(),
        )
    }

    /**
     * テスト用Entity宣言を生成する
     * @param property Entityへ設定するプロパティ宣言
     * @return モック化したEntity宣言
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private fun entityOf(property: KSPropertyDeclaration): KSClassDeclaration {
        val entity = mock<KSClassDeclaration>()
        val qualifiedName = nameOf("test.MigrationEntity")
        whenever(entity.qualifiedName).thenReturn(qualifiedName)
        whenever(entity.getAllProperties()).thenReturn(sequenceOf(property))
        return entity
    }

    /**
     * テスト用プロパティ宣言を生成する
     * @param name プロパティ名
     * @param typeName 完全修飾型名
     * @param value MigrationDefault値
     * @return モック化したプロパティ宣言
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private fun propertyOf(
        name: String,
        typeName: String,
        value: String,
    ): KSPropertyDeclaration {
        val property = mock<KSPropertyDeclaration>()
        val simpleName = nameOf(name)
        val typeReference = typeReferenceOf(typeName)
        val migrationDefault = migrationDefaultOf(value)
        whenever(property.simpleName).thenReturn(simpleName)
        whenever(property.type).thenReturn(typeReference)
        whenever(property.annotations).thenReturn(sequenceOf(migrationDefault))
        return property
    }

    /**
     * テスト用MigrationDefaultアノテーションを生成する
     * @param value アノテーション値
     * @return モック化したMigrationDefaultアノテーション
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private fun migrationDefaultOf(value: String): KSAnnotation {
        val annotation = mock<KSAnnotation>()
        val argument = mock<KSValueArgument>()
        val shortName = nameOf(MigrationDefault::class.simpleName!!)
        val annotationType = typeReferenceOf(MigrationDefault::class.qualifiedName!!)
        val argumentName = nameOf(MIGRATION_DEFAULT_VALUE)
        whenever(annotation.shortName).thenReturn(shortName)
        whenever(annotation.annotationType).thenReturn(annotationType)
        whenever(argument.name).thenReturn(argumentName)
        whenever(argument.value).thenReturn(value)
        whenever(annotation.arguments).thenReturn(listOf(argument))
        return annotation
    }

    /**
     * テスト用型参照を生成する
     * @param qualifiedName 完全修飾型名
     * @return モック化した型参照
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private fun typeReferenceOf(qualifiedName: String): KSTypeReference {
        val typeReference = mock<KSTypeReference>()
        val type = mock<KSType>()
        val declaration = mock<KSDeclaration>()
        val declarationQualifiedName = nameOf(qualifiedName)
        val declarationSimpleName = nameOf(qualifiedName.substringAfterLast('.'))
        whenever(typeReference.resolve()).thenReturn(type)
        whenever(type.isMarkedNullable).thenReturn(false)
        whenever(type.declaration).thenReturn(declaration)
        whenever(declaration.qualifiedName).thenReturn(declarationQualifiedName)
        whenever(declaration.simpleName).thenReturn(declarationSimpleName)
        return typeReference
    }

    /**
     * テスト用KSNameを生成する
     * @param value 名前の文字列表現
     * @return モック化したKSP名
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private fun nameOf(value: String): KSName {
        val name = mock<KSName>()
        whenever(name.asString()).thenReturn(value)
        whenever(name.getShortName()).thenReturn(value.substringAfterLast('.'))
        whenever(name.getQualifier()).thenReturn(value.substringBeforeLast('.', ""))
        return name
    }
}
