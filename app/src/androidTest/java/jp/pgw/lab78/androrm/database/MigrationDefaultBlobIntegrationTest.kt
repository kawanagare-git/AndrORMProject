package jp.pgw.lab78.androrm.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.MigrationDefault
import jp.pgw.lab78.androrm.common.database.annotation.PrimaryKey
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** MigrationDefaultのBLOB値を実SQLiteアップグレードで検証する。 */
@RunWith(AndroidJUnit4::class)
class MigrationDefaultBlobIntegrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** 前回のテストDBを削除し、旧スキーマのレコードを作成する。 */
    @Before
    fun setUp() {
        context.deleteDatabase(DATABASE_NAME)
        AndrOrmDatabaseHelper(
            context = context,
            databaseName = DATABASE_NAME,
            version = 1,
            entities = listOf(MigrationBlobV1::class),
        ).use { helper ->
            helper.executeDml(
                "insert into MIGRATION_BLOB_TEST (ID) values (?)",
                listOf(1L),
            )
        }
    }

    /** Version 2へ移行した既存行にBLOB MigrationDefaultが設定される。 */
    @Test
    fun upgrade_withBlobMigrationDefault_transfersBinaryLiteral() {
        AndrOrmDatabaseHelper(
            context = context,
            databaseName = DATABASE_NAME,
            version = 2,
            entities = listOf(MigrationBlobV2::class),
        ).use { helper ->
            helper.executeSelectAsCursor(
                "select PAYLOAD from MIGRATION_BLOB_TEST where ID = ?",
                listOf(1L),
            ).use { cursor ->
                assertEquals(1, cursor.count)
                assertEquals(true, cursor.moveToFirst())
                assertArrayEquals(byteArrayOf(0, 1, 127, -1), cursor.getBlob(0))
            }
        }
    }

    /** Version 1のテーブル定義。 */
    @Table(name = "MIGRATION_BLOB_TEST")
    data class MigrationBlobV1(
        @PrimaryKey
        @Column val id: Long,
    ) : TableDefinitionEntity

    /** BLOB列をMigrationDefaultで追加するVersion 2のテーブル定義。 */
    @Table(name = "MIGRATION_BLOB_TEST")
    data class MigrationBlobV2(
        @PrimaryKey
        @Column val id: Long,
        @MigrationDefault("X'00017FFF'")
        @Column val payload: ByteArray,
    ) : TableDefinitionEntity

    private companion object {
        const val DATABASE_NAME = "migration-blob-integration-test.db"
    }
}
