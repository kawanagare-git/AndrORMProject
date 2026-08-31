package jp.pgw.lab78.androrm.database

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.annotation.View
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.ViewDefinitionEntity
import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertThrows
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
}