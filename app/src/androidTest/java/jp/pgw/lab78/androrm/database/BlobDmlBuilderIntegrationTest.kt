package jp.pgw.lab78.androrm.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.ABSERT
import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.INSERT
import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.UPDATE
import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.UPSERT
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.PrimaryKey
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.database.entities.absert.BlobDmlBuilderDefinitionAbsert
import jp.pgw.lab78.androrm.database.entities.insert.BlobDmlBuilderDefinitionInsert
import jp.pgw.lab78.androrm.database.entities.update.BlobDmlBuilderDefinitionUpdate
import jp.pgw.lab78.androrm.database.entities.upsert.BlobDmlBuilderDefinitionUpsert
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** AndrORMのDML Builderを通したBLOBの登録・更新・競合処理を検証する。 */
@RunWith(AndroidJUnit4::class)
class BlobDmlBuilderIntegrationTest {
    private lateinit var helper: AndrOrmDatabaseHelper

    /** BLOB DML対象テーブルを作成する。 */
    @Before
    fun setUp() {
        helper = AndrOrmDatabaseHelper(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            databaseName = null,
            version = 1,
            entities = listOf(BlobDmlBuilderDefinition::class),
        )
        helper.writableDatabase
    }

    /** インメモリデータベースを閉じる。 */
    @After
    fun tearDown() {
        helper.close()
    }

    /** INSERT、UPDATE、UPSERT、ABSERTでBLOB値を保持する。 */
    @Test
    fun dmlBuilders_preserveBlobValues() {
        val first = binary(0x00, 0x01, 0x7F, 0x80, 0xFF)
        val updated = binary(0xFF, 0x80, 0x7F, 0x01, 0x00)

        helper.executeDml(
            Insert(BlobDmlBuilderDefinitionInsert::class)
                .addEntity(BlobDmlBuilderDefinitionInsert(1L, first)),
        )
        helper.executeDml(
            Update(BlobDmlBuilderDefinitionUpdate::class)
                .set(BlobDmlBuilderDefinitionUpdate(1L, updated))
                .where { BlobDmlBuilderDefinitionUpdate::id eq 1L },
        )
        helper.executeDml(
            Upsert(BlobDmlBuilderDefinitionUpsert::class)
                .onConflict { column(BlobDmlBuilderDefinitionUpsert::id) }
                .set { BlobDmlBuilderDefinitionUpsert::payload assign excluded(BlobDmlBuilderDefinitionUpsert::payload) }
                .addEntity(BlobDmlBuilderDefinitionUpsert(1L, first)),
        )
        helper.executeDml(
            Upsert(BlobDmlBuilderDefinitionUpsert::class)
                .onConflict { column(BlobDmlBuilderDefinitionUpsert::id) }
                .set { BlobDmlBuilderDefinitionUpsert::payload assign excluded(BlobDmlBuilderDefinitionUpsert::payload) }
                .addEntity(BlobDmlBuilderDefinitionUpsert(3L, updated)),
        )
        helper.executeDml(
            Absert(BlobDmlBuilderDefinitionAbsert::class)
                .onConflict { column(BlobDmlBuilderDefinitionAbsert::id) }
                .addEntity(BlobDmlBuilderDefinitionAbsert(2L, first)),
        )
        helper.executeDml(
            Absert(BlobDmlBuilderDefinitionAbsert::class)
                .onConflict { column(BlobDmlBuilderDefinitionAbsert::id) }
                .addEntity(BlobDmlBuilderDefinitionAbsert(2L, updated)),
        )

        helper.executeSelectAsCursor("select ID, PAYLOAD from BLOB_DML_BUILDER order by ID").use { cursor ->
            assertEquals(3, cursor.count)
            cursor.moveToFirst()
            assertEquals(1L, cursor.getLong(0))
            assertArrayEquals(first, cursor.getBlob(1))
            cursor.moveToNext()
            assertEquals(2L, cursor.getLong(0))
            assertArrayEquals(first, cursor.getBlob(1))
            cursor.moveToNext()
            assertEquals(3L, cursor.getLong(0))
            assertArrayEquals(updated, cursor.getBlob(1))
        }
    }

    /** 指定した符号境界の値をBLOBとして作成する。 */
    private fun binary(vararg values: Int): ByteArray = values.map(Int::toByte).toByteArray()

    /** BLOB DML Builderのテーブル定義。 */
    @Table(name = "BLOB_DML_BUILDER")
    @Projection(entityNameExtend = "Insert", properties = [ColumnProjection("id"), ColumnProjection("payload")], commonInterface = [INSERT])
    @Projection(entityNameExtend = "Update", properties = [ColumnProjection("id"), ColumnProjection("payload")], commonInterface = [UPDATE])
    @Projection(entityNameExtend = "Upsert", properties = [ColumnProjection("id"), ColumnProjection("payload")], commonInterface = [UPSERT])
    @Projection(entityNameExtend = "Absert", properties = [ColumnProjection("id"), ColumnProjection("payload")], commonInterface = [ABSERT])
    data class BlobDmlBuilderDefinition(
        @PrimaryKey @Column val id: Long,
        @Column val payload: ByteArray,
    ) : TableDefinitionEntity
}
