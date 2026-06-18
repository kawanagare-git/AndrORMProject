package jp.pgw.lab78.androrm.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.pgw.lab78.androrm.common.dml.interfaces.InsertEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.database.entities.*
import jp.pgw.lab78.androrm.database.entities.insert.*
import jp.pgw.lab78.androrm.database.entities.resource.AndroidTestSeedData
import jp.pgw.lab78.androrm.database.support.AndroidTestCsvExporter
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import kotlin.reflect.KClass

/**
 * ## AndrORM AndroidTest
 * ### DB作成、テーブル作成、初期データ投入、件数確認を実行する
 * @author Masahiro Inoue
 * @since 2026-06-16
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AndrOrmDatabaseAndroidTest {
    companion object {
        /** ログ出力有効フラグ */
        private const val LOGGING_ENABLED_PROPERTY = "androrm.logging.enabled"

        /**
         * ## androidTest 開始前処理
         * ### androidTest 実行時のみファイルログ出力を停止する
         * @author Masahiro Inoue
         * @since 2026-06-16
         */
        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            System.setProperty(LOGGING_ENABLED_PROPERTY, "false")
        }

        /**
         * ## androidTest 終了後処理
         * ### ログ出力停止フラグを解除する
         * @author Masahiro Inoue
         * @since 2026-06-16
         */
        @JvmStatic
        @AfterClass
        fun afterClass() {
            System.clearProperty(LOGGING_ENABLED_PROPERTY)
        }
    }

    @get:Rule
    val testName = TestName()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * ## テストDB名
     */
    private val databaseName = "androrm_android_test.db"

    /**
     * ## テーブル定義 Entity
     */
    private val tableDefinitions: Array<KClass<out TableDefinitionEntity>> = arrayOf(
        CharacterStaticInfo::class,
        ItemMaster::class,
        SpellsMaster::class,
        CharacterStatus::class,
        CharacterPossessions::class,
        CharacterEquip::class,
        WeaponMastery::class,
        MagicTypeMastery::class,
        CharacterSpells::class,
    )

    /**
     * ## 各テスト後 CSV 出力
     * ### 各 step 終了時点の DB 状態を CSV へ出力する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    @After
    fun afterEachTest() {
        val databaseHelper = createDatabaseHelper()

        databaseHelper.use { helper ->
            val outputUris = AndroidTestCsvExporter.exportAllTablesToDownload(
                db = helper.readableDatabase,
                context = InstrumentationRegistry.getInstrumentation().targetContext,
                stepName = testName.methodName,
            )

            Log.i(
                "AndrOrmCsv",
                "CSV output count=${outputUris.size}, stepName=${testName.methodName}",
            )
        }
    }

    /**
     * ## step01 DB作成
     * ### 既存DBを削除して、新規DBを作成する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    @Test
    fun step01_createDatabase() {
        context.deleteDatabase(databaseName)

        val databaseHelper = createDatabaseHelper()

        databaseHelper.use { helper ->
            val db = helper.writableDatabase

            assertTrue(db.isOpen)
        }
    }

    /**
     * ## step02 テーブル作成確認
     * ### 作成されたテーブル名を確認する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    @Test
    fun step02_verifyTables() {
        val databaseHelper = createDatabaseHelper()

        databaseHelper.use { helper ->
            val actualTableNames = findUserTableNames(helper.readableDatabase).toSet()
            val expectedTableNames = AndroidTestSeedData.rowCountByTable.keys

            assertEquals(expectedTableNames, actualTableNames)
        }
    }

    /**
     * ## step03 初期データ投入
     * ### SeedData を各テーブルへ投入する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    @Test
    fun step03_insertSeedData() {
        val databaseHelper = createDatabaseHelper()

        databaseHelper.use { helper ->
            clearAllTables(helper.writableDatabase)
            insertSeedData(helper)
        }
    }

    /**
     * ## step04 初期データ件数確認
     * ### SeedData 投入後の各テーブル件数を確認する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    @Test
    fun step04_verifyInsertedRowCounts() {
        val databaseHelper = createDatabaseHelper()

        databaseHelper.use { helper ->
            AndroidTestSeedData.rowCountByTable.forEach { (tableName, expectedCount) ->
                assertEquals(
                    "tableName=$tableName",
                    expectedCount,
                    countRows(
                        db = helper.readableDatabase,
                        tableName = tableName,
                    ),
                )
            }
        }
    }

    /**
     * ## DB Helper 生成
     * ### androidTest 用 DB Helper を生成する
     * @return AndrOrmDatabaseHelper
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun createDatabaseHelper(): AndrOrmDatabaseHelper =
        AndrOrmDatabaseHelper(
            context = context,
            databaseName = databaseName,
            version = 1,
            entities = tableDefinitions,
        )

    /**
     * ## SeedData 投入
     * ### テーブル別に InsertEntity へ変換して投入する
     * @param databaseHelper DB Helper
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun insertSeedData(
        databaseHelper: AndrOrmDatabaseHelper,
    ) {
        insertAll(
            databaseHelper = databaseHelper,
            entityClass = CharacterStaticInfoInsert::class,
            entities = AndroidTestSeedData.characterStaticInfoList.map { entity ->
                CharacterStaticInfoInsert(
                    characterPk = entity.characterPk,
                    userId = entity.userId,
                    characterNo = entity.characterNo,
                    characterName = entity.characterName,
                    createMethod = entity.createMethod,
                    updateMethod = entity.updateMethod,
                )
            },
        )

        insertAll(
            databaseHelper = databaseHelper,
            entityClass = ItemMasterInsert::class,
            entities = AndroidTestSeedData.itemMasterList.map { entity ->
                ItemMasterInsert(
                    itemPk = entity.itemPk,
                    itemType = entity.itemType,
                    itemName = entity.itemName,
                    mainEffect = entity.mainEffect,
                    subEffect = entity.subEffect,
                    equipableSlot = entity.equipableSlot,
                    createMethod = entity.createMethod,
                    updateMethod = entity.updateMethod,
                )
            },
        )

        insertAll(
            databaseHelper = databaseHelper,
            entityClass = SpellsMasterInsert::class,
            entities = AndroidTestSeedData.spellsMasterList.map { entity ->
                SpellsMasterInsert(
                    magicId = entity.magicId,
                    magicTypeId = entity.magicTypeId,
                    magicName = entity.magicName,
                    mainEffect = entity.mainEffect,
                    subEffect = entity.subEffect,
                    createMethod = entity.createMethod,
                    updateMethod = entity.updateMethod,
                )
            },
        )

        insertAll(
            databaseHelper = databaseHelper,
            entityClass = CharacterStatusInsert::class,
            entities = AndroidTestSeedData.characterStatusList.map { entity ->
                CharacterStatusInsert(
                    characterPk = entity.characterPk,
                    statusType = entity.statusType,
                    value = entity.value,
                    createMethod = entity.createMethod,
                    updateMethod = entity.updateMethod,
                )
            },
        )

        insertAll(
            databaseHelper = databaseHelper,
            entityClass = CharacterPossessionsInsert::class,
            entities = AndroidTestSeedData.characterPossessionsList.map { entity ->
                CharacterPossessionsInsert(
                    characterPk = entity.characterPk,
                    itemPk = entity.itemPk,
                    itemStatus = entity.itemStatus,
                    createMethod = entity.createMethod,
                    updateMethod = entity.updateMethod,
                )
            },
        )

        insertAll(
            databaseHelper = databaseHelper,
            entityClass = CharacterEquipInsert::class,
            entities = AndroidTestSeedData.characterEquipList.map { entity ->
                CharacterEquipInsert(
                    characterPk = entity.characterPk,
                    equipSlot = entity.equipSlot,
                    itemPk = entity.itemPk,
                    createMethod = entity.createMethod,
                    updateMethod = entity.updateMethod,
                )
            },
        )

        insertAll(
            databaseHelper = databaseHelper,
            entityClass = WeaponMasteryInsert::class,
            entities = AndroidTestSeedData.weaponMasteryList.map { entity ->
                WeaponMasteryInsert(
                    characterPk = entity.characterPk,
                    weaponTypeId = entity.weaponTypeId,
                    createMethod = entity.createMethod,
                    updateMethod = entity.updateMethod,
                )
            },
        )

        insertAll(
            databaseHelper = databaseHelper,
            entityClass = MagicTypeMasteryInsert::class,
            entities = AndroidTestSeedData.magicTypeMasteryList.map { entity ->
                MagicTypeMasteryInsert(
                    characterPk = entity.characterPk,
                    magicTypeMastery = entity.magicTypeMastery,
                    mastery = entity.mastery,
                    createMethod = entity.createMethod,
                    updateMethod = entity.updateMethod,
                )
            },
        )

        insertAll(
            databaseHelper = databaseHelper,
            entityClass = CharacterSpellsInsert::class,
            entities = AndroidTestSeedData.characterSpellsList.map { entity ->
                CharacterSpellsInsert(
                    characterPk = entity.characterPk,
                    magicId = entity.magicId,
                    createMethod = entity.createMethod,
                    updateMethod = entity.updateMethod,
                )
            },
        )
    }

    /**
     * ## Insert 一括実行
     * ### InsertEntity のリストをまとめて投入する
     * @param databaseHelper DB Helper
     * @param entityClass InsertEntity クラス
     * @param entities 投入 Entity リスト
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun <T : InsertEntity> insertAll(
        databaseHelper: AndrOrmDatabaseHelper,
        entityClass: KClass<out T>,
        entities: List<T>,
    ) {
        val insert = Insert(entityClass)

        insert.addEntities(entities)
        databaseHelper.executeDml(insert)
    }

    /**
     * ## 全テーブルデータ削除
     * ### step03 の再実行時に主キー重複しないよう、既存データを削除する
     * @param db SQLiteDatabase
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun clearAllTables(
        db: SQLiteDatabase,
    ) {
        tableDefinitions
            .map { tableDefinition -> tableDefinition.simpleName!!.toSnakeCase() }
            .forEach { tableName ->
                db.execSQL("delete from ${quoteIdentifier(tableName)}")
            }
    }

    /**
     * ## ユーザーテーブル名取得
     * ### SQLite 管理テーブルを除外してテーブル名を取得する
     * @param db SQLiteDatabase
     * @return テーブル名リスト
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun findUserTableNames(
        db: SQLiteDatabase,
    ): List<String> {
        val tableNames = mutableListOf<String>()

        db.rawQuery(
            """
                select name
                from sqlite_master
                where type = 'table'
                  and name not like 'sqlite_%'
                  and name <> 'android_metadata'
                order by name
            """.trimIndent(),
            emptyArray<String>(),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                tableNames += cursor.getString(0)
            }
        }

        return tableNames
    }

    /**
     * ## 件数取得
     * ### 指定テーブルの件数を取得する
     * @param db SQLiteDatabase
     * @param tableName テーブル名
     * @return 件数
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun countRows(
        db: SQLiteDatabase,
        tableName: String,
    ): Int =
        db.rawQuery(
            "select count(*) from ${quoteIdentifier(tableName)}",
            emptyArray<String>(),
        ).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    /**
     * ## SQL 識別子クォート
     * ### SQLite 用に識別子をダブルクォートで囲む
     * @param identifier 識別子
     * @return クォート済み識別子
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun quoteIdentifier(
        identifier: String,
    ): String =
        "\"${identifier.replace("\"", "\"\"")}\""

    /**
     * ## スネークケース変換
     * ### クラス名からテーブル名を生成する
     * @receiver 変換前文字列
     * @return スネークケース大文字
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun String.toSnakeCase(): String =
        replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()

    /**
     * ## shell コマンド実行
     * ### UiAutomation 経由で shell コマンドを実行し、標準出力を取得する
     * @param command shell コマンド
     * @return 標準出力
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun executeShellCommand(
        command: String,
    ): String {
        val fileDescriptor = InstrumentationRegistry
            .getInstrumentation()
            .uiAutomation
            .executeShellCommand(command)

        return ParcelFileDescriptor.AutoCloseInputStream(fileDescriptor)
            .bufferedReader()
            .use { reader ->
                reader.readText()
            }
    }
}
