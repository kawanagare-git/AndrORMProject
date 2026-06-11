package jp.pgw.lab78.androrm.database

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import jp.pgw.lab78.androrm.common.MessageConstants.AE00021
import jp.pgw.lab78.androrm.common.MessageConstants.AE00022
import jp.pgw.lab78.androrm.common.MessageConstants.AE00023
import jp.pgw.lab78.androrm.common.MessageConstants.AE00024
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.entities.define.SalaryEntity
import jp.pgw.lab78.androrm.database.entities.define.TestAllEntity
import jp.pgw.lab78.androrm.database.entities.define.TestLargeEntity
import jp.pgw.lab78.androrm.database.entities.select.EmployeeEntity
import jp.pgw.lab78.androrm.database.entities.select.EmployeeEntityIdSelection
import jp.pgw.lab78.androrm.database.entities.select.SalaryEntitySelective
import jp.pgw.lab78.androrm.database.interfaces.QueryBuilderLike
import jp.pgw.lab78.androrm.database.queryparts.JoinType.LEFT
import jp.pgw.lab78.androrm.database.utility.EntityManager.SelectColumnTarget
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.lang.reflect.InvocationTargetException
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.KClass
import jp.pgw.lab78.androrm.database.entities.define.EmployeeEntity as EmployeeTableEntity

class AndrOrmDatabaseHelperTest {

    @Test
    fun testOnCreate_withDefineEntity_executeCreateTableAndIndexQueries() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val helper = TestAndrOrmDatabaseHelper(
            TestLargeEntity::class,
        )

        helper.onCreate(db)

        assertEquals(
            listOf(
                "create table TEST_LARGE_ENTITY " +
                        "(ID INTEGER, CODE TEXT, NAME TEXT, FURIGANA TEXT, GENDER TEXT, " +
                        "BIRTHDAY DATETIME, PLACE_OF_BIRTH TEXT, EMAIL TEXT, PHONE_NUMBER TEXT, " +
                        "POSTAL_CODE TEXT, PREFECTURE TEXT, CITY TEXT, ADDRESS_LINE TEXT, " +
                        "SCORE REAL, BALANCE INTEGER, ACTIVE INTEGER, REGISTERED_AT DATETIME, " +
                        "LAST_LOGIN_AT DATETIME, MEMO TEXT, CREATED_AT DATETIME, UPDATED_AT DATETIME, " +
                        "primary key (ID))",
                "create index if not exists IDX_TEST_LARGE_ENTITY_ACTIVE_CREATED0 " +
                        "on TEST_LARGE_ENTITY (ACTIVE, CREATED_AT)",
                "create unique index if not exists UQ_TEST_CODE0 " +
                        "on TEST_LARGE_ENTITY (CODE)",
                "create unique index if not exists UQ_TEST_PERSONAL_INFO0 " +
                        "on TEST_LARGE_ENTITY (NAME, FURIGANA, GENDER, BIRTHDAY, PLACE_OF_BIRTH)",
            ),
            captureExecutedSql(db, 4),
        )
    }

    @Test
    fun testOnUpgrade_withDefineEntity_executeStandardMigrationQueries() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val helper = TestAndrOrmDatabaseHelper(
            TestAllEntity::class,
        )

        helper.onUpgrade(
            db = db,
            oldVersion = 1,
            newVersion = 2,
        )

        assertEquals(
            listOf(
                "create table TEST_ALL_ENTITY_new " +
                        "(ID INTEGER, NAME TEXT, ADDRESS TEXT, BIRTHDAY DATETIME, " +
                        "SUB_ID INTEGER, UPDATE_DATE DATETIME, INSERT_DATE_TIME DATETIME)",
                "insert into TEST_ALL_ENTITY_new " +
                        "(ID,NAME,ADDRESS,BIRTHDAY,SUB_ID,UPDATE_DATE,INSERT_DATE_TIME) " +
                        "select ID,NAME,ADDRESS,BIRTHDAY,SUB_ID,UPDATE_DATE,INSERT_DATE_TIME " +
                        "from TEST_ALL_ENTITY",
                "drop table if exists TEST_ALL_ENTITY",
                "alter table TEST_ALL_ENTITY_new rename to TEST_ALL_ENTITY",
            ),
            captureExecutedSql(db, 4),
        )
    }

    @Test
    fun testMigrateDatabase_customColumnMappingsUnknownEntity_throwsIllegalStateException() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val helper = TestAndrOrmDatabaseHelper(
            TestLargeEntity::class,
            customColumnMappings = mapOf(
                TestUnknownEntity::class to listOf(
                    "ID" to "ID",
                ),
            ),
        )

        val actual = assertThrows<IllegalStateException> {
            helper.callMigrateDatabase(db)
        }

        assertEquals(
            AE00021.format(TestUnknownEntity::class),
            actual.message,
        )
    }

    @Test
    fun testMigrateDatabase_customColumnMappingsUnknownOldColumn_throwsIllegalArgumentException() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val helper = TestAndrOrmDatabaseHelper(
            TestLargeEntity::class,
            customColumnMappings = mapOf(
                TestLargeEntity::class to listOf(
                    "UNKNOWN_OLD_COLUMN" to "NAME",
                ),
            ),
        )

        val actual = assertThrows<IllegalArgumentException> {
            helper.callMigrateDatabase(db)
        }

        assertEquals(
            AE00022.format("UNKNOWN_OLD_COLUMN"),
            actual.message,
        )
    }

    @Test
    fun testExecuteDml_withoutBindValues_compileStatementAndExecuteUpdateDelete() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val statement = Mockito.mock(SQLiteStatement::class.java)
        val helper = Mockito.spy(
            TestAndrOrmDatabaseHelper(
                TestLargeEntity::class,
            )
        )
        val sql = "delete from TEST_LARGE_ENTITY"

        Mockito.doReturn(db)
            .`when`(helper)
            .writableDatabase
        Mockito.`when`(
            db.compileStatement(sql),
        ).thenReturn(statement)
        Mockito.`when`(statement.executeUpdateDelete()).thenReturn(1)

        val query = TestQueryBuilderLike(
            query = sql,
        )

        val actual = helper.executeDml(query)

        assertEquals(1, actual)
        Mockito.verify(db).compileStatement(sql)
        Mockito.verify(statement).executeUpdateDelete()
    }

    @Test
    fun testExecuteDml_withBindValues_bindArgsAndExecuteUpdateDelete() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val statement = Mockito.mock(SQLiteStatement::class.java)
        val helper = Mockito.spy(
            TestAndrOrmDatabaseHelper(
                TestLargeEntity::class,
            )
        )
        val sql = "update TEST_LARGE_ENTITY " +
                "set NAME = ?, BIRTHDAY = ?, SCORE = ?, ACTIVE = ?, REGISTERED_AT = ? " +
                "where ID = ?"
        val birthday = LocalDate.of(1968, 1, 7)
        val registeredAt = LocalDateTime.of(2026, 6, 5, 7, 36, 0)

        Mockito.doReturn(db)
            .`when`(helper)
            .writableDatabase
        Mockito.`when`(
            db.compileStatement(sql),
        ).thenReturn(statement)
        Mockito.`when`(statement.executeUpdateDelete()).thenReturn(1)

        val query = TestQueryBuilderLikeWithBindValues(
            query = sql,
            bindValues = listOf(
                "川流",
                birthday,
                98.25,
                true,
                registeredAt,
                100L,
            ),
        )

        val actual = helper.executeDml(query)

        assertEquals(1, actual)
        Mockito.verify(db).compileStatement(sql)
        Mockito.verify(statement).bindString(1, "川流")
        Mockito.verify(statement).bindString(2, birthday.toString())
        Mockito.verify(statement).bindDouble(3, 98.25)
        Mockito.verify(statement).bindLong(4, 1L)
        Mockito.verify(statement).bindString(5, registeredAt.toString())
        Mockito.verify(statement).bindLong(6, 100L)
        Mockito.verify(statement).executeUpdateDelete()
    }

    @Test
    fun testExecuteDml_withNullBindValue_bindNull() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val statement = Mockito.mock(SQLiteStatement::class.java)
        val helper = Mockito.spy(
            TestAndrOrmDatabaseHelper(
                TestLargeEntity::class,
            )
        )
        val sql = "update TEST_LARGE_ENTITY set MEMO = ? where ID = ?"

        Mockito.doReturn(db)
            .`when`(helper)
            .writableDatabase
        Mockito.`when`(
            db.compileStatement(sql),
        ).thenReturn(statement)
        Mockito.`when`(statement.executeUpdateDelete()).thenReturn(1)

        val actual = helper.executeDml(sql, listOf(null, 100L))

        assertEquals(1, actual)
        Mockito.verify(statement).bindNull(1)
        Mockito.verify(statement).bindLong(2, 100L)
        Mockito.verify(statement).executeUpdateDelete()
    }

    @Test
    fun testExecuteDml_withByteArrayBindValue_bindBlob() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val statement = Mockito.mock(SQLiteStatement::class.java)
        val helper = Mockito.spy(
            TestAndrOrmDatabaseHelper(
                TestLargeEntity::class,
            )
        )
        val sql = "update TEST_LARGE_ENTITY set MEMO = ? where ID = ?"
        val binary = byteArrayOf(1, 2, 3)

        Mockito.doReturn(db)
            .`when`(helper)
            .writableDatabase
        Mockito.`when`(
            db.compileStatement(sql),
        ).thenReturn(statement)
        Mockito.`when`(statement.executeUpdateDelete()).thenReturn(1)

        val actual = helper.executeDml(sql, listOf(binary, 100L))

        assertEquals(1, actual)
        Mockito.verify(statement).bindBlob(1, binary)
        Mockito.verify(statement).bindLong(2, 100L)
        Mockito.verify(statement).executeUpdateDelete()
    }

    @Test
    fun testExecuteSelectAsCursor_withoutBindValues_rawQuery() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val cursor = Mockito.mock(Cursor::class.java)
        val helper = Mockito.spy(
            TestAndrOrmDatabaseHelper(
                TestLargeEntity::class,
            )
        )
        val sql = "select ID, NAME, SCORE from TEST_LARGE_ENTITY"

        Mockito.doReturn(db)
            .`when`(helper)
            .readableDatabase
        Mockito.`when`(
            db.rawQuery(sql, null),
        ).thenReturn(cursor)

        val actual = helper.executeSelectAsCursor(sql)

        assertSame(cursor, actual)
        Mockito.verify(db).rawQuery(sql, null)
    }

    @Test
    fun testExecuteSelectAsCursor_withBindValues_rawQueryWithSelectionArgs() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val cursor = Mockito.mock(Cursor::class.java)
        val helper = Mockito.spy(
            TestAndrOrmDatabaseHelper(
                TestLargeEntity::class,
            )
        )
        val sql = "select ID, NAME, SCORE from TEST_LARGE_ENTITY " +
                "where ID = ? and ACTIVE = ? and REGISTERED_AT >= ?"
        val registeredAt = LocalDateTime.of(2026, 6, 5, 7, 36, 0)

        Mockito.doReturn(db)
            .`when`(helper)
            .readableDatabase
        Mockito.`when`(
            db.rawQuery(Mockito.eq(sql), Mockito.any<Array<String>>()),
        ).thenReturn(cursor)

        val actual = helper.executeSelectAsCursor(
            sql,
            listOf(100L, true, registeredAt),
        )

        val bindArgsCaptor = ArgumentCaptor.forClass(Array<String>::class.java)
        assertSame(cursor, actual)
        Mockito.verify(db).rawQuery(
            Mockito.eq(sql),
            bindArgsCaptor.capture(),
        )
        assertEquals(
            listOf("100", "1", registeredAt.toString()),
            bindArgsCaptor.value.toList(),
        )
    }

    @Test
    fun testExecuteSelectAsCursor_withNullBindValue_throwsIllegalArgumentException() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val helper = Mockito.spy(
            TestAndrOrmDatabaseHelper(
                TestLargeEntity::class,
            )
        )
        val sql = "select ID from TEST_LARGE_ENTITY where MEMO = ?"

        Mockito.doReturn(db)
            .`when`(helper)
            .readableDatabase

        val actual = assertThrows<IllegalArgumentException> {
            helper.executeSelectAsCursor(sql, listOf(null))
        }

        assertEquals(AE00023.format(1), actual.message)
        Mockito.verify(db, Mockito.never()).rawQuery(
            Mockito.anyString(),
            Mockito.any<Array<String>>(),
        )
    }

    @Test
    fun testExecuteSelectAsCursor_withByteArrayBindValue_throwsIllegalArgumentException() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val helper = Mockito.spy(
            TestAndrOrmDatabaseHelper(
                TestLargeEntity::class,
            )
        )
        val sql = "select ID from TEST_LARGE_ENTITY where MEMO = ?"

        Mockito.doReturn(db)
            .`when`(helper)
            .readableDatabase

        val actual = assertThrows<IllegalArgumentException> {
            helper.executeSelectAsCursor(sql, listOf(byteArrayOf(1, 2, 3)))
        }

        assertEquals(AE00024.format(1), actual.message)
        Mockito.verify(db, Mockito.never()).rawQuery(
            Mockito.anyString(),
            Mockito.any<Array<String>>(),
        )
    }

    @Test
    fun testExecuteSelectAsMapList_withRows_returnMapList() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val cursor = Mockito.mock(Cursor::class.java)
        val helper = Mockito.spy(
            TestAndrOrmDatabaseHelper(
                TestLargeEntity::class,
            )
        )
        val sql = "select ID, NAME, SCORE, MEMO from TEST_LARGE_ENTITY"

        Mockito.doReturn(db)
            .`when`(helper)
            .readableDatabase
        Mockito.`when`(
            db.rawQuery(sql, null),
        ).thenReturn(cursor)
        Mockito.`when`(cursor.columnNames).thenReturn(
            arrayOf("ID", "NAME", "SCORE", "MEMO"),
        )
        Mockito.`when`(cursor.moveToNext()).thenReturn(true, true, false)
        Mockito.`when`(cursor.getType(0)).thenReturn(Cursor.FIELD_TYPE_INTEGER)
        Mockito.`when`(cursor.getType(1)).thenReturn(Cursor.FIELD_TYPE_STRING)
        Mockito.`when`(cursor.getType(2)).thenReturn(Cursor.FIELD_TYPE_FLOAT)
        Mockito.`when`(cursor.getType(3)).thenReturn(Cursor.FIELD_TYPE_STRING)
        Mockito.`when`(cursor.getLong(0)).thenReturn(1L, 2L)
        Mockito.`when`(cursor.getString(1)).thenReturn("川流", "プラテス")
        Mockito.`when`(cursor.getDouble(2)).thenReturn(98.25, 88.5)
        Mockito.`when`(cursor.getString(3)).thenReturn("初回", "二回目")

        val actual = helper.executeSelectAsMapList(sql)

        assertEquals(
            listOf(
                linkedMapOf(
                    "ID" to 1L,
                    "NAME" to "川流",
                    "SCORE" to 98.25,
                    "MEMO" to "初回",
                ),
                linkedMapOf(
                    "ID" to 2L,
                    "NAME" to "プラテス",
                    "SCORE" to 88.5,
                    "MEMO" to "二回目",
                ),
            ),
            actual,
        )
        Mockito.verify(cursor).close()
    }

    @Test
    fun testExecuteSelectAsMapList_withSelectInstance_returnsMapList() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val cursor = Mockito.mock(Cursor::class.java)
        val helper = Mockito.spy(
            TestAndrOrmDatabaseHelper(
                SalaryEntity::class,
            )
        )

        val select = Select(SalaryEntitySelective::class)
        val expectedSql = select.build()

        val columnNames = arrayOf(
            "SAL_EMPLOYEE_ID",
            "SAL_PAY_MONTH",
            "SAL_GROSS",
            "SAL_TOTAL_GROSS",
            "SAL_MAX_GROSS",
            "SAL_AVG_GROSS",
            "SAL_MAX_DEDUCTION",
            "SAL_AVG_DEDUCTION",
        )

        Mockito.doReturn(db)
            .`when`(helper)
            .readableDatabase

        Mockito.`when`(
            db.rawQuery(expectedSql, null),
        ).thenReturn(cursor)

        Mockito.`when`(cursor.columnNames).thenReturn(columnNames)
        Mockito.`when`(cursor.moveToNext()).thenReturn(true, false)

        Mockito.`when`(cursor.getType(0)).thenReturn(Cursor.FIELD_TYPE_STRING)
        Mockito.`when`(cursor.getType(1)).thenReturn(Cursor.FIELD_TYPE_STRING)
        Mockito.`when`(cursor.getType(2)).thenReturn(Cursor.FIELD_TYPE_INTEGER)
        Mockito.`when`(cursor.getType(3)).thenReturn(Cursor.FIELD_TYPE_INTEGER)
        Mockito.`when`(cursor.getType(4)).thenReturn(Cursor.FIELD_TYPE_INTEGER)
        Mockito.`when`(cursor.getType(5)).thenReturn(Cursor.FIELD_TYPE_FLOAT)
        Mockito.`when`(cursor.getType(6)).thenReturn(Cursor.FIELD_TYPE_INTEGER)
        Mockito.`when`(cursor.getType(7)).thenReturn(Cursor.FIELD_TYPE_FLOAT)

        Mockito.`when`(cursor.getString(0)).thenReturn("EMP001")
        Mockito.`when`(cursor.getString(1)).thenReturn("2026-06")
        Mockito.`when`(cursor.getLong(2)).thenReturn(300_000L)
        Mockito.`when`(cursor.getLong(3)).thenReturn(300_000L)
        Mockito.`when`(cursor.getLong(4)).thenReturn(300_000L)
        Mockito.`when`(cursor.getDouble(5)).thenReturn(300_000.0)
        Mockito.`when`(cursor.getLong(6)).thenReturn(50_000L)
        Mockito.`when`(cursor.getDouble(7)).thenReturn(50_000.0)

        val actual = helper.executeSelectAsMapList(select)
        assertEquals(
            listOf(
                linkedMapOf(
                    "SAL_EMPLOYEE_ID" to "EMP001",
                    "SAL_PAY_MONTH" to "2026-06",
                    "SAL_GROSS" to 300_000L,
                    "SAL_TOTAL_GROSS" to 300_000L,
                    "SAL_MAX_GROSS" to 300_000L,
                    "SAL_AVG_GROSS" to 300_000.0,
                    "SAL_MAX_DEDUCTION" to 50_000L,
                    "SAL_AVG_DEDUCTION" to 50_000.0,
                )
            ),
            actual,
        )

        Mockito.verify(db).rawQuery(expectedSql, null)
        Mockito.verify(cursor).close()
    }

    @Test
    fun testExecuteSelectAsEntityList_withSelectInstance_returnsEntityListMap() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val cursor = Mockito.mock(Cursor::class.java)
        val helper = Mockito.spy(
            TestAndrOrmDatabaseHelper(
                SalaryEntity::class,
            )
        )

        val select = Select(SalaryEntitySelective::class)
        val expectedSql = select.build()

        Mockito.doReturn(db)
            .`when`(helper)
            .readableDatabase

        Mockito.`when`(
            db.rawQuery(expectedSql, null),
        ).thenReturn(cursor)

        Mockito.`when`(cursor.columnNames).thenReturn(
            arrayOf(
                "SAL_EMPLOYEE_ID",
                "SAL_PAY_MONTH",
                "SAL_GROSS",
                "SAL_TOTAL_GROSS",
                "SAL_MAX_GROSS",
                "SAL_AVG_GROSS",
                "SAL_MAX_DEDUCTION",
                "SAL_AVG_DEDUCTION",
            )
        )

        Mockito.`when`(cursor.moveToNext()).thenReturn(true, true, false)

        Mockito.`when`(cursor.getType(0)).thenReturn(Cursor.FIELD_TYPE_STRING)
        Mockito.`when`(cursor.getType(1)).thenReturn(Cursor.FIELD_TYPE_STRING)
        Mockito.`when`(cursor.getType(2)).thenReturn(Cursor.FIELD_TYPE_INTEGER)
        Mockito.`when`(cursor.getType(3)).thenReturn(Cursor.FIELD_TYPE_INTEGER)
        Mockito.`when`(cursor.getType(4)).thenReturn(Cursor.FIELD_TYPE_INTEGER)
        Mockito.`when`(cursor.getType(5)).thenReturn(Cursor.FIELD_TYPE_FLOAT)
        Mockito.`when`(cursor.getType(6)).thenReturn(Cursor.FIELD_TYPE_INTEGER)
        Mockito.`when`(cursor.getType(7)).thenReturn(Cursor.FIELD_TYPE_FLOAT)

        Mockito.`when`(cursor.getString(0)).thenReturn("EMP001", "EMP002")
        Mockito.`when`(cursor.getString(1)).thenReturn("2026-06", "2026-07")
        Mockito.`when`(cursor.getLong(2)).thenReturn(300_000L, 320_000L)
        Mockito.`when`(cursor.getLong(3)).thenReturn(300_000L, 620_000L)
        Mockito.`when`(cursor.getLong(4)).thenReturn(300_000L, 320_000L)
        Mockito.`when`(cursor.getDouble(5)).thenReturn(300_000.0, 310_000.0)
        Mockito.`when`(cursor.getLong(6)).thenReturn(50_000L, 55_000L)
        Mockito.`when`(cursor.getDouble(7)).thenReturn(50_000.0, 52_500.0)

        val actual: List<Map<String, SelectEntity?>> =
            helper.executeSelectAsEntityList(query = select)

        assertEquals(
            listOf(
                mapOf(
                    "SAL" to SalaryEntitySelective(
                        employeeId = "EMP001",
                        payMonth = "2026-06",
                        gross = 300_000,
                        totalGross = 300_000L,
                        maxGross = 300_000,
                        avgGross = 300_000.0,
                        maxDeduction = 50_000,
                        avgDeduction = 50_000.0,
                    )
                ),
                mapOf(
                    "SAL" to SalaryEntitySelective(
                        employeeId = "EMP002",
                        payMonth = "2026-07",
                        gross = 320_000,
                        totalGross = 620_000L,
                        maxGross = 320_000,
                        avgGross = 310_000.0,
                        maxDeduction = 55_000,
                        avgDeduction = 52_500.0,
                    )
                ),
            ),
            actual,
        )

        Mockito.verify(db).rawQuery(expectedSql, null)
        Mockito.verify(cursor).close()
    }

    @Test
    fun testCreateSelectEntity_withoutColumnTarget_throwsColumnTargetNotFound() {
        val helper = TestAndrOrmDatabaseHelper(
            SalaryEntity::class,
        )
        val row = linkedMapOf<String, Any?>(
            "SAL_EMPLOYEE_ID" to "EMP001",
        )

        val actual = assertThrows<InvocationTargetException> {
            helper.invokeCreateSelectEntity(
                row = row,
                columnTargets = emptyList(),
            )
        }

        assertEquals(
            "Column target not found. property=employeeId",
            actual.cause?.message,
        )
    }

    @Test
    fun testCreateSelectEntity_withoutColumnValue_throwsColumnValueNotFound() {
        val helper = TestAndrOrmDatabaseHelper(
            SalaryEntity::class,
        )
        val row = emptyMap<String, Any?>()
        val columnTargets = listOf(
            SelectColumnTarget(
                propertyName = "employeeId",
                resultColumnName = "SAL_EMPLOYEE_ID",
            )
        )

        val actual = assertThrows<InvocationTargetException> {
            helper.invokeCreateSelectEntity(
                row = row,
                columnTargets = columnTargets,
            )
        }

        assertEquals(
            "Column value not found. column=SAL_EMPLOYEE_ID",
            actual.cause?.message,
        )
    }

    @Test
    fun testExecuteSelectAsMapList_withBlob_returnByteArrayValue() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val cursor = Mockito.mock(Cursor::class.java)
        val helper = Mockito.spy(
            TestAndrOrmDatabaseHelper(
                TestLargeEntity::class,
            )
        )
        val sql = "select ID, MEMO from TEST_LARGE_ENTITY"
        val binary = byteArrayOf(1, 2, 3)

        Mockito.doReturn(db)
            .`when`(helper)
            .readableDatabase
        Mockito.`when`(
            db.rawQuery(sql, null),
        ).thenReturn(cursor)
        Mockito.`when`(cursor.columnNames).thenReturn(arrayOf("ID", "MEMO"))
        Mockito.`when`(cursor.moveToNext()).thenReturn(true, false)
        Mockito.`when`(cursor.getType(0)).thenReturn(Cursor.FIELD_TYPE_INTEGER)
        Mockito.`when`(cursor.getType(1)).thenReturn(Cursor.FIELD_TYPE_BLOB)
        Mockito.`when`(cursor.getLong(0)).thenReturn(1L)
        Mockito.`when`(cursor.getBlob(1)).thenReturn(binary)

        val actual = helper.executeSelectAsMapList(sql)

        assertEquals(1L, actual[0]["ID"])
        assertArrayEquals(binary, actual[0]["MEMO"] as ByteArray)
        Mockito.verify(cursor).close()
    }

    @Test
    fun testExecuteSelectAsEntityList_withLeftJoin_returnsOneToOneOneToManyAndNoJoinedEntity() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val cursor = Mockito.mock(Cursor::class.java)
        val helper = Mockito.spy(
            TestAndrOrmDatabaseHelper(
                EmployeeTableEntity::class,
            )
        )
        val select = Select(EmployeeEntityIdSelection::class)
            .join(LEFT, EmployeeEntity::class) {
                EmployeeEntityIdSelection::employeeId eq EmployeeEntity::employeeId
            }
        val expectedSql = select.build()

        Mockito.doReturn(db)
            .`when`(helper)
            .readableDatabase

        Mockito.`when`(
            db.rawQuery(expectedSql, null),
        ).thenReturn(cursor)

        Mockito.`when`(cursor.columnNames).thenReturn(
            arrayOf(
                "EMP_ID_EMPLOYEE_ID",
                "EMP_EMPLOYEE_ID",
                "EMP_NAME",
                "EMP_ADDRESS",
                "EMP_GENDER",
                "EMP_POSITION",
            )
        )

        Mockito.`when`(cursor.moveToNext()).thenReturn(true, true, true, true, false)

        Mockito.`when`(cursor.getType(0)).thenReturn(
            Cursor.FIELD_TYPE_STRING,
            Cursor.FIELD_TYPE_STRING,
            Cursor.FIELD_TYPE_STRING,
            Cursor.FIELD_TYPE_STRING,
        )
        Mockito.`when`(cursor.getType(1)).thenReturn(
            Cursor.FIELD_TYPE_STRING,
            Cursor.FIELD_TYPE_STRING,
            Cursor.FIELD_TYPE_STRING,
            Cursor.FIELD_TYPE_NULL,
        )
        Mockito.`when`(cursor.getType(2)).thenReturn(
            Cursor.FIELD_TYPE_STRING,
            Cursor.FIELD_TYPE_STRING,
            Cursor.FIELD_TYPE_STRING,
            Cursor.FIELD_TYPE_NULL,
        )
        Mockito.`when`(cursor.getType(3)).thenReturn(
            Cursor.FIELD_TYPE_STRING,
            Cursor.FIELD_TYPE_STRING,
            Cursor.FIELD_TYPE_STRING,
            Cursor.FIELD_TYPE_NULL,
        )
        Mockito.`when`(cursor.getType(4)).thenReturn(
            Cursor.FIELD_TYPE_STRING,
            Cursor.FIELD_TYPE_STRING,
            Cursor.FIELD_TYPE_STRING,
            Cursor.FIELD_TYPE_NULL,
        )
        Mockito.`when`(cursor.getType(5)).thenReturn(
            Cursor.FIELD_TYPE_STRING,
            Cursor.FIELD_TYPE_STRING,
            Cursor.FIELD_TYPE_STRING,
            Cursor.FIELD_TYPE_NULL,
        )

        Mockito.`when`(cursor.getString(0)).thenReturn(
            "EMP001",
            "EMP002",
            "EMP002",
            "EMP003",
        )
        Mockito.`when`(cursor.getString(1)).thenReturn(
            "EMP001",
            "EMP002",
            "EMP002",
        )
        Mockito.`when`(cursor.getString(2)).thenReturn(
            "川流一郎",
            "川流二郎A",
            "川流二郎B",
        )
        Mockito.`when`(cursor.getString(3)).thenReturn(
            "東京都千代田区",
            "東京都中央区A",
            "東京都中央区B",
        )
        Mockito.`when`(cursor.getString(4)).thenReturn(
            "M",
            "M",
            "M",
        )
        Mockito.`when`(cursor.getString(5)).thenReturn(
            "SE",
            "PG-A",
            "PG-B",
        )

        val actual: List<Map<String, SelectEntity?>> =
            helper.executeSelectAsEntityList(query = select)
        println(actual)
        assertEquals(
            listOf(
                mapOf(
                    "EMP_ID" to EmployeeEntityIdSelection(
                        employeeId = "EMP001",
                    ),
                    "EMP" to EmployeeEntity(
                        employeeId = "EMP001",
                        employeeSubId = null,
                        name = "川流一郎",
                        address = "東京都千代田区",
                        gender = "M",
                        position = "SE",
                    ),
                ),
                mapOf(
                    "EMP_ID" to EmployeeEntityIdSelection(
                        employeeId = "EMP002",
                    ),
                    "EMP" to EmployeeEntity(
                        employeeId = "EMP002",
                        employeeSubId = null,
                        name = "川流二郎A",
                        address = "東京都中央区A",
                        gender = "M",
                        position = "PG-A",
                    ),
                ),
                mapOf(
                    "EMP_ID" to EmployeeEntityIdSelection(
                        employeeId = "EMP002",
                    ),
                    "EMP" to EmployeeEntity(
                        employeeId = "EMP002",
                        employeeSubId = null,
                        name = "川流二郎B",
                        address = "東京都中央区B",
                        gender = "M",
                        position = "PG-B",
                    ),
                ),
                mapOf(
                    "EMP_ID" to EmployeeEntityIdSelection(
                        employeeId = "EMP003",
                    ),
                    "EMP" to null,
                ),
            ),
            actual,
        )

        Mockito.verify(db).rawQuery(expectedSql, null)
        Mockito.verify(cursor).close()
    }

    private class TestQueryBuilderLike(
        private val query: String,
    ) : QueryBuilderLike<TableDefinitionEntity> {

        override fun build(): String = query
    }

    private class TestQueryBuilderLikeWithBindValues(
        private val query: String,
        bindValues: List<Any?>,
    ) : QueryWithBindValues(),
        QueryBuilderLike<TableDefinitionEntity> {

        init {
            addBindValues(bindValues)
        }

        override fun build(): String = query
    }

    private fun TestAndrOrmDatabaseHelper.invokeCreateSelectEntity(
        row: Map<String, Any?>,
        columnTargets: List<SelectColumnTarget>,
    ): SelectEntity {
        val method = AndrOrmDatabaseHelper::class.java.getDeclaredMethod(
            "createSelectEntity",
            KClass::class.java,
            Map::class.java,
            List::class.java,
        )
        method.isAccessible = true
        return method.invoke(
            this,
            SalaryEntitySelective::class,
            row,
            columnTargets,
        ) as SelectEntity
    }

    private fun captureExecutedSql(
        db: SQLiteDatabase,
        count: Int,
    ): List<String> {
        val captor = ArgumentCaptor.forClass(String::class.java)

        Mockito.verify(
            db,
            Mockito.times(count),
        ).execSQL(captor.capture())

        return captor.allValues
    }

    private open class TestAndrOrmDatabaseHelper(
        vararg entities: KClass<out TableDefinitionEntity>,
        private val customColumnMappings:
        Map<KClass<out TableDefinitionEntity>, List<Pair<String, String>>> = emptyMap(),
    ) : AndrOrmDatabaseHelper(
        Mockito.mock(Context::class.java),
        "androrm_test.db",
        1,
        *entities,
    ) {

        fun callMigrateDatabase(
            db: SQLiteDatabase,
            newVersion: Int = 1,
        ) {
            migrateDatabase(db, newVersion)
        }

        override fun resolveColumnMappings():
                Map<KClass<out TableDefinitionEntity>, List<Pair<String, String>>> =
            customColumnMappings
    }

    @Table(name = "TEST_UNKNOWN_ENTITY", alias = "TUE")
    private data class TestUnknownEntity(
        @Column(name = "ID")
        val id: Long,
    ) : TableDefinitionEntity
}
