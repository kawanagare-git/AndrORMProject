package jp.pgw.lab78.androrm.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.annotation.View
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.ViewDefinitionEntity
import jp.pgw.lab78.androrm.database.reference.TableRef
import jp.pgw.lab78.androrm.database.view.ViewSqlLiteralRenderer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.time.LocalDate

/**
 * ## ViewSelect／CreateView テスト
 * ### VIEW 定義が bind 値を持たず、通常 Select と独立して構築されることを検証する
 * @author Masahiro Inoue
 * @since 2026-08-31
 */
class ViewSelectTest {
    /** 値が SQL リテラルへ展開され、build を再実行しても変化しないことを確認する。 */
    @Test
    fun viewSelectRendersSqlLiteralsWithoutBindValues() {
        val query = ViewSelect(EmployeeSelect::class)
            .where {
                EmployeeSelect::name eq "O'Brien"
                EmployeeSelect::id gt 10
            }
            .order { EmployeeSelect::id.desc }
            .limit(20)
            .offset(5)

        val expected = "select E.ID as E_ID, E.NAME as E_NAME from EMPLOYEE E " +
                "where E.NAME = 'O''Brien' and E.ID > 10 order by E.ID desc nulls first " +
                "limit 20 offset 5"

        assertEquals(expected, query.build())
        assertEquals(expected, query.build())
        assertEquals(emptyList<Any?>(), query.bindValues)
    }

    /** 独立したViewExistsSelectが相関EXISTSとSQLリテラルを生成することを確認する。 */
    @Test
    fun viewSelectBuildsCorrelatedExistsWithSqlLiterals() {
        val outerTable = TableRef(EmployeeSelect::class, "OUTER_E")
        val existsTable = TableRef(EmployeeSelect::class, "EXISTS_E")
        val query = ViewSelect(outerTable).where {
            exists(existsTable) {
                existsTable[EmployeeSelect::id] eq outerTable[EmployeeSelect::id]
                existsTable[EmployeeSelect::name] eq "active"
            }
        }

        assertEquals(
            "select OUTER_E.ID as OUTER_E_ID, OUTER_E.NAME as OUTER_E_NAME " +
                    "from EMPLOYEE OUTER_E where exists (select 1 from EMPLOYEE EXISTS_E " +
                    "where EXISTS_E.ID = OUTER_E.ID and EXISTS_E.NAME = 'active')",
            query.build(),
        )
        assertEquals(emptyList<Any?>(), query.bindValues)
    }

    /** 通常 Select は従来どおり値を bind することを固定する。 */
    @Test
    fun normalSelectStillUsesBindValues() {
        val query = Select(EmployeeSelect::class).where {
            EmployeeSelect::name eq "O'Brien"
        }

        assertEquals(
            "select E.ID as E_ID, E.NAME as E_NAME from EMPLOYEE E where E.NAME = ?",
            query.build(),
        )
        assertEquals(listOf("O'Brien"), query.bindValues)
    }

    /** CREATE VIEW が明示列リストと ViewSelect を使用することを確認する。 */
    @Test
    fun createViewBuildsExplicitColumnList() {
        val query = ViewSelect(EmployeeSelect::class).where {
            EmployeeSelect::id ge 1
        }
        val createView = CreateView(ActiveEmployeeViewDefinition::class, query)

        assertEquals(
            "create view \"ACTIVE_EMPLOYEE\" (\"ID\", \"NAME\") as " +
                    "select E.ID as E_ID, E.NAME as E_NAME from EMPLOYEE E where E.ID >= 1",
            createView.build(),
        )
        assertEquals("drop view if exists \"ACTIVE_EMPLOYEE\"", createView.buildDropQuery())
    }

    /** VIEW 定義列数と SELECT 出力列数の不一致を拒否する。 */
    @Test
    fun createViewRejectsColumnCountMismatch() {
        val createView = CreateView(EmployeeIdViewDefinition::class, ViewSelect(EmployeeSelect::class))

        assertThrows<IllegalArgumentException> { createView.build() }
    }

    /** 通常 Select を ViewSelect のサブクエリへ混在させないことを確認する。 */
    @Test
    fun viewSelectRejectsNormalSelectSubquery() {
        val normalSubquery = Select(EmployeeSelect::class).where {
            EmployeeSelect::id gt 0
        }

        assertThrows<IllegalArgumentException> {
            ViewSelect(EmployeeSelect::class).where {
                EmployeeSelect::id inSelect normalSubquery
            }
        }
    }

    /** VIEW を通常 Select の FROM 元と JOIN 先に使用できることを確認する。 */
    @Test
    fun generatedViewEntityCanBeSelectedAndJoined() {
        val fromView = Select(ActiveEmployeeViewSelect::class)
        val joinedView = Select(EmployeeSelect::class).join(
            jp.pgw.lab78.androrm.database.queryparts.JoinType.LEFT,
            ActiveEmployeeViewSelect::class,
        ) {
            EmployeeSelect::id eq ActiveEmployeeViewSelect::id
        }

        assertTrue(fromView.build().contains("from ACTIVE_EMPLOYEE AV"))
        assertTrue(joinedView.build().contains("left join ACTIVE_EMPLOYEE AV"))
    }

    /** raw 条件の引用符内疑問符を置換せず、実パラメータだけを展開する。 */
    @Test
    fun rawSqlScannerExpandsOnlyRealAnonymousParameters() {
        val actual = ViewSqlLiteralRenderer.expandAnonymousParameters(
            "NAME = '?' AND ID = ? -- ?\nAND NOTE = '?'",
            listOf(7),
        )
        assertEquals("NAME = '?' AND ID = 7 -- ?\nAND NOTE = '?'", actual)
    }

    /** SQLite リテラルの型別表現を確認する。 */
    @Test
    fun literalRendererSupportsRequiredTypes() {
        assertEquals("NULL", ViewSqlLiteralRenderer.renderValue(null))
        assertEquals("1", ViewSqlLiteralRenderer.renderValue(true))
        assertEquals("12.5", ViewSqlLiteralRenderer.renderValue(12.5))
        assertEquals("'2026-08-31'", ViewSqlLiteralRenderer.renderValue(LocalDate.of(2026, 8, 31)))
        assertEquals("X'00FF'", ViewSqlLiteralRenderer.renderValue(byteArrayOf(0, -1)))
        assertThrows<IllegalArgumentException> { ViewSqlLiteralRenderer.renderValue(Double.NaN) }
        assertThrows<IllegalArgumentException> { ViewSqlLiteralRenderer.renderValue("a\u0000b") }
    }

    /** onCreate でテーブル作成後に VIEW を作成することを確認する。 */
    @Test
    fun databaseHelperCreatesViewAfterTables() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val createView = CreateView(
            ActiveEmployeeViewDefinition::class,
            ViewSelect(EmployeeSelect::class),
        )
        val helper = AndrOrmDatabaseHelper(
            context = Mockito.mock(Context::class.java),
            databaseName = "view_test.db",
            version = 1,
            entities = listOf(EmployeeTableDefinition::class),
            views = listOf(createView),
        )

        helper.onCreate(db)

        val captor = ArgumentCaptor.forClass(String::class.java)
        Mockito.verify(db, Mockito.times(2)).execSQL(captor.capture())
        assertTrue(captor.allValues[0].startsWith("create table"))
        assertTrue(captor.allValues[1].startsWith("create view"))
    }

    /** onUpgrade で VIEW を参照元移行前に削除し、移行後に再作成することを確認する。 */
    @Test
    fun databaseHelperRecreatesViewsOnUpgrade() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val createView = CreateView(
            ActiveEmployeeViewDefinition::class,
            ViewSelect(EmployeeSelect::class),
        )
        val helper = AndrOrmDatabaseHelper(
            context = Mockito.mock(Context::class.java),
            databaseName = "view_upgrade_test.db",
            version = 2,
            entities = emptyList(),
            views = listOf(createView),
        )

        helper.onUpgrade(db, 1, 2)

        val captor = ArgumentCaptor.forClass(String::class.java)
        Mockito.verify(db, Mockito.times(2)).execSQL(captor.capture())
        assertEquals("drop view if exists \"ACTIVE_EMPLOYEE\"", captor.allValues[0])
        assertTrue(captor.allValues[1].startsWith("create view \"ACTIVE_EMPLOYEE\""))
    }

    /** obsoleteViewNames で指定した廃止 VIEW をアップグレード時に削除することを確認する。 */
    @Test
    fun databaseHelperDropsObsoleteViewsOnUpgrade() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val helper = object : AndrOrmDatabaseHelper(
            context = Mockito.mock(Context::class.java),
            databaseName = "obsolete_view_test.db",
            version = 2,
            entities = emptyList(),
            views = emptyList(),
        ) {
            override fun obsoleteViewNames(oldVersion: Int, newVersion: Int): List<String> =
                listOf("OLD_EMPLOYEE_VIEW")
        }

        helper.onUpgrade(db, 1, 2)

        Mockito.verify(db).execSQL("drop view if exists \"OLD_EMPLOYEE_VIEW\"")
    }

    @Table(name = "EMPLOYEE", alias = "E")
    private data class EmployeeSelect(
        @Column(name = "ID") val id: Int,
        @Column(name = "NAME") val name: String,
    ) : SelectEntity

    @Table(name = "EMPLOYEE", alias = "E")
    private data class EmployeeTableDefinition(
        @Column(name = "ID") val id: Int,
        @Column(name = "NAME") val name: String,
    ) : TableDefinitionEntity

    @View(name = "ACTIVE_EMPLOYEE", alias = "AV")
    private data class ActiveEmployeeViewDefinition(
        @Column(name = "ID") val id: Int,
        @Column(name = "NAME") val name: String,
    ) : ViewDefinitionEntity

    @View(name = "EMPLOYEE_ID_VIEW", alias = "EIV")
    private data class EmployeeIdViewDefinition(
        @Column(name = "ID") val id: Int,
    ) : ViewDefinitionEntity

    @View(name = "ACTIVE_EMPLOYEE", alias = "AV")
    private data class ActiveEmployeeViewSelect(
        @Column(name = "ID") val id: Int,
        @Column(name = "NAME") val name: String,
    ) : SelectEntity
}
