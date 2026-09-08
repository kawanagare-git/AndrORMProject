package jp.pgw.lab78.androrm.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.SELECT
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.database.entities.select.AllTypeDefinitionSelect
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** 全対応型をKSP生成CursorEntityMapperで実SQLiteから復元する。 */
@RunWith(AndroidJUnit4::class)
class AllTypeCursorMapperIntegrationTest {
    private lateinit var helper: AndrOrmDatabaseHelper

    /** 全対応型テーブルを作成し、1行を登録する。 */
    @Before
    fun setUp() {
        helper = AndrOrmDatabaseHelper(
            context = InstrumentationRegistry.getInstrumentation().targetContext,
            databaseName = null,
            version = 1,
            entities = emptyList(),
        )
        helper.writableDatabase.execSQL(
            "create table ALL_TYPE_MAPPER (" +
                    "ID integer, LONG_VALUE integer, FLOAT_VALUE real, DOUBLE_VALUE real, " +
                    "ENABLED integer, NAME text, EVENT_DATE text, EVENT_TIME text, EVENT_AT text, " +
                    "PAYLOAD blob, OPTIONAL_NAME text, OPTIONAL_PAYLOAD blob)",
        )
        helper.executeDml(
            "insert into ALL_TYPE_MAPPER values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            listOf(
                7L,
                9000000000L,
                1.25,
                2.5,
                1L,
                "SAMPLE",
                "2026-09-07",
                "12:34:56",
                "2026-09-07T12:34:56",
                byteArrayOf(0, 1, 127, -128, -1),
                null,
                null,
            ),
        )
    }

    /** インメモリデータベースを閉じる。 */
    @After
    fun tearDown() {
        helper.close()
    }

    /** 全対応型とnullable型をEntityへ直接復元する。 */
    @Test
    fun executeSelectAsEntityList_mapsAllSupportedTypes() {
        assertTrue(AllTypeDefinitionSelect is CursorEntityMapper<*>)
        val actual = helper.executeSelectAsEntityList(Select(AllTypeDefinitionSelect::class))
            .single().getValue("ATM") as AllTypeDefinitionSelect

        assertEquals(7, actual.id)
        assertEquals(9000000000L, actual.longValue)
        assertEquals(1.25f, actual.floatValue, 0.0001f)
        assertEquals(2.5, actual.doubleValue, 0.0001)
        assertTrue(actual.enabled)
        assertEquals("SAMPLE", actual.name)
        assertEquals(LocalDate.of(2026, 9, 7), actual.eventDate)
        assertEquals(LocalTime.of(12, 34, 56), actual.eventTime)
        assertEquals(LocalDateTime.of(2026, 9, 7, 12, 34, 56), actual.eventAt)
        assertArrayEquals(byteArrayOf(0, 1, 127, -128, -1), actual.payload)
        assertNull(actual.optionalName)
        assertNull(actual.optionalPayload)
    }

    /** 全対応型のSELECT Entity定義。 */
    @Projection(
        entityNameExtend = "Select",
        properties = [
            ColumnProjection("id"), ColumnProjection("longValue"), ColumnProjection("floatValue"),
            ColumnProjection("doubleValue"), ColumnProjection("enabled"), ColumnProjection("name"),
            ColumnProjection("eventDate"), ColumnProjection("eventTime"), ColumnProjection("eventAt"),
            ColumnProjection("payload"), ColumnProjection("optionalName"),
            ColumnProjection("optionalPayload"),
        ],
        commonInterface = [SELECT],
    )
    @Table(name = "ALL_TYPE_MAPPER", alias = "ATM")
    data class AllTypeDefinition(
        @Column(name = "ID") val id: Int,
        @Column(name = "LONG_VALUE") val longValue: Long,
        @Column(name = "FLOAT_VALUE") val floatValue: Float,
        @Column(name = "DOUBLE_VALUE") val doubleValue: Double,
        @Column(name = "ENABLED") val enabled: Boolean,
        @Column(name = "NAME") val name: String,
        @Column(name = "EVENT_DATE") val eventDate: LocalDate,
        @Column(name = "EVENT_TIME") val eventTime: LocalTime,
        @Column(name = "EVENT_AT") val eventAt: LocalDateTime,
        @Column(name = "PAYLOAD") val payload: ByteArray,
        @Column(name = "OPTIONAL_NAME") val optionalName: String?,
        @Column(name = "OPTIONAL_PAYLOAD") val optionalPayload: ByteArray?,
    ) : TableDefinitionEntity
}
