package jp.pgw.lab78.androrm.database

import android.database.Cursor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Function
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.database.entities.select.BlobProjectionDefinitionSelect
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** KSP生成BLOB通常列とSQL関数のBLOB結果を実SQLiteで検証する。 */
@RunWith(AndroidJUnit4::class)
class BlobProjectionIntegrationTest {
    private lateinit var helper: AndrOrmDatabaseHelper

    /** 通常列のKSP生成元からテーブルを作成し、既知のバイト列を登録する。 */
    @Before
    fun setUp() {
        helper = AndrOrmDatabaseHelper(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            databaseName = null,
            version = 1,
            entities = listOf(BlobProjectionDefinition::class),
        )
        helper.executeDml(
            "insert into BLOB_PROJECTION_TEST (ID, PAYLOAD, OPTIONAL_PAYLOAD) values (?, ?, ?)",
            listOf(1L, byteArrayOf(0, 1, 127, -128, -1), null),
        )
        helper.executeDml(
            "insert into BLOB_PROJECTION_TEST (ID, PAYLOAD, OPTIONAL_PAYLOAD) values (?, ?, ?)",
            listOf(2L, ByteArray(0), ByteArray(0)),
        )
    }

    /** インメモリデータベースを閉じる。 */
    @After
    fun tearDown() {
        helper.close()
    }

    /** 実際にKSP生成したEntityの型、null許容性と取得内容を検証する。 */
    @Test
    fun generatedColumns_preserveByteArrayTypesAndValues() {
        assertTrue(BlobProjectionDefinitionSelect is CursorEntityMapper<*>)
        assertEquals(ByteArray::class, BlobProjectionDefinitionSelect::payload.returnType.classifier)
        assertFalse(BlobProjectionDefinitionSelect::payload.returnType.isMarkedNullable)
        assertEquals(ByteArray::class, BlobProjectionDefinitionSelect::optionalPayload.returnType.classifier)
        assertTrue(BlobProjectionDefinitionSelect::optionalPayload.returnType.isMarkedNullable)
        val query = Select(BlobProjectionDefinitionSelect::class)
        val rows = helper.executeSelectAsEntityList(query)
            .map { it.getValue("BP") as BlobProjectionDefinitionSelect }.sortedBy { it.id }
        assertEquals(listOf(1L, 2L), rows.map { it.id })
        assertArrayEquals(byteArrayOf(0, 1, 127, -128, -1), rows[0].payload)
        assertNull(rows[0].optionalPayload)
        assertArrayEquals(ByteArray(0), rows[1].payload)
        assertArrayEquals(ByteArray(0), rows[1].optionalPayload)
        assertTrue(query.bindValues.isEmpty())
    }

    /** raw SQL関数のBLOB結果をCursorで取得する。 */
    @Test
    fun blobFunction_cursorPreservesBinaryResult() {
        val query = Select(BlobFunctionSelect::class).where { BlobFunctionSelect::id eq 1L }
        helper.executeSelectAsCursor(query).use { cursor ->
            assertEquals(1, cursor.count)
            assertTrue(cursor.moveToFirst())
            val index = cursor.getColumnIndexOrThrow("BF_BINARY_VALUE")
            assertEquals(Cursor.FIELD_TYPE_BLOB, cursor.getType(index))
            assertArrayEquals(byteArrayOf(0, 1, 127, -128, -1), cursor.getBlob(index))
        }
        assertEquals(listOf(1L), query.bindValues)
        assertTrue(query.build().contains("substr(X'00017F80FF', 1, 5)"))
    }

    /** raw SQL関数のBLOB結果をMapで取得する。 */
    @Test
    fun blobFunction_mapPreservesBinaryResult() {
        val query = Select(BlobFunctionSelect::class).where { BlobFunctionSelect::id eq 1L }
        val rows = helper.executeSelectAsMapList(query)
        assertEquals(1, rows.size)
        assertArrayEquals(byteArrayOf(0, 1, 127, -128, -1), rows.single().getValue("BF_BINARY_VALUE") as ByteArray)
        assertEquals(listOf(1L), query.bindValues)
    }

    /** raw SQL関数のBLOB結果をByteArray型のEntityプロパティへ復元する。 */
    @Test
    fun blobFunction_entityPreservesBinaryResult() {
        val query = Select(BlobFunctionSelect::class).where { BlobFunctionSelect::id eq 1L }
        val rows = helper.executeSelectAsEntityList(query)
        assertEquals(1, rows.size)
        val actual = rows.single().getValue("BF") as BlobFunctionSelect
        assertEquals(1L, actual.id)
        assertArrayEquals(byteArrayOf(0, 1, 127, -128, -1), actual.binaryValue)
        assertEquals(listOf(1L), query.bindValues)
    }

    /** KSP戻り値推論から独立して実行系のBLOB関数復元を検証するEntity。 */
    @Table(name = "BLOB_PROJECTION_TEST", alias = "BF")
    data class BlobFunctionSelect(
        @Column val id: Long,
        @Function(
            columnFunction = ColumnFunction.CUSTOM,
            alias = "BINARY_VALUE",
            raw = "substr(X'00017F80FF', 1, 5)",
        )
        val binaryValue: ByteArray,
    ) : SelectEntity
}

/** 通常カラムのByteArray型とnull許容性を検証するKSP生成元。 */
@Projection(
    entityNameExtend = "Select",
    properties = [ColumnProjection("id"), ColumnProjection("payload"), ColumnProjection("optionalPayload")],
    commonInterface = [DMLInterfaceEnum.SELECT],
)
@Table(name = "BLOB_PROJECTION_TEST", alias = "BP")
data class BlobProjectionDefinition(
    @Column val id: Long,
    @Column val payload: ByteArray,
    @Column val optionalPayload: ByteArray?,
) : TableDefinitionEntity
