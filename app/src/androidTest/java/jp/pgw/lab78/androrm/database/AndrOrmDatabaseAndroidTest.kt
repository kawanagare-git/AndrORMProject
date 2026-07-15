package jp.pgw.lab78.androrm.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableAlias
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableName
import jp.pgw.lab78.androrm.common.database.SupportFunction.toSnakeCase
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Function
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction
import jp.pgw.lab78.androrm.common.dml.interfaces.*
import jp.pgw.lab78.androrm.database.entities.*
import jp.pgw.lab78.androrm.database.entities.absert.CharacterEquipAbsert
import jp.pgw.lab78.androrm.database.entities.delete.*
import jp.pgw.lab78.androrm.database.entities.insert.*
import jp.pgw.lab78.androrm.database.entities.resource.AndroidTestSeedData
import jp.pgw.lab78.androrm.database.entities.select.*
import jp.pgw.lab78.androrm.database.entities.update.*
import jp.pgw.lab78.androrm.database.entities.upsert.*
import jp.pgw.lab78.androrm.database.interfaces.plus
import jp.pgw.lab78.androrm.database.queryparts.JoinType
import jp.pgw.lab78.androrm.database.queryparts.OnConflictClauseBuilder
import jp.pgw.lab78.androrm.database.queryparts.UpsertSetClauseBuilder
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

        /** 更新・Upsert 対象割合 */
        private const val UPDATE_TARGET_PERCENT = 30

        /** SQLite の旧バインド変数上限 999 を超えない Upsert 件数 */
        private const val UPSERT_BATCH_SIZE = 100

        /** DML 系実行時カウント */
        private var actualCount = 0L

        private var testStep: String = EMPTY_STRING
        private var verifyStep: String = EMPTY_STRING

        /** 各テーブルの件数を保持（単純件数） */
        private var beforeTableRowCounts: MutableMap<String, Long> = mutableMapOf()
        private var afterTableRowCounts: MutableMap<String, Long> = mutableMapOf()
        private var deleteTableRowCounts: MutableMap<String, Long> = mutableMapOf()

        private var isUpgrade = false
        private var version = if (isUpgrade) 2 else 1
        private var index = if (isUpgrade) 1 else 0

        /** JOIN SELECT 結果：CharacterSpells + SpellsMaster */
        private var cspSmJoinResult: List<Map<String, SelectEntity?>> = emptyList()

        /** JOIN SELECT 結果：CharacterStaticInfo + CharacterPossessions + CharacterEquip */
        private var csinCpbCebJoinResult: List<Map<String, SelectEntity?>> = emptyList()

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
                afterTableRowCounts[tableName] = expectedCount.toLong()
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
            ), arrayOf(
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
            val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)
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
            actualCount = updateData(helper, testStep).toLong()
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
        var expectedCount = 0L
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
            actualCount = upsertData(helper).toLong()
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
        var addCount: Long
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
            actualCount = absertData(helper).toLong()
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
        var addCount: Long
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
     * ## step11 JOIN SELECT 実行
     * ### JOIN SELECT を実行し、結果を CSV へ出力する
     * @author Masahiro Inoue
     * @since 2026-07-08
     */
    @Test
    fun step11_selectJoinSeedData() {
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)
        databaseHelper.use { helper ->
            helper.transaction {
                cspSmJoinResult = selectDataJoin1Tbl(helper)
                AndroidTestCsvExporter.exportSelectEntityResultToDownload(
                    context = InstrumentationRegistry.getInstrumentation().targetContext,
                    stepName = testName.methodName,
                    resultName = "select_join_CSP_SM",
                    rows = cspSmJoinResult,
                )
                csinCpbCebJoinResult = selectDataJoin2Tbl(helper)
                AndroidTestCsvExporter.exportSelectEntityResultToDownload(
                    context = InstrumentationRegistry.getInstrumentation().targetContext,
                    stepName = testName.methodName,
                    resultName = "select_join_CSIN_CPB_CEB",
                    rows = csinCpbCebJoinResult,
                )
                actualCount = (cspSmJoinResult.size + csinCpbCebJoinResult.size).toLong()
            }
        }
    }

    /**
     * ## step12 JOIN SELECT 結果検証
     * ### step11 で取得した JOIN SELECT 結果を検証する
     * @author Masahiro Inoue
     * @since 2026-07-08
     */
    @Test
    fun step12_verifySelectJoin() {
        assertTrue(
            "step11_selectJoinSeedData の結果がありません。",
            cspSmJoinResult.isNotEmpty(),
        )
        assertTrue(
            "step11_selectJoinSeedData の結果がありません。",
            csinCpbCebJoinResult.isNotEmpty(),
        )
        assertSelectDataJoin1Tbl(cspSmJoinResult)
        assertSelectDataJoin2Tbl(csinCpbCebJoinResult)
        actualCount = (cspSmJoinResult.size + csinCpbCebJoinResult.size).toLong()
    }

    /**
     * ## step13 削除
     * ### SeedData 投入後の各テーブルへ削除
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    @Test
    fun step13_deleteSeedData() {
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)
        deleteTargetStep(databaseHelper, "step03")
        refreshTableRowCounts(databaseHelper)
        databaseHelper.use { helper ->
            actualCount = deleteData(helper).toLong()
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
        version: Int, vararg entities: KClass<out TableDefinitionEntity>
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

    /**
     * ## 更新対象セグメント取得
     * ### リストを30%単位で分割し、指定位置の対象データを取得する
     * @param segmentIndex 0: step05、1: step07
     */
    private fun <T> List<T>.targetSegment(segmentIndex: Int): List<T> {
        val targetCount = size * UPDATE_TARGET_PERCENT / 100
        return drop(targetCount * segmentIndex).take(targetCount)
    }

    /** step05 の ITEM_MASTER 更新対象 */
    private fun step05ItemMasterTargets(): List<ItemMaster> {
        val targetCount = AndroidTestSeedData.itemMasterList.size * UPDATE_TARGET_PERCENT / 100
        val existingTargets = AndroidTestSeedData.itemMasterList.filter { entity ->
            (entity.mainEffect == "MP" && entity.subEffect == null) || entity.itemType == 6
        }
        val existingItemPks = existingTargets.map { entity -> entity.itemPk }.toSet()
        val additionalTargets =
            AndroidTestSeedData.itemMasterList.filter { entity -> entity.itemPk !in existingItemPks }
                .take((targetCount - existingTargets.size).coerceAtLeast(0))
        return existingTargets + additionalTargets
    }

    /** step05 の SPELLS_MASTER 更新対象 */
    private fun step05SpellsMasterTargets(): List<SpellsMaster> {
        val targetCount = AndroidTestSeedData.spellsMasterList.size * UPDATE_TARGET_PERCENT / 100
        val existingTargets = AndroidTestSeedData.spellsMasterList.filter { entity ->
            entity.magicTypeId in 3..5 && entity.subEffect == null
        }
        val existingMagicIds = existingTargets.map { entity -> entity.magicId }.toSet()
        val additionalTargets =
            AndroidTestSeedData.spellsMasterList.filter { entity -> entity.magicId !in existingMagicIds }
                .take((targetCount - existingTargets.size).coerceAtLeast(0))
        return existingTargets + additionalTargets
    }

    private fun updateData(
        databaseHelper: AndrOrmDatabaseHelper,
        testStep: String,
    ): Int {
        val updateTime = LocalDateTime.now()
        val step05CharacterPks =
            AndroidTestSeedData.characterStaticInfoList.targetSegment(segmentIndex = 0)
                .map { entity -> entity.characterPk }
        val step05WeaponMasteryCharacterPks =
            AndroidTestSeedData.weaponMasteryList.targetSegment(segmentIndex = 0)
                .map { entity -> entity.characterPk }
        val step05MagicTypeMasteryCharacterPks =
            AndroidTestSeedData.magicTypeMasteryList.targetSegment(segmentIndex = 0)
                .map { entity -> entity.characterPk }

        val updateCharacterStaticInfo = Update(CharacterStaticInfoV1UpdateAudit::class).set {
            CharacterStaticInfoV1UpdateAudit::updateMethod assign testStep
            CharacterStaticInfoV1UpdateAudit::updateTime assign updateTime
        }.where {
            CharacterStaticInfoV1UpdateAudit::characterPk inList step05CharacterPks
        }

        val charStatus = TableRef(CharacterStatusUpdate::class, "CS")
        val updateStr = Update(charStatus).set {
            CharacterStatusUpdate::value becomes charStatus[CharacterStatusUpdate::value] + 10
            CharacterStatusUpdate::updateMethod assign testStep
            CharacterStatusUpdate::updateTime assign updateTime
        }.where { charStatus[CharacterStatusUpdate::statusType] eq "STR" }
        val updateInt = Update(charStatus).set {
            CharacterStatusUpdate::value becomes charStatus[CharacterStatusUpdate::value] + 5
            CharacterStatusUpdate::updateMethod assign testStep
            CharacterStatusUpdate::updateTime assign updateTime
        }.where { charStatus[CharacterStatusUpdate::statusType] eq "INT" }
        val updateVit = Update(charStatus).set {
            CharacterStatusUpdate::value becomes charStatus[CharacterStatusUpdate::value] + 3
            CharacterStatusUpdate::updateMethod assign testStep
            CharacterStatusUpdate::updateTime assign updateTime
        }.where { charStatus[CharacterStatusUpdate::statusType] eq "VIT" }

        val setItemMEff = ItemMasterUpdateEffect(
            mainEffect = "STR",
            subEffect = "HP",
            updateMethod = testStep,
            updateTime = updateTime,
        )
        val updateItemEffect = Update(ItemMasterUpdateEffect::class).set(setItemMEff).where {
            ItemMasterUpdateEffect::mainEffect eq "MP"
            ItemMasterUpdateEffect::subEffect.isNull(Unit)
        }
        val setItemMEqu = ItemMasterUpdateEquip(
            itemType = 3,
            equipableSlot = 4,
            updateMethod = testStep,
            updateTime = updateTime,
        )
        val updateItemEquip = Update(ItemMasterUpdateEquip::class).set(setItemMEqu)
            .where { ItemMasterUpdateEquip::itemType eq 6 }
        val existingStep05ItemPks = AndroidTestSeedData.itemMasterList.filter { entity ->
            (entity.mainEffect == "MP" && entity.subEffect == null) || entity.itemType == 6
        }.map { entity -> entity.itemPk }.toSet()
        val additionalStep05ItemPks =
            step05ItemMasterTargets().filter { entity -> entity.itemPk !in existingStep05ItemPks }
                .map { entity -> entity.itemPk }
        val updateAdditionalItems = Update(ItemMasterUpdateAudit::class).set {
            ItemMasterUpdateAudit::updateMethod assign testStep
            ItemMasterUpdateAudit::updateTime assign updateTime
        }.where { ItemMasterUpdateAudit::itemPk inList additionalStep05ItemPks }

        val selectSpell = Select(SpellsMasterId::class).where {
            SpellsMasterId::magicTypeId between 3 and 5
            SpellsMasterId::subEffect isNull Unit
        }
        val updateSpells = Update(SpellsMasterUpdate::class).set {
            SpellsMasterUpdate::mainEffect assign "nihil"
            SpellsMasterUpdate::subEffect assign "all status"
            SpellsMasterUpdate::updateMethod assign testStep
            SpellsMasterUpdate::updateTime assign updateTime
        }.where { SpellsMasterUpdate::magicId inSelect selectSpell }
        val existingStep05MagicIds = AndroidTestSeedData.spellsMasterList.filter { entity ->
            entity.magicTypeId in 3..5 && entity.subEffect == null
        }.map { entity -> entity.magicId }.toSet()
        val additionalStep05MagicIds =
            step05SpellsMasterTargets().filter { entity -> entity.magicId !in existingStep05MagicIds }
                .map { entity -> entity.magicId }
        val updateAdditionalSpells = Update(SpellsMasterUpdate::class).set {
            SpellsMasterUpdate::updateMethod assign testStep
            SpellsMasterUpdate::updateTime assign updateTime
        }.where { SpellsMasterUpdate::magicId inList additionalStep05MagicIds }

        val updateCharacterPossessions = Update(CharacterPossessionsUpdateAudit::class).set {
            CharacterPossessionsUpdateAudit::updateMethod assign testStep
            CharacterPossessionsUpdateAudit::updateTime assign updateTime
        }.where {
            CharacterPossessionsUpdateAudit::characterPk inList step05CharacterPks
        }
        val setItem = "11567"
        val cbb = TableRef(CharacterPossessionsBase::class, "CPB")
        val updateCharacterEquip =
            Update(CharacterEquipUpdateAudit::class)
                .set {
                    CharacterEquipUpdateAudit::itemPk becomes setItem
                    CharacterEquipUpdateAudit::updateMethod assign testStep
                    CharacterEquipUpdateAudit::updateTime assign updateTime
                }.where {
                    CharacterEquipUpdateAudit::characterPk inList step05CharacterPks
                    CharacterEquipUpdateAudit::equipSlot eq 2
                    exists(cbb) {
                        cbb[CharacterPossessionsBase::itemPk] eq setItem
                    }
                }
        val updateWeaponMastery = Update(WeaponMasteryUpdateAudit::class).set {
            WeaponMasteryUpdateAudit::updateMethod assign testStep
            WeaponMasteryUpdateAudit::updateTime assign updateTime
        }.where {
            WeaponMasteryUpdateAudit::characterPk inList step05WeaponMasteryCharacterPks
        }
        val updateMagicTypeMastery = Update(MagicTypeMasteryUpdateAudit::class).set {
            MagicTypeMasteryUpdateAudit::updateMethod assign testStep
            MagicTypeMasteryUpdateAudit::updateTime assign updateTime
        }.where {
            MagicTypeMasteryUpdateAudit::characterPk inList step05MagicTypeMasteryCharacterPks
        }
        val updateCharacterSpells = Update(CharacterSpellsUpdateAudit::class).set {
            CharacterSpellsUpdateAudit::updateMethod assign testStep
            CharacterSpellsUpdateAudit::updateTime assign updateTime
        }.where {
            CharacterSpellsUpdateAudit::characterPk inList step05CharacterPks
        }

        return databaseHelper.transaction {
            var count = 0
            count += databaseHelper.executeDml(updateCharacterStaticInfo)
            count += databaseHelper.executeDml(updateStr)
            count += databaseHelper.executeDml(updateInt)
            count += databaseHelper.executeDml(updateVit)
            count += databaseHelper.executeDml(updateItemEffect)
            count += databaseHelper.executeDml(updateItemEquip)
            count += databaseHelper.executeDml(updateAdditionalItems)
            count += databaseHelper.executeDml(updateSpells)
            count += databaseHelper.executeDml(updateAdditionalSpells)
            count += databaseHelper.executeDml(updateCharacterPossessions)
            count += databaseHelper.executeDml(updateCharacterEquip)
            count += databaseHelper.executeDml(updateWeaponMastery)
            count += databaseHelper.executeDml(updateMagicTypeMastery)
            count += databaseHelper.executeDml(updateCharacterSpells)
            count
        }
    }

    /**
     * ## Upsert 分割実行
     * ### SQLite のバインド変数上限を超えない件数に分割して実行する
     */
    private fun <T : UpsertEntity> executeUpsertInBatches(
        databaseHelper: AndrOrmDatabaseHelper,
        entityClass: KClass<out T>,
        entities: List<T>,
        onConflict: OnConflictClauseBuilder<T>.() -> Unit,
        set: UpsertSetClauseBuilder<T>.() -> Unit,
    ): Int {
        var count = 0
        entities.chunked(UPSERT_BATCH_SIZE).forEach { batch ->
            val upsert = Upsert(entityClass).onConflict(onConflict).set(set).addEntities(batch)
            count += databaseHelper.executeDml(upsert)
        }
        return count
    }

    private fun upsertData(
        databaseHelper: AndrOrmDatabaseHelper,
    ): Int {
        val updateTime = LocalDateTime.now()
        val step07CharacterPks =
            AndroidTestSeedData.characterStaticInfoList.targetSegment(segmentIndex = 1)
                .map { entity -> entity.characterPk }.toSet()
        val step05ItemPks = step05ItemMasterTargets().map { entity -> entity.itemPk }.toSet()
        val step07ItemTargets =
            AndroidTestSeedData.itemMasterList.filter { entity -> entity.itemPk !in step05ItemPks }
                .take(AndroidTestSeedData.itemMasterList.size * UPDATE_TARGET_PERCENT / 100)
        val step05MagicIds = step05SpellsMasterTargets().map { entity -> entity.magicId }.toSet()
        val step07SpellsMasterTargets =
            AndroidTestSeedData.spellsMasterList.filter { entity -> entity.magicId !in step05MagicIds }
                .take(AndroidTestSeedData.spellsMasterList.size * UPDATE_TARGET_PERCENT / 100)
        val step07StatusTypes = setOf("HP", "MP", "SP")

        val characterStaticInfoList =
            AndroidTestSeedData.characterStaticInfoList.filter { entity -> entity.characterPk in step07CharacterPks }
                .map { entity ->
                    CharacterStaticInfoV1Upsert(
                        characterPk = entity.characterPk,
                        userId = entity.userId,
                        characterNo = entity.characterNo,
                        characterName = entity.characterName,
                        createMethod = testStep,
                        updateMethod = testStep,
                        updateTime = updateTime,
                    )
                }
        val itemMasterList = step07ItemTargets.map { entity ->
            ItemMasterUpsert(
                itemPk = entity.itemPk,
                itemType = entity.itemType,
                itemName = entity.itemName,
                mainEffect = entity.mainEffect,
                subEffect = entity.subEffect,
                equipableSlot = entity.equipableSlot,
                createMethod = testStep,
                updateMethod = testStep,
                updateTime = updateTime,
            )
        }
        val spellsMasterList = step07SpellsMasterTargets.map { entity ->
            SpellsMasterUpsert(
                magicId = entity.magicId,
                magicTypeId = entity.magicTypeId,
                magicName = entity.magicName,
                mainEffect = entity.mainEffect,
                subEffect = entity.subEffect,
                createMethod = testStep,
                updateMethod = testStep,
                updateTime = updateTime,
            )
        }
        val characterStatusList =
            AndroidTestSeedData.characterStatusList.filter { entity -> entity.statusType in step07StatusTypes }
                .map { entity ->
                    CharacterStatusUpsert(
                        characterPk = entity.characterPk,
                        statusType = entity.statusType,
                        value = entity.value,
                        createMethod = testStep,
                        updateMethod = testStep,
                        updateTime = updateTime,
                    )
                }
        val characterPossessionsList =
            AndroidTestSeedData.characterPossessionsList.filter { entity -> entity.characterPk in step07CharacterPks }
                .map { entity ->
                    CharacterPossessionsUpsert(
                        characterPk = entity.characterPk,
                        itemPk = entity.itemPk,
                        itemStatus = entity.itemStatus,
                        createMethod = testStep,
                        updateMethod = testStep,
                        updateTime = updateTime,
                    )
                }
        val characterEquipList =
            AndroidTestSeedData.characterEquipList.filter { entity -> entity.characterPk in step07CharacterPks }
                .map { entity ->
                    CharacterEquipUpsert(
                        characterPk = entity.characterPk,
                        equipSlot = entity.equipSlot,
                        itemPk = entity.itemPk,
                        createMethod = testStep,
                        updateMethod = testStep,
                        updateTime = updateTime,
                    )
                }
        val weaponMasteryList =
            AndroidTestSeedData.weaponMasteryList.targetSegment(segmentIndex = 1).map { entity ->
                WeaponMasteryUpsert(
                    characterPk = entity.characterPk,
                    weaponTypeId = entity.weaponTypeId,
                    mastery = entity.mastery,
                    createMethod = testStep,
                    updateMethod = testStep,
                    updateTime = updateTime,
                )
            }
        val magicTypeMasteryList =
            AndroidTestSeedData.magicTypeMasteryList.targetSegment(segmentIndex = 1).map { entity ->
                MagicTypeMasteryUpsert(
                    characterPk = entity.characterPk,
                    magicTypeMastery = entity.magicTypeMastery,
                    mastery = entity.mastery,
                    createMethod = testStep,
                    updateMethod = testStep,
                    updateTime = updateTime,
                )
            }
        val characterSpellsList =
            AndroidTestSeedData.characterSpellsList.filter { entity -> entity.characterPk in step07CharacterPks }
                .map { entity ->
                    CharacterSpellsUpsert(
                        characterPk = entity.characterPk,
                        magicId = entity.magicId,
                        createMethod = testStep,
                        updateMethod = testStep,
                        updateTime = updateTime,
                    )
                }

        return databaseHelper.transaction {
            var count = 0
            count += executeUpsertInBatches(
                databaseHelper = databaseHelper,
                entityClass = CharacterStaticInfoV1Upsert::class,
                entities = characterStaticInfoList,
                onConflict = {
                    key(CharacterStaticInfoV1Upsert::characterPk)
                },
                set = {
                    CharacterStaticInfoV1Upsert::userId assign excluded(CharacterStaticInfoV1Upsert::userId)
                    CharacterStaticInfoV1Upsert::characterNo assign excluded(
                        CharacterStaticInfoV1Upsert::characterNo
                    )
                    CharacterStaticInfoV1Upsert::characterName assign excluded(
                        CharacterStaticInfoV1Upsert::characterName
                    )
                    CharacterStaticInfoV1Upsert::updateMethod assign excluded(
                        CharacterStaticInfoV1Upsert::updateMethod
                    )
                    CharacterStaticInfoV1Upsert::updateTime assign excluded(
                        CharacterStaticInfoV1Upsert::updateTime
                    )
                },
            )
            count += executeUpsertInBatches(
                databaseHelper = databaseHelper,
                entityClass = ItemMasterUpsert::class,
                entities = itemMasterList,
                onConflict = {
                    key(ItemMasterUpsert::itemPk)
                },
                set = {
                    ItemMasterUpsert::itemType assign excluded(ItemMasterUpsert::itemType)
                    ItemMasterUpsert::itemName assign excluded(ItemMasterUpsert::itemName)
                    ItemMasterUpsert::mainEffect assign excluded(ItemMasterUpsert::mainEffect)
                    ItemMasterUpsert::subEffect assign excluded(ItemMasterUpsert::subEffect)
                    ItemMasterUpsert::equipableSlot assign excluded(ItemMasterUpsert::equipableSlot)
                    ItemMasterUpsert::updateMethod assign excluded(ItemMasterUpsert::updateMethod)
                    ItemMasterUpsert::updateTime assign excluded(ItemMasterUpsert::updateTime)
                },
            )
            count += executeUpsertInBatches(
                databaseHelper = databaseHelper,
                entityClass = SpellsMasterUpsert::class,
                entities = spellsMasterList,
                onConflict = {
                    key(SpellsMasterUpsert::magicId)
                },
                set = {
                    SpellsMasterUpsert::magicTypeId assign excluded(SpellsMasterUpsert::magicTypeId)
                    SpellsMasterUpsert::magicName assign excluded(SpellsMasterUpsert::magicName)
                    SpellsMasterUpsert::mainEffect assign excluded(SpellsMasterUpsert::mainEffect)
                    SpellsMasterUpsert::subEffect assign excluded(SpellsMasterUpsert::subEffect)
                    SpellsMasterUpsert::updateMethod assign excluded(SpellsMasterUpsert::updateMethod)
                    SpellsMasterUpsert::updateTime assign excluded(SpellsMasterUpsert::updateTime)
                },
            )
            count += executeUpsertInBatches(
                databaseHelper = databaseHelper,
                entityClass = CharacterStatusUpsert::class,
                entities = characterStatusList,
                onConflict = {
                    key(CharacterStatusUpsert::characterPk)
                    key(CharacterStatusUpsert::statusType)
                },
                set = {
                    CharacterStatusUpsert::value assign excluded(CharacterStatusUpsert::value)
                    CharacterStatusUpsert::updateMethod assign excluded(CharacterStatusUpsert::updateMethod)
                    CharacterStatusUpsert::updateTime assign excluded(CharacterStatusUpsert::updateTime)
                },
            )
            count += executeUpsertInBatches(
                databaseHelper = databaseHelper,
                entityClass = CharacterPossessionsUpsert::class,
                entities = characterPossessionsList,
                onConflict = {
                    key(CharacterPossessionsUpsert::characterPk)
                    key(CharacterPossessionsUpsert::itemPk)
                },
                set = {
                    CharacterPossessionsUpsert::itemStatus assign excluded(
                        CharacterPossessionsUpsert::itemStatus
                    )
                    CharacterPossessionsUpsert::updateMethod assign excluded(
                        CharacterPossessionsUpsert::updateMethod
                    )
                    CharacterPossessionsUpsert::updateTime assign excluded(
                        CharacterPossessionsUpsert::updateTime
                    )
                },
            )
            count += executeUpsertInBatches(
                databaseHelper = databaseHelper,
                entityClass = CharacterEquipUpsert::class,
                entities = characterEquipList,
                onConflict = {
                    key(CharacterEquipUpsert::characterPk)
                    key(CharacterEquipUpsert::equipSlot)
                },
                set = {
                    CharacterEquipUpsert::itemPk assign excluded(CharacterEquipUpsert::itemPk)
                    CharacterEquipUpsert::updateMethod assign excluded(CharacterEquipUpsert::updateMethod)
                    CharacterEquipUpsert::updateTime assign excluded(CharacterEquipUpsert::updateTime)
                },
            )
            count += executeUpsertInBatches(
                databaseHelper = databaseHelper,
                entityClass = WeaponMasteryUpsert::class,
                entities = weaponMasteryList,
                onConflict = {
                    key(WeaponMasteryUpsert::characterPk)
                    key(WeaponMasteryUpsert::weaponTypeId)
                },
                set = {
                    WeaponMasteryUpsert::mastery assign excluded(WeaponMasteryUpsert::mastery)
                    WeaponMasteryUpsert::updateMethod assign excluded(WeaponMasteryUpsert::updateMethod)
                    WeaponMasteryUpsert::updateTime assign excluded(WeaponMasteryUpsert::updateTime)
                },
            )
            count += executeUpsertInBatches(
                databaseHelper = databaseHelper,
                entityClass = MagicTypeMasteryUpsert::class,
                entities = magicTypeMasteryList,
                onConflict = {
                    key(MagicTypeMasteryUpsert::characterPk)
                    key(MagicTypeMasteryUpsert::magicTypeMastery)
                },
                set = {
                    MagicTypeMasteryUpsert::mastery assign excluded(MagicTypeMasteryUpsert::mastery)
                    MagicTypeMasteryUpsert::updateMethod assign excluded(MagicTypeMasteryUpsert::updateMethod)
                    MagicTypeMasteryUpsert::updateTime assign excluded(MagicTypeMasteryUpsert::updateTime)
                },
            )
            count += executeUpsertInBatches(
                databaseHelper = databaseHelper,
                entityClass = CharacterSpellsUpsert::class,
                entities = characterSpellsList,
                onConflict = {
                    key(CharacterSpellsUpsert::characterPk)
                    key(CharacterSpellsUpsert::magicId)
                },
                set = {
                    CharacterSpellsUpsert::updateMethod assign excluded(CharacterSpellsUpsert::updateMethod)
                    CharacterSpellsUpsert::updateTime assign excluded(CharacterSpellsUpsert::updateTime)
                },
            )
            count
        }
    }

    private fun absertData(
        databaseHelper: AndrOrmDatabaseHelper,
    ): Long {
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
                actualCount = databaseHelper.executeDml(absert).toLong()
            }
        }
        return actualCount
    }

    private fun selectDataJoin1Tbl(
        databaseHelper: AndrOrmDatabaseHelper,
    ): List<Map<String, SelectEntity?>> {
        actualCount = 0
        val selectJoin = Select(CharacterSpellsBase::class).join(
            JoinType.LEFT,
            joinedEntity = SpellsMasterBase::class,
            on = { CharacterSpellsBase::magicId eq SpellsMasterBase::magicId }).where {
            CharacterSpellsBase::characterPk inList listOf(77211, 77273, 77399, 78144, 9502)
        }.order {
            CharacterSpellsBase::characterPk.asc
            CharacterSpellsBase::magicId.nullsLast
        }
        return databaseHelper.transaction {
            databaseHelper.executeSelectAsEntityList(selectJoin)
        }.also {
            Log.d(testStep, selectJoin.queryString)
            Log.d(testStep, selectJoin.bindValues.joinToString())
        }
    }

    private fun selectDataJoin2Tbl(
        databaseHelper: AndrOrmDatabaseHelper,
    ): List<Map<String, SelectEntity?>> {
        val selectJoin = Select(CharacterStaticInfoV1Name::class).join(
            JoinType.INNER, joinedEntity = CharacterEquipBase::class, on = {
                CharacterStaticInfoV1Name::characterPk eq CharacterEquipBase::characterPk
            }).join(
            JoinType.LEFT, joinedEntity = CharacterPossessionsBase::class, on = {
                CharacterStaticInfoV1Name::characterPk eq CharacterPossessionsBase::characterPk
                CharacterEquipBase::itemPk eq CharacterPossessionsBase::itemPk
            }).where {
            CharacterStaticInfoV1Name::characterPk inList listOf(
                77211,
                77273,
                77399,
                78144,
                77274
            )
        }.order {
            CharacterStaticInfoV1Name::characterPk.asc
            CharacterEquipBase::itemPk.nullsLast
        }
        actualCount = 0
        return databaseHelper.transaction {
            databaseHelper.executeSelectAsEntityList(selectJoin)
        }.also {
            Log.d(testStep, selectJoin.queryString)
            Log.d(testStep, selectJoin.bindValues.joinToString())
        }
    }

    private fun deleteData(
        databaseHelper: AndrOrmDatabaseHelper,
    ): Long {
        actualCount = 0
        val deleteTables = listOf(
            Delete(CharacterEquipDelete::class).where { CharacterEquipDelete::updateMethod eq "step03" },
            Delete(CharacterPossessionsDelete::class).where { CharacterPossessionsDelete::updateMethod eq "step03" },
            Delete(CharacterSpellsDelete::class).where { CharacterSpellsDelete::updateMethod eq "step03" },
            Delete(CharacterStaticInfoV1Delete::class).where { CharacterStaticInfoV1Delete::updateMethod eq "step03" },
            Delete(CharacterStatusDelete::class).where { CharacterStatusDelete::updateMethod eq "step03" },
            Delete(ItemMasterDelete::class).where { ItemMasterDelete::updateMethod eq "step03" },
            Delete(MagicTypeMasteryDelete::class).where { MagicTypeMasteryDelete::updateMethod eq "step03" },
            Delete(SpellsMasterDelete::class).where { SpellsMasterDelete::updateMethod eq "step03" },
            Delete(WeaponMasteryDelete::class).where { WeaponMasteryDelete::updateMethod eq "step03" },
        )
        databaseHelper.transaction {
            deleteTables.forEach {
                actualCount += databaseHelper.executeDml(it)
            }
        }
        return actualCount
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
    ): Long {
        val sql = "select count(*) from ${quoteString(tableName)}" + if (where.isEmpty()) {
            EMPTY_STRING
        } else {
            where.joinToString(" and ", " where ")
        }
        Log.d("SQL", "$testStep / $sql")
        return db.rawQuery(sql, emptyArray<String>()).use { cursor ->
            cursor.moveToFirst()
            cursor.getLong(0)
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
                characterSpells.characterPk == 9502 && characterSpells.magicId == 2 && row["SM_B"] == null
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
        val actualCountByCharacterPk = result.map { row ->
            val characterStaticInfo = row.getValue("CSI_N") as CharacterStaticInfoV1Name
            characterStaticInfo.characterPk
        }.groupingBy { characterPk -> characterPk }.eachCount()
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

    /** 削除エンティティ定義*/
    val deleteEntityList = listOf<KClass<out DeleteCountEntity>>(
        CharacterEquipDeleteCount::class,
        CharacterPossessionsDeleteCount::class,
        CharacterSpellsDeleteCount::class,
        CharacterStaticInfoDeleteCount::class,
        CharacterStatusDeleteCount::class,
        ItemMasterDeleteCount::class,
        MagicTypeMasteryDeleteCount::class,
        SpellsMasterDeleteCount::class,
        WeaponMasteryDeleteCount::class,
    )

    /**
     * ## 削除対象ステップカウント
     * ### 削除対象のステップ（更新機能）の件数を数える
     * @param databaseHelper AndrOrmDatabaseHelper のインスタンス
     * @param step 削除対象機能
     * @author Masahiro Inoue
     * @since 2026-07-14
     */
    private fun deleteTargetStep(databaseHelper: AndrOrmDatabaseHelper, step: String) {
        deleteEntityList.forEach {
            val targetTable = TableRef(it, it.getTableAlias())
            val deleteCount =
                Select(targetTable).where { targetTable[DeleteCountEntity::updateMethod] eq step }
            val rowCount = databaseHelper.executeSelectAsMapList(deleteCount)[0]
            deleteTableRowCounts[it.getTableName()] = rowCount.getValue("DC_COUNT") as Long
        }
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

    interface DeleteCountEntity : SelectEntity, DeleteEntity {
        val updateMethod: String
    }

    @Table("CHARACTER_EQUIP", "DC")
    data class CharacterEquipDeleteCount(
        @Function(columnFunction = ColumnFunction.COUNT_ALL, alias = "COUNT") val count: Long,
        @Column(hideFromSelect = true) override val updateMethod: String,
    ) : DeleteCountEntity

    @Table("CHARACTER_POSSESSIONS", "DC")
    data class CharacterPossessionsDeleteCount(
        @Function(columnFunction = ColumnFunction.COUNT_ALL, alias = "COUNT") val count: Long,
        @Column(hideFromSelect = true) override val updateMethod: String,
    ) : DeleteCountEntity

    @Table("CHARACTER_SPELLS", "DC")
    data class CharacterSpellsDeleteCount(
        @Function(columnFunction = ColumnFunction.COUNT_ALL, alias = "COUNT") val count: Long,
        @Column(hideFromSelect = true) override val updateMethod: String,
    ) : DeleteCountEntity

    @Table("CHARACTER_STATIC_INFO", "DC")
    data class CharacterStaticInfoDeleteCount(
        @Function(columnFunction = ColumnFunction.COUNT_ALL, alias = "COUNT") val count: Long,
        @Column(hideFromSelect = true) override val updateMethod: String,
    ) : DeleteCountEntity

    @Table("CHARACTER_STATUS", "DC")
    data class CharacterStatusDeleteCount(
        @Function(columnFunction = ColumnFunction.COUNT_ALL, alias = "COUNT") val count: Long,
        @Column(hideFromSelect = true) override val updateMethod: String,
    ) : DeleteCountEntity

    @Table("ITEM_MASTER", "DC")
    data class ItemMasterDeleteCount(
        @Function(columnFunction = ColumnFunction.COUNT_ALL, alias = "COUNT") val count: Long,
        @Column(hideFromSelect = true) override val updateMethod: String,
    ) : DeleteCountEntity

    @Table("MAGIC_TYPE_MASTERY", "DC")
    data class MagicTypeMasteryDeleteCount(
        @Function(columnFunction = ColumnFunction.COUNT_ALL, alias = "COUNT") val count: Long,
        @Column(hideFromSelect = true) override val updateMethod: String,
    ) : DeleteCountEntity

    @Table("SPELLS_MASTER", "DC")
    data class SpellsMasterDeleteCount(
        @Function(columnFunction = ColumnFunction.COUNT_ALL, alias = "COUNT") val count: Long,
        @Column(hideFromSelect = true) override val updateMethod: String,
    ) : DeleteCountEntity

    @Table("WEAPON_MASTERY", "DC")
    data class WeaponMasteryDeleteCount(
        @Function(columnFunction = ColumnFunction.COUNT_ALL, alias = "COUNT") val count: Long,
        @Column(hideFromSelect = true) override val updateMethod: String,
    ) : DeleteCountEntity
}
