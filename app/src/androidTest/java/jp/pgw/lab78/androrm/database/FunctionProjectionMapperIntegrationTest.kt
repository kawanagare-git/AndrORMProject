package jp.pgw.lab78.androrm.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.SELECT
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.FunctionProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.database.entities.select.FunctionProjectionDefinitionSelect
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** FunctionProjectionの通常型・BLOB型とKSP CursorEntityMapperを実SQLiteで検証する。 */
@RunWith(AndroidJUnit4::class)
class FunctionProjectionMapperIntegrationTest {
    private lateinit var helper: AndrOrmDatabaseHelper

    /** FunctionProjectionの検証対象テーブルを作成する。 */
    @Before
    fun setUp() {
        helper = AndrOrmDatabaseHelper(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            databaseName = null,
            version = 1,
            entities = emptyList(),
        )
        helper.writableDatabase.execSQL(
            "create table FUNCTION_PROJECTION_TEST (ID integer not null, NAME text not null, PAYLOAD blob not null)",
        )
        helper.executeDml(
            "insert into FUNCTION_PROJECTION_TEST (ID, NAME, PAYLOAD) values (?, ?, ?)",
            listOf(1L, "SAMPLE", byteArrayOf(0, 1, 127, -128, -1)),
        )
    }

    /** インメモリデータベースを閉じる。 */
    @After
    fun tearDown() {
        helper.close()
    }

    /** LENGTHとSUBSTRのFunctionProjectionをKSP Mapper経由でEntityへ復元する。 */
    @Test
    fun executeSelectAsEntityList_mapsFunctionProjectionValues() {
        assertTrue(FunctionProjectionDefinitionSelect is CursorEntityMapper<*>)

        val rows = helper.executeSelectAsEntityList(
            Select(FunctionProjectionDefinitionSelect::class),
        )
        val actual = rows.single().getValue("FP") as FunctionProjectionDefinitionSelect

        assertEquals(1L, actual.id)
        assertEquals(6, actual.nameLength)
        assertArrayEquals(byteArrayOf(0, 1), actual.payloadPart)
    }

    /** FunctionProjectionの型推論対象となるテーブル定義。 */
    @Projection(
        entityNameExtend = "Select",
        properties = [ColumnProjection("id")],
        functions = [
            FunctionProjection(
                function = ColumnFunction.LENGTH,
                args = ["name"],
                alias = "NAME_LENGTH",
            ),
            FunctionProjection(
                function = ColumnFunction.SUBSTR,
                args = ["payload", "1", "2"],
                alias = "PAYLOAD_PART",
            ),
        ],
        commonInterface = [SELECT],
    )
    @Table(name = "FUNCTION_PROJECTION_TEST", alias = "FP")
    data class FunctionProjectionDefinition(
        @Column val id: Long,
        @Column val name: String,
        @Column val payload: ByteArray,
    ) : TableDefinitionEntity
}
