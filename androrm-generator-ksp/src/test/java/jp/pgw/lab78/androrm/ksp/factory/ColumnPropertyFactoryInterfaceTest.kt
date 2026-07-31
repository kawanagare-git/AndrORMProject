package jp.pgw.lab78.androrm.ksp.factory

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotation
import com.squareup.kotlinpoet.AnnotationSpec
import jp.pgw.lab78.androrm.common.Constants.DIRECTORY_DELIMITER
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger
import jp.pgw.lab78.androrm.ksp.resolver.KspColumnAnnotationResolver
import jp.pgw.lab78.androrm.ksp.testsupport.KspSymbolMockFactory.columnAnnotationOf
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * ## インターフェースColumnプロパティ生成テスト
 * ### Resolverで解決したColumn設定が生成アノテーションへコピーされることを検証する
 * @author Masahiro Inoue
 * @since 2026-07-31
 */
class ColumnPropertyFactoryInterfaceTest {

    private lateinit var target: ColumnPropertyFactory

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
        target = ColumnPropertyFactory(
            columnAnnotationResolver = mock<KspColumnAnnotationResolver>(),
        )
    }

    /**
     * ## name・defaultコピー確認
     * ### インターフェースから解決されたColumnのname・alias・defaultを維持する
     */
    @Test
    fun copyColumnAnnotation_shouldCopyResolvedColumnDefinition() {
        val columnAnnotation = columnAnnotationOf(
            name = "CREATED_AT",
            alias = "CREATED_AT_ALIAS",
            defaultValue = "CURRENT_TIMESTAMP_ISO",
        )

        val actual = invokeCopyColumnAnnotation(
            columnAnnotation = columnAnnotation,
            hideFromSelect = false,
        ).toString()

        assertTrue(actual.contains("name = \"CREATED_AT\""))
        assertTrue(
            actual.contains("alias = \"CREATED_AT_ALIAS\""),
        )
        assertTrue(
            actual.contains(
                "default = \"CURRENT_TIMESTAMP_ISO\"",
            ),
        )
        assertTrue(actual.contains("hideFromSelect = false"))
    }

    /**
     * ## Projection非表示設定優先確認
     * ### 元ColumnのhideFromSelectではなくProjection指定値を使用する
     */
    @Test
    fun copyColumnAnnotation_shouldUseProjectionHideFromSelect() {
        val columnAnnotation = columnAnnotationOf(
            name = "UPDATED_AT",
            hideFromSelect = false,
            defaultValue = "CURRENT_TIMESTAMP_ISO",
        )

        val actual = invokeCopyColumnAnnotation(
            columnAnnotation = columnAnnotation,
            hideFromSelect = true,
        ).toString()

        assertTrue(actual.contains("hideFromSelect = true"))
        assertFalse(actual.contains("hideFromSelect = false"))
    }

    /** copyColumnAnnotationをreflectionで実行 */
    private fun invokeCopyColumnAnnotation(
        columnAnnotation: KSAnnotation,
        hideFromSelect: Boolean,
    ): AnnotationSpec {
        val method = ColumnPropertyFactory::class.java.getDeclaredMethod(
            "copyColumnAnnotation",
            KSAnnotation::class.java,
            Boolean::class.javaPrimitiveType!!,
        )
        method.isAccessible = true

        return method.invoke(
            target,
            columnAnnotation,
            hideFromSelect,
        ) as AnnotationSpec
    }
}
