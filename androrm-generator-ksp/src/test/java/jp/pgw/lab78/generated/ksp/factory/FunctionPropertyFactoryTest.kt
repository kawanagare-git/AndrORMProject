package jp.pgw.lab78.generated.ksp.factory

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import jp.pgw.lab78.androrm.common.Constants.DIRECTORY_DELIMITER
import jp.pgw.lab78.androrm.common.annotation.FunctionProjection
import jp.pgw.lab78.androrm.common.annotation.ReturnHint
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction
import jp.pgw.lab78.androrm.ksp.factory.FunctionPropertyFactory
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.lang.reflect.InvocationTargetException

class FunctionPropertyFactoryTest {

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

    private fun functionProjectionOf(
        function: ColumnFunction = ColumnFunction.MAX,
        alias: String = "TEST_ALIAS",
        vararg args: String,
    ): FunctionProjection =
        FunctionProjection(
            function = function,
            args = arrayOf(*args),
            alias = alias,
            returnHint = ReturnHint.AUTO,
            hideFromSelect = false,
            raw = "",
        )

    /**
     * private fun checkFunctionArgs(
     *     properties: Set<String>,
     *     functionProjection: FunctionProjection
     * )
     *
     * を reflection で呼び出す。
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