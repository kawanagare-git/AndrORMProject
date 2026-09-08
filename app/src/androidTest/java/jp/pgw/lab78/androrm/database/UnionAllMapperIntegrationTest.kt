package jp.pgw.lab78.androrm.database

import android.database.Cursor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.SELECT
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.database.entities.select.UnionResultDefinitionSelect
import jp.pgw.lab78.androrm.database.entities.select.UnionSourceADefinitionSelect
import jp.pgw.lab78.androrm.database.entities.select.UnionSourceBDefinitionSelect
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** UNION ALLの実SQLite経路とKSP CursorEntityMapper経路を検証する。 */
@RunWith(AndroidJUnit4::class)
class UnionAllMapperIntegrationTest {
    private lateinit var helper: AndrOrmDatabaseHelper

    /** UNION ALLの2テーブルと重複値を作成する。 */
    @Before
    fun setUp() {
        helper = AndrOrmDatabaseHelper(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            databaseName = null,
            version = 1,
            entities = emptyList(),
        )
        helper.writableDatabase.execSQL("create table UNION_SOURCE_A (ID integer not null, LABEL text not null)")
        helper.writableDatabase.execSQL("create table UNION_SOURCE_B (ID integer not null, LABEL text not null)")
        listOf(
            "insert into UNION_SOURCE_A (ID, LABEL) values (1, 'SAMPLE-A')",
            "insert into UNION_SOURCE_A (ID, LABEL) values (2, 'COMMON')",
            "insert into UNION_SOURCE_B (ID, LABEL) values (3, 'SAMPLE-B')",
            "insert into UNION_SOURCE_B (ID, LABEL) values (4, 'COMMON')",
        ).forEach(helper.writableDatabase::execSQL)
    }

    /** インメモリデータベースを閉じる。 */
    @After
    fun tearDown() {
        helper.close()
    }

    /** UNION ALLのMap、Cursor、Entity取得が重複行を保持する。 */
    @Test
    fun unionAll_realSqlite_preservesDuplicateRowsAcrossResultApis() {
        val union = createUnion()

        val mapRows = helper.executeSelectAsMapList(union)
        assertEquals(listOf(1L, 2L, 3L, 4L), mapRows.map { it.getValue("UR_ID") })
        assertEquals(2, mapRows.count { it.getValue("UR_LABEL") == "COMMON" })

        helper.executeSelectAsCursor(union).use { cursor ->
            assertEquals(4, cursor.count)
            assertTrue(cursor.moveToFirst())
            assertEquals(Cursor.FIELD_TYPE_INTEGER, cursor.getType(0))
            assertEquals(1L, cursor.getLong(0))
        }

        assertTrue(UnionResultDefinitionSelect is CursorEntityMapper<*>)
        val entities = helper.executeSelectAsEntityList(union)
            .map { it.getValue("UR") as UnionResultDefinitionSelect }
        assertEquals(listOf(1L, 2L, 3L, 4L), entities.map { it.id })
        assertEquals(2, entities.count { it.label == "COMMON" })
    }

    /** UNION ALLの各Selectの条件を指定順のbind値で実SQLiteへ渡す。 */
    @Test
    fun unionAll_realSqlite_preservesSelectBindOrder() {
        val union = UnionAll.unionAll(
            UnionResultDefinitionSelect::class,
            Select(UnionSourceADefinitionSelect::class)
                .where { UnionSourceADefinitionSelect::id eq 2L },
            Select(UnionSourceBDefinitionSelect::class)
                .where { UnionSourceBDefinitionSelect::id eq 4L },
        )

        assertEquals(listOf(2L, 4L), union.bindValues)
        val entities = helper.executeSelectAsEntityList(union)
            .map { it.getValue("UR") as UnionResultDefinitionSelect }
        assertEquals(listOf(2L, 4L), entities.map { it.id })
        assertEquals(listOf("COMMON", "COMMON"), entities.map { it.label })
    }

    /** UNION ALL結果Entityの型定義。 */
    @Projection(
        entityNameExtend = "Select",
        properties = [ColumnProjection("id"), ColumnProjection("label")],
        commonInterface = [SELECT],
    )
    @Table(name = "UNION_RESULT_UNUSED", alias = "UR")
    data class UnionResultDefinition(
        @Column val id: Long,
        @Column val label: String,
    ) : TableDefinitionEntity

    /** UNION ALL第1構成Selectの型定義。 */
    @Projection(
        entityNameExtend = "Select",
        properties = [ColumnProjection("id"), ColumnProjection("label")],
        commonInterface = [SELECT],
    )
    @Table(name = "UNION_SOURCE_A", alias = "UA")
    data class UnionSourceADefinition(
        @Column val id: Long,
        @Column val label: String,
    ) : TableDefinitionEntity

    /** UNION ALL第2構成Selectの型定義。 */
    @Projection(
        entityNameExtend = "Select",
        properties = [ColumnProjection("id"), ColumnProjection("label")],
        commonInterface = [SELECT],
    )
    @Table(name = "UNION_SOURCE_B", alias = "UB")
    data class UnionSourceBDefinition(
        @Column val id: Long,
        @Column val label: String,
    ) : TableDefinitionEntity

    /** コンポーネント条件なしのUNION ALLを生成する。 */
    private fun createUnion(): UnionAll<out SelectEntity> = UnionAll.unionAll(
        UnionResultDefinitionSelect::class,
        Select(UnionSourceADefinitionSelect::class),
        Select(UnionSourceBDefinitionSelect::class),
    )
}
