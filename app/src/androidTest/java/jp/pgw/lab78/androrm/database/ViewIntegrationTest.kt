package jp.pgw.lab78.androrm.database

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.SELECT
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.annotation.View
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.ViewDefinitionEntity
import jp.pgw.lab78.androrm.database.entities.select.KspActiveEmployeeViewDefinitionSelect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ## SQLite VIEW 結合テスト
 * ### 実 SQLite で VIEW 作成、通常 Select 参照、読み取り専用制約を確認する
 * @author Masahiro Inoue
 * @since 2026-08-31
 */
@RunWith(AndroidJUnit4::class)
class ViewIntegrationTest {
    /** VIEW が作成・参照でき、書き込みが拒否されることを確認する。 */
    @Test
    fun createQueryAndRejectWrite() {
        val db = SQLiteDatabase.create(null)
        try {
            db.execSQL("create table EMPLOYEE (ID integer not null, NAME text not null, ACTIVE integer not null)")
            db.execSQL("insert into EMPLOYEE values (1, 'Alice', 1), (2, 'Bob', 0)")
            val viewSelect = ViewSelect(EmployeeSelect::class).where {
                EmployeeSelect::active eq true
            }
            db.execSQL(CreateView(ActiveEmployeeViewDefinition::class, viewSelect).build())

            val selectSql = Select(ActiveEmployeeViewSelect::class).build()
            db.rawQuery(selectSql, null).use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals(1L, cursor.getLong(cursor.getColumnIndexOrThrow("AV_ID")))
                assertEquals("Alice", cursor.getString(cursor.getColumnIndexOrThrow("AV_NAME")))
            }
            assertThrows(SQLiteException::class.java) {
                db.execSQL("insert into ACTIVE_EMPLOYEE (ID, NAME) values (3, 'Carol')")
            }
        } finally {
            db.close()
        }
    }

    /** KSP生成されたVIEW SelectEntityをCursorEntityMapper経由で取得する。 */
    @Test
    fun kspViewSelect_mapsFromRealSqliteView() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val viewSelect = ViewSelect(EmployeeSelect::class).where {
            EmployeeSelect::active eq true
        }
        val helper = AndrOrmDatabaseHelper(
            context = context,
            databaseName = null,
            version = 1,
            entities = listOf(EmployeeSelect::class),
            views = listOf(CreateView(KspActiveEmployeeViewDefinition::class, viewSelect)),
        )
        try {
            helper.writableDatabase
            helper.executeDml(
                "insert into EMPLOYEE (ID, NAME, ACTIVE) values (?, ?, ?)",
                listOf(1L, "Alice", 1L),
            )

            assertTrue(KspActiveEmployeeViewDefinitionSelect is CursorEntityMapper<*>)
            val rows = helper.executeSelectAsEntityList(
                Select(KspActiveEmployeeViewDefinitionSelect::class),
            )
            val actual = rows.single().getValue("KAV") as KspActiveEmployeeViewDefinitionSelect
            assertEquals(1, actual.id)
            assertEquals("Alice", actual.name)
        } finally {
            helper.close()
        }
    }

    /** 実 SQLite のUpgradeでVIEWを再作成し、廃止VIEWを削除する。 */
    @Test
    fun upgrade_recreatesViewsAndDropsObsoleteView() {
        val db = SQLiteDatabase.create(null)
        try {
            db.execSQL("create table EMPLOYEE (ID integer not null, NAME text not null, ACTIVE integer not null)")
            db.execSQL("insert into EMPLOYEE values (1, 'Alice', 1)")
            db.execSQL("create view OLD_EMPLOYEE_VIEW as select ID, NAME, ACTIVE from EMPLOYEE")

            val helper = object : AndrOrmDatabaseHelper(
                context = InstrumentationRegistry.getInstrumentation().targetContext,
                databaseName = null,
                version = 2,
                entities = emptyList(),
                views = listOf(
                    CreateView(
                        ActiveEmployeeViewDefinition::class,
                        ViewSelect(EmployeeSelect::class),
                    ),
                ),
            ) {
                override fun obsoleteViewNames(oldVersion: Int, newVersion: Int): List<String> =
                    listOf("OLD_EMPLOYEE_VIEW")
            }

            helper.onUpgrade(db, 1, 2)
            db.rawQuery("select ID, NAME, ACTIVE from ACTIVE_EMPLOYEE", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1L, cursor.getLong(0))
                assertEquals("Alice", cursor.getString(1))
                assertEquals(1L, cursor.getLong(2))
            }
            assertThrows(SQLiteException::class.java) {
                db.rawQuery("select * from OLD_EMPLOYEE_VIEW", null).use { it.count }
            }
            helper.close()
        } finally {
            db.close()
        }
    }

    @Table(name = "EMPLOYEE", alias = "E")
    private data class EmployeeSelect(
        @Column(name = "ID") val id: Int,
        @Column(name = "NAME") val name: String,
        @Column(name = "ACTIVE") val active: Boolean,
    ) : SelectEntity

    @View(name = "ACTIVE_EMPLOYEE", alias = "AV")
    private data class ActiveEmployeeViewDefinition(
        @Column(name = "ID") val id: Int,
        @Column(name = "NAME") val name: String,
        @Column(name = "ACTIVE") val active: Boolean,
    ) : ViewDefinitionEntity

    @View(name = "ACTIVE_EMPLOYEE", alias = "AV")
    private data class ActiveEmployeeViewSelect(
        @Column(name = "ID") val id: Int,
        @Column(name = "NAME") val name: String,
        @Column(name = "ACTIVE") val active: Boolean,
    ) : SelectEntity

    /** KSPで生成するVIEW定義。 */
    @Projection(
        entityNameExtend = "Select",
        properties = [
            ColumnProjection("id"),
            ColumnProjection("name"),
        ],
        commonInterface = [SELECT],
    )
    @View(name = "KSP_ACTIVE_EMPLOYEE", alias = "KAV")
    private data class KspActiveEmployeeViewDefinition(
        @Column(name = "ID") val id: Int,
        @Column(name = "NAME") val name: String,
        @Column(name = "ACTIVE") val active: Boolean,
    ) : KspViewDefinitionMarker

    /** KSPのViewDefinitionEntity判定が間接継承を認識することを確認するマーカー。 */
    private interface KspViewDefinitionMarker : ViewDefinitionEntity
}