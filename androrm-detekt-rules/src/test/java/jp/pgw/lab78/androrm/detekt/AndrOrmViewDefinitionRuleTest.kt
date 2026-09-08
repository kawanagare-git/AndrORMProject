package jp.pgw.lab78.androrm.detekt

import io.gitlab.arturbosch.detekt.test.compileAndLint
import io.gitlab.arturbosch.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

/**
 * ## VIEW 定義検査ルールテスト
 * @author Masahiro Inoue
 * @since 2026-08-31
 */
class AndrOrmViewDefinitionRuleTest {
    /** 正しい VIEW 定義が報告されないことを確認する。 */
    @Test
    fun validViewDefinitionIsAllowed() {
        val findings = AndrOrmViewDefinitionRule().compileAndLint(
            """
            @View(name = "EMPLOYEE_VIEW")
            data class EmployeeView(val id: Int) : ViewDefinitionEntity
            """
        )
        assertEquals(0, findings.size)
    }

    /** @View とマーカーの不一致を確認する。 */
    @Test
    fun viewAnnotationRequiresViewDefinitionEntity() {
        val findings = AndrOrmViewDefinitionRule().compileAndLint(
            """
            @View(name = "EMPLOYEE_VIEW")
            data class EmployeeView(val id: Int)
            """
        )
        assertEquals(1, findings.size)
    }

    /** テーブルと VIEW の物理名衝突を確認する。 */
    @Test
    fun tableAndViewNameCollisionIsReported() {
        val findings = AndrOrmViewDefinitionRule().compileAndLint(
            """
            @Table(name = "EMPLOYEE")
            data class Employee(val id: Int) : TableDefinitionEntity

            @View(name = "employee")
            data class EmployeeView(val id: Int) : ViewDefinitionEntity
            """
        )
        assertEquals(1, findings.size)
    }

    /** ViewDefinitionEntityだけを指定した定義を拒否する。 */
    @Test
    fun viewDefinitionMarkerRequiresViewAnnotation() {
        val findings = AndrOrmViewDefinitionRule().compileAndLint(
            "data class EmployeeView(val id: Int) : ViewDefinitionEntity",
        )
        assertEquals(1, findings.size)
    }

    /** ViewDefinitionEntityを1段経由する継承を認識する。 */
    @Test
    fun indirectViewDefinitionMarkerIsAllowed() {
        val findings = AndrOrmViewDefinitionRule().compileAndLint(
            """
            interface CustomViewDefinition : ViewDefinitionEntity
            @View(name = "EMPLOYEE_VIEW")
            data class EmployeeView(val id: Int) : CustomViewDefinition
            """
        )
        assertEquals(0, findings.size)
    }

    /** ViewDefinitionEntityを複数段経由する継承を認識する。 */
    @Test
    fun multiLevelViewDefinitionMarkerIsAllowed() {
        val findings = AndrOrmViewDefinitionRule().compileAndLint(
            """
            interface RootViewDefinition : ViewDefinitionEntity
            interface CustomViewDefinition : RootViewDefinition
            @View(name = "EMPLOYEE_VIEW")
            data class EmployeeView(val id: Int) : CustomViewDefinition
            """
        )
        assertEquals(0, findings.size)
    }

    /** 正規マーカーのalias importを解決する。 */
    @Test
    fun aliasedCanonicalViewMarkerIsAllowed() {
        val findings = AndrOrmViewDefinitionRule().compileAndLint(
            """
            import jp.pgw.lab78.androrm.common.dml.interfaces.ViewDefinitionEntity as VDE
            @View(name = "EMPLOYEE_VIEW")
            data class EmployeeView(val id: Int) : VDE
            """
        )
        assertEquals(0, findings.size)
    }

    /** 正規マーカーの完全修飾名を解決する。 */
    @Test
    fun fullyQualifiedCanonicalViewMarkerIsAllowed() {
        val findings = AndrOrmViewDefinitionRule().compileAndLint(
            """
            @View(name = "EMPLOYEE_VIEW")
            data class EmployeeView(
                val id: Int,
            ) : jp.pgw.lab78.androrm.common.dml.interfaces.ViewDefinitionEntity
            """
        )
        assertEquals(0, findings.size)
    }

    /** 別packageの同名インターフェースを正規マーカーと誤認しない。 */
    @Test
    fun sameNamedNonCanonicalViewMarkerIsRejected() {
        val findings = AndrOrmViewDefinitionRule().compileAndLint(
            """
            package unrelated
            interface ViewDefinitionEntity
            @View(name = "EMPLOYEE_VIEW")
            data class EmployeeView(val id: Int) : ViewDefinitionEntity
            """
        )
        assertEquals(1, findings.size)
    }

    /** 別ファイルの1段間接継承を認識する。 */
    @Test
    fun indirectViewDefinitionMarkerInAnotherFileIsAllowed() {
        val root = Files.createTempDirectory(Path.of("build"), "androrm-view-cross-file")
        Files.createDirectories(root.resolve("kotlin"))
        Files.writeString(
            root.resolve("kotlin/CustomView.kt"),
            "interface CustomView : ViewDefinitionEntity",
        )
        val target = root.resolve("kotlin/SampleView.kt")
        Files.writeString(
            target,
            """
            @View(name = "EMPLOYEE_VIEW")
            data class SampleView(val id: Int) : CustomView
            """.trimIndent(),
        )

        assertEquals(0, AndrOrmViewDefinitionRule().lint(target).size)
    }

    /** 別ファイルの複数段間接継承を認識する。 */
    @Test
    fun multiLevelViewDefinitionMarkerInAnotherFileIsAllowed() {
        val root = Files.createTempDirectory(Path.of("build"), "androrm-view-cross-file-multi")
        Files.createDirectories(root.resolve("kotlin"))
        Files.writeString(
            root.resolve("kotlin/RootView.kt"),
            "interface RootView : ViewDefinitionEntity",
        )
        Files.writeString(
            root.resolve("kotlin/CustomView.kt"),
            "interface CustomView : RootView",
        )
        val target = root.resolve("kotlin/SampleView.kt")
        Files.writeString(
            target,
            """
            @View(name = "EMPLOYEE_VIEW")
            data class SampleView(val id: Int) : CustomView
            """.trimIndent(),
        )

        assertEquals(0, AndrOrmViewDefinitionRule().lint(target).size)
    }
}
