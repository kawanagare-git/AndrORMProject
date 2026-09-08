package jp.pgw.lab78.generated.ksp.factory

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import jp.pgw.lab78.androrm.common.Constants.DIRECTORY_DELIMITER
import jp.pgw.lab78.androrm.common.annotation.FunctionProjection
import jp.pgw.lab78.androrm.common.database.columns.base.AndrOrmValueType
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction
import jp.pgw.lab78.androrm.ksp.factory.FunctionPropertyFactory
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger
import jp.pgw.lab78.androrm.ksp.testsupport.KspSymbolMockFactory.propertyDeclarationOf
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import java.lang.reflect.InvocationTargetException

/**
 * ## 関数プロパティ生成テストクラス
 * ### SQL関数の引数として許可されるプロパティ名とリテラルを検証する
 * @author Masahiro Inoue
 * @since 2026-05-03
 */
class FunctionPropertyFactoryTest {

    /** SUBSTRのByteArray引数をreturnHintなしでByteArrayへ推論する。 */
    @Test
    fun createAll_substrByteArrayWithoutReturnHint_usesByteArrayReturnType() {
        val projection = FunctionProjection(
            function = ColumnFunction.SUBSTR,
            args = arrayOf("payload", "1", "2"),
            alias = "PAYLOAD_PART",
        )

        val actual = target.createAll(
            functions = listOf(projection),
            propsByName = mapOf(
                "payload" to propertyDeclarationOf(
                    propertyName = "payload",
                    typeName = "kotlin.ByteArray",
                ),
            ),
        ).single()

        assertEquals(com.squareup.kotlinpoet.BYTE_ARRAY, actual.propertySpec.type)
        assertEquals("payloadPart", actual.propertySpec.name)
    }

    /** FunctionProjection引数の外側空白を検証、型推論、生成アノテーションで統一して除去する。 */
    @Test
    fun createAll_normalizesFunctionArgumentOuterWhitespace() {
        val projection = FunctionProjection(
            function = ColumnFunction.SUBSTR,
            args = arrayOf(" payload ", " 1 ", " 2 "),
            alias = "PAYLOAD_PART",
        )

        val actual = target.createAll(
            functions = listOf(projection),
            propsByName = mapOf(
                "payload" to propertyDeclarationOf("payload", "kotlin.ByteArray"),
            ),
        ).single()

        assertEquals(com.squareup.kotlinpoet.BYTE_ARRAY, actual.propertySpec.type)
        assertTrue(actual.propertySpec.annotations.single().toString().contains("args = [\"payload\", \"1\", \"2\"]"))
    }

    /** BLOBリテラルをFunctionProjectionの引数として受理する。 */
    @ParameterizedTest
    @ValueSource(strings = ["X'00FF'", "X'00ff'", "x'00FF'", "x'00ff'", "X''", "x''"])
    fun checkFunctionArgs_blobLiteral_isAccepted(literal: String) {
        val projection = functionProjectionOf(
            function = ColumnFunction.COALESCE,
            alias = "PAYLOAD_VALUE",
            "payload",
            literal,
        )

        assertDoesNotThrow {
            invokeCheckFunctionArgs(projection, setOf("payload"))
        }
    }

    /** BLOBリテラルをByteArrayとしてCOALESCEの戻り値型推論へ渡す。 */
    @Test
    fun createAll_coalesceWithBlobLiteral_usesByteArrayReturnType() {
        val projection = functionProjectionOf(
            function = ColumnFunction.COALESCE,
            alias = "PAYLOAD_VALUE",
            "payload",
            "X''",
        )

        val actual = target.createAll(
            functions = listOf(projection),
            propsByName = mapOf(
                "payload" to propertyDeclarationOf(
                    propertyName = "payload",
                    typeName = "kotlin.ByteArray",
                ),
            ),
        ).single()

        assertEquals(com.squareup.kotlinpoet.BYTE_ARRAY, actual.propertySpec.type)
    }

    /** 奇数桁または16進数以外のBLOBリテラルを拒否する。 */
    @ParameterizedTest
    @ValueSource(strings = ["X'0'", "X'GG'", "X'001'", "X'00G0'"])
    fun checkFunctionArgs_invalidBlobLiteral_logsError(literal: String) {
        val projection = functionProjectionOf(
            function = ColumnFunction.COALESCE,
            alias = "INVALID_BLOB",
            literal,
        )

        invokeCheckFunctionArgs(projection, emptySet())

        verify(mockKspLogger, atLeastOnce()).error(
            check<String> { message ->
                assertTrue(message.contains("is invalid"))
                assertTrue(message.contains(literal))
            },
            anyOrNull(),
        )
    }

    /** 型推論不能なraw BLOB関数は要件7.2のreturnHintからByteArrayを生成する。 */
    @Test
    fun createAll_rawBlobFunction_usesByteArrayReturnHint() {
        val projection = FunctionProjection(
            function = ColumnFunction.CUSTOM,
            args = arrayOf(),
            alias = "BINARY_VALUE",
            raw = "substr(X'00017F80FF', 1, 5)",
            returnHint = AndrOrmValueType.BYTE_ARRAY,
        )
        val actual = target.createAll(listOf(projection), emptyMap()).single()
        assertEquals(com.squareup.kotlinpoet.BYTE_ARRAY, actual.propertySpec.type)
        assertEquals("binaryValue", actual.propertySpec.name)
        assertTrue(actual.propertySpec.annotations.single().toString().contains(projection.raw))
    }

    /** 非表示のBLOB関数でも戻り値ヒントを維持し、生成型のみnullableにする。 */
    @Test
    fun createAll_hiddenRawBlobFunction_usesNullableByteArrayReturnHint() {
        val projection = FunctionProjection(
            function = ColumnFunction.CUSTOM,
            args = arrayOf(),
            alias = "BINARY_VALUE",
            raw = "substr(X'00017F80FF', 1, 5)",
            returnHint = AndrOrmValueType.BYTE_ARRAY,
            hideFromSelect = true,
        )
        val actual = target.createAll(listOf(projection), emptyMap()).single()
        assertEquals(com.squareup.kotlinpoet.BYTE_ARRAY.copy(nullable = true), actual.propertySpec.type)
        assertTrue(actual.hideFromSelect)
    }

    private lateinit var target: FunctionPropertyFactory
    private lateinit var mockKspLogger: KSPLogger

    private val properties = setOf(
        "employeeId",
        "payMonth",
        "gross",
        "deduction",
        "createdAt",
        "createdBy",
        "updatedAt",
        "updatedBy",
    )

    /**
     * ## テスト前処理
     * ### KSPロガーと関数プロパティ生成対象を初期化する
     * @author Masahiro Inoue
     * @since 2026-05-03
     */
    @BeforeEach
    fun setUp() {
        // モックの生成
        val mockProcessor = mock<SymbolProcessorEnvironment>()
        mockKspLogger = mock<KSPLogger>()
        // モックの挙動を定義
        whenever(mockProcessor.logger).thenReturn(mockKspLogger)
        whenever(mockProcessor.options).thenReturn(
            mapOf("androrm.moduleDir" to "build${DIRECTORY_DELIMITER}test-module")
        )
        // ログの初期化
        CreateLogger.initialize(mockProcessor)
        // 実行インスタンスを生成
        target = FunctionPropertyFactory()
    }

    /**
     * ## テスト用関数Projection生成
     * @param function SQLカラム関数
     * @param alias 関数列の別名
     * @param args 関数引数
     * @return テスト用関数Projection
     * @author Masahiro Inoue
     * @since 2026-05-03
     */
    private fun functionProjectionOf(
        function: ColumnFunction = ColumnFunction.MAX,
        alias: String = "TEST_ALIAS",
        vararg args: String,
    ): FunctionProjection =
        FunctionProjection(
            function = function,
            args = arrayOf(*args),
            alias = alias,
            returnHint = AndrOrmValueType.AUTO,
            hideFromSelect = false,
            raw = "",
        )

    /**
     * private fun checkFunctionArgs(
     *     functionProjection: FunctionProjection,
     *     properties: Collection<String>
     * )
     *
     * を reflection で呼び出す。
     * @param functionProjection 検証する関数Projection
     * @param properties 利用可能なプロパティ名
     * @author Masahiro Inoue
     * @since 2026-05-03
     */
    private fun invokeCheckFunctionArgs(
        functionProjection: FunctionProjection,
        properties: Collection<String>,
    ) {
        val method = FunctionPropertyFactory::class.java.getDeclaredMethod(
            "checkFunctionArgs",
            FunctionProjection::class.java,
            Collection::class.java,
        )
        method.isAccessible = true

        try {
            method.invoke(target, functionProjection, properties)
        } catch (e: InvocationTargetException) {
            val cause = e.targetException
            if (cause is RuntimeException) throw cause
            throw e
        }
    }

    /**
     * ## 空引数の検証
     * ### 引数を持たない関数が受理されることを確認する
     * @author Masahiro Inoue
     * @since 2026-05-03
     */
    @Test
    fun checkFunctionArgs_shouldPass_whenArgsIsEmpty() {
        val projection = functionProjectionOf(
            function = ColumnFunction.COUNT,
            alias = "ALL_COUNT",
        )

        assertDoesNotThrow {
            invokeCheckFunctionArgs(projection, properties)
        }
    }

    /**
     * ## 既存プロパティ名引数の検証
     * ### Entityに存在するプロパティ名が受理されることを確認する
     * @author Masahiro Inoue
     * @since 2026-05-03
     */
    @Test
    fun checkFunctionArgs_shouldPass_whenArgIsExistingPropertyName() {
        val projection = functionProjectionOf(
            function = ColumnFunction.MAX,
            alias = "MAX_GROSS",
            "gross",
        )

        assertDoesNotThrow {
            invokeCheckFunctionArgs(projection, properties)
        }
    }

    /**
     * ## 文字列リテラル引数の検証
     * ### SQL文字列リテラルが受理されることを確認する
     * @author Masahiro Inoue
     * @since 2026-05-03
     */
    @Test
    fun checkFunctionArgs_shouldPass_whenArgIsStringLiteral() {
        val projection = functionProjectionOf(
            function = ColumnFunction.IFNULL,
            alias = "DEFAULT_NAME",
            "'unknown'",
        )

        assertDoesNotThrow {
            invokeCheckFunctionArgs(projection, properties)
        }
    }

    /**
     * ## 整数リテラル引数の検証
     * ### 整数リテラルが受理されることを確認する
     * @author Masahiro Inoue
     * @since 2026-05-03
     */
    @Test
    fun checkFunctionArgs_shouldPass_whenArgIsIntegerLiteral() {
        val projection = functionProjectionOf(
            function = ColumnFunction.IFNULL,
            alias = "DEFAULT_NUM",
            "1",
        )

        assertDoesNotThrow {
            invokeCheckFunctionArgs(projection, properties)
        }
    }

    /**
     * ## 小数リテラル引数の検証
     * ### 小数リテラルが受理されることを確認する
     * @author Masahiro Inoue
     * @since 2026-05-03
     */
    @Test
    fun checkFunctionArgs_shouldPass_whenArgIsDecimalLiteral() {
        val projection = functionProjectionOf(
            function = ColumnFunction.IFNULL,
            alias = "DEFAULT_DECIMAL",
            "1.25",
        )

        assertDoesNotThrow {
            invokeCheckFunctionArgs(projection, properties)
        }
    }

    /**
     * ## Booleanリテラル引数の検証
     * ### Booleanリテラルが受理されることを確認する
     * @author Masahiro Inoue
     * @since 2026-05-03
     */
    @Test
    fun checkFunctionArgs_shouldPass_whenArgIsBooleanLiteral() {
        val projection = functionProjectionOf(
            function = ColumnFunction.IFNULL,
            alias = "DEFAULT_FLAG",
            "true",
        )

        assertDoesNotThrow {
            invokeCheckFunctionArgs(projection, properties)
        }
    }

    /**
     * ## 未知プロパティ名引数の検証
     * ### 存在しないプロパティ名がエラーログへ記録されることを確認する
     * @author Masahiro Inoue
     * @since 2026-05-03
     */
    @Test
    fun checkFunctionArgs_shouldLogError_whenArgIsUnknownPropertyName() {
        val projection = functionProjectionOf(
            function = ColumnFunction.MAX,
            alias = "INVALID_CASE",
            "unknownColumn",
        )

        assertDoesNotThrow {
            invokeCheckFunctionArgs(projection, properties)
        }

        verify(mockKspLogger, atLeastOnce()).error(
            check<String> { message ->
                assertTrue(message.contains("is invalid"))
                assertTrue(message.contains("INVALID_CASE"))
                assertTrue(message.contains("unknownColumn"))
            },
            anyOrNull()
        )
    }

    /**
     * ## 混在引数内の不正値検証
     * ### 正常値と不正値が混在しても不正な引数がエラーログへ記録されることを確認する
     * @author Masahiro Inoue
     * @since 2026-05-03
     */
    @Test
    fun checkFunctionArgs_shouldLogError_whenMixedArgsContainInvalidValue() {
        val projection = functionProjectionOf(
            function = ColumnFunction.COALESCE,
            alias = "MIXED_CASE",
            "gross",
            "unknownColumn",
        )

        assertDoesNotThrow {
            invokeCheckFunctionArgs(projection, properties)
        }

        verify(mockKspLogger, atLeastOnce()).error(
            check<String> { message ->
                assertTrue(message.contains("is invalid"))
                assertTrue(message.contains("MIXED_CASE"))
                assertTrue(message.contains("unknownColumn"))
            },
            anyOrNull()
        )
    }
}