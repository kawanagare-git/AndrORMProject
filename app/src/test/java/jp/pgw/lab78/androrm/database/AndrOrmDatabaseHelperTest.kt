package jp.pgw.lab78.androrm.database

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import jp.pgw.lab78.androrm.common.MessageConstants.AE00021
import jp.pgw.lab78.androrm.common.MessageConstants.AE00022
import jp.pgw.lab78.androrm.common.database.annotation.*
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.interfaces.QueryBuilderLike
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.time.LocalDate
import kotlin.reflect.KClass

class AndrOrmDatabaseHelperTest {

    @Test
    fun testOnCreate_executeDmlCreateTableAndIndexQueries() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val helper = TestAndrOrmDatabaseHelper(
            TestMigrationEntity::class,
        )

        helper.onCreate(db)

        assertEquals(
            listOf(
                "create table TEST_MIGRATION_ENTITY " +
                        "(ID INTEGER, NEW_NAME TEXT, AGE INTEGER, primary key (ID))",
                "create index if not exists IDX_TEST_MIGRATION_ENTITY_AGE0 " +
                        "on TEST_MIGRATION_ENTITY (AGE)",
            ),
            captureExecutedSql(db, 2),
        )
    }

    @Test
    fun testOnUpgrade_executeDmlStandardMigrationQueries() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val helper = TestAndrOrmDatabaseHelper(
            TestMigrationEntity::class,
        )

        helper.onUpgrade(
            db = db,
            oldVersion = 1,
            newVersion = 2,
        )

        assertEquals(
            listOf(
                "create table TEST_MIGRATION_ENTITY_new " +
                        "(ID INTEGER, NEW_NAME TEXT, AGE INTEGER, primary key (ID))",
                "insert into TEST_MIGRATION_ENTITY_new (ID,NEW_NAME,AGE) " +
                        "select ID,OLD_NAME,AGE from TEST_MIGRATION_ENTITY",
                "create index if not exists IDX_TEST_MIGRATION_ENTITY_AGE2 " +
                        "on TEST_MIGRATION_ENTITY_new (AGE)",
                "drop table if exists TEST_MIGRATION_ENTITY",
                "alter table TEST_MIGRATION_ENTITY_new rename to TEST_MIGRATION_ENTITY",
            ),
            captureExecutedSql(db, 5),
        )
    }

    @Test
    fun testMigrateDatabase_customColumnMappings() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val helper = TestAndrOrmDatabaseHelper(
            TestMigrationEntity::class,
            customColumnMappings = mapOf(
                TestMigrationEntity::class to listOf(
                    "OLD_NAME" to "CUSTOM_NAME",
                ),
            ),
        )

        helper.callMigrateDatabase(db)

        assertEquals(
            listOf(
                "create table TEST_MIGRATION_ENTITY_new " +
                        "(ID INTEGER, NEW_NAME TEXT, AGE INTEGER, primary key (ID))",
                "insert into TEST_MIGRATION_ENTITY_new (ID,CUSTOM_NAME,AGE) " +
                        "select ID,OLD_NAME,AGE from TEST_MIGRATION_ENTITY",
                "create index if not exists IDX_TEST_MIGRATION_ENTITY_AGE1 " +
                        "on TEST_MIGRATION_ENTITY_new (AGE)",
                "drop table if exists TEST_MIGRATION_ENTITY",
                "alter table TEST_MIGRATION_ENTITY_new rename to TEST_MIGRATION_ENTITY",
            ),
            captureExecutedSql(db, 5),
        )
    }

    @Test
    fun testMigrateDatabase_customColumnMappingsUnknownEntity_throwsIllegalStateException() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val helper = TestAndrOrmDatabaseHelper(
            TestMigrationEntity::class,
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
            TestMigrationEntity::class,
            customColumnMappings = mapOf(
                TestMigrationEntity::class to listOf(
                    "UNKNOWN_OLD_COLUMN" to "CUSTOM_NAME",
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
    fun testExecute_withoutBindValues_executeDml() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val statement = Mockito.mock(SQLiteStatement::class.java)
        val helper = Mockito.spy(
            TestAndrOrmDatabaseHelper(
                TestMigrationEntity::class,
            )
        )

        Mockito.doReturn(db)
            .`when`(helper)
            .writableDatabase
        Mockito.`when`(
            db.compileStatement("delete from TEST_MIGRATION_ENTITY"),
        ).thenReturn(statement)
        Mockito.`when`(statement.executeUpdateDelete()).thenReturn(1)

        val query = TestQueryBuilderLike(
            query = "delete from TEST_MIGRATION_ENTITY",
        )

        val actual = helper.executeDml(query)

        assertEquals(1, actual)
        Mockito.verify(db).compileStatement(
            "delete from TEST_MIGRATION_ENTITY",
        )
        Mockito.verify(statement).executeUpdateDelete()
    }

    @Test
    fun testExecute_withBindValues_bindArgsAndExecuteDml() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val statement = Mockito.mock(SQLiteStatement::class.java)
        val helper = Mockito.spy(
            TestAndrOrmDatabaseHelper(
                TestMigrationEntity::class,
            )
        )

        val sql = "update TEST_MIGRATION_ENTITY set AGE = ? where ID = ?"

        Mockito.doReturn(db)
            .`when`(helper)
            .writableDatabase

        Mockito.`when`(
            db.compileStatement(sql),
        ).thenReturn(statement)

        Mockito.`when`(
            statement.executeUpdateDelete(),
        ).thenReturn(1)

        val query = TestQueryBuilderLikeWithBindValues(
            query = sql,
            bindValues = listOf(20, "00010"),
        )

        val actual = helper.executeDml(query)

        assertEquals(1, actual)

        Mockito.verify(db).compileStatement(sql)
        Mockito.verify(statement).bindLong(1, 20L)
        Mockito.verify(statement).bindString(2, "00010")
        Mockito.verify(statement).executeUpdateDelete()
    }

    @Test
    fun testExecute_withBindValues_executeDmlInsert() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val statement = Mockito.mock(SQLiteStatement::class.java)
        val helper = Mockito.spy(
            TestAndrOrmDatabaseHelper(
                TestMigrationEntity::class,
            )
        )

        val sql =
            "insert into TEST_INSERT_ENTITY (NAME, ADDRESS, BIRTHDAY, AGE, INSERT_DATE_TIME)" +
                    " values(?, ?, ?, ?, ?)"
        val now = LocalDate.now()
        val barth = LocalDate.of(1968, 1, 7)
        Mockito.doReturn(db)
            .`when`(helper)
            .writableDatabase

        Mockito.`when`(
            db.compileStatement(sql),
        ).thenReturn(statement)

        Mockito.`when`(
            statement.executeUpdateDelete(),
        ).thenReturn(1)

        val query = TestQueryBuilderLikeWithBindValues(
            query = sql,
            bindValues = listOf("川流", "愛知県豊田市", barth, 58, now),
        )

        val actual = helper.executeDml(query)

        assertEquals(1, actual)

        Mockito.verify(db).compileStatement(sql)
        Mockito.verify(statement).bindString(1, "川流")
        Mockito.verify(statement).bindString(2, "愛知県豊田市")
        Mockito.verify(statement).bindString(3, barth.toString())
        Mockito.verify(statement).bindLong(4, 58)
        Mockito.verify(statement).bindString(5, now.toString())
        Mockito.verify(statement).executeUpdateDelete()
    }

    @Test
    fun testExecuteSelectAsCursor_withoutBindValues_rawQuery() {
        val db = Mockito.mock(SQLiteDatabase::class.java)
        val cursor = Mockito.mock(Cursor::class.java)
        val helper = Mockito.spy(
            TestAndrOrmDatabaseHelper(
                TestMigrationEntity::class,
            )
        )
        val sql = "select ID, NEW_NAME, AGE from TEST_MIGRATION_ENTITY"

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
                TestMigrationEntity::class,
            )
        )
        val sql =
            "select ID, NEW_NAME, AGE from TEST_MIGRATION_ENTITY where AGE = ? and NEW_NAME = ?"

        Mockito.doReturn(db)
            .`when`(helper)
            .readableDatabase

        Mockito.`when`(
            db.rawQuery(Mockito.eq(sql), Mockito.any<Array<String>>()),
        ).thenReturn(cursor)

        val actual = helper.executeSelectAsCursor(sql, listOf(20, "川流"))

        val bindArgsCaptor = ArgumentCaptor.forClass(Array<String>::class.java)
        assertSame(cursor, actual)
        Mockito.verify(db).rawQuery(
            Mockito.eq(sql),
            bindArgsCaptor.capture(),
        )
        assertEquals(
            listOf("20", "川流"),
            bindArgsCaptor.value.toList(),
        )
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

    @Table(name = "TEST_MIGRATION_ENTITY", alias = "TME")
    @Index(
        name = "IDX_TEST_MIGRATION_ENTITY_AGE",
        properties = ["age"],
    )
    private data class TestMigrationEntity(
        @PrimaryKey
        @Column(name = "ID")
        val id: Long,

        @Column(name = "NEW_NAME")
        @ColumnOldName(name = "OLD_NAME")
        val name: String,

        @Column(name = "AGE")
        val age: Int,
    ) : TableDefinitionEntity

    @Table(name = "TEST_UNKNOWN_ENTITY", alias = "TUE")
    private data class TestUnknownEntity(
        @Column(name = "ID")
        val id: Long,
    ) : TableDefinitionEntity
}
