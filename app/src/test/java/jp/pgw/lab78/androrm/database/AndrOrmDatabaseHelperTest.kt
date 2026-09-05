package jp.pgw.lab78.androrm.database

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteStatement
import android.os.SystemClock
import jp.pgw.lab78.androrm.common.MessageConstants.AE00020
import jp.pgw.lab78.androrm.common.MessageConstants.AE00021
import jp.pgw.lab78.androrm.common.MessageConstants.AE00022
import jp.pgw.lab78.androrm.common.MessageConstants.AE00023
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Index
import jp.pgw.lab78.androrm.common.database.annotation.MigrationDefault
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

/**
 * AndrOrmDatabaseHelperのDDL、移行、DML、SELECT実行を検証する。
 *
 * @author Masahiro Inoue
 * @since 2026-05-31
 */
class AndrOrmDatabaseHelperTest {

    private fun <T> withMockedElapsedRealtimeNanos(
        start: Long = 1_000L,
        end: Long = 1_500L,
        block: () -> T,
    ): T =
        Mockito.mockStatic(SystemClock::class.java).use { systemClock ->
            systemClock.`when`<Long> {
                SystemClock.elapsedRealtimeNanos()
            }.thenReturn(start, end)

            block()
        }

    /**
     * Entity間でインデックス名が重複した場合にDDL実行前に例外となることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
    @Test
    fun testOnCreate_duplicateIndexNamesAcrossEntities_throwsBeforeExecutingDdl() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val helper = TestAndrOrmDatabaseHelper(
            TestDuplicateIndexFirstEntity::class,
            TestDuplicateIndexSecondEntity::class,
        )

        val actual = assertThrows<IllegalArgumentException> {
            helper.onCreate(db)
        }

        assertEquals(AE00020.format("shared_index_0"), actual.message)
        Mockito.verifyNoInteractions(db)
    }

    /**
     * 同じEntityを重複登録した場合は事前検査せず、SQLiteのCREATE TABLE例外を通知することを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Test
    fun testOnCreate_duplicateSameEntity_delegatesDuplicateTableErrorToSQLite() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val createTableQuery = Create(TestLargeEntity::class).build()
        var createTableCount = 0
        Mockito.doAnswer { invocation ->
            if (invocation.getArgument<String>(0) == createTableQuery) {
                createTableCount++
                if (createTableCount == 2) {
                    throw SQLiteException("table TEST_LARGE_ENTITY already exists")
                }
            }
            null
        }.`when`(db).execSQL(Mockito.anyString())
        val helper = TestAndrOrmDatabaseHelper(
            TestLargeEntity::class,
            TestLargeEntity::class,
        )

        assertThrows<SQLiteException> {
            helper.onCreate(db)
        }

        assertEquals(
            listOf(
                createTableQuery,
                """create index "IDX_TEST_LARGE_ENTITY_ACTIVE_CREATED_0" """ +
                        """on "TEST_LARGE_ENTITY" ("ACTIVE", "CREATED_AT")""",
                """create unique index "UQ_TEST_CODE_0" """ +
                        """on "TEST_LARGE_ENTITY" ("CODE")""",
                """create unique index "UQ_TEST_PERSONAL_INFO_0" """ +
                        """on "TEST_LARGE_ENTITY" ("NAME", "FURIGANA", "GENDER", "BIRTHDAY", "PLACE_OF_BIRTH")""",
                createTableQuery,
            ),
            captureExecutedSql(db, 5),
        )
    }

    /**
     * DB作成時に定義Entityのテーブルとインデックス作成文が実行されることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
    @Test
    fun testOnCreate_withDefineEntity_executeCreateTableAndIndexQueries() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val helper = TestAndrOrmDatabaseHelper(
            TestLargeEntity::class,
        )

        helper.onCreate(db)

        assertEquals(
            listOf(
                """create table "TEST_LARGE_ENTITY" """ +
                        """("ID" INTEGER not null, "CODE" TEXT not null, "NAME" TEXT not null, "FURIGANA" TEXT not null, "GENDER" TEXT not null, """ +
                        """"BIRTHDAY" DATETIME not null, "PLACE_OF_BIRTH" TEXT not null, "EMAIL" TEXT not null, "PHONE_NUMBER" TEXT not null, """ +
                        """"POSTAL_CODE" TEXT not null, "PREFECTURE" TEXT not null, "CITY" TEXT not null, "ADDRESS_LINE" TEXT not null, """ +
                        """"SCORE" REAL not null, "BALANCE" INTEGER not null, "ACTIVE" INTEGER not null, "REGISTERED_AT" DATETIME not null, """ +
                        """"LAST_LOGIN_AT" DATETIME not null, "MEMO" TEXT not null, "CREATED_AT" DATETIME not null, "UPDATED_AT" DATETIME not null, """ +
                        """primary key ("ID"))""",
                """create index "IDX_TEST_LARGE_ENTITY_ACTIVE_CREATED_0" """ +
                        """on "TEST_LARGE_ENTITY" ("ACTIVE", "CREATED_AT")""",
                """create unique index "UQ_TEST_CODE_0" """ +
                        """on "TEST_LARGE_ENTITY" ("CODE")""",
                """create unique index "UQ_TEST_PERSONAL_INFO_0" """ +
                        """on "TEST_LARGE_ENTITY" ("NAME", "FURIGANA", "GENDER", "BIRTHDAY", "PLACE_OF_BIRTH")""",
            ),
            captureExecutedSql(db, 4),
        )
    }

    /**
     * DB更新時に標準移行クエリが順番どおり実行されることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
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
                "drop table if exists TEST_ALL_ENTITY_new",
                """create table "TEST_ALL_ENTITY_new" """ +
                        """("ID" INTEGER not null, "NAME" TEXT not null, "ADDRESS" TEXT not null, "BIRTHDAY" DATETIME not null, """ +
                        """"SUB_ID" INTEGER, "UPDATE_DATE" DATETIME not null, "INSERT_DATE_TIME" DATETIME not null)""",
                "insert into TEST_ALL_ENTITY_new " +
                        "(ID,NAME,ADDRESS,BIRTHDAY,SUB_ID,UPDATE_DATE,INSERT_DATE_TIME) " +
                        "select ID,NAME,ADDRESS,BIRTHDAY,SUB_ID,UPDATE_DATE,INSERT_DATE_TIME " +
                        "from TEST_ALL_ENTITY",
                "drop table if exists TEST_ALL_ENTITY",
                "alter table TEST_ALL_ENTITY_new rename to TEST_ALL_ENTITY",
            ),
            captureExecutedSql(db, 5),
        )
    }

    /**
     * 移行時にMigrationDefaultを使用し、未指定のnullableカラムを転送対象から除外することを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
    @Test
    fun testOnUpgrade_withMigrationDefaultAndNullableColumns_usesAnnotationAndOmitsNullableColumn() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val cursor = Mockito.mock(Cursor::class.java)
        Mockito.doReturn(cursor).`when`(db).rawQuery(
            Mockito.eq("select * from TEST_UPGRADE_DEFAULT_ENTITY limit 0"),
            Mockito.any(),
        )
        Mockito.doReturn(arrayOf("ID", "NAME")).`when`(cursor).columnNames
        val helper = TestAndrOrmDatabaseHelper(
            TestUpgradeDefaultEntity::class,
        )

        helper.onUpgrade(
            db = db,
            oldVersion = 1,
            newVersion = 2,
        )

        assertEquals(
            listOf(
                "drop table if exists TEST_UPGRADE_DEFAULT_ENTITY_new",
                """create table "TEST_UPGRADE_DEFAULT_ENTITY_new" """ +
                        """("ID" INTEGER not null, "NAME" TEXT not null, "SCORE" INTEGER not null, "LABEL" TEXT not null, "NOTE" TEXT)""",
                "insert into TEST_UPGRADE_DEFAULT_ENTITY_new (ID,NAME,SCORE,LABEL) " +
                        "select ID,NAME,1,'database' from TEST_UPGRADE_DEFAULT_ENTITY",
                "drop table if exists TEST_UPGRADE_DEFAULT_ENTITY",
                "alter table TEST_UPGRADE_DEFAULT_ENTITY_new rename to TEST_UPGRADE_DEFAULT_ENTITY",
            ),
            captureExecutedSql(db, 5),
        )
    }

    /**
     * Kotlin既定値だけを持つnon-nullカラムの移行を拒否することを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
    @Test
    fun testOnUpgrade_withKotlinDefaultButWithoutMigrationDefault_throwsError() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val cursor = Mockito.mock(Cursor::class.java)
        Mockito.doReturn(cursor).`when`(db).rawQuery(
            Mockito.eq("select * from TEST_UPGRADE_MISSING_DEFAULT_ENTITY limit 0"),
            Mockito.any(),
        )
        Mockito.doReturn(arrayOf("ID")).`when`(cursor).columnNames
        val helper = TestAndrOrmDatabaseHelper(TestUpgradeMissingDefaultEntity::class)

        val actual = assertThrows<IllegalStateException> {
            helper.onUpgrade(db = db, oldVersion = 1, newVersion = 2)
        }

        assertTrue(actual.message.orEmpty().contains("TestUpgradeMissingDefaultEntity"))
        assertTrue(actual.message.orEmpty().contains("score"))
        assertTrue(
            actual.message.orEmpty().contains("非nullableカラム追加時に @MigrationDefault が必要")
        )
    }

    /**
     * 不正なMigrationDefaultを持つカラムの移行を拒否することを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
    @Test
    fun testOnUpgrade_withInvalidMigrationDefault_throwsError() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val cursor = Mockito.mock(Cursor::class.java)
        Mockito.doReturn(cursor).`when`(db).rawQuery(
            Mockito.eq("select * from TEST_UPGRADE_INVALID_DEFAULT_ENTITY limit 0"),
            Mockito.any(),
        )
        Mockito.doReturn(arrayOf("ID")).`when`(cursor).columnNames
        val helper = TestAndrOrmDatabaseHelper(TestUpgradeInvalidDefaultEntity::class)

        val actual = assertThrows<IllegalArgumentException> {
            helper.onUpgrade(db = db, oldVersion = 1, newVersion = 2)
        }

        assertTrue(actual.message.orEmpty().contains("TestUpgradeInvalidDefaultEntity"))
        assertTrue(actual.message.orEmpty().contains("enabled"))
        assertTrue(actual.message.orEmpty().contains("@MigrationDefault('true')"))
    }

    /**
     * 未登録Entityのカラム対応定義を指定した場合に例外となることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
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

    /**
     * 未知の旧カラムを移行対応へ指定した場合に例外となることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
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

    /**
     * バインド値がないDMLをコンパイルして実行することを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
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

        val actual = withMockedElapsedRealtimeNanos {
            helper.executeDml(query)
        }

        assertEquals(1, actual)
        Mockito.verify(db).compileStatement(sql)
        Mockito.verify(statement).executeUpdateDelete()
    }

    /**
     * DMLの各値を順番どおりバインドして実行することを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
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
            queryString = sql,
            bindValues = listOf(
                "川流",
                birthday,
                98.25,
                true,
                registeredAt,
                100L,
            ),
        )

        val actual = withMockedElapsedRealtimeNanos {
            helper.executeDml(query)
        }

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

    /**
     * DMLのnull値がbindNullでバインドされることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
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

        val actual = withMockedElapsedRealtimeNanos {
            helper.executeDml(sql, listOf(null, 100L))
        }

        assertEquals(1, actual)
        assertEquals(500L, helper.queryExecutionTime)
        Mockito.verify(statement).bindNull(1)
        Mockito.verify(statement).bindLong(2, 100L)
        Mockito.verify(statement).executeUpdateDelete()
    }

    /**
     * DMLのByteArray値がBLOBとしてバインドされることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
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

        val actual = withMockedElapsedRealtimeNanos {
            helper.executeDml(sql, listOf(binary, 100L))
        }

        assertEquals(1, actual)
        assertEquals(500L, helper.queryExecutionTime)
        Mockito.verify(statement).bindBlob(1, binary)
        Mockito.verify(statement).bindLong(2, 100L)
        Mockito.verify(statement).executeUpdateDelete()
    }

    /**
     * バインド値がないSELECTをrawQueryで実行することを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
    @Test
    fun testExecuteSelectAsCursor_withoutBindValues_usesTypedCursorFactory() {
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
        stubSelectCursor(db, sql, cursor)

        val actual = helper.executeSelectAsCursor(sql)

        assertSame(cursor, actual)
        verifySelectCursor(db, sql)
    }

    /**
     * SELECTのバインド値が型付きCursorFactory経路へ渡されることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
    @Test
    fun testExecuteSelectAsCursor_withBindValues_usesTypedCursorFactory() {
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
        stubSelectCursor(db, sql, cursor)

        val actual = helper.executeSelectAsCursor(
            sql,
            listOf(100L, true, registeredAt),
        )

        assertSame(cursor, actual)
        verifySelectCursor(db, sql)
    }

    /**
     * Cursor取得時にnullのバインド値を拒否することを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
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
        Mockito.verify(db, Mockito.never()).rawQueryWithFactory(
            Mockito.any(SQLiteDatabase.CursorFactory::class.java),
            Mockito.anyString(),
            Mockito.any<Array<String>>(),
            Mockito.isNull(),
        )
    }

    /**
     * Cursor取得時にByteArrayのバインド値を型付きCursorFactory経路へ渡すことを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
    @Test
    fun testExecuteSelectAsCursor_withByteArrayBindValue_usesTypedCursorFactory() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val cursor = Mockito.mock(Cursor::class.java)
        val helper = Mockito.spy(
            TestAndrOrmDatabaseHelper(
                TestLargeEntity::class,
            )
        )
        val sql = "select ID from TEST_LARGE_ENTITY where MEMO = ?"

        Mockito.doReturn(db)
            .`when`(helper)
            .readableDatabase
        stubSelectCursor(db, sql, cursor)

        val actual = helper.executeSelectAsCursor(sql, listOf(byteArrayOf(1, 2, 3)))

        assertSame(cursor, actual)
        verifySelectCursor(db, sql)
    }

    /**
     * Cursorの複数行がMapの一覧へ変換されることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
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
        stubSelectCursor(db, sql, cursor)
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

        val actual = withMockedElapsedRealtimeNanos {
            helper.executeSelectAsMapList(sql)
        }

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

    /**
     * Selectインスタンスの実行結果がMapの一覧として返ることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
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

        stubSelectCursor(db, expectedSql, cursor)

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

        val actual = withMockedElapsedRealtimeNanos {
            helper.executeSelectAsMapList(select)
        }

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

        verifySelectCursor(db, expectedSql)
        Mockito.verify(cursor).close()
    }

    /**
     * Selectインスタンスの実行結果がEntity一覧のMapへ変換されることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
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

        stubSelectCursor(db, expectedSql, cursor)

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

        val actual = withMockedElapsedRealtimeNanos {
            helper.executeSelectAsEntityList(query = select)
        }

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

        verifySelectCursor(db, expectedSql)
        Mockito.verify(cursor).close()
    }

    /**
     * SELECT結果に対応するカラム定義がない場合に例外となることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
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

    /**
     * SELECT結果に必要なカラム値がない場合に例外となることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
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

    /**
     * BLOBカラムがByteArrayとしてMapへ格納されることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
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
        stubSelectCursor(db, sql, cursor)
        Mockito.`when`(cursor.columnNames).thenReturn(arrayOf("ID", "MEMO"))
        Mockito.`when`(cursor.moveToNext()).thenReturn(true, false)
        Mockito.`when`(cursor.getType(0)).thenReturn(Cursor.FIELD_TYPE_INTEGER)
        Mockito.`when`(cursor.getType(1)).thenReturn(Cursor.FIELD_TYPE_BLOB)
        Mockito.`when`(cursor.getLong(0)).thenReturn(1L)
        Mockito.`when`(cursor.getBlob(1)).thenReturn(binary)

        val actual = withMockedElapsedRealtimeNanos {
            helper.executeSelectAsMapList(sql)
        }
        assertEquals(1L, actual[0]["ID"])
        assertArrayEquals(binary, actual[0]["MEMO"] as ByteArray)
        Mockito.verify(cursor).close()
    }

    /**
     * LEFT JOIN結果が一対一・一対多・結合先なしのEntity構造へ変換されることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
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
            .join(LEFT, EmployeeEntity::class)
            .on { EmployeeEntityIdSelection::employeeId eq EmployeeEntity::employeeId }
        val expectedSql = select.build()

        Mockito.doReturn(db)
            .`when`(helper)
            .readableDatabase

        stubSelectCursor(db, expectedSql, cursor)

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

        val actual = withMockedElapsedRealtimeNanos {
            helper.executeSelectAsEntityList(query = select)
        }

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

        verifySelectCursor(db, expectedSql)
        Mockito.verify(cursor).close()
    }

    /**
     * バインド値を持たない検証用QueryBuilderLike実装。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
    private class TestQueryBuilderLike(
        private val query: String,
    ) : QueryBuilderLike<TableDefinitionEntity> {

        /**
         * コンストラクタで指定されたクエリを返す。
         *
         * @return 検証用クエリ
         * @author Masahiro Inoue
         * @since 2026-05-31
         */
        override fun build(): String = query
    }

    /**
     * バインド値を保持する検証用QueryBuilderLike実装。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
    private class TestQueryBuilderLikeWithBindValues(
        private val queryString: String,
        bindValues: List<Any?>,
    ) : QueryWithBindValues(),
        QueryBuilderLike<TableDefinitionEntity> {

        init {
            addBindValues(bindValues)
        }

        /**
         * コンストラクタで指定されたクエリを返す。
         *
         * @return 検証用クエリ
         * @author Masahiro Inoue
         * @since 2026-05-31
         */
        override fun build(): String = queryString
    }

    /**
     * ## SELECT Cursorスタブ設定
     * ### 型付きCursorFactoryを使用するSELECT実行結果を設定する
     * @param db スタブ対象データベース
     * @param sql 実行対象SELECT文
     * @param cursor 返却するCursor
     * @author Masahiro Inoue
     * @since 2026-09-03
     */
    private fun stubSelectCursor(
        db: SQLiteDatabase,
        sql: String,
        cursor: Cursor,
    ) {
        Mockito.`when`(
            db.rawQueryWithFactory(
                Mockito.any(SQLiteDatabase.CursorFactory::class.java),
                Mockito.eq(sql),
                Mockito.any<Array<String>>(),
                Mockito.isNull(),
            ),
        ).thenReturn(cursor)
    }

    /**
     * ## SELECT Cursor実行検証
     * ### 指定SELECT文が型付きCursorFactory経路へ渡されたことを検証する
     * @param db 検証対象データベース
     * @param sql 期待するSELECT文
     * @author Masahiro Inoue
     * @since 2026-09-03
     */
    private fun verifySelectCursor(
        db: SQLiteDatabase,
        sql: String,
    ) {
        Mockito.verify(db).rawQueryWithFactory(
            Mockito.any(SQLiteDatabase.CursorFactory::class.java),
            Mockito.eq(sql),
            Mockito.any<Array<String>>(),
            Mockito.isNull(),
        )
    }

    /**
     * リフレクションを使用して非公開のSELECT Entity生成処理を呼び出す。
     *
     * @receiver 検証対象のデータベースヘルパー
     * @param row SELECT結果の行データ
     * @param columnTargets 結果カラムとプロパティの対応
     * @return 生成されたSELECT Entity
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
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

    /**
     * 指定回数実行されたexecSQLのSQL文字列を取得する。
     *
     * @param db 検証対象のSQLiteDatabase
     * @param count 期待する実行回数
     * @return 実行されたSQL文字列の一覧
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
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

    /**
     * DB移行処理とカラム対応定義を公開して検証するデータベースヘルパー。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
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

        /**
         * 保護されたDB移行処理をテストから呼び出す。
         *
         * @param db 移行対象のSQLiteDatabase
         * @param newVersion 新しいDBバージョン
         * @author Masahiro Inoue
         * @since 2026-05-31
         */
        fun callMigrateDatabase(
            db: SQLiteDatabase,
            newVersion: Int = 1,
        ) {
            migrateDatabase(db, newVersion)
        }

        /**
         * 検証用に指定されたカラム対応定義を返す。
         *
         * @return Entityごとのカラム対応定義
         * @author Masahiro Inoue
         * @since 2026-05-31
         */
        override fun resolveColumnMappings():
                Map<KClass<out TableDefinitionEntity>, List<Pair<String, String>>> =
            customColumnMappings
    }

    /**
     * 未登録Entityのカラム対応定義を検証するためのEntity。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
    @Table(name = "TEST_UNKNOWN_ENTITY", alias = "TUE")
    private data class TestUnknownEntity(
        @Column(name = "ID")
        val id: Long,
    ) : TableDefinitionEntity

    /**
     * Entity間インデックス名重複検証の一つ目のEntity。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
    @Table(name = "TEST_DUPLICATE_INDEX_FIRST", alias = "TDIF")
    @Index(name = "SHARED_INDEX", properties = ["id"])
    private data class TestDuplicateIndexFirstEntity(
        @Column(name = "ID")
        val id: Long,
    ) : TableDefinitionEntity

    /**
     * Entity間インデックス名重複検証の二つ目のEntity。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
    @Table(name = "TEST_DUPLICATE_INDEX_SECOND", alias = "TDIS")
    @Index(name = "shared_index", properties = ["id"])
    private data class TestDuplicateIndexSecondEntity(
        @Column(name = "ID")
        val id: Long,
    ) : TableDefinitionEntity

    /**
     * MigrationDefaultとnullableカラムを持つ移行検証用Entity。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
    @Table(name = "TEST_UPGRADE_DEFAULT_ENTITY", alias = "TUDE")
    private data class TestUpgradeDefaultEntity(
        @Column(name = "ID")
        val id: Long,
        @Column(name = "NAME")
        val name: String,
        @Column(name = "SCORE")
        @MigrationDefault("1")
        val score: Int = 99,
        @Column(name = "LABEL")
        @MigrationDefault("'database'")
        val label: String = "kotlin",
        @Column(name = "NOTE")
        val note: String? = null,
    ) : TableDefinitionEntity

    /**
     * 必須のMigrationDefaultを持たない移行検証用Entity。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
    @Table(name = "TEST_UPGRADE_MISSING_DEFAULT_ENTITY", alias = "TUMDE")
    private data class TestUpgradeMissingDefaultEntity(
        @Column(name = "ID")
        val id: Long,
        @Column(name = "SCORE")
        val score: Int = 99,
    ) : TableDefinitionEntity

    /**
     * 不正なMigrationDefaultを持つ移行検証用Entity。
     *
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
    @Table(name = "TEST_UPGRADE_INVALID_DEFAULT_ENTITY", alias = "TUIDE")
    private data class TestUpgradeInvalidDefaultEntity(
        @Column(name = "ID")
        val id: Long,
        @Column(name = "ENABLED")
        @MigrationDefault("true")
        val enabled: Boolean,
    ) : TableDefinitionEntity
}
