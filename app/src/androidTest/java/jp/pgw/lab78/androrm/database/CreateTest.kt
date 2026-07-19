package jp.pgw.lab78.androrm.database

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.PrimaryKey
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ## Create実DB境界条件テスト
 * ### Createが生成したDDLをインメモリSQLiteへ適用し、主キーなしと複合主キーの制約を検証する
 * @author Masahiro Inoue
 * @since 2026-07-20
 */
@RunWith(AndroidJUnit4::class)
class CreateTest {

    /**
     * ## 主キーなしテーブルの検証
     * ### 主キー句を持たないDDLでは同じID値の複数行を登録できることを確認する
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Test
    fun build_withoutPrimaryKey_allowsDuplicateColumnValuesOnSQLite() {
        SQLiteDatabase.create(null).use { database ->
            database.execSQL(Create(TestNoPrimaryKeyDatabaseEntity::class).build())

            database.execSQL(
                """insert into "TEST_NO_PRIMARY_KEY_DATABASE" ("ID", "MEMO") values (1, 'first')"""
            )
            database.execSQL(
                """insert into "TEST_NO_PRIMARY_KEY_DATABASE" ("ID", "MEMO") values (1, 'second')"""
            )

            database.rawQuery(
                """select count(*) from "TEST_NO_PRIMARY_KEY_DATABASE" where "ID" = 1""",
                null,
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(2, cursor.getInt(0))
            }
        }
    }

    /**
     * ## 複合主キーテーブルの検証
     * ### 一部のキーが同じ行は許可し、キー全体が重複する行はSQLiteが拒否することを確認する
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Test
    fun build_withCompositePrimaryKey_rejectsDuplicateKeyPairOnSQLite() {
        SQLiteDatabase.create(null).use { database ->
            database.execSQL(Create(TestCompositePrimaryKeyDatabaseEntity::class).build())
            database.execSQL(
                """insert into "TEST_COMPOSITE_PRIMARY_KEY_DATABASE" """ +
                        """("PARENT_ID", "CHILD_ID", "MEMO") values (1, 2, 'first')"""
            )
            database.execSQL(
                """insert into "TEST_COMPOSITE_PRIMARY_KEY_DATABASE" """ +
                        """("PARENT_ID", "CHILD_ID", "MEMO") values (1, 3, 'second')"""
            )

            assertThrows(SQLiteConstraintException::class.java) {
                database.execSQL(
                    """insert into "TEST_COMPOSITE_PRIMARY_KEY_DATABASE" """ +
                            """("PARENT_ID", "CHILD_ID", "MEMO") values (1, 2, 'duplicate')"""
                )
            }
        }
    }

    /**
     * ## 主キーなし実DB検証用Entity
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Table(name = "TEST_NO_PRIMARY_KEY_DATABASE", alias = "TNPKD")
    private data class TestNoPrimaryKeyDatabaseEntity(
        @Column(name = "ID")
        val id: Int,
        @Column(name = "MEMO")
        val memo: String?,
    ) : TableDefinitionEntity

    /**
     * ## 複合主キー実DB検証用Entity
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Table(name = "TEST_COMPOSITE_PRIMARY_KEY_DATABASE", alias = "TCPKD")
    private data class TestCompositePrimaryKeyDatabaseEntity(
        @PrimaryKey
        @Column(name = "PARENT_ID")
        val parentId: Int,
        @PrimaryKey
        @Column(name = "CHILD_ID")
        val childId: Int,
        @Column(name = "MEMO")
        val memo: String?,
    ) : TableDefinitionEntity
}
