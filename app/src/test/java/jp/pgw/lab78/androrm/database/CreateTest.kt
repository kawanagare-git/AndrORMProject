package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.MessageConstants.AE00008
import jp.pgw.lab78.androrm.common.MessageConstants.AE00019
import jp.pgw.lab78.androrm.common.MessageConstants.AE00020
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Index
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.annotation.Unique
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.database.entities.define.TestLargeEntity
import org.junit.Assert.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CreateTest {

    @Test
    fun testBuild_testLargeEntity() {
        val actual = Create(TestLargeEntity::class).build()

        assertEquals(
            "create table TEST_LARGE_ENTITY " +
                    "(ID INTEGER, CODE TEXT, NAME TEXT, FURIGANA TEXT, GENDER TEXT, " +
                    "BIRTHDAY DATETIME, PLACE_OF_BIRTH TEXT, EMAIL TEXT, PHONE_NUMBER TEXT, " +
                    "POSTAL_CODE TEXT, PREFECTURE TEXT, CITY TEXT, ADDRESS_LINE TEXT, " +
                    "SCORE REAL, BALANCE INTEGER, ACTIVE INTEGER, REGISTERED_AT DATETIME, " +
                    "LAST_LOGIN_AT DATETIME, MEMO TEXT, CREATED_AT DATETIME, UPDATED_AT DATETIME, " +
                    "primary key (ID))",
            actual,
        )
    }

    @Test
    fun testBuild_tableNameOverride_testLargeEntity() {
        val actual = Create(
            entityClass = TestLargeEntity::class,
            tableNameOverride = "TEST_LARGE_ENTITY_WORK",
        ).build()

        assertEquals(
            "create table TEST_LARGE_ENTITY_WORK " +
                    "(ID INTEGER, CODE TEXT, NAME TEXT, FURIGANA TEXT, GENDER TEXT, " +
                    "BIRTHDAY DATETIME, PLACE_OF_BIRTH TEXT, EMAIL TEXT, PHONE_NUMBER TEXT, " +
                    "POSTAL_CODE TEXT, PREFECTURE TEXT, CITY TEXT, ADDRESS_LINE TEXT, " +
                    "SCORE REAL, BALANCE INTEGER, ACTIVE INTEGER, REGISTERED_AT DATETIME, " +
                    "LAST_LOGIN_AT DATETIME, MEMO TEXT, CREATED_AT DATETIME, UPDATED_AT DATETIME, " +
                    "primary key (ID))",
            actual,
        )
    }

    @Test
    fun testBuildIndexQueries_testLargeEntity() {
        val actual = Create(TestLargeEntity::class).buildIndexQueries(2)

        assertEquals(
            listOf(
                "create index if not exists IDX_TEST_LARGE_ENTITY_ACTIVE_CREATED2 " +
                        "on TEST_LARGE_ENTITY (ACTIVE, CREATED_AT)",
                "create unique index if not exists UQ_TEST_CODE2 " +
                        "on TEST_LARGE_ENTITY (CODE)",
                "create unique index if not exists UQ_TEST_PERSONAL_INFO2 " +
                        "on TEST_LARGE_ENTITY (NAME, FURIGANA, GENDER, BIRTHDAY, PLACE_OF_BIRTH)",
            ),
            actual,
        )
    }

    @Test
    fun testBuildIndexQueries_emptyIndexProperties_throwsIllegalArgumentException() {
        val actual = assertThrows(IllegalArgumentException::class.java) {
            Create(TestEmptyIndexPropertiesEntity::class).buildIndexQueries(3)
        }

        assertEquals(AE00019, actual.message)
    }

    @Test
    fun testBuildIndexQueries_emptyUniqueProperties_throwsIllegalArgumentException() {
        val actual = assertThrows(IllegalArgumentException::class.java) {
            Create(TestEmptyUniquePropertiesEntity::class).buildIndexQueries(4)
        }

        assertEquals(AE00019, actual.message)
    }

    @Test
    fun testBuildIndexQueries_duplicateIndexName_throwsIllegalArgumentException() {
        val actual = assertThrows(IllegalArgumentException::class.java) {
            Create(TestDuplicateIndexNameEntity::class).buildIndexQueries(0)
        }

        assertEquals(
            AE00020.format("IDX_DUPLICATE0"),
            actual.message,
        )
    }

    @Test
    fun testBuildIndexQueries_duplicateIndexNameIgnoreCase_throwsIllegalArgumentException() {
        val actual = assertThrows(IllegalArgumentException::class.java) {
            Create(TestDuplicateIndexNameIgnoreCaseEntity::class).buildIndexQueries(9)
        }

        assertEquals(
            AE00020.format("idx_duplicate9"),
            actual.message,
        )
    }

    @Test
    fun testBuildIndexQueries_unknownIndexProperty_throwsIllegalStateException() {
        val actual = assertThrows(IllegalStateException::class.java) {
            Create(TestUnknownIndexPropertyEntity::class).buildIndexQueries(100)
        }

        assertEquals(
            AE00008.format(
                "unknownProperty",
                TestUnknownIndexPropertyEntity::class.qualifiedName,
            ),
            actual.message,
        )
    }

    @Test
    fun testBuildIndexQueries_unknownUniqueProperty_throwsIllegalStateException() {
        val actual = assertThrows(IllegalStateException::class.java) {
            Create(TestUnknownUniquePropertyEntity::class).buildIndexQueries(1000)
        }

        assertEquals(
            AE00008.format(
                "unknownProperty",
                TestUnknownUniquePropertyEntity::class.qualifiedName,
            ),
            actual.message,
        )
    }

    @Table(name = "TEST_EMPTY_INDEX_PROPERTIES_ENTITY", alias = "TEI")
    @Index(
        name = "IDX_EMPTY",
        properties = [],
    )
    private data class TestEmptyIndexPropertiesEntity(
        @Column(name = "ID")
        val id: Int,
    ) : TableDefinitionEntity

    @Table(name = "TEST_EMPTY_UNIQUE_PROPERTIES_ENTITY", alias = "TEU")
    @Unique(
        name = "UQ_EMPTY",
        properties = [],
    )
    private data class TestEmptyUniquePropertiesEntity(
        @Column(name = "ID")
        val id: Int,
    ) : TableDefinitionEntity

    @Table(name = "TEST_DUPLICATE_INDEX_NAME_ENTITY", alias = "TDI")
    @Index(
        name = "IDX_DUPLICATE",
        properties = ["name"],
    )
    @Unique(
        name = "IDX_DUPLICATE",
        properties = ["code"],
    )
    private data class TestDuplicateIndexNameEntity(
        @Column(name = "CODE")
        val code: String,

        @Column(name = "NAME")
        val name: String,
    ) : TableDefinitionEntity

    @Table(name = "TEST_DUPLICATE_INDEX_NAME_IGNORE_CASE_ENTITY", alias = "TDC")
    @Index(
        name = "IDX_DUPLICATE",
        properties = ["name"],
    )
    @Unique(
        name = "idx_duplicate",
        properties = ["code"],
    )
    private data class TestDuplicateIndexNameIgnoreCaseEntity(
        @Column(name = "CODE")
        val code: String,

        @Column(name = "NAME")
        val name: String,
    ) : TableDefinitionEntity

    @Table(name = "TEST_UNKNOWN_INDEX_PROPERTY_ENTITY", alias = "TUI")
    @Index(
        name = "IDX_UNKNOWN_PROPERTY",
        properties = ["unknownProperty"],
    )
    private data class TestUnknownIndexPropertyEntity(
        @Column(name = "ID")
        val id: Int,
    ) : TableDefinitionEntity

    @Table(name = "TEST_UNKNOWN_UNIQUE_PROPERTY_ENTITY", alias = "TUU")
    @Unique(
        name = "UQ_UNKNOWN_PROPERTY",
        properties = ["unknownProperty"],
    )
    private data class TestUnknownUniquePropertyEntity(
        @Column(name = "ID")
        val id: Int,
    ) : TableDefinitionEntity
}