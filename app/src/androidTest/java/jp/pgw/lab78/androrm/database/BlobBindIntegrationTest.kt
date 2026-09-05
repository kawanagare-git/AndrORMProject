package jp.pgw.lab78.androrm.database

import android.database.Cursor
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ## BLOBバインド結合テスト
 * ### 実SQLiteでByteArrayの登録、SELECT条件バインド、結果復元を検証する
 * @author Masahiro Inoue
 * @since 2026-09-03
 */
@RunWith(AndroidJUnit4::class)
class BlobBindIntegrationTest {
    /** テスト対象データベースヘルパー */
    private lateinit var helper: AndrOrmDatabaseHelper

    /**
     * ## テスト前処理
     * ### BLOBカラムを持つインメモリデータベースを作成する
     * @author Masahiro Inoue
     * @since 2026-09-03
     */
    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        helper = AndrOrmDatabaseHelper(
            context = context,
            databaseName = null,
            version = 1,
            entities = emptyList(),
        )
        helper.writableDatabase.execSQL(
            "create table BLOB_BIND_TEST (ID integer not null, PAYLOAD blob not null)",
        )
    }

    /**
     * ## テスト後処理
     * ### インメモリデータベースを閉じる
     * @author Masahiro Inoue
     * @since 2026-09-03
     */
    @After
    fun tearDown() {
        helper.close()
    }

    /**
     * ## BLOB検索検証
     * ### ByteArrayをSELECT条件へバインドし、一致行をByteArrayとして取得する
     * @author Masahiro Inoue
     * @since 2026-09-03
     */
    @Test
    fun executeSelectAsMapList_withBlobBind_returnsMatchingBinaryRow() {
        val expected = byteArrayOf(0x00, 0x7F, 0x80.toByte(), 0xFF.toByte())
        helper.executeDml(
            "insert into BLOB_BIND_TEST (ID, PAYLOAD) values (?, ?)",
            listOf(1L, expected),
        )
        helper.executeDml(
            "insert into BLOB_BIND_TEST (ID, PAYLOAD) values (?, ?)",
            listOf(2L, byteArrayOf(1, 2, 3)),
        )

        val actual = helper.executeSelectAsMapList(
            "select ID, PAYLOAD from BLOB_BIND_TEST where PAYLOAD = ?",
            listOf(expected),
        )

        assertEquals(1, actual.size)
        assertEquals(1L, actual.single()["ID"])
        assertArrayEquals(expected, actual.single()["PAYLOAD"] as ByteArray)
    }

    /** 符号境界を含むBLOBをSELECT Entityへ復元する。 */
    @Test
    fun executeSelectAsEntityList_preservesEveryByte() {
        val expected = byteArrayOf(0x00, 0x01, 0x7F, 0x80.toByte(), 0xFF.toByte())
        helper.executeDml(
            "insert into BLOB_BIND_TEST (ID, PAYLOAD) values (?, ?)",
            listOf(1L, expected),
        )
        val query = Select(BlobSelect::class).where { BlobSelect::id eq 1L }
        val rows = helper.executeSelectAsEntityList(query)
        assertEquals(listOf(1L), query.bindValues)
        assertEquals(1, rows.size)
        val actual = rows.single().getValue("BB") as BlobSelect
        assertEquals(1L, actual.id)
        assertArrayEquals(expected, actual.payload)
    }

    /** NULL、空BLOB、非空BLOBのSQLite型とCursor取得値を区別する。 */
    @Test
    fun executeSelectAsCursor_distinguishesNullEmptyAndBinary() {
        insertNullableRows()
        helper.executeSelectAsCursor(
            "select ID, PAYLOAD, typeof(PAYLOAD), length(PAYLOAD) from NULLABLE_BLOB_TEST order by ID",
        ).use { cursor ->
            assertEquals(3, cursor.count)
            assertTrue(cursor.moveToNext())
            assertEquals(1L, cursor.getLong(0))
            assertEquals(Cursor.FIELD_TYPE_NULL, cursor.getType(1))
            assertTrue(cursor.isNull(1))
            assertEquals("null", cursor.getString(2))
            assertTrue(cursor.isNull(3))
            assertTrue(cursor.moveToNext())
            assertEquals(2L, cursor.getLong(0))
            assertEquals(Cursor.FIELD_TYPE_BLOB, cursor.getType(1))
            assertArrayEquals(ByteArray(0), cursor.getBlob(1))
            assertEquals("blob", cursor.getString(2))
            assertEquals(0, cursor.getInt(3))
            assertTrue(cursor.moveToNext())
            assertEquals(3L, cursor.getLong(0))
            assertEquals(Cursor.FIELD_TYPE_BLOB, cursor.getType(1))
            assertArrayEquals(byteArrayOf(0, 1, 127, -128, -1), cursor.getBlob(1))
            assertEquals("blob", cursor.getString(2))
            assertEquals(5, cursor.getInt(3))
        }
    }

    /** Map復元時にNULLと長さ0のByteArrayを混同しない。 */
    @Test
    fun executeSelectAsMapList_distinguishesNullEmptyAndBinary() {
        insertNullableRows()
        val rows = helper.executeSelectAsMapList("select ID, PAYLOAD from NULLABLE_BLOB_TEST order by ID")
        assertEquals(listOf(1L, 2L, 3L), rows.map { it.getValue("ID") })
        assertNull(rows[0].getValue("PAYLOAD"))
        assertArrayEquals(ByteArray(0), rows[1].getValue("PAYLOAD") as ByteArray)
        assertArrayEquals(byteArrayOf(0, 1, 127, -128, -1), rows[2].getValue("PAYLOAD") as ByteArray)
    }

    /** Entityを維持したままnullable BLOBの3状態を復元する。 */
    @Test
    fun executeSelectAsEntityList_distinguishesNullEmptyAndBinary() {
        insertNullableRows()
        val rows = helper.executeSelectAsEntityList(Select(NullableBlobSelect::class))
            .map { it.getValue("NB") as NullableBlobSelect }.sortedBy { it.id }
        assertEquals(listOf(1L, 2L, 3L), rows.map { it.id })
        assertNull(rows[0].payload)
        assertArrayEquals(ByteArray(0), rows[1].payload)
        assertArrayEquals(byteArrayOf(0, 1, 127, -128, -1), rows[2].payload)
    }

    /** 空BLOBの条件バインドはNULL行や非空BLOB行と一致しない。 */
    @Test
    fun emptyBlobCondition_matchesOnlyEmptyBlob() {
        insertNullableRows()
        val empty = helper.executeSelectAsMapList(
            "select ID from NULLABLE_BLOB_TEST where PAYLOAD = ?", listOf(ByteArray(0)),
        )
        val nullRows = helper.executeSelectAsMapList(
            "select ID from NULLABLE_BLOB_TEST where PAYLOAD is null",
        )
        assertEquals(listOf(2L), empty.map { it.getValue("ID") })
        assertEquals(listOf(1L), nullRows.map { it.getValue("ID") })
    }

    /** 要件で定めた3状態を同一nullable列へ型付きDMLで登録する。 */
    private fun insertNullableRows() {
        helper.writableDatabase.execSQL("create table NULLABLE_BLOB_TEST (ID integer not null, PAYLOAD blob)")
        val payloads = listOf(null, ByteArray(0), byteArrayOf(0, 1, 127, -128, -1))
        payloads.forEachIndexed { index, payload ->
            helper.executeDml(
                "insert into NULLABLE_BLOB_TEST (ID, PAYLOAD) values (?, ?)",
                listOf(index.toLong() + 1L, payload),
            )
        }
    }

    /** non-null BLOBの復元対象。 */
    @Table(name = "BLOB_BIND_TEST", alias = "BB")
    data class BlobSelect(@Column val id: Long, @Column val payload: ByteArray) : SelectEntity

    /** NULLのBLOBを持つ行も識別列によってEntityとして保持する。 */
    @Table(name = "NULLABLE_BLOB_TEST", alias = "NB")
    data class NullableBlobSelect(@Column val id: Long, @Column val payload: ByteArray?) : SelectEntity
}
