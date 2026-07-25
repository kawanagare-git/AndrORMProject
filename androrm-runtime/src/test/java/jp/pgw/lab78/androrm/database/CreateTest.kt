package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.MessageConstants.AE00008
import jp.pgw.lab78.androrm.common.MessageConstants.AE00009
import jp.pgw.lab78.androrm.common.MessageConstants.AE00019
import jp.pgw.lab78.androrm.common.MessageConstants.AE00020
import jp.pgw.lab78.androrm.common.MessageConstants.AE00037
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Index
import jp.pgw.lab78.androrm.common.database.annotation.PrimaryKey
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.annotation.Unique
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.database.entities.define.TestLargeEntity
import org.junit.Assert.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * CREATE TABLE文とインデックス作成文の生成仕様を検証する。
 *
 * @author Masahiro Inoue
 * @since 2026-05-30
 */
class CreateTest {

    /**
     * 大規模Entityから期待するCREATE TABLE文が生成されることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Test
    fun testBuild_testLargeEntity() {
        val actual = Create(TestLargeEntity::class).build()

        assertEquals(
            """create table "TEST_LARGE_ENTITY" """ +
                    """("ID" INTEGER not null, "CODE" TEXT not null, "NAME" TEXT not null, "FURIGANA" TEXT not null, "GENDER" TEXT not null, """ +
                    """"BIRTHDAY" DATETIME not null, "PLACE_OF_BIRTH" TEXT not null, "EMAIL" TEXT not null, "PHONE_NUMBER" TEXT not null, """ +
                    """"POSTAL_CODE" TEXT not null, "PREFECTURE" TEXT not null, "CITY" TEXT not null, "ADDRESS_LINE" TEXT not null, """ +
                    """"SCORE" REAL not null, "BALANCE" INTEGER not null, "ACTIVE" INTEGER not null, "REGISTERED_AT" DATETIME not null, """ +
                    """"LAST_LOGIN_AT" DATETIME not null, "MEMO" TEXT not null, "CREATED_AT" DATETIME not null, "UPDATED_AT" DATETIME not null, """ +
                    """primary key ("ID"))""",
            actual,
        )
    }

    /**
     * 上書きしたテーブル名がCREATE TABLE文へ反映されることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Test
    fun testBuild_tableNameOverride_testLargeEntity() {
        val actual = Create(
            entityClass = TestLargeEntity::class,
            tableNameOverride = "TEST_LARGE_ENTITY_WORK",
        ).build()

        assertEquals(
            """create table "TEST_LARGE_ENTITY_WORK" """ +
                    """("ID" INTEGER not null, "CODE" TEXT not null, "NAME" TEXT not null, "FURIGANA" TEXT not null, "GENDER" TEXT not null, """ +
                    """"BIRTHDAY" DATETIME not null, "PLACE_OF_BIRTH" TEXT not null, "EMAIL" TEXT not null, "PHONE_NUMBER" TEXT not null, """ +
                    """"POSTAL_CODE" TEXT not null, "PREFECTURE" TEXT not null, "CITY" TEXT not null, "ADDRESS_LINE" TEXT not null, """ +
                    """"SCORE" REAL not null, "BALANCE" INTEGER not null, "ACTIVE" INTEGER not null, "REGISTERED_AT" DATETIME not null, """ +
                    """"LAST_LOGIN_AT" DATETIME not null, "MEMO" TEXT not null, "CREATED_AT" DATETIME not null, "UPDATED_AT" DATETIME not null, """ +
                    """primary key ("ID"))""",
            actual,
        )
    }

    /**
     * 大規模Entityのインデックス作成文が生成されることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Test
    fun testBuildIndexQueries_testLargeEntity() {
        val actual = Create(TestLargeEntity::class).buildIndexQueries(2)

        assertEquals(
            listOf(
                """create index "IDX_TEST_LARGE_ENTITY_ACTIVE_CREATED_2" """ +
                        """on "TEST_LARGE_ENTITY" ("ACTIVE", "CREATED_AT")""",
                """create unique index "UQ_TEST_CODE_2" """ +
                        """on "TEST_LARGE_ENTITY" ("CODE")""",
                """create unique index "UQ_TEST_PERSONAL_INFO_2" """ +
                        """on "TEST_LARGE_ENTITY" ("NAME", "FURIGANA", "GENDER", "BIRTHDAY", "PLACE_OF_BIRTH")""",
            ),
            actual,
        )
    }

    /**
     * 名前未指定のインデックス名がアノテーションのプロパティから生成されることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Test
    fun testBuildIndexQueries_emptyNames_buildNamesFromAnnotationProperties() {
        val actual = Create(TestDefaultIndexNameEntity::class).buildIndexQueries(7)

        assertEquals(
            listOf(
                """create index "IDX_TEST_DEFAULT_INDEX_NAME_ENTITY_NAME_CODE_7" """ +
                        """on "TEST_DEFAULT_INDEX_NAME_ENTITY" ("NAME", "CODE")""",
                """create unique index "UNIQ_TEST_DEFAULT_INDEX_NAME_ENTITY_CODE_7" """ +
                        """on "TEST_DEFAULT_INDEX_NAME_ENTITY" ("CODE")""",
            ),
            actual,
        )
    }

    /**
     * インデックス作成文を再生成しても同じ結果となることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Test
    fun testBuildIndexQueries_calledTwice_returnsSameQueries() {
        val create = Create(TestDefaultIndexNameEntity::class)
        val first = create.buildIndexQueries(7)

        val second = create.buildIndexQueries(7)

        assertEquals(first, second)
    }

    /**
     * カラムの既定値がDEFAULT句へ反映されることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Test
    fun testBuild_withDefaultColumn_buildsDefaultClause() {
        val actual = Create(TestDefaultColumnEntity::class).build()

        assertEquals(
            """create table "TEST_DEFAULT_COLUMN_ENTITY" """ +
                    """("ID" INTEGER not null default 0, """ +
                    """"NAME" TEXT not null default '未設定', """ +
                    """"ACTIVE" INTEGER not null default 1 /* 0:false / 1:true */, """ +
                    """"REGISTERED_DATE" DATETIME not null default CURRENT_DATE, """ +
                    """"REGISTERED_TIME" DATETIME not null default CURRENT_TIME, """ +
                    """"CREATED_AT" DATETIME not null default CURRENT_TIMESTAMP, """ +
                    """"MEMO" TEXT default NULL, """ +
                    """"BINARY_DATA" BLOB default NULL)""",
            actual,
        )
    }

    /**
     * Indexのプロパティが空の場合に検証例外となることを確認する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Test
    fun testBuildIndexQueries_emptyIndexProperties_throwsIllegalArgumentException() {
        val actual = assertThrows(IllegalArgumentException::class.java) {
            Create(TestEmptyIndexPropertiesEntity::class).buildIndexQueries(3)
        }

        assertEquals(AE00019, actual.message)
    }

    /**
     * Uniqueのプロパティが空の場合に検証例外となることを確認する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Test
    fun testBuildIndexQueries_emptyUniqueProperties_throwsIllegalArgumentException() {
        val actual = assertThrows(IllegalArgumentException::class.java) {
            Create(TestEmptyUniquePropertiesEntity::class).buildIndexQueries(4)
        }

        assertEquals(AE00019, actual.message)
    }

    /**
     * 同じインデックス名が重複した場合に検証例外となることを確認する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Test
    fun testBuildIndexQueries_duplicateIndexName_throwsIllegalArgumentException() {
        val actual = assertThrows(IllegalArgumentException::class.java) {
            Create(TestDuplicateIndexNameEntity::class).buildIndexQueries(0)
        }

        assertEquals(
            AE00020.format("IDX_DUPLICATE_0"),
            actual.message,
        )
    }

    /**
     * 大文字小文字だけが異なるインデックス名を重複として拒否することを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Test
    fun testBuildIndexQueries_duplicateIndexNameIgnoreCase_throwsIllegalArgumentException() {
        val actual = assertThrows(IllegalArgumentException::class.java) {
            Create(TestDuplicateIndexNameIgnoreCaseEntity::class).buildIndexQueries(9)
        }

        assertEquals(
            AE00020.format("idx_duplicate_9"),
            actual.message,
        )
    }

    /**
     * Indexが未知のプロパティを参照した場合に例外となることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
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

    /**
     * Uniqueが未知のプロパティを参照した場合に例外となることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
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

    /**
     * 予約語や特殊文字を含むSQL識別子が引用されることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Test
    fun testBuild_sqlIdentifiers_quotesReservedWordsAndSpecialCharacters() {
        val actual = Create(TestSpecialIdentifierEntity::class).build()

        assertEquals(
            """create table "select table" """ +
                    """("primary-key" INTEGER not null, "quoted""column" TEXT not null, """ +
                    """primary key ("primary-key"))""",
            actual,
        )
    }

    /**
     * SQL断片に見える上書きテーブル名が一つの識別子として引用されることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Test
    fun testBuild_tableNameOverrideContainingSqlFragment_quotesAsOneIdentifier() {
        val actual = Create(
            entityClass = TestSpecialIdentifierEntity::class,
            tableNameOverride = "work; drop table users;--",
        ).build()

        assertEquals(
            """create table "work; drop table users;--" """ +
                    """("primary-key" INTEGER not null, "quoted""column" TEXT not null, """ +
                    """primary key ("primary-key"))""",
            actual,
        )
    }

    /**
     * インデックス名、テーブル名、カラム名がSQL識別子として引用されることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Test
    fun testBuildIndexQueries_sqlIdentifiers_quotesIndexTableAndColumns() {
        val actual = Create(TestSpecialIdentifierEntity::class).buildIndexQueries(3)

        assertEquals(
            listOf(
                """create index "index name""_3" """ +
                        """on "select table" ("quoted""column")""",
            ),
            actual,
        )
    }

    /**
     * 空白だけの上書きテーブル名を拒否することを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Test
    fun testCreate_blankTableNameOverride_throwsIllegalArgumentException() {
        val actual = assertThrows(IllegalArgumentException::class.java) {
            Create(
                entityClass = TestLargeEntity::class,
                tableNameOverride = "   ",
            )
        }

        assertEquals(AE00037.format("   "), actual.message)
    }

    /**
     * NUL文字を含む上書きテーブル名を拒否することを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Test
    fun testCreate_nullCharacterTableNameOverride_throwsIllegalArgumentException() {
        val invalidIdentifier = "TABLE\u0000NAME"

        val actual = assertThrows(IllegalArgumentException::class.java) {
            Create(
                entityClass = TestLargeEntity::class,
                tableNameOverride = invalidIdentifier,
            )
        }

        assertEquals(AE00037.format(invalidIdentifier), actual.message)
    }

    /**
     * ## 未対応Kotlin型のCREATEテスト
     * ### 未対応型を持つEntityを意図した検証例外で拒否することを確認する
     * @author Masahiro Inoue
     * @since 2026-07-19
     */
    @Test
    fun testBuild_unsupportedKotlinType_throwsIllegalArgumentException() {
        val actual = assertThrows(IllegalArgumentException::class.java) {
            Create(TestUnsupportedTypeEntity::class).build()
        }

        assertEquals(AE00009.format(BigDecimal::class), actual.message)
    }

    /**
     * 利用者指定インデックス名とバージョンの境界がアンダースコアで区切られることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Test
    fun testBuildIndexQueries_versionBoundary_usesUnderscoreSeparator() {
        val usedIndexNames = mutableSetOf<String>()

        val first = Create(TestIndexVersionBoundaryFirstEntity::class)
            .buildIndexQueries(20, usedIndexNames)
        val second = Create(TestIndexVersionBoundarySecondEntity::class)
            .buildIndexQueries(0, usedIndexNames)

        assertEquals(
            listOf(
                """create index "INDEX1_20" on "TEST_INDEX_VERSION_BOUNDARY_FIRST" ("ID")""",
            ),
            first,
        )
        assertEquals(
            listOf(
                """create index "INDEX12_0" on "TEST_INDEX_VERSION_BOUNDARY_SECOND" ("ID")""",
            ),
            second,
        )
    }

    /**
     * 異なるEntity間のインデックス名重複が共有済み名称集合で拒否されることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Test
    fun testBuildIndexQueries_duplicateNameAcrossEntities_throwsIllegalArgumentException() {
        val usedIndexNames = mutableSetOf<String>()
        Create(TestDuplicateIndexAcrossEntitiesFirstEntity::class)
            .buildIndexQueries(6, usedIndexNames)

        val actual = assertThrows(IllegalArgumentException::class.java) {
            Create(TestDuplicateIndexAcrossEntitiesSecondEntity::class)
                .buildIndexQueries(6, usedIndexNames)
        }

        assertEquals(AE00020.format("shared_create_index_6"), actual.message)
    }

    /**
     * DEFAULT NULLの混在表記が大文字へ正規化されることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Test
    fun testBuild_mixedCaseNullDefault_normalizesToUppercaseNull() {
        val actual = Create(TestMixedCaseNullDefaultEntity::class).build()

        assertEquals(
            """create table "TEST_MIXED_CASE_NULL_DEFAULT" ("MEMO" TEXT default NULL)""",
            actual,
        )
    }

    /**
     * 主キーを持たないEntityでもnull許容性に応じたカラム制約だけが生成されることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Test
    fun testBuild_withoutPrimaryKey_buildsNullableConstraintsWithoutPrimaryKeyClause() {
        val actual = Create(TestNoPrimaryKeyEntity::class).build()

        assertEquals(
            """create table "TEST_NO_PRIMARY_KEY" ("ID" INTEGER not null, "MEMO" TEXT)""",
            actual,
        )
    }

    /**
     * 複数の主キープロパティから宣言順どおりに複合主キー句が生成されることを検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Test
    fun testBuild_compositePrimaryKey_buildsOrderedPrimaryKeyClause() {
        val actual = Create(TestCompositePrimaryKeyEntity::class).build()

        assertEquals(
            """create table "TEST_COMPOSITE_PRIMARY_KEY" """ +
                    """("PARENT_ID" INTEGER not null, "CHILD_ID" INTEGER not null, "MEMO" TEXT, """ +
                    """primary key ("PARENT_ID", "CHILD_ID"))""",
            actual,
        )
    }

    /**
     * 空のIndexプロパティを持つ検証用Entity。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Table(name = "TEST_EMPTY_INDEX_PROPERTIES_ENTITY", alias = "TEI")
    @Index(
        name = "IDX_EMPTY",
        properties = [],
    )
    private data class TestEmptyIndexPropertiesEntity(
        @Column(name = "ID")
        val id: Int,
    ) : TableDefinitionEntity

    /**
     * 空のUniqueプロパティを持つ検証用Entity。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Table(name = "TEST_EMPTY_UNIQUE_PROPERTIES_ENTITY", alias = "TEU")
    @Unique(
        name = "UQ_EMPTY",
        properties = [],
    )
    private data class TestEmptyUniquePropertiesEntity(
        @Column(name = "ID")
        val id: Int,
    ) : TableDefinitionEntity

    /**
     * 重複するインデックス名を持つ検証用Entity。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
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

    /**
     * 大文字小文字だけが異なる重複インデックス名を持つ検証用Entity。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
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

    /**
     * 名前未指定のIndexとUniqueを持つ検証用Entity。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Table(name = "TEST_DEFAULT_INDEX_NAME_ENTITY", alias = "TDIN")
    @Index(
        properties = ["name", "code"],
    )
    @Unique(
        properties = ["code"],
    )
    private data class TestDefaultIndexNameEntity(
        @Column(name = "CODE")
        val code: String,

        @Column(name = "NAME")
        val name: String,
    ) : TableDefinitionEntity

    /**
     * 未知のプロパティを参照するIndexを持つ検証用Entity。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Table(name = "TEST_UNKNOWN_INDEX_PROPERTY_ENTITY", alias = "TUI")
    @Index(
        name = "IDX_UNKNOWN_PROPERTY",
        properties = ["unknownProperty"],
    )
    private data class TestUnknownIndexPropertyEntity(
        @Column(name = "ID")
        val id: Int,
    ) : TableDefinitionEntity

    /**
     * 未知のプロパティを参照するUniqueを持つ検証用Entity。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Table(name = "TEST_UNKNOWN_UNIQUE_PROPERTY_ENTITY", alias = "TUU")
    @Unique(
        name = "UQ_UNKNOWN_PROPERTY",
        properties = ["unknownProperty"],
    )
    private data class TestUnknownUniquePropertyEntity(
        @Column(name = "ID")
        val id: Int,
    ) : TableDefinitionEntity

    /**
     * 予約語や特殊文字を含むSQL識別子を持つ検証用Entity。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Table(name = "select table", alias = "TSI")
    @Index(
        name = """index name"""",
        properties = ["quotedColumn"],
    )
    private data class TestSpecialIdentifierEntity(
        @PrimaryKey
        @Column(name = "primary-key")
        val primaryKey: Int,

        @Column(name = """quoted"column""")
        val quotedColumn: String,
    ) : TableDefinitionEntity

    /**
     * ## 未対応Kotlin型テストEntity
     * ### SQLite型変換Mapに登録されていないBigDecimalをカラム型として定義する
     * @author Masahiro Inoue
     * @since 2026-07-19
     */
    @Table(name = "TEST_UNSUPPORTED_TYPE_ENTITY", alias = "TUT")
    private data class TestUnsupportedTypeEntity(
        @Column(name = "AMOUNT")
        val amount: BigDecimal,
    ) : TableDefinitionEntity

    /**
     * インデックス名とバージョン境界の一つ目の検証用Entity。
     *
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Table(name = "TEST_INDEX_VERSION_BOUNDARY_FIRST", alias = "TIVBF")
    @Index(name = "INDEX1", properties = ["id"])
    private data class TestIndexVersionBoundaryFirstEntity(
        @Column(name = "ID")
        val id: Int,
    ) : TableDefinitionEntity

    /**
     * インデックス名とバージョン境界の二つ目の検証用Entity。
     *
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Table(name = "TEST_INDEX_VERSION_BOUNDARY_SECOND", alias = "TIVBS")
    @Index(name = "INDEX12", properties = ["id"])
    private data class TestIndexVersionBoundarySecondEntity(
        @Column(name = "ID")
        val id: Int,
    ) : TableDefinitionEntity

    /**
     * Entity間インデックス名重複の一つ目の検証用Entity。
     *
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Table(name = "TEST_DUPLICATE_INDEX_ACROSS_ENTITIES_FIRST", alias = "TDIAEF")
    @Index(name = "SHARED_CREATE_INDEX", properties = ["id"])
    private data class TestDuplicateIndexAcrossEntitiesFirstEntity(
        @Column(name = "ID")
        val id: Int,
    ) : TableDefinitionEntity

    /**
     * Entity間インデックス名重複の二つ目の検証用Entity。
     *
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Table(name = "TEST_DUPLICATE_INDEX_ACROSS_ENTITIES_SECOND", alias = "TDIAES")
    @Index(name = "shared_create_index", properties = ["id"])
    private data class TestDuplicateIndexAcrossEntitiesSecondEntity(
        @Column(name = "ID")
        val id: Int,
    ) : TableDefinitionEntity

    /**
     * DEFAULT NULLの混在表記を持つ検証用Entity。
     *
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Table(name = "TEST_MIXED_CASE_NULL_DEFAULT", alias = "TMCND")
    private data class TestMixedCaseNullDefaultEntity(
        @Column(name = "MEMO", default = "Null")
        val memo: String?,
    ) : TableDefinitionEntity

    /**
     * 主キーを持たず、nullableとnon-nullの両方を持つ検証用Entity。
     *
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Table(name = "TEST_NO_PRIMARY_KEY", alias = "TNPK")
    private data class TestNoPrimaryKeyEntity(
        @Column(name = "ID")
        val id: Int,
        @Column(name = "MEMO")
        val memo: String?,
    ) : TableDefinitionEntity

    /**
     * 複合主キーを持つ検証用Entity。
     *
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Table(name = "TEST_COMPOSITE_PRIMARY_KEY", alias = "TCPK")
    private data class TestCompositePrimaryKeyEntity(
        @PrimaryKey
        @Column(name = "PARENT_ID")
        val parentId: Int,
        @PrimaryKey
        @Column(name = "CHILD_ID")
        val childId: Int,
        @Column(name = "MEMO")
        val memo: String?,
    ) : TableDefinitionEntity

    /**
     * 各Kotlin型のDEFAULT値を持つ検証用Entity。
     *
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    @Table(name = "TEST_DEFAULT_COLUMN_ENTITY", alias = "TDC")
    private data class TestDefaultColumnEntity(
        @Column(name = "ID", default = "0")
        val id: Int,

        @Column(name = "NAME", default = "'未設定'")
        val name: String,

        @Column(name = "ACTIVE", default = "1")
        val active: Boolean,

        @Column(name = "REGISTERED_DATE", default = "CURRENT_DATE")
        val registeredDate: LocalDate,

        @Column(name = "REGISTERED_TIME", default = "CURRENT_TIME")
        val registeredTime: LocalTime,

        @Column(name = "CREATED_AT", default = "CURRENT_TIMESTAMP")
        val createdAt: LocalDateTime,

        @Column(name = "MEMO", default = "NULL")
        val memo: String?,

        @Column(name = "BINARY_DATA", default = "NULL")
        val binaryData: ByteArray?,
    ) : TableDefinitionEntity {
        /**
         * 全プロパティを比較して同値性を判定する。
         *
         * @param other 比較対象
         * @return 全プロパティが等しい場合はtrue
         * @author Masahiro Inoue
         * @since 2026-05-30
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TestDefaultColumnEntity

            if (id != other.id) return false
            if (active != other.active) return false
            if (name != other.name) return false
            if (registeredDate != other.registeredDate) return false
            if (registeredTime != other.registeredTime) return false
            if (createdAt != other.createdAt) return false
            if (memo != other.memo) return false
            if (!binaryData.contentEquals(other.binaryData)) return false

            return true
        }

        /**
         * 全プロパティからハッシュ値を生成する。
         *
         * @return 生成されたハッシュ値
         * @author Masahiro Inoue
         * @since 2026-05-30
         */
        override fun hashCode(): Int {
            var result = id
            result = 31 * result + active.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + registeredDate.hashCode()
            result = 31 * result + registeredTime.hashCode()
            result = 31 * result + createdAt.hashCode()
            result = 31 * result + (memo?.hashCode() ?: 0)
            result = 31 * result + (binaryData?.contentHashCode() ?: 0)
            return result
        }
    }
}