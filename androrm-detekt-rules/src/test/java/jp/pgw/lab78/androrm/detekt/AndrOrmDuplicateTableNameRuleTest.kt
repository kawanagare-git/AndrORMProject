package jp.pgw.lab78.androrm.detekt

import io.gitlab.arturbosch.detekt.test.compileAndLint
import jp.pgw.lab78.androrm.detekt.AndrOrmDuplicateTableNameRule
import jp.pgw.lab78.androrm.detekt.AndrOrmDetektMessages.duplicateTableName
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * ## テーブル名重複検査ルールテスト
 * ### TableDefinitionEntity間の明示テーブル名重複だけが検出されることを検証する
 * @author Masahiro Inoue
 * @since 2026-07-20
 */
class AndrOrmDuplicateTableNameRuleTest {

    /**
     * ## 異なるテーブル名の検証
     * ### 異なる明示テーブル名を持つテーブル定義Entityが検出されないことを確認する
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Test
    fun `different explicit table names are allowed`() {
        val code = """
            @Table(name = "EMPLOYEE")
            data class Employee(val id: Int) : TableDefinitionEntity

            @Table(name = "DEPARTMENT")
            data class Department(val id: Int) : TableDefinitionEntity
        """

        val findings = AndrOrmDuplicateTableNameRule().compileAndLint(code)

        assertEquals(0, findings.size)
    }

    /**
     * ## 大文字小文字非依存の重複検証
     * ### 名前付き引数と位置引数で同じテーブル名を指定した異なるEntityが検出されることを確認する
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Test
    fun `duplicate explicit table names are reported ignoring case`() {
        val code = """
            class TestDefinitions {
                @Table(name = "EMPLOYEE")
                data class EmployeeFirst(val id: Int) : TableDefinitionEntity

                @Table("employee")
                data class EmployeeSecond(val id: Int) : TableDefinitionEntity
            }
        """

        val findings = AndrOrmDuplicateTableNameRule().compileAndLint(code)

        assertEquals(1, findings.size)
        assertEquals(
            duplicateTableName(
                tableName = "employee",
                firstEntity = "TestDefinitions.EmployeeFirst",
                duplicateEntity = "TestDefinitions.EmployeeSecond",
            ),
            findings.single().message,
        )
    }

    /**
     * ## テーブル定義Entity以外の同名テーブル検証
     * ### DML用Entity等が同じ@Table.nameを持っても検出されないことを確認する
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Test
    fun `same table name on non table definition entity is allowed`() {
        val code = """
            @Table(name = "EMPLOYEE")
            data class EmployeeDefinition(val id: Int) : TableDefinitionEntity

            @Table(name = "EMPLOYEE")
            data class EmployeeSelect(val id: Int) : SelectEntity
        """

        val findings = AndrOrmDuplicateTableNameRule().compileAndLint(code)

        assertEquals(0, findings.size)
    }

    /**
     * ## テーブル名未指定の検証
     * ### nameを省略または空文字にしたEntityが明示名重複として検出されないことを確認する
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Test
    fun `omitted and blank table names are allowed`() {
        val code = """
            @Table(alias = "FIRST")
            data class FirstEntity(val id: Int) : TableDefinitionEntity

            @Table(name = "")
            data class SecondEntity(val id: Int) : TableDefinitionEntity
        """

        val findings = AndrOrmDuplicateTableNameRule().compileAndLint(code)

        assertEquals(0, findings.size)
    }

    /**
     * ## 意図的な重複の抑制検証
     * ### バージョン移行テスト等でルールIDを明示抑制したEntityが報告されないことを確認する
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Test
    fun `suppressed duplicate table name is allowed`() {
        val code = """
            @Table(name = "MIGRATION_TARGET")
            data class MigrationV1(val id: Int) : TableDefinitionEntity

            @Table(name = "MIGRATION_TARGET")
            @Suppress("AndrOrmDuplicateTableNameRule")
            data class MigrationV2(val id: Int) : TableDefinitionEntity
        """

        val findings = AndrOrmDuplicateTableNameRule().compileAndLint(code)

        assertEquals(0, findings.size)
    }
}
