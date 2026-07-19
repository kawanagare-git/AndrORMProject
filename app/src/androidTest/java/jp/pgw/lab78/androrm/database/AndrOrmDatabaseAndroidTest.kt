package jp.pgw.lab78.androrm.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.pgw.lab78.androrm.common.Constants.D_QUOTE
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
    /**
     * ## Android結合テスト共通状態
     * ### 全テストStepで共有する設定値、件数および検証結果を保持する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    companion object {
        /** ログ出力有効フラグ */
        private const val LOGGING_ENABLED_PROPERTY = "androrm.logging.enabled"

        /** 更新・Upsert 対象割合 */
        private const val UPDATE_TARGET_PERCENT = 30

        /** SQLite の旧バインド変数上限 999 を超えない Upsert 件数 */
        private const val UPSERT_BATCH_SIZE = 100

        /** step19で各テーブルへ追加を試行する件数 */
        private const val SAVEPOINT_INSERT_COUNT = 10

        /** step19のマスタ登録後に作成するsavepoint名 */
        private const val AFTER_MASTER_INSERT_SAVEPOINT = "STEP19_AFTER_MASTER_INSERT"

        /** step19用アイテム主キー基準値 */
        private const val SAVEPOINT_ITEM_PK_BASE = 300_000

        /** step19用魔法主キー基準値 */
        private const val SAVEPOINT_MAGIC_ID_BASE = 400_000

        /** step19用キャラクタ主キー基準値 */
        private const val SAVEPOINT_CHARACTER_PK_BASE = 500_000

        /** DML 系実行時カウント */
        private var actualCount = 0L

        private var testStep: String = EMPTY_STRING
        private var verifyStep: String = EMPTY_STRING

        /** 各テーブルの件数を保持（単純件数） */
        private var beforeTableRowCounts: MutableMap<String, Long> = mutableMapOf()
        private var afterTableRowCounts: MutableMap<String, Long> = mutableMapOf()
        private var deleteTableRowCounts: MutableMap<String, Long> = mutableMapOf()

        /** アップグレード前の各テーブル件数 */
        private var preUpgradeTableRowCounts: Map<String, Long> = emptyMap()

        /** step19実行前の各テーブル件数 */
        private var preSavepointTableRowCounts: Map<String, Long> = emptyMap()

        /** step19でsavepointへ戻る契機になった制約違反 */
        private var savepointRollbackException: SQLiteConstraintException? = null

        /** step21実行前のテーブル別キー集合 */
        private var preAbsertKeysByTable: Map<String, Set<List<String?>>> = emptyMap()

        /** step21実行前のテーブル別全行スナップショット */
        private var preAbsertRowsByTable: Map<String, Map<List<String?>, List<String?>>> = emptyMap()

        /** step21で投入するテーブル別キー集合 */
        private var inputAbsertKeysByTable: Map<String, Set<List<String?>>> = emptyMap()

        /** step21のテーブル別実登録件数 */
        private var absertAffectedRowsByTable: Map<String, Long> = emptyMap()

        /** step21のsavepoint実行結果 */
        private var absertSavepointResult: AndrOrmDatabaseHelper.SavepointResult<*>? = null

        private var isUpgrade = false
        private var version = if (isUpgrade) 2 else 1
        private var index = if (isUpgrade) 1 else 0

        /** JOIN SELECT 結果：CharacterSpells + SpellsMaster */
        private var cspSmJoinResult: List<Map<String, SelectEntity?>> = emptyList()

        /** JOIN SELECT 結果：CharacterStaticInfo + CharacterPossessions + CharacterEquip */
        private var csinCpbCebJoinResult: List<Map<String, SelectEntity?>> = emptyList()

        /** EXISTS SELECT 結果：CharacterEquip に対応する CharacterPossessions が存在する装備 */
        private var existsSelectResult: List<Map<String, SelectEntity?>> = emptyList()

        /** EXISTS SELECT のバインド値 */
        private var existsSelectBindValues: List<Any?> = emptyList()

        /** EXISTS SELECT のクエリ文字列 */
        private var existsSelectQuery: String = EMPTY_STRING

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
     * ## step19投入データ
     * ### savepoint前後へ投入する8テーブル分のInsertEntityを保持する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private data class Step19InsertData(
        val itemMasterList: List<ItemMasterInsert>,
        val magicTypeMasteryList: List<MagicTypeMasteryInsert>,
        val spellsMasterList: List<SpellsMasterInsert>,
        val characterEquipList: List<CharacterEquipInsert>,
        val characterSpellsList: List<CharacterSpellsInsert>,
        val characterStaticInfoList: List<CharacterStaticInfoV2Insert>,
        val characterStatusList: List<CharacterStatusInsert>,
        val characterPossessionsList: List<CharacterPossessionsInsert>,
    )

    /**
     * ## step21投入データ
     * ### step03由来とstep19由来を結合した9テーブル分のAbsert対象を保持する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private data class Step21AbsertData(
        val characterStaticInfoList: List<CharacterStaticInfoV2Insert>,
        val itemMasterList: List<ItemMasterInsert>,
        val spellsMasterList: List<SpellsMasterInsert>,
        val characterStatusList: List<CharacterStatusInsert>,
        val characterPossessionsList: List<CharacterPossessionsInsert>,
        val characterEquipList: List<CharacterEquipInsert>,
        val weaponMasteryList: List<WeaponMasteryInsert>,
        val magicTypeMasteryList: List<MagicTypeMasteryInsert>,
        val characterSpellsList: List<CharacterSpellsInsert>,
    )

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
     * ### 更新後の各テーブル件数を確認する
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
     * ## step08 Upsert 後データ件数確認
     * ### Upsert 後の各テーブル件数を確認する
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
     * ## step10 Absert 後データ件数確認
     * ### Absert 後の各テーブル件数を確認する
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
     * ## step14 削除後データ件数確認
     * ### step13 で削除した後の各テーブル件数と削除件数を確認する
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    @Test
    fun step14_verifyDeletedRowCounts() {
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)
        refreshTableRowCounts(databaseHelper)

        deleteTableRowCounts.forEach { (tableName, deleteCount) ->
            beforeTableRowCounts[tableName] = beforeTableRowCounts.getValue(tableName) - deleteCount
        }

        val expectedDeleteCount = deleteTableRowCounts.values.sum()
        assertNotEquals(0, actualCount)
        assertEquals(expectedDeleteCount, actualCount)
        assertEquals(beforeTableRowCounts, afterTableRowCounts.toMap())
        Log.d("step14", "actualCount = $actualCount")
    }

    /**
     * ## step15 EXISTS SELECT 実行
     * ### 装備スロットを2～5に限定し、対応する所持品とアイテムマスターをカラム比較で存在検査する
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    @Test
    fun step15_selectExists() {
        val characterEquip = TableRef(CharacterEquipBase::class, "CE_EXISTS")
        val characterPossessions = TableRef(CharacterPossessionsBase::class, "CP_EXISTS")
        val itemMaster = TableRef(ItemMasterBase::class, "IM_EXISTS")
        val selectExists = Select(characterEquip).where {
            characterEquip[CharacterEquipBase::equipSlot] between (2 to 5)
            exists(characterPossessions) {
                characterPossessions[CharacterPossessionsBase::characterPk] eq
                        characterEquip[CharacterEquipBase::characterPk]
                characterPossessions[CharacterPossessionsBase::itemPk] eq
                        characterEquip[CharacterEquipBase::itemPk]
            }
            exists(itemMaster) {
                itemMaster[ItemMasterBase::itemPk] eq characterEquip[CharacterEquipBase::itemPk]
            }
        }
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)

        databaseHelper.use { helper ->
            existsSelectResult = helper.transaction {
                helper.executeSelectAsEntityList(selectExists)
            }
            existsSelectQuery = selectExists.queryString
            existsSelectBindValues = selectExists.bindValues.toList()
            AndroidTestCsvExporter.exportSelectEntityResultToDownload(
                context = InstrumentationRegistry.getInstrumentation().targetContext,
                stepName = testName.methodName,
                resultName = "select_exists_CE_CP_IM",
                rows = existsSelectResult,
            )
            actualCount = existsSelectResult.size.toLong()
            Log.d(testStep, selectExists.queryString)
            Log.d(testStep, selectExists.bindValues.joinToString())
        }
    }

    /**
     * ## step16 EXISTS SELECT 結果検証
     * ### step15 で取得した相関 EXISTS SELECT の結果を検証する
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    @Test
    fun step16_verifySelectExists() {
        assertTrue(
            "step15_selectExists の結果がありません。",
            existsSelectResult.isNotEmpty(),
        )
        assertEquals(55, existsSelectResult.size)
        assertEquals(listOf(2, 5), existsSelectBindValues)
        assertTrue(
            existsSelectQuery.contains(
                "exists (select 1 from ITEM_MASTER IM_EXISTS " +
                        "where IM_EXISTS.ITEM_PK = CE_EXISTS.ITEM_PK)"
            )
        )

        val characterEquipList = existsSelectResult.map { row ->
            row.getValue("CE_EXISTS") as CharacterEquipBase
        }
        assertEquals(characterEquipList.size, characterEquipList.toSet().size)
        assertTrue(characterEquipList.all { entity -> entity.equipSlot in 2..5 })
        actualCount = characterEquipList.size.toLong()
    }

    /**
     * ## step17 データベースアップグレード
     * ### CharacterStaticInfoV2 を使用してデータベースをバージョン2へアップグレードする
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    @Test
    fun step17_upgradeDatabase() {
        preUpgradeTableRowCounts = afterTableRowCounts.toMap()
        isUpgrade = true
        version = 2
        index = 1
        currentTableDefinitions = tableDefinitions[index]

        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)
        databaseHelper.use { helper ->
            assertEquals(version, helper.writableDatabase.version)
        }
    }

    /**
     * ## step18 データベースアップグレード結果検証
     * ### バージョン、テーブル定義、追加カラムおよび既存データ件数を検証する
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    @Test
    fun step18_verifyUpgradedDatabase() {
        assertTrue(isUpgrade)
        assertEquals(2, version)
        assertSame(CharacterStaticInfoV2::class, currentTableDefinitions.first())

        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)
        databaseHelper.use { helper ->
            val db = helper.readableDatabase
            assertEquals(version, db.version)
            assertEquals(
                AndroidTestSeedData.rowCountByTable.keys,
                findUserTableNames(db).toSet(),
            )
            assertTrue(
                findTableColumnNames(db, CharacterStaticInfoV2::class.getTableName())
                    .contains("MAIN_ELEMENT")
            )
            db.rawQuery(
                "select MAIN_ELEMENT from ${CharacterStaticInfoV2::class.getTableName()}",
                emptyArray<String>(),
            ).use { cursor ->
                assertTrue(cursor.count > 0)
                while (cursor.moveToNext()) {
                    assertEquals(1, cursor.getInt(0))
                }
            }
        }

        refreshTableRowCounts(createDatabaseHelper(version, *currentTableDefinitions))
        assertEquals(preUpgradeTableRowCounts, afterTableRowCounts.toMap())
    }

    /**
     * ## step19 savepointを使用した追加と部分ロールバック
     * ### マスタ3テーブルの追加後にsavepointを作成し、CHARACTER_POSSESSIONSの複合キー重複で後続5テーブルを戻す
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    @Test
    fun step19_insertWithSavepointRollback() {
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)
        val insertData = createSavepointInsertData()
        savepointRollbackException = null

        databaseHelper.use { helper ->
            preSavepointTableRowCounts = currentTableDefinitions.associate { entity ->
                val tableName = entity.getTableName()
                tableName to getTableRows(helper.readableDatabase, tableName)
            }

            helper.transaction {
                insertAll(databaseHelper, ItemMasterInsert::class, insertData.itemMasterList)
                insertAll(
                    databaseHelper,
                    MagicTypeMasteryInsert::class,
                    insertData.magicTypeMasteryList,
                )
                insertAll(databaseHelper, SpellsMasterInsert::class, insertData.spellsMasterList)
                val savepointResult =
                    databaseHelper.savepoint(AFTER_MASTER_INSERT_SAVEPOINT) {
                        insertAll(
                            databaseHelper,
                            CharacterEquipInsert::class,
                            insertData.characterEquipList,
                        )
                        insertAll(
                            databaseHelper,
                            CharacterSpellsInsert::class,
                            insertData.characterSpellsList,
                        )
                        insertAll(
                            databaseHelper,
                            CharacterStaticInfoV2Insert::class,
                            insertData.characterStaticInfoList,
                        )
                        insertAll(
                            databaseHelper,
                            CharacterStatusInsert::class,
                            insertData.characterStatusList,
                        )
                        insertAll(
                            databaseHelper,
                            CharacterPossessionsInsert::class,
                            insertData.characterPossessionsList,
                        )
                    }
                savepointRollbackException = savepointResult.failure as? SQLiteConstraintException
            }
        }
    }

    /**
     * ## step20 savepointロールバック結果検証
     * ### savepointより前の3テーブルだけが10件増え、後続5テーブルを含む他テーブルは増えていないことを確認する
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    @Test
    fun step20_verifySavepointRollback() {
        val expectedAddedRowsByTable = mapOf(
            ItemMaster::class.getTableName() to 10L,
            MagicTypeMastery::class.getTableName() to 10L,
            SpellsMaster::class.getTableName() to 10L,
            CharacterEquip::class.getTableName() to 0L,
            CharacterSpells::class.getTableName() to 0L,
            CharacterStaticInfoV2::class.getTableName() to 0L,
            CharacterStatus::class.getTableName() to 0L,
            CharacterPossessions::class.getTableName() to 0L,
            WeaponMastery::class.getTableName() to 0L,
        )
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)

        assertCharacterPossessionsUniqueKeyViolation(savepointRollbackException)
        databaseHelper.use { helper ->
            assertSavepointAddedRowCounts(helper, expectedAddedRowsByTable)
        }
    }

    /**
     * ## step21 一括Absertとsavepoint
     * ### 各テーブルのstep03由来データとstep19由来データを結合し、テーブルごとに1回のAbsertで登録する
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    @Test
    fun step21_absertCombinedDataWithSavepoint() {
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)
        val absertData = createStep21AbsertData()
        val keyColumnsByTable = absertKeyColumnsByTable()
        val affectedRows = mutableMapOf<String, Long>()

        databaseHelper.use { helper ->
            preAbsertKeysByTable = keyColumnsByTable.mapValues { (tableName, columns) ->
                getTableKeySet(helper.readableDatabase, tableName, columns)
            }
            preAbsertRowsByTable = keyColumnsByTable.mapValues { (tableName, columns) ->
                getTableRowsByKey(helper.readableDatabase, tableName, columns)
            }
            inputAbsertKeysByTable = createInputAbsertKeysByTable(absertData)

            helper.transaction {
                affectedRows[ItemMaster::class.getTableName()] = absertAll(
                    helper, ItemMasterInsert::class, absertData.itemMasterList,
                ) { key(ItemMasterInsert::itemPk) }
                affectedRows[MagicTypeMastery::class.getTableName()] = absertAll(
                    helper, MagicTypeMasteryInsert::class, absertData.magicTypeMasteryList,
                ) {
                    key(MagicTypeMasteryInsert::characterPk)
                    key(MagicTypeMasteryInsert::magicTypeMastery)
                }
                affectedRows[SpellsMaster::class.getTableName()] = absertAll(
                    helper, SpellsMasterInsert::class, absertData.spellsMasterList,
                ) { key(SpellsMasterInsert::magicId) }

                absertSavepointResult = helper.savepoint("AFTER_MASTER_ABSERT") {
                    affectedRows[CharacterEquip::class.getTableName()] = absertAll(
                        helper, CharacterEquipInsert::class, absertData.characterEquipList,
                    ) {
                        key(CharacterEquipInsert::characterPk)
                        key(CharacterEquipInsert::equipSlot)
                    }
                    affectedRows[CharacterSpells::class.getTableName()] = absertAll(
                        helper, CharacterSpellsInsert::class, absertData.characterSpellsList,
                    ) {
                        key(CharacterSpellsInsert::characterPk)
                        key(CharacterSpellsInsert::magicId)
                    }
                    affectedRows[CharacterStaticInfoV2::class.getTableName()] = absertAll(
                        helper, CharacterStaticInfoV2Insert::class, absertData.characterStaticInfoList,
                    ) { key(CharacterStaticInfoV2Insert::characterPk) }
                    affectedRows[CharacterStatus::class.getTableName()] = absertAll(
                        helper, CharacterStatusInsert::class, absertData.characterStatusList,
                    ) {
                        key(CharacterStatusInsert::characterPk)
                        key(CharacterStatusInsert::statusType)
                    }
                    affectedRows[CharacterPossessions::class.getTableName()] = absertAll(
                        helper, CharacterPossessionsInsert::class, absertData.characterPossessionsList,
                    ) {
                        key(CharacterPossessionsInsert::characterPk)
                        key(CharacterPossessionsInsert::itemPk)
                    }
                    affectedRows[WeaponMastery::class.getTableName()] = absertAll(
                        helper, WeaponMasteryInsert::class, absertData.weaponMasteryList,
                    ) {
                        key(WeaponMasteryInsert::characterPk)
                        key(WeaponMasteryInsert::weaponTypeId)
                    }
                }
            }
        }
        absertAffectedRowsByTable = affectedRows.toMap()
    }

    /**
     * ## step22 一括Absert結果検証
     * ### step21実行前と投入対象のキー和集合、追加件数、savepoint成功を全テーブルで検証する
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    @Test
    fun step22_verifyCombinedAbsert() {
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)
        assertTrue("step21のsavepoint処理が失敗しています。", absertSavepointResult?.isSuccess == true)

        databaseHelper.use { helper ->
            absertKeyColumnsByTable().forEach { (tableName, columns) ->
                val expectedKeys = preAbsertKeysByTable.getValue(tableName) +
                    inputAbsertKeysByTable.getValue(tableName)
                val actualKeys = getTableKeySet(helper.readableDatabase, tableName, columns)
                val expectedAddedCount =
                    (inputAbsertKeysByTable.getValue(tableName) -
                        preAbsertKeysByTable.getValue(tableName)).size.toLong()
                assertEquals("$tableName のキー集合が不正です。", expectedKeys, actualKeys)
                assertEquals(
                    "$tableName のAbsert追加件数が不正です。",
                    expectedAddedCount,
                    absertAffectedRowsByTable.getValue(tableName),
                )
                val actualRowsByKey = getTableRowsByKey(helper.readableDatabase, tableName, columns)
                val conflictKeys = preAbsertKeysByTable.getValue(tableName)
                    .intersect(inputAbsertKeysByTable.getValue(tableName))
                conflictKeys.forEach { key ->
                    assertEquals(
                        "$tableName の既存行がAbsertで更新されています。key=$key",
                        preAbsertRowsByTable.getValue(tableName).getValue(key),
                        actualRowsByKey.getValue(key),
                    )
                }
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
        version: Int, vararg entities: KClass<out TableDefinitionEntity>
    ): AndrOrmDatabaseHelper = AndrOrmDatabaseHelper(
        context = context,
        databaseName = databaseName,
        version = version,
        entities = entities.toList(),
    )

    /**
     * ## CHARACTER_POSSESSIONS複合キー衝突検証
     * ### step19で保存した例外が対象テーブルのCHARACTER_PKとITEM_PKの衝突であることを確認する
     * @param exception step19で保存したSQLite制約違反
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private fun assertCharacterPossessionsUniqueKeyViolation(
        exception: SQLiteConstraintException?,
    ) {
        val exceptionMessage = exception?.message.orEmpty()
        val requiredFragments = listOf(
            "CHARACTER_POSSESSIONS",
            "CHARACTER_PK",
            "ITEM_PK",
        )
        assertTrue(
            "CHARACTER_POSSESSIONSの(CHARACTER_PK, ITEM_PK)重複ではありません: $exceptionMessage",
            exception != null && requiredFragments.all { fragment ->
                exceptionMessage.contains(fragment)
            },
        )
    }

    /**
     * ## SAVEPOINT追加件数検証
     * ### 全テーブルの総件数差分とSAVEPOINT使用時の作成行数をテーブル別期待値Mapで確認する
     * @param databaseHelper DB Helper
     * @param expectedAddedRowsByTable テーブル別の追加期待件数
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private fun assertSavepointAddedRowCounts(
        databaseHelper: AndrOrmDatabaseHelper,
        expectedAddedRowsByTable: Map<String, Long>,
    ) {
        assertEquals(expectedAddedRowsByTable.keys, preSavepointTableRowCounts.keys)
        val actualTableRowCounts = expectedAddedRowsByTable.keys.associateWith { tableName ->
            getTableRows(databaseHelper.readableDatabase, tableName)
        }
        val expectedTableRowCounts = preSavepointTableRowCounts.mapValues { (tableName, count) ->
            count + expectedAddedRowsByTable.getValue(tableName)
        }
        assertEquals(expectedTableRowCounts, actualTableRowCounts)

        val actualAddedRowsByTable = expectedAddedRowsByTable.keys.associateWith { tableName ->
            getTableRows(
                databaseHelper.readableDatabase,
                tableName,
                "CREATE_METHOD = '$verifyStep'",
            )
        }
        assertEquals(expectedAddedRowsByTable, actualAddedRowsByTable)
    }

    /**
     * ## SAVEPOINT用投入データ生成
     * ### 既存キー範囲と重複しない10件を各テーブル用に生成し、所持品の10件目だけ複合キーを1件目と重複させる
     * @return savepoint前後に投入するInsertEntity一覧
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private fun createSavepointInsertData(): Step19InsertData {
        val existingCharacterPks = AndroidTestSeedData.characterStaticInfoList
            .take(SAVEPOINT_INSERT_COUNT)
            .map { entity -> entity.characterPk }
        val itemMasterList = (1..SAVEPOINT_INSERT_COUNT).map { index ->
            ItemMasterInsert(
                itemPk = SAVEPOINT_ITEM_PK_BASE + index,
                itemType = 100 + index,
                itemName = "STEP19_ITEM_%02d".format(index),
                mainEffect = "STEP19_MAIN_$index",
                subEffect = if (index % 2 == 0) "STEP19_SUB_$index" else null,
                equipableSlot = 2 + (index - 1) % 4,
                createMethod = testStep,
                updateMethod = testStep,
            )
        }
        val magicTypeMasteryList = existingCharacterPks.mapIndexed { index, characterPk ->
            MagicTypeMasteryInsert(
                characterPk = characterPk,
                magicTypeMastery = 100 + index,
                mastery = 10 + index,
                createMethod = testStep,
                updateMethod = testStep,
            )
        }
        val spellsMasterList = (1..SAVEPOINT_INSERT_COUNT).map { index ->
            SpellsMasterInsert(
                magicId = SAVEPOINT_MAGIC_ID_BASE + index,
                magicTypeId = 100 + index,
                magicName = "STEP19_MAGIC_%02d".format(index),
                mainEffect = "STEP19_MAGIC_MAIN_$index",
                subEffect = if (index % 2 == 0) "STEP19_MAGIC_SUB_$index" else null,
                createMethod = testStep,
                updateMethod = testStep,
            )
        }
        val characterEquipList = (1..SAVEPOINT_INSERT_COUNT).map { index ->
            CharacterEquipInsert(
                characterPk = SAVEPOINT_CHARACTER_PK_BASE + index,
                equipSlot = 2,
                itemPk = SAVEPOINT_ITEM_PK_BASE + index,
                createMethod = testStep,
                updateMethod = testStep,
            )
        }
        val characterSpellsList = (1..SAVEPOINT_INSERT_COUNT).map { index ->
            CharacterSpellsInsert(
                characterPk = SAVEPOINT_CHARACTER_PK_BASE + index,
                magicId = SAVEPOINT_MAGIC_ID_BASE + index,
                createMethod = testStep,
                updateMethod = testStep,
            )
        }
        val characterStaticInfoList = (1..SAVEPOINT_INSERT_COUNT).map { index ->
            CharacterStaticInfoV2Insert(
                characterPk = SAVEPOINT_CHARACTER_PK_BASE + index,
                userId = "STEP19_USER_%02d".format(index),
                characterNo = 1,
                characterName = "STEP19_CHARACTER_%02d".format(index),
                createMethod = testStep,
                mainElement = 1 + (index - 1) % 6,
                updateMethod = testStep,
            )
        }
        val characterStatusList = (1..SAVEPOINT_INSERT_COUNT).map { index ->
            CharacterStatusInsert(
                characterPk = SAVEPOINT_CHARACTER_PK_BASE + index,
                statusType = "STEP19_STATUS",
                value = 1_000 + index,
                createMethod = testStep,
                updateMethod = testStep,
            )
        }
        val characterPossessionsList = (1..SAVEPOINT_INSERT_COUNT).map { index ->
            val uniqueKeyIndex = if (index == SAVEPOINT_INSERT_COUNT) 1 else index
            CharacterPossessionsInsert(
                characterPk = SAVEPOINT_CHARACTER_PK_BASE + uniqueKeyIndex,
                itemPk = SAVEPOINT_ITEM_PK_BASE + uniqueKeyIndex,
                itemStatus = if (index == SAVEPOINT_INSERT_COUNT) {
                    "UNIQUE_KEY_CONFLICT"
                } else {
                    "STEP19_HOLD_$index"
                },
                createMethod = testStep,
                updateMethod = testStep,
            )
        }
        return Step19InsertData(
            itemMasterList = itemMasterList,
            magicTypeMasteryList = magicTypeMasteryList,
            spellsMasterList = spellsMasterList,
            characterEquipList = characterEquipList,
            characterSpellsList = characterSpellsList,
            characterStaticInfoList = characterStaticInfoList,
            characterStatusList = characterStatusList,
            characterPossessionsList = characterPossessionsList,
        )
    }

    /**
     * ## step21用Absertデータ生成
     * ### step03由来とstep19由来のEntityをテーブル別に結合する
     * @return テーブル別の一括Absert対象
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private fun createStep21AbsertData(): Step21AbsertData {
        val savepointData = createSavepointInsertData()
        return Step21AbsertData(
            characterStaticInfoList = AndroidTestSeedData.characterStaticInfoList.map { entity ->
                CharacterStaticInfoV2Insert(
                    characterPk = entity.characterPk,
                    userId = entity.userId,
                    characterNo = entity.characterNo,
                    characterName = entity.characterName,
                    createMethod = "step03",
                    mainElement = 1,
                    updateMethod = "step03",
                )
            } + savepointData.characterStaticInfoList,
            itemMasterList = AndroidTestSeedData.itemMasterList.map { entity ->
                ItemMasterInsert(
                    itemPk = entity.itemPk,
                    itemType = entity.itemType,
                    itemName = entity.itemName,
                    mainEffect = entity.mainEffect,
                    subEffect = entity.subEffect,
                    equipableSlot = entity.equipableSlot,
                    createMethod = "step03",
                    updateMethod = "step03",
                )
            } + savepointData.itemMasterList,
            spellsMasterList = AndroidTestSeedData.spellsMasterList.map { entity ->
                SpellsMasterInsert(
                    magicId = entity.magicId,
                    magicTypeId = entity.magicTypeId,
                    magicName = entity.magicName,
                    mainEffect = entity.mainEffect,
                    subEffect = entity.subEffect,
                    createMethod = "step03",
                    updateMethod = "step03",
                )
            } + savepointData.spellsMasterList,
            characterStatusList = AndroidTestSeedData.characterStatusList.map { entity ->
                CharacterStatusInsert(
                    characterPk = entity.characterPk,
                    statusType = entity.statusType,
                    value = entity.value,
                    createMethod = "step03",
                    updateMethod = "step03",
                )
            } + savepointData.characterStatusList,
            characterPossessionsList = AndroidTestSeedData.characterPossessionsList.map { entity ->
                CharacterPossessionsInsert(
                    characterPk = entity.characterPk,
                    itemPk = entity.itemPk,
                    itemStatus = entity.itemStatus,
                    createMethod = "step03",
                    updateMethod = "step03",
                )
            } + savepointData.characterPossessionsList,
            characterEquipList = AndroidTestSeedData.characterEquipList.map { entity ->
                CharacterEquipInsert(
                    characterPk = entity.characterPk,
                    equipSlot = entity.equipSlot,
                    itemPk = entity.itemPk!!,
                    createMethod = "step03",
                    updateMethod = "step03",
                )
            } + savepointData.characterEquipList,
            weaponMasteryList = AndroidTestSeedData.weaponMasteryList.map { entity ->
                WeaponMasteryInsert(
                    characterPk = entity.characterPk,
                    weaponTypeId = entity.weaponTypeId,
                    createMethod = "step03",
                    updateMethod = "step03",
                )
            },
            magicTypeMasteryList = AndroidTestSeedData.magicTypeMasteryList.map { entity ->
                MagicTypeMasteryInsert(
                    characterPk = entity.characterPk,
                    magicTypeMastery = entity.magicTypeMastery,
                    mastery = entity.mastery,
                    createMethod = "step03",
                    updateMethod = "step03",
                )
            } + savepointData.magicTypeMasteryList,
            characterSpellsList = AndroidTestSeedData.characterSpellsList.map { entity ->
                CharacterSpellsInsert(
                    characterPk = entity.characterPk,
                    magicId = entity.magicId,
                    createMethod = "step03",
                    updateMethod = "step03",
                )
            } + savepointData.characterSpellsList,
        )
    }

    /**
     * ## Absert競合キー定義
     * ### step21対象全テーブルの競合判定カラムを返す
     * @return テーブル名と競合判定カラム名の対応
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private fun absertKeyColumnsByTable(): Map<String, List<String>> = mapOf(
        CharacterStaticInfoV2::class.getTableName() to listOf("CHARACTER_PK"),
        ItemMaster::class.getTableName() to listOf("ITEM_PK"),
        SpellsMaster::class.getTableName() to listOf("MAGIC_ID"),
        CharacterStatus::class.getTableName() to listOf("CHARACTER_PK", "STATUS_TYPE"),
        CharacterPossessions::class.getTableName() to listOf("CHARACTER_PK", "ITEM_PK"),
        CharacterEquip::class.getTableName() to listOf("CHARACTER_PK", "EQUIP_SLOT"),
        WeaponMastery::class.getTableName() to listOf("CHARACTER_PK", "WEAPON_TYPE_ID"),
        MagicTypeMastery::class.getTableName() to listOf("CHARACTER_PK", "MAGIC_TYPE_MASTERY"),
        CharacterSpells::class.getTableName() to listOf("CHARACTER_PK", "MAGIC_ID"),
    )

    /**
     * ## step21投入キー生成
     * ### 一括Absert対象Entityからテーブル別の競合キー集合を生成する
     * @param data step21投入データ
     * @return テーブル別の競合キー集合
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private fun createInputAbsertKeysByTable(
        data: Step21AbsertData,
    ): Map<String, Set<List<String?>>> = mapOf(
        CharacterStaticInfoV2::class.getTableName() to data.characterStaticInfoList.map { entity ->
            listOf(entity.characterPk.toString())
        }.toSet(),
        ItemMaster::class.getTableName() to data.itemMasterList.map { entity ->
            listOf(entity.itemPk.toString())
        }.toSet(),
        SpellsMaster::class.getTableName() to data.spellsMasterList.map { entity ->
            listOf(entity.magicId.toString())
        }.toSet(),
        CharacterStatus::class.getTableName() to data.characterStatusList.map { entity ->
            listOf(entity.characterPk.toString(), entity.statusType)
        }.toSet(),
        CharacterPossessions::class.getTableName() to data.characterPossessionsList.map { entity ->
            listOf(entity.characterPk.toString(), entity.itemPk.toString())
        }.toSet(),
        CharacterEquip::class.getTableName() to data.characterEquipList.map { entity ->
            listOf(entity.characterPk.toString(), entity.equipSlot.toString())
        }.toSet(),
        WeaponMastery::class.getTableName() to data.weaponMasteryList.map { entity ->
            listOf(entity.characterPk.toString(), entity.weaponTypeId.toString())
        }.toSet(),
        MagicTypeMastery::class.getTableName() to data.magicTypeMasteryList.map { entity ->
            listOf(entity.characterPk.toString(), entity.magicTypeMastery.toString())
        }.toSet(),
        CharacterSpells::class.getTableName() to data.characterSpellsList.map { entity ->
            listOf(entity.characterPk.toString(), entity.magicId.toString())
        }.toSet(),
    )

    /**
     * ## テーブルキー集合取得
     * ### 指定テーブルの競合キーを文字列リストの集合として取得する
     * @param db 読み取り対象DB
     * @param tableName テーブル名
     * @param columns キーカラム名
     * @return テーブル内のキー集合
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private fun getTableKeySet(
        db: SQLiteDatabase,
        tableName: String,
        columns: List<String>,
    ): Set<List<String?>> = db.rawQuery(
        "select ${columns.joinToString()} from $tableName",
        emptyArray<String>(),
    ).use { cursor ->
        buildSet {
            while (cursor.moveToNext()) {
                add(columns.indices.map { columnIndex -> cursor.getString(columnIndex) })
            }
        }
    }

    /**
     * ## テーブル行スナップショット取得
     * ### テーブルの全カラム値を競合キー別に取得する
     * @param db 読み取り対象DB
     * @param tableName テーブル名
     * @param keyColumns キーカラム名
     * @return 競合キーと全カラム値の対応
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private fun getTableRowsByKey(
        db: SQLiteDatabase,
        tableName: String,
        keyColumns: List<String>,
    ): Map<List<String?>, List<String?>> = db.rawQuery(
        "select * from $tableName",
        emptyArray<String>(),
    ).use { cursor ->
        val keyColumnIndexes = keyColumns.map { columnName -> cursor.getColumnIndexOrThrow(columnName) }
        buildMap {
            while (cursor.moveToNext()) {
                val key = keyColumnIndexes.map { columnIndex -> cursor.getString(columnIndex) }
                val row = (0 until cursor.columnCount).map { columnIndex -> cursor.getString(columnIndex) }
                put(key, row)
            }
        }
    }

    /**
     * ## Absert一括実行
     * ### 結合済みEntity一覧を1つのAbsert文として実行する
     * @param databaseHelper DB Helper
     * @param entityClass Absert対象Entityクラス
     * @param entities 結合済みEntity一覧
     * @param onConflict 競合判定キー定義
     * @return 実登録件数
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private fun <T : AbsertEntity> absertAll(
        databaseHelper: AndrOrmDatabaseHelper,
        entityClass: KClass<out T>,
        entities: List<T>,
        onConflict: OnConflictClauseBuilder<T>.() -> Unit,
    ): Long {
        val absert = Absert(entityClass).onConflict(onConflict).addEntities(entities)
        return databaseHelper.executeDml(absert).toLong()
    }

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
     * @return 指定位置に対応する更新対象リスト
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun <T> List<T>.targetSegment(segmentIndex: Int): List<T> {
        val targetCount = size * UPDATE_TARGET_PERCENT / 100
        return drop(targetCount * segmentIndex).take(targetCount)
    }

    /**
     * ## ITEM_MASTER更新対象取得
     * @return UPDATE対象とするアイテム一覧
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun itemMasterUpdateTargets(): List<ItemMaster> {
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

    /**
     * ## SPELLS_MASTER更新対象取得
     * @return UPDATE対象とする魔法一覧
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun spellsMasterUpdateTargets(): List<SpellsMaster> {
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

    /**
     * ## UPDATEデータ実行
     * ### 各テーブルの対象行を一つのトランザクションで更新する
     * @param databaseHelper DB操作ヘルパー
     * @param testStep 更新元として記録するテストStep
     * @return 全UPDATEの影響行数
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun updateData(
        databaseHelper: AndrOrmDatabaseHelper,
        testStep: String,
    ): Int {
        val updateTime = LocalDateTime.now()
        val updateCharacterPks =
            AndroidTestSeedData.characterStaticInfoList.targetSegment(segmentIndex = 0)
                .map { entity -> entity.characterPk }
        val updateWeaponMasteryCharacterPks =
            AndroidTestSeedData.weaponMasteryList.targetSegment(segmentIndex = 0)
                .map { entity -> entity.characterPk }
        val updateMagicTypeMasteryCharacterPks =
            AndroidTestSeedData.magicTypeMasteryList.targetSegment(segmentIndex = 0)
                .map { entity -> entity.characterPk }

        val updateCharacterStaticInfo = Update(CharacterStaticInfoV1UpdateAudit::class).set {
            CharacterStaticInfoV1UpdateAudit::updateMethod assign testStep
            CharacterStaticInfoV1UpdateAudit::updateTime assign updateTime
        }.where {
            CharacterStaticInfoV1UpdateAudit::characterPk inList updateCharacterPks
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
        val existingUpdateItemPks = AndroidTestSeedData.itemMasterList.filter { entity ->
            (entity.mainEffect == "MP" && entity.subEffect == null) || entity.itemType == 6
        }.map { entity -> entity.itemPk }.toSet()
        val additionalUpdateItemPks =
            itemMasterUpdateTargets().filter { entity -> entity.itemPk !in existingUpdateItemPks }
                .map { entity -> entity.itemPk }
        val updateAdditionalItems = Update(ItemMasterUpdateAudit::class).set {
            ItemMasterUpdateAudit::updateMethod assign testStep
            ItemMasterUpdateAudit::updateTime assign updateTime
        }.where { ItemMasterUpdateAudit::itemPk inList additionalUpdateItemPks }

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
        val existingUpdateMagicIds = AndroidTestSeedData.spellsMasterList.filter { entity ->
            entity.magicTypeId in 3..5 && entity.subEffect == null
        }.map { entity -> entity.magicId }.toSet()
        val additionalUpdateMagicIds =
            spellsMasterUpdateTargets().filter { entity -> entity.magicId !in existingUpdateMagicIds }
                .map { entity -> entity.magicId }
        val updateAdditionalSpells = Update(SpellsMasterUpdate::class).set {
            SpellsMasterUpdate::updateMethod assign testStep
            SpellsMasterUpdate::updateTime assign updateTime
        }.where { SpellsMasterUpdate::magicId inList additionalUpdateMagicIds }

        val updateCharacterPossessions = Update(CharacterPossessionsUpdateAudit::class).set {
            CharacterPossessionsUpdateAudit::updateMethod assign testStep
            CharacterPossessionsUpdateAudit::updateTime assign updateTime
        }.where {
            CharacterPossessionsUpdateAudit::characterPk inList updateCharacterPks
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
                    CharacterEquipUpdateAudit::characterPk inList updateCharacterPks
                    CharacterEquipUpdateAudit::equipSlot eq 2
                    exists(cbb) {
                        cbb[CharacterPossessionsBase::itemPk] eq setItem
                    }
                }
        val updateWeaponMastery = Update(WeaponMasteryUpdateAudit::class).set {
            WeaponMasteryUpdateAudit::updateMethod assign testStep
            WeaponMasteryUpdateAudit::updateTime assign updateTime
        }.where {
            WeaponMasteryUpdateAudit::characterPk inList updateWeaponMasteryCharacterPks
        }
        val updateMagicTypeMastery = Update(MagicTypeMasteryUpdateAudit::class).set {
            MagicTypeMasteryUpdateAudit::updateMethod assign testStep
            MagicTypeMasteryUpdateAudit::updateTime assign updateTime
        }.where {
            MagicTypeMasteryUpdateAudit::characterPk inList updateMagicTypeMasteryCharacterPks
        }
        val updateCharacterSpells = Update(CharacterSpellsUpdateAudit::class).set {
            CharacterSpellsUpdateAudit::updateMethod assign testStep
            CharacterSpellsUpdateAudit::updateTime assign updateTime
        }.where {
            CharacterSpellsUpdateAudit::characterPk inList updateCharacterPks
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
     * @param databaseHelper DB操作ヘルパー
     * @param entityClass Upsert対象Entityクラス
     * @param entities 登録するEntity一覧
     * @param onConflict 競合キー設定
     * @param set 競合時の更新設定
     * @return 全バッチの影響行数
     * @author Masahiro Inoue
     * @since 2026-06-16
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

    /**
     * ## UPSERTデータ実行
     * ### 各テーブルの対象データを一つのトランザクションでUPSERTする
     * @param databaseHelper DB操作ヘルパー
     * @return 全UPSERTの影響行数
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun upsertData(
        databaseHelper: AndrOrmDatabaseHelper,
    ): Int {
        val updateTime = LocalDateTime.now()
        val upsertCharacterPks =
            AndroidTestSeedData.characterStaticInfoList.targetSegment(segmentIndex = 1)
                .map { entity -> entity.characterPk }.toSet()
        val updatedItemPks = itemMasterUpdateTargets().map { entity -> entity.itemPk }.toSet()
        val upsertItemTargets =
            AndroidTestSeedData.itemMasterList.filter { entity -> entity.itemPk !in updatedItemPks }
                .take(AndroidTestSeedData.itemMasterList.size * UPDATE_TARGET_PERCENT / 100)
        val updatedMagicIds = spellsMasterUpdateTargets().map { entity -> entity.magicId }.toSet()
        val upsertSpellsMasterTargets =
            AndroidTestSeedData.spellsMasterList.filter { entity -> entity.magicId !in updatedMagicIds }
                .take(AndroidTestSeedData.spellsMasterList.size * UPDATE_TARGET_PERCENT / 100)
        val upsertStatusTypes = setOf("HP", "MP", "SP")

        val characterStaticInfoList =
            AndroidTestSeedData.characterStaticInfoList.filter { entity -> entity.characterPk in upsertCharacterPks }
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
        val itemMasterList = upsertItemTargets.map { entity ->
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
        val spellsMasterList = upsertSpellsMasterTargets.map { entity ->
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
            AndroidTestSeedData.characterStatusList.filter { entity -> entity.statusType in upsertStatusTypes }
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
            AndroidTestSeedData.characterPossessionsList.filter { entity -> entity.characterPk in upsertCharacterPks }
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
            AndroidTestSeedData.characterEquipList.filter { entity -> entity.characterPk in upsertCharacterPks }
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
            AndroidTestSeedData.characterSpellsList.filter { entity -> entity.characterPk in upsertCharacterPks }
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

    /**
     * ## ABSERTデータ実行
     * ### 装備データを複合競合キー付きでABSERTする
     * @param databaseHelper DB操作ヘルパー
     * @return ABSERTの影響行数
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
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

    /**
     * ## 1テーブルJOINデータ取得
     * ### キャラクタ魔法と魔法マスタをLEFT JOINして取得する
     * @param databaseHelper DB操作ヘルパー
     * @return Entity別名をキーとするJOIN結果
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
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

    /**
     * ## 2テーブルJOINデータ取得
     * ### キャラクタ基本情報へ装備と所持品をJOINして取得する
     * @param databaseHelper DB操作ヘルパー
     * @return Entity別名をキーとするJOIN結果
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
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

    /**
     * ## DELETEデータ実行
     * ### 全対象テーブルからstep03で更新された行を削除する
     * @param databaseHelper DB操作ヘルパー
     * @return 全DELETEの影響行数
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
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
     * ## テーブルカラム名取得
     * @param db SQLiteDatabase
     * @param tableName テーブル名
     * @return 指定テーブルのカラム名リスト
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private fun findTableColumnNames(
        db: SQLiteDatabase,
        tableName: String,
    ): List<String> {
        val columnNames = mutableListOf<String>()
        db.rawQuery("pragma table_info(${quoteString(tableName)})", emptyArray<String>())
            .use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    columnNames += cursor.getString(nameIndex)
                }
            }
        return columnNames
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

    /**
     * ## 1テーブルJOIN結果検証
     * ### キャラクタ魔法と魔法マスタの結合件数および未結合行を確認する
     * @param result 検証するJOIN結果
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
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

    /**
     * ## 2テーブルJOIN結果検証
     * ### キャラクタ別件数と装備・所持品の結合関係を確認する
     * @param result 検証するJOIN結果
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
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
    private fun quoteString(str: String): String =
        D_QUOTE + str.replace(D_QUOTE, D_QUOTE + D_QUOTE) + D_QUOTE

    /**
     * ## 削除件数取得Entityインターフェース
     * ### 削除対象Stepを条件指定するための共通プロパティを定義する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    interface DeleteCountEntity : SelectEntity, DeleteEntity {
        val updateMethod: String
    }

    /**
     * CHARACTER_EQUIPの削除対象件数を取得する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    @Table("CHARACTER_EQUIP", "DC")
    data class CharacterEquipDeleteCount(
        @Function(columnFunction = ColumnFunction.COUNT_ALL, alias = "COUNT") val count: Long,
        @Column(hideFromSelect = true) override val updateMethod: String,
    ) : DeleteCountEntity

    /**
     * CHARACTER_POSSESSIONSの削除対象件数を取得する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    @Table("CHARACTER_POSSESSIONS", "DC")
    data class CharacterPossessionsDeleteCount(
        @Function(columnFunction = ColumnFunction.COUNT_ALL, alias = "COUNT") val count: Long,
        @Column(hideFromSelect = true) override val updateMethod: String,
    ) : DeleteCountEntity

    /**
     * CHARACTER_SPELLSの削除対象件数を取得する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    @Table("CHARACTER_SPELLS", "DC")
    data class CharacterSpellsDeleteCount(
        @Function(columnFunction = ColumnFunction.COUNT_ALL, alias = "COUNT") val count: Long,
        @Column(hideFromSelect = true) override val updateMethod: String,
    ) : DeleteCountEntity

    /**
     * CHARACTER_STATIC_INFOの削除対象件数を取得する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    @Table("CHARACTER_STATIC_INFO", "DC")
    data class CharacterStaticInfoDeleteCount(
        @Function(columnFunction = ColumnFunction.COUNT_ALL, alias = "COUNT") val count: Long,
        @Column(hideFromSelect = true) override val updateMethod: String,
    ) : DeleteCountEntity

    /**
     * CHARACTER_STATUSの削除対象件数を取得する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    @Table("CHARACTER_STATUS", "DC")
    data class CharacterStatusDeleteCount(
        @Function(columnFunction = ColumnFunction.COUNT_ALL, alias = "COUNT") val count: Long,
        @Column(hideFromSelect = true) override val updateMethod: String,
    ) : DeleteCountEntity

    /**
     * ITEM_MASTERの削除対象件数を取得する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    @Table("ITEM_MASTER", "DC")
    data class ItemMasterDeleteCount(
        @Function(columnFunction = ColumnFunction.COUNT_ALL, alias = "COUNT") val count: Long,
        @Column(hideFromSelect = true) override val updateMethod: String,
    ) : DeleteCountEntity

    /**
     * MAGIC_TYPE_MASTERYの削除対象件数を取得する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    @Table("MAGIC_TYPE_MASTERY", "DC")
    data class MagicTypeMasteryDeleteCount(
        @Function(columnFunction = ColumnFunction.COUNT_ALL, alias = "COUNT") val count: Long,
        @Column(hideFromSelect = true) override val updateMethod: String,
    ) : DeleteCountEntity

    /**
     * SPELLS_MASTERの削除対象件数を取得する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    @Table("SPELLS_MASTER", "DC")
    data class SpellsMasterDeleteCount(
        @Function(columnFunction = ColumnFunction.COUNT_ALL, alias = "COUNT") val count: Long,
        @Column(hideFromSelect = true) override val updateMethod: String,
    ) : DeleteCountEntity

    /**
     * WEAPON_MASTERYの削除対象件数を取得する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    @Table("WEAPON_MASTERY", "DC")
    data class WeaponMasteryDeleteCount(
        @Function(columnFunction = ColumnFunction.COUNT_ALL, alias = "COUNT") val count: Long,
        @Column(hideFromSelect = true) override val updateMethod: String,
    ) : DeleteCountEntity
}
