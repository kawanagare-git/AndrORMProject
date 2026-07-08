package jp.pgw.lab78.androrm.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableName
import jp.pgw.lab78.androrm.common.database.SupportFunction.toSnakeCase
import jp.pgw.lab78.androrm.common.dml.interfaces.InsertEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.database.entities.*
import jp.pgw.lab78.androrm.database.entities.absert.CharacterEquipAbsert
import jp.pgw.lab78.androrm.database.entities.insert.*
import jp.pgw.lab78.androrm.database.entities.resource.AndroidTestSeedData
import jp.pgw.lab78.androrm.database.entities.select.*
import jp.pgw.lab78.androrm.database.entities.update.CharacterStatusUpdate
import jp.pgw.lab78.androrm.database.entities.update.ItemMasterUpdateEffect
import jp.pgw.lab78.androrm.database.entities.update.ItemMasterUpdateEquip
import jp.pgw.lab78.androrm.database.entities.update.SpellsMasterUpdate
import jp.pgw.lab78.androrm.database.entities.upsert.CharacterPossessionsUpsert
import jp.pgw.lab78.androrm.database.interfaces.plus
import jp.pgw.lab78.androrm.database.queryparts.JoinType
import jp.pgw.lab78.androrm.database.reference.TableRef
import jp.pgw.lab78.androrm.database.support.AndroidTestCsvExporter
import org.junit.*
import org.junit.Assert.*
import org.junit.rules.TestName
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.use

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

        /** DML 系実行時カウント */
        private var actualCount = 0

        private var testStep: String = EMPTY_STRING
        private var verifyStep: String = EMPTY_STRING

        /** 各テーブルの件数を保持（単純件数） */
        private var beforeTableRowCounts: MutableMap<String, Int> = mutableMapOf()
        private var afterTableRowCounts: MutableMap<String, Int> = mutableMapOf()

        private var isUpgrade = false
        private var version = if (isUpgrade) 2 else 1
        private var index = if (isUpgrade) 1 else 0

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
            AndroidTestSeedData.rowCountByTable.forEach { (tableName, expectedCount) ->
                afterTableRowCounts[tableName] = expectedCount
            }
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

        /**
         * ## テーブル定義 Entity
         */
        private val tableDefinitions = arrayOf(
            arrayOf(
                CharacterStaticInfoV1::class,
                ItemMaster::class,
                SpellsMaster::class,
                CharacterStatus::class,
                CharacterPossessions::class,
                CharacterEquip::class,
                WeaponMastery::class,
                MagicTypeMastery::class,
                CharacterSpells::class,
            ),
            arrayOf(
                CharacterStaticInfoV2::class,
                ItemMaster::class,
                SpellsMaster::class,
                CharacterStatus::class,
                CharacterPossessions::class,
                CharacterEquip::class,
                WeaponMastery::class,
                MagicTypeMastery::class,
                CharacterSpells::class,
            )
        )
    }

    @get:Rule
    val testName = TestName()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * ## テストDB名
     */
    private val databaseName = "androrm_android_test.db"

    var currentTableDefinitions = tableDefinitions[index]

    /**
     * ## Test Step 初期化
     * ### 各 Step 開始前の変数初期化
     * @author Masahiro Inoue
     * @since 2026-07-05
     */
    @Before
    fun beforeEachTest() {
        verifyStep = testStep
        testStep = testName.methodName.substringBefore("_")
    }

    /**
     * ## 各テスト後 CSV 出力
     * ### 各 step 終了時点の DB 状態を CSV へ出力する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    @After
    fun afterEachTest() {
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)

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
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)

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
        try {
            val databaseHelper =
                createDatabaseHelper(version, *currentTableDefinitions)
            databaseHelper.use { helper ->
                val actualTableNames = findUserTableNames(helper.readableDatabase).toSet()
                val expectedTableNames = AndroidTestSeedData.rowCountByTable.keys

                assertEquals(expectedTableNames, actualTableNames)
            }
        } catch (throwable: Throwable) {
            Log.e(
                "SeedDataInit",
                "AndroidTestSeedData initialization failed.",
                throwable,
            )

            throwable.cause?.let { cause ->
                Log.e(
                    "SeedDataInit",
                    "cause=${cause::class.qualifiedName}: ${cause.message}",
                    cause,
                )
            }

            throw throwable
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
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)
        databaseHelper.use { helper ->
            helper.transaction {
                clearAllTables(helper.writableDatabase)
                insertSeedData(helper)
            }
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
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)
        refreshTableRowCounts(databaseHelper)
        assertEquals(beforeTableRowCounts, afterTableRowCounts.toMap())
    }

    /**
     * ## step05 初期データ更新
     * ### SeedData データを更新する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    @Test
    fun step05_updatedSeedData() {
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)
        databaseHelper.use { helper ->
            actualCount = updateData(helper, testStep)
        }
    }

    /**
     * ## step06 更新データ件数確認
     * ### SeedData 投入後の各テーブル件数を確認する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    @Test
    fun step06_verifyUpdatedRowCounts() {
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)
        var expectedCount = 0
        databaseHelper.use { helper ->
            AndroidTestSeedData.rowCountByTable.forEach { (tableName) ->
                expectedCount += getTableRows(
                    db = helper.readableDatabase,
                    tableName = tableName.toSnakeCase(),
                    "UPDATE_METHOD = '$verifyStep'"
                )
            }
        }
        Log.d("step06", "actualCount = $actualCount")
        assertNotEquals(0, actualCount)
        assertEquals(expectedCount, actualCount)
    }

    /**
     * ## step07 追加・更新
     * ### SeedData 投入後の各テーブルに追加・更新
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    @Test
    fun step07_upsertSeedData() {
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)
        databaseHelper.use { helper ->
            actualCount = upsertData(helper)
        }
    }

    /**
     * ## step06 更新データ件数確認
     * ### SeedData 投入後の各テーブル件数を確認する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    @Test
    fun step08_verifyUpdatedRowCounts() {
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)
        refreshTableRowCounts(databaseHelper)
        var addCount: Int
        currentTableDefinitions.forEach {
            val tableName = it.getTableName()
            addCount = getTableRows(
                db = databaseHelper.readableDatabase,
                tableName = tableName,
                "UPDATE_METHOD = '$verifyStep'",
            )
            beforeTableRowCounts[tableName] = beforeTableRowCounts.getValue(tableName) + addCount
            addCount = getTableRows(
                db = databaseHelper.readableDatabase,
                tableName = tableName,
                "UPDATE_METHOD = '$verifyStep'",
                "UPDATE_METHOD <> CREATE_METHOD",
            )
            beforeTableRowCounts[tableName] = beforeTableRowCounts.getValue(tableName) - addCount
        }
        assertEquals(beforeTableRowCounts, afterTableRowCounts.toMap())
        Log.d("step08", "actualCount = $actualCount")
    }

    /**
     * ## step09 追加
     * ### SeedData 投入後の各テーブルに追加 ※重複キーに対しては何もしない
     * @author Masahiro Inoue
     * @since 2026-07-07
     */
    @Test
    fun step09_absertSeedData() {
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)
        databaseHelper.use { helper ->
            actualCount = absertData(helper)
        }
    }

    /**
     * ## step06 追加データ件数確認
     * ### SeedData 投入後の各テーブル件数を確認する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    @Test
    fun step10_verifyAbsertRowCounts() {
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)
        refreshTableRowCounts(databaseHelper)
        var addCount: Int
        currentTableDefinitions.forEach {
            val tableName = it.getTableName()
            addCount = getTableRows(
                db = databaseHelper.readableDatabase,
                tableName = tableName,
                "UPDATE_METHOD = '$verifyStep'",
            )
            beforeTableRowCounts[tableName] = beforeTableRowCounts.getValue(tableName) + addCount
        }
        assertEquals(beforeTableRowCounts, afterTableRowCounts.toMap())
        Log.d("step10", "actualCount = $actualCount")
    }

    /**
     * ## step07 追加・更新
     * ### SeedData 投入後の各テーブルに追加・更新
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    @Test
    fun step11_selectJoinSeedData() {
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)
        databaseHelper.use { helper ->
            helper.transaction {
                val cspSmResult = selectDataJoin1Tbl(helper)
                assertSelectDataJoin1Tbl(cspSmResult)
                AndroidTestCsvExporter.exportSelectEntityResultToDownload(
                    context = InstrumentationRegistry.getInstrumentation().targetContext,
                    stepName = testName.methodName,
                    resultName = "select_join_CSP_SM",
                    rows = cspSmResult,
                )
                val csinCpbCebResult = selectDataJoin2Tbl(helper)
                assertSelectDataJoin2Tbl(csinCpbCebResult)
                AndroidTestCsvExporter.exportSelectEntityResultToDownload(
                    context = InstrumentationRegistry.getInstrumentation().targetContext,
                    stepName = testName.methodName,
                    resultName = "select_join_CSIN_CPB_CEB",
                    rows = csinCpbCebResult,
                )
                actualCount = cspSmResult.size + csinCpbCebResult.size
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
    private fun createDatabaseHelper(
        version: Int,
        vararg entities: KClass<out TableDefinitionEntity>
    ): AndrOrmDatabaseHelper = AndrOrmDatabaseHelper(
        context = context,
        databaseName = databaseName,
        version = version,
        entities = entities.toList(),
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
            entityClass = CharacterStaticInfoV1Insert::class,
            entities = AndroidTestSeedData.characterStaticInfoList.map { entity ->
                CharacterStaticInfoV1Insert(
                    characterPk = entity.characterPk,
                    userId = entity.userId,
                    characterNo = entity.characterNo,
                    characterName = entity.characterName,
                    createMethod = testStep,
                    updateMethod = testStep,
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
                    createMethod = testStep,
                    updateMethod = testStep,
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
                    createMethod = testStep,
                    updateMethod = testStep,
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
                    createMethod = testStep,
                    updateMethod = testStep,
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
                    createMethod = testStep,
                    updateMethod = testStep,
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
                    itemPk = entity.itemPk!!,
                    createMethod = testStep,
                    updateMethod = testStep,
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
                    createMethod = testStep,
                    updateMethod = testStep,
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
                    createMethod = testStep,
                    updateMethod = testStep,
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
                    createMethod = testStep,
                    updateMethod = testStep,
                )
            },
        )
    }

    private fun updateData(
        databaseHelper: AndrOrmDatabaseHelper,
        testStep: String,
    ): Int {
        val charStatus = TableRef(CharacterStatusUpdate::class, "CS")
        val updateStr = Update(charStatus).set {
            CharacterStatusUpdate::value becomes charStatus[CharacterStatusUpdate::value] + 10
            CharacterStatusUpdate::updateMethod assign testStep
            CharacterStatusUpdate::updateTime assign LocalDateTime.now().plusDays(1)
        }.where { charStatus[CharacterStatusUpdate::statusType] eq "STR" }
        val updateInt = Update(charStatus).set {
            CharacterStatusUpdate::value becomes charStatus[CharacterStatusUpdate::value] + 5
            CharacterStatusUpdate::updateMethod assign testStep
            CharacterStatusUpdate::updateTime assign LocalDateTime.now().plusDays(1)
        }.where { charStatus[CharacterStatusUpdate::statusType] eq "INT" }
        val setItemMEff = ItemMasterUpdateEffect(
            mainEffect = "STR",
            subEffect = "HP",
            updateMethod = testStep,
            updateTime = LocalDateTime.now().plusDays(1)
        )
        val updateItemEffect = Update(ItemMasterUpdateEffect::class).set(setItemMEff).where {
            ItemMasterUpdateEffect::mainEffect eq "MP"
            ItemMasterUpdateEffect::subEffect.isNull(Unit)
        }
        val setItemMEqu = ItemMasterUpdateEquip(
            itemType = 3,
            equipableSlot = 4,
            updateMethod = testStep,
            updateTime = LocalDateTime.now().plusDays(1)
        )
        val updateItemEquip = Update(ItemMasterUpdateEquip::class).set(setItemMEqu)
            .where { ItemMasterUpdateEquip::itemType eq 6 }
        val selectSpell = Select(SpellsMasterId::class).where {
            SpellsMasterId::magicTypeId between 3 and 5
            SpellsMasterId::subEffect isNull Unit
        }
        val updateSpells = Update(SpellsMasterUpdate::class).set {
            SpellsMasterUpdate::mainEffect assign "nihil"
            SpellsMasterUpdate::subEffect assign "all status"
            SpellsMasterUpdate::updateMethod assign testStep
            SpellsMasterUpdate::updateTime assign LocalDateTime.now().plusDays(1)
        }.where { SpellsMasterUpdate::magicId inSelect selectSpell }
        return databaseHelper.transaction {
            var count = 0
            count += databaseHelper.executeDml(updateStr)
            count += databaseHelper.executeDml(updateInt)
            count += databaseHelper.executeDml(updateItemEffect)
            count += databaseHelper.executeDml(updateItemEquip)
            count += databaseHelper.executeDml(updateSpells)
            count
        }
    }

    private fun upsertData(
        databaseHelper: AndrOrmDatabaseHelper,
    ): Int {
        val upsertSeedDataList = listOf<CharacterPossessionsUpsert>(
            CharacterPossessionsUpsert(
                characterPk = 77211,
                itemPk = 11637,
                itemStatus = "EQUIP",
                createMethod = testStep,
                updateMethod = testStep,
                updateTime = LocalDateTime.now().minusDays(2)
            ),
            CharacterPossessionsUpsert(
                characterPk = 77211,
                itemPk = 11638,
                itemStatus = "EQUIP",
                createMethod = testStep,
                updateMethod = testStep,
                updateTime = LocalDateTime.now().minusDays(2)
            ),
            CharacterPossessionsUpsert(
                characterPk = 77211,
                itemPk = 11639,
                itemStatus = "EQUIP",
                createMethod = testStep,
                updateMethod = testStep,
                updateTime = LocalDateTime.now().minusDays(2)
            ),
            CharacterPossessionsUpsert(
                characterPk = 77211,
                itemPk = 11640,
                itemStatus = "EQUIP",
                createMethod = testStep,
                updateMethod = testStep,
                updateTime = LocalDateTime.now().minusDays(2)
            ),
            CharacterPossessionsUpsert(
                characterPk = 77211,
                itemPk = 11641,
                itemStatus = "EQUIP",
                createMethod = testStep,
                updateMethod = testStep,
                updateTime = LocalDateTime.now().minusDays(2)
            ),
        )
        actualCount = 0
        databaseHelper.transaction {
            upsertSeedDataList.forEach { _ ->
                val upsert = Upsert(CharacterPossessionsUpsert::class).onConflict {
                    key(CharacterPossessionsUpsert::characterPk)
                    key(CharacterPossessionsUpsert::itemPk)
                }.set {
                    CharacterPossessionsUpsert::itemStatus becomes "EQUIP"
                    CharacterPossessionsUpsert::updateMethod assign testStep
                    CharacterPossessionsUpsert::updateTime assign LocalDateTime.now()
                        .minusMonths(1)
                }.addEntities(upsertSeedDataList)
                actualCount = databaseHelper.executeDml(upsert)
            }
        }
        return actualCount
    }

    private fun absertData(
        databaseHelper: AndrOrmDatabaseHelper,
    ): Int {
        val absertSeedDataList = listOf(
            CharacterEquipAbsert(
                characterPk = 77274,
                equipSlot = 4,
                itemPk = 11637,
                createMethod = testStep,
                createTime = LocalDateTime.now(),
                updateMethod = testStep,
                updateTime = LocalDateTime.now()
            ),
            CharacterEquipAbsert(
                characterPk = 77274,
                equipSlot = 5,
                itemPk = 11638,
                createMethod = testStep,
                createTime = LocalDateTime.now(),
                updateMethod = testStep,
                updateTime = LocalDateTime.now()
            ),
            CharacterEquipAbsert(
                characterPk = 77274,
                equipSlot = 6,
                itemPk = 11639,
                createMethod = testStep,
                createTime = LocalDateTime.now(),
                updateMethod = testStep,
                updateTime = LocalDateTime.now()
            ),
            CharacterEquipAbsert(
                characterPk = 77274,
                equipSlot = 7,
                itemPk = null,
                createMethod = testStep,
                createTime = LocalDateTime.now(),
                updateMethod = testStep,
                updateTime = LocalDateTime.now()
            ),
            CharacterEquipAbsert(
                characterPk = 77274,
                equipSlot = 8,
                itemPk = 10060,
                createMethod = testStep,
                createTime = LocalDateTime.now(),
                updateMethod = testStep,
                updateTime = LocalDateTime.now()
            ),
        )
        actualCount = 0
        databaseHelper.transaction {
            absertSeedDataList.forEach { _ ->
                val absert = Absert(CharacterEquipAbsert::class).onConflict {
                    key(CharacterEquipAbsert::characterPk)
                    key(CharacterEquipAbsert::equipSlot)
                }.addEntities(absertSeedDataList)
                actualCount = databaseHelper.executeDml(absert)
            }
        }
        return actualCount
    }

    private fun selectDataJoin1Tbl(
        databaseHelper: AndrOrmDatabaseHelper,
    ): List<Map<String, SelectEntity?>> {
        actualCount = 0
        val selectJoin = Select(CharacterSpellsBase::class)
            .join(
                JoinType.LEFT,
                joinedEntity = SpellsMasterBase::class,
                on = { CharacterSpellsBase::magicId eq SpellsMasterBase::magicId }
            )
            .where {
                CharacterSpellsBase::characterPk inList listOf(77211, 77273, 77399, 78144, 9502)
            }
        return databaseHelper.transaction {
            databaseHelper.executeSelectAsEntityList(selectJoin)
        }
    }

    private fun selectDataJoin2Tbl(
        databaseHelper: AndrOrmDatabaseHelper,
    ): List<Map<String, SelectEntity?>> {
        val selectJoin = Select(CharacterStaticInfoV1Name::class)
            .join(
                JoinType.INNER,
                joinedEntity = CharacterEquipBase::class,
                on = {
                    CharacterStaticInfoV1Name::characterPk eq CharacterEquipBase::characterPk
                }
            )
            .join(
                JoinType.LEFT,
                joinedEntity = CharacterPossessionsBase::class,
                on = {
                    CharacterStaticInfoV1Name::characterPk eq CharacterPossessionsBase::characterPk
                    CharacterEquipBase::itemPk eq CharacterPossessionsBase::itemPk
                }
            )
            .where {
                CharacterStaticInfoV1Name::characterPk inList
                        listOf(77211, 77273, 77399, 78144, 77274)
            }
        actualCount = 0
        return databaseHelper.transaction {
            databaseHelper.executeSelectAsEntityList(selectJoin)
        }
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
        currentTableDefinitions.map { tableDefinition -> tableDefinition.getTableName() }
            .forEach { tableName ->
                db.execSQL("delete from ${quoteString(tableName)}")
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
     * ## 全テーブル件数更新
     * @param databaseHelper AndrOrmDatabaseHelper のインスタンス
     * @author Masahiro Inoue
     * @since 2026-07-03
     */
    private fun refreshTableRowCounts(databaseHelper: AndrOrmDatabaseHelper) {
        databaseHelper.use { helper ->
            beforeTableRowCounts = afterTableRowCounts.toMutableMap()
            afterTableRowCounts.clear()
            currentTableDefinitions.forEach {
                val tableName = it.getTableName()
                afterTableRowCounts[tableName] = getTableRows(
                    db = helper.readableDatabase,
                    tableName = tableName,
                )
            }
        }
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
    private fun getTableRows(
        db: SQLiteDatabase,
        tableName: String,
        vararg where: String,
    ): Int {
        val sql = "select count(*) from ${quoteString(tableName)}" +
                if (where.isEmpty()) {
                    EMPTY_STRING
                } else {
                    where.joinToString(" and ", " where ")
                }
        Log.d("SQL", "$testStep / $sql")
        return db.rawQuery(sql, emptyArray<String>())
            .use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }.also {
                Log.d("COUNT", "$testStep / ${quoteString(tableName)} has records = $it")
            }
    }

    private fun assertSelectDataJoin1Tbl(
        result: List<Map<String, SelectEntity?>>,
    ) {
        assertEquals(113, result.size)
        val targetCharacterPkList = listOf(77211, 77273, 77399, 78144, 9502)
        result.forEach { row ->
            val characterSpells = row.getValue("CSP_B") as CharacterSpellsBase
            val spellsMaster = row["SM_B"] as SpellsMasterBase?
            assertTrue(characterSpells.characterPk in targetCharacterPkList)
            if (spellsMaster != null) {
                assertEquals(characterSpells.magicId, spellsMaster.magicId)
            }
        }
        assertEquals(
            1,
            result.count { row ->
                row["SM_B"] == null
            },
        )
        assertTrue(
            result.any { row ->
                val characterSpells = row.getValue("CSP_B") as CharacterSpellsBase
                characterSpells.characterPk == 9502 &&
                        characterSpells.magicId == 2 &&
                        row["SM_B"] == null
            },
        )
    }

    private fun assertSelectDataJoin2Tbl(
        result: List<Map<String, SelectEntity?>>,
    ) {
        assertEquals(32, result.size)
        val expectedCountByCharacterPk = mapOf(
            77211 to 6,
            77273 to 6,
            77274 to 8,
            77399 to 6,
            78144 to 6,
        )
        val actualCountByCharacterPk = result
            .map { row ->
                val characterStaticInfo = row.getValue("CSI_N") as CharacterStaticInfoV1Name
                characterStaticInfo.characterPk
            }
            .groupingBy { characterPk -> characterPk }
            .eachCount()
        assertEquals(expectedCountByCharacterPk, actualCountByCharacterPk)
        result.forEach { row ->
            val characterStaticInfo = row.getValue("CSI_N") as CharacterStaticInfoV1Name
            val characterEquip = row.getValue("CE_B") as CharacterEquipBase
            val characterPossessions = row["CP_B"] as CharacterPossessionsBase?
            assertEquals(characterStaticInfo.characterPk, characterEquip.characterPk)
            if (characterPossessions != null) {
                assertEquals(characterEquip.characterPk, characterPossessions.characterPk)
                assertEquals(characterEquip.itemPk, characterPossessions.itemPk)
            }
        }
        assertEquals(
            2,
            result.count { row ->
                row["CP_B"] == null
            },
        )
    }

    /**
     * ## SQL 識別子クォート
     * ### SQLite 用に文字列をダブルクォートで囲む
     * @param str クォートで囲む文字列
     * @return クォート済み文字列
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun quoteString(str: String): String = "\"${str.replace("\"", "\"\"")}\""

//    private fun String.toSnakeCase(): String = replace(Regex("([a-z])([A-Z])"), "$1_$2").uppercase()
}
