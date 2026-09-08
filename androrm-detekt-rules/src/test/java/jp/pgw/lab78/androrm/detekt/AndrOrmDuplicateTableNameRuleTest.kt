package jp.pgw.lab78.androrm.detekt

import io.gitlab.arturbosch.detekt.test.compileAndLint
import io.gitlab.arturbosch.detekt.test.lint
import jp.pgw.lab78.androrm.detekt.AndrOrmDuplicateTableNameRule
import jp.pgw.lab78.androrm.detekt.AndrOrmDetektMessages.duplicateTableName
import kotlin.test.Test
import kotlin.test.assertEquals
import java.nio.file.Files
import java.nio.file.Path

/**
 * ## テーブル名重複検査ルールテスト
 * ### TableDefinitionEntity間の明示テーブル名重複だけが検出されることを検証する
 * @author Masahiro Inoue
 * @since 2026-07-20
 */
class AndrOrmDuplicateTableNameRuleTest {

    /** 別ファイルの1段間接継承をテーブル重複判定へ反映する。 */
    @Test
    fun indirectTableDefinitionMarkerInAnotherFileIsRecognized() {
        val root = Files.createTempDirectory(Path.of("build"), "androrm-table-cross-file")
        Files.createDirectories(root.resolve("kotlin"))
        Files.writeString(
            root.resolve("kotlin/CustomTable.kt"),
            "interface CustomTable : TableDefinitionEntity",
        )
        Files.writeString(
            root.resolve("kotlin/First.kt"),
            """
            @Table(name = "EMPLOYEE")
            data class First(val id: Int) : CustomTable
            """.trimIndent(),
        )
        val target = root.resolve("kotlin/Second.kt")
        Files.writeString(
            target,
            """
            @Table(name = "employee")
            data class Second(val id: Int) : CustomTable
            """.trimIndent(),
        )

        assertEquals(1, AndrOrmDuplicateTableNameRule().lint(target).size)
    }

    /** 別ファイルの複数段間接継承をテーブル重複判定へ反映する。 */
    @Test
    fun multiLevelTableDefinitionMarkerInAnotherFileIsRecognized() {
        val root = Files.createTempDirectory(Path.of("build"), "androrm-table-cross-file-multi")
        Files.createDirectories(root.resolve("kotlin"))
        Files.writeString(
            root.resolve("kotlin/RootTable.kt"),
            "interface RootTable : TableDefinitionEntity",
        )
        Files.writeString(
            root.resolve("kotlin/CustomTable.kt"),
            "interface CustomTable : RootTable",
        )
        Files.writeString(
            root.resolve("kotlin/First.kt"),
            """
            @Table(name = "EMPLOYEE")
            data class First(val id: Int) : CustomTable
            """.trimIndent(),
        )
        val target = root.resolve("kotlin/Second.kt")
        Files.writeString(
            target,
            """
            @Table(name = "employee")
            data class Second(val id: Int) : CustomTable
            """.trimIndent(),
        )

        assertEquals(1, AndrOrmDuplicateTableNameRule().lint(target).size)
    }

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
    /** 正規TableDefinitionEntityのalias importと完全修飾名を解決する。 */
    @Test
    fun canonicalTableMarkerAliasAndFqnAreRecognized() {
        val findings = AndrOrmDuplicateTableNameRule().compileAndLint(
            """
            import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity as TDE
            @Table(name = "EMPLOYEE")
            data class First(val id: Int) : TDE
            @Table(name = "employee")
            data class Second(val id: Int) : jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
            """
        )
        assertEquals(1, findings.size)
    }

    /** 別packageの同名TableDefinitionEntityを正規マーカーと誤認しない。 */
    @Test
    fun sameNamedNonCanonicalTableMarkerIsIgnored() {
        val findings = AndrOrmDuplicateTableNameRule().compileAndLint(
            """
            package unrelated
            interface TableDefinitionEntity
            @Table(name = "EMPLOYEE")
            data class First(val id: Int) : TableDefinitionEntity
            @Table(name = "employee")
            data class Second(val id: Int) : TableDefinitionEntity
            """
        )
        assertEquals(0, findings.size)
    }
}
