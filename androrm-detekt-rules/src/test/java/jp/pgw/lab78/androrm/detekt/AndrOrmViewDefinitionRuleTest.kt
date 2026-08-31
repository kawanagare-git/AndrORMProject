package jp.pgw.lab78.androrm.detekt

import io.gitlab.arturbosch.detekt.test.compileAndLint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
}
