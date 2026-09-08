package jp.pgw.lab78.androrm.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.PrimaryKey
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** @Column(default) の BLOB が実SQLiteのCREATE TABLE後INSERTで適用されることを検証する。 */
@RunWith(AndroidJUnit4::class)
class BlobColumnDefaultIntegrationTest {
    private lateinit var helper: AndrOrmDatabaseHelper

    /** @Column(default) を含むテーブルを作成する。 */
    @Before
    fun setUp() {
        helper = AndrOrmDatabaseHelper(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            databaseName = null,
            version = 1,
            entities = listOf(BlobColumnDefaultDefinition::class),
        )
        helper.writableDatabase
    }

    /** インメモリデータベースを閉じる。 */
    @After
    fun tearDown() {
        helper.close()
    }

    /** payloadを明示せずINSERTしても既定のBLOB値を取得できる。 */
    @Test
    fun insertWithoutBlobColumn_usesColumnDefault() {
        helper.executeDml("insert into BLOB_COLUMN_DEFAULT (ID) values (?)", listOf(1L))

        helper.executeSelectAsCursor(
            "select PAYLOAD from BLOB_COLUMN_DEFAULT where ID = ?",
            listOf(1L),
        ).use { cursor ->
            assertEquals(1, cursor.count)
            assertEquals(true, cursor.moveToFirst())
            assertArrayEquals(byteArrayOf(0x00, 0x80.toByte(), 0xFF.toByte()), cursor.getBlob(0))
        }
    }

    /** @Column(default) のBLOB定義。 */
    @Table(name = "BLOB_COLUMN_DEFAULT")
    data class BlobColumnDefaultDefinition(
        @PrimaryKey @Column val id: Long,
        @Column(default = "X'0080FF'") val payload: ByteArray,
    ) : TableDefinitionEntity
}
