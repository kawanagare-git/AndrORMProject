package jp.pgw.lab78.androrm.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import jp.pgw.lab78.androrm.common.Constants.D_QUOTE
import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableName
import jp.pgw.lab78.androrm.common.database.SupportFunction.toSnakeCase
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Function
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.annotation.View
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction
import jp.pgw.lab78.androrm.common.dml.interfaces.*
import jp.pgw.lab78.androrm.database.entities.*
import jp.pgw.lab78.androrm.database.entities.absert.CharacterEquipAbsert
import jp.pgw.lab78.androrm.database.entities.delete.*
import jp.pgw.lab78.androrm.database.entities.insert.*
import jp.pgw.lab78.androrm.database.entities.resource.AndroidTestSeedData
import jp.pgw.lab78.androrm.database.entities.resource.AndroidTestSeedData.CUMULATIVE_CHARACTER_COUNT
import jp.pgw.lab78.androrm.database.entities.resource.AndroidTestSeedData.CUMULATIVE_DATA_STEP
import jp.pgw.lab78.androrm.database.entities.resource.AndroidTestSeedData.CUMULATIVE_MAX_LEVEL
import jp.pgw.lab78.androrm.database.entities.resource.AndroidTestSeedData.CUMULATIVE_STATUS_COUNT_PER_LEVEL
import jp.pgw.lab78.androrm.database.entities.resource.AndroidTestSeedData.SeedUpsertData
import jp.pgw.lab78.androrm.database.entities.resource.AndroidTestSeedData.Step21AbsertData
import jp.pgw.lab78.androrm.database.entities.select.*
import jp.pgw.lab78.androrm.database.entities.update.*
import jp.pgw.lab78.androrm.database.entities.upsert.*
import jp.pgw.lab78.androrm.database.interfaces.plus
import jp.pgw.lab78.androrm.database.queryparts.JoinType
import jp.pgw.lab78.androrm.database.queryparts.OnConflictClauseBuilder
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

        /** DML 系実行時カウント */
        private var actualCount = 0L

        /** UPSERTを分割実行する1バッチの件数 */
        private const val UPSERT_BATCH_SIZE = 100

        /** step19でマスタ登録後に作成するsavepoint名 */
        private const val AFTER_MASTER_INSERT_SAVEPOINT = "STEP19_AFTER_MASTER_INSERT"

        /** step21でマスタ登録後に作成するsavepoint名 */
        private const val AFTER_MASTER_ABSERT_SAVEPOINT = "AFTER_MASTER_ABSERT"

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
        private var preAbsertRowsByTable: Map<String, Map<List<String?>, List<String?>>> =
            emptyMap()

        /** step21で投入するテーブル別キー集合 */
        private var inputAbsertKeysByTable: Map<String, Set<List<String?>>> = emptyMap()

        /** step21のテーブル別実登録件数 */
        private var absertAffectedRowsByTable: Map<String, Long> = emptyMap()

        /** step21のsavepoint実行結果 */
        private var absertSavepointResult: AndrOrmDatabaseHelper.SavepointResult<*>? = null

        /** step23実行前のCHARACTER_STATUS件数 */
        private var preCumulativeStatusRowCount = 0L

        /** step23のUPSERT投入データ */
        private var cumulativeStatusData: List<CharacterStatusV2Upsert> = emptyList()

        /** step23のUPSERT処理件数 */
        private var cumulativeUpsertAffectedRows = 0

        /** step25のCHARACTER_STATUS単体集計結果 */
        private var cumulativeStatusResult: List<Map<String, Any?>> = emptyList()

        /** step25のLEFT JOIN集計結果 */
        private var cumulativeJoinResult: List<Map<String, Any?>> = emptyList()

        /** step25のCHARACTER_STATUS単体集計SQL */
        private var cumulativeStatusQuery = EMPTY_STRING

        /** step25のLEFT JOIN集計SQL */
        private var cumulativeJoinQuery = EMPTY_STRING

        /** step25のCHARACTER_STATUS単体集計バインド値 */
        private var cumulativeStatusBindValues: List<Any?> = emptyList()

        /** step25のLEFT JOIN集計バインド値 */
        private var cumulativeJoinBindValues: List<Any?> = emptyList()

        /** step27で登録するBLOB値 */
        private val step27BlobPayload = byteArrayOf(0x00, 0x01, 0x7F, 0x80.toByte(), 0xFF.toByte())

        /** step28のView検証結果 */
        private var step28ViewResult: List<Map<String, Any?>> = emptyList()

        /** step29のBLOB検索結果 */
        private var step29BlobSearchResult: List<Map<String, Any?>> = emptyList()

        /** step29のBLOB検索SQL */
        private var step29BlobSearchQuery = EMPTY_STRING

        /** step29のBLOB検索バインド値 */
        private var step29BlobSearchBindValues: List<Any?> = emptyList()

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
        private val tableDefinitions = arrayOf<Array<KClass<out TableDefinitionEntity>>>(
            arrayOf(
                CharacterStaticInfoV1::class,
                ItemMaster::class,
                SpellsMaster::class,
                CharacterStatusV1::class,
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
                CharacterStatusV2::class,
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
     * ## ステータス累計キー
     * ### characterPkとstatusTypeを累計結果の識別キーとして保持する
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    private data class CumulativeStatusKey(
        val characterPk: Int,
        val statusType: String,
    )

    /**
     * ## step23ステータス行キー
     * ### characterPk、level、statusTypeを投入行の識別キーとして保持する
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    private data class CumulativeStatusRowKey(
        val characterPk: Int,
        val level: Int,
        val statusType: String,
    )

    /**
     * ## ステータス累計値
     * ### level最大値とvalue累計を保持する
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    private data class CumulativeStatusValue(
        val maxLevel: Int,
        val totalValue: Long,
    )

    /**
     * ## LEFT JOIN累計キー
     * ### characterPk、characterName、statusTypeを結合集計結果の識別キーとして保持する
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    private data class CumulativeJoinKey(
        val characterPk: Int,
        val characterName: String,
        val statusType: String,
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
            actualCount = upsertData(helper, testStep).toLong()
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
            actualCount = absertData(helper, testStep).toLong()
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
        actualCount = 0
        val deleteTarget = listOf<String>("step03", "step99")
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)
        deleteTarget.forEach { deleteTargetStep(databaseHelper, it) }
        refreshTableRowCounts(databaseHelper)
        databaseHelper.use { helper ->
            deleteTarget.forEach {
                actualCount += deleteData(helper, it)
            }
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
            existsSelectQuery = selectExists.query
            existsSelectBindValues = selectExists.bindValues.toList()
            AndroidTestCsvExporter.exportSelectEntityResultToDownload(
                context = InstrumentationRegistry.getInstrumentation().targetContext,
                stepName = testName.methodName,
                resultName = "select_exists_CE_CP_IM",
                rows = existsSelectResult,
            )
            actualCount = existsSelectResult.size.toLong()
            Log.d(testStep, selectExists.query)
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
        savepointRollbackException = null

        databaseHelper.use { helper ->
            preSavepointTableRowCounts = currentTableDefinitions.associate { entity ->
                val tableName = entity.getTableName()
                tableName to getTableRows(helper.readableDatabase, tableName)
            }

            val insertData = AndroidTestSeedData.createSavepointInsertData(testStep)
            val savepointResult = helper.transaction {
                insertAll(helper, ItemMasterInsert::class, insertData.itemMasterList)
                insertAll(
                    helper,
                    MagicTypeMasteryInsert::class,
                    insertData.magicTypeMasteryList,
                )
                insertAll(helper, SpellsMasterInsert::class, insertData.spellsMasterList)
                helper.savepoint(AFTER_MASTER_INSERT_SAVEPOINT) {
                    insertAll(helper, CharacterEquipInsert::class, insertData.characterEquipList)
                    insertAll(helper, CharacterSpellsInsert::class, insertData.characterSpellsList)
                    insertAll(
                        helper,
                        CharacterStaticInfoV2Insert::class,
                        insertData.characterStaticInfoList,
                    )
                    insertAll(
                        helper,
                        CharacterStatusV2Insert::class,
                        insertData.characterStatusList,
                    )
                    insertAll(
                        helper,
                        CharacterPossessionsInsert::class,
                        insertData.characterPossessionsList,
                    )
                }
            }
            savepointRollbackException = savepointResult.failure as? SQLiteConstraintException
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
            CharacterStatusV2::class.getTableName() to 0L,
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
        val keyColumnsByTable = absertKeyColumnsByTable()

        databaseHelper.use { helper ->
            preAbsertKeysByTable = keyColumnsByTable.mapValues { (tableName, columns) ->
                getTableKeySet(helper.readableDatabase, tableName, columns)
            }
            preAbsertRowsByTable = keyColumnsByTable.mapValues { (tableName, columns) ->
                getTableRowsByKey(helper.readableDatabase, tableName, columns)
            }
            val data = AndroidTestSeedData.createStep21AbsertData(testStep)
            val affectedRows = mutableMapOf<String, Long>()
            val savepointResult = helper.transaction {
                affectedRows[ItemMaster::class.getTableName()] = absertAll(
                    helper, ItemMasterInsert::class, data.itemMasterList,
                ) { key(ItemMasterInsert::itemPk) }
                affectedRows[MagicTypeMastery::class.getTableName()] = absertAll(
                    helper, MagicTypeMasteryInsert::class, data.magicTypeMasteryList,
                ) {
                    key(MagicTypeMasteryInsert::characterPk)
                    key(MagicTypeMasteryInsert::magicTypeMastery)
                }
                affectedRows[SpellsMaster::class.getTableName()] = absertAll(
                    helper, SpellsMasterInsert::class, data.spellsMasterList,
                ) { key(SpellsMasterInsert::magicId) }

                helper.savepoint(AFTER_MASTER_ABSERT_SAVEPOINT) {
                    affectedRows[CharacterEquip::class.getTableName()] = absertAll(
                        helper, CharacterEquipInsert::class, data.characterEquipList,
                    ) {
                        key(CharacterEquipInsert::characterPk)
                        key(CharacterEquipInsert::equipSlot)
                    }
                    affectedRows[CharacterSpells::class.getTableName()] = absertAll(
                        helper, CharacterSpellsInsert::class, data.characterSpellsList,
                    ) {
                        key(CharacterSpellsInsert::characterPk)
                        key(CharacterSpellsInsert::magicId)
                    }
                    affectedRows[CharacterStaticInfoV2::class.getTableName()] = absertAll(
                        helper,
                        CharacterStaticInfoV2Insert::class,
                        data.characterStaticInfoList,
                    ) { key(CharacterStaticInfoV2Insert::characterPk) }
                    affectedRows[CharacterStatusV2::class.getTableName()] = absertAll(
                        helper, CharacterStatusV2Insert::class, data.characterStatusList,
                    ) {
                        key(CharacterStatusV2Insert::characterPk)
                        key(CharacterStatusV2Insert::level)
                        key(CharacterStatusV2Insert::statusType)
                    }
                    affectedRows[CharacterPossessions::class.getTableName()] = absertAll(
                        helper,
                        CharacterPossessionsInsert::class,
                        data.characterPossessionsList,
                    ) {
                        key(CharacterPossessionsInsert::characterPk)
                        key(CharacterPossessionsInsert::itemPk)
                    }
                    affectedRows[WeaponMastery::class.getTableName()] = absertAll(
                        helper, WeaponMasteryInsert::class, data.weaponMasteryList,
                    ) {
                        key(WeaponMasteryInsert::characterPk)
                        key(WeaponMasteryInsert::weaponTypeId)
                    }
                }
            }
            inputAbsertKeysByTable = createInputAbsertKeysByTable(data)
            absertAffectedRowsByTable = affectedRows.toMap()
            absertSavepointResult = savepointResult
        }
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
        assertTrue(
            "step21のsavepoint処理が失敗しています。",
            absertSavepointResult?.isSuccess == true
        )

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
     * ## step23 累計検証用ステータスUPSERT
     * ### 既存5キャラクターへlevel 1～10、各level 3種類のステータスを一括UPSERTする
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Test
    fun step23_upsertCumulativeStatusData() {
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)

        databaseHelper.use { helper ->
            preCumulativeStatusRowCount = getTableRows(
                helper.readableDatabase,
                CharacterStatusV2::class.getTableName(),
            )
            cumulativeStatusData = AndroidTestSeedData.createCumulativeStatusData(
                LocalDateTime.now(),
            )
            val upsert = Upsert(CharacterStatusV2Upsert::class)
                .onConflict {
                    key(CharacterStatusV2Upsert::characterPk)
                    key(CharacterStatusV2Upsert::level)
                    key(CharacterStatusV2Upsert::statusType)
                }
                .set {
                    CharacterStatusV2Upsert::value assign
                            excluded(CharacterStatusV2Upsert::value)
                    CharacterStatusV2Upsert::updateMethod assign
                            excluded(CharacterStatusV2Upsert::updateMethod)
                    CharacterStatusV2Upsert::updateTime assign
                            excluded(CharacterStatusV2Upsert::updateTime)
                }
                .addEntities(cumulativeStatusData)
            cumulativeUpsertAffectedRows = helper.transaction {
                helper.executeDml(upsert)
            }
        }
    }

    /**
     * ## step24 累計検証用ステータス確認
     * ### step23の処理件数、追加件数、複合キー、level別件数および値を検証する
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Test
    fun step24_verifyCumulativeStatusData() {
        val expectedRowCount =
            CUMULATIVE_CHARACTER_COUNT * CUMULATIVE_MAX_LEVEL * CUMULATIVE_STATUS_COUNT_PER_LEVEL
        val expectedConflictCount = CUMULATIVE_CHARACTER_COUNT * CUMULATIVE_STATUS_COUNT_PER_LEVEL
        val expectedRows = cumulativeStatusData.associate { entity ->
            CumulativeStatusRowKey(
                entity.characterPk,
                entity.level,
                entity.statusType
            ) to entity.value
        }
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)

        databaseHelper.use { helper ->
            val select = Select(Step23CharacterStatusDetail::class).where {
                Step23CharacterStatusDetail::updateMethod eq CUMULATIVE_DATA_STEP
            }
            val actualRows = helper.executeSelectAsMapList(select)
            val actualValues = actualRows.associate { row ->
                CumulativeStatusRowKey(
                    characterPk = (row.getValue("CS23D_CHARACTER_PK") as Number).toInt(),
                    level = (row.getValue("CS23D_LEVEL") as Number).toInt(),
                    statusType = row.getValue("CS23D_STATUS_TYPE") as String,
                ) to (row.getValue("CS23D_VALUE") as Number).toInt()
            }
            val actualCountByCharacterAndLevel = actualValues.keys
                .groupingBy { key -> key.characterPk to key.level }
                .eachCount()
            val actualTotalCount = getTableRows(
                helper.readableDatabase,
                CharacterStatusV2::class.getTableName(),
            )

            assertEquals(
                "step23のUPSERT処理件数が不正です。",
                expectedRowCount,
                cumulativeUpsertAffectedRows
            )
            assertEquals("step23対象行数が不正です。", expectedRowCount, actualRows.size)
            assertEquals("step23の複合キーまたは値が不正です。", expectedRows, actualValues)
            assertTrue(
                "characterPk・levelごとのステータス件数が3件ではありません。",
                actualCountByCharacterAndLevel.values.all { count ->
                    count == CUMULATIVE_STATUS_COUNT_PER_LEVEL
                },
            )
            assertEquals(
                "step23の新規追加件数が不正です。",
                preCumulativeStatusRowCount + expectedRowCount - expectedConflictCount,
                actualTotalCount,
            )
        }
    }

    /**
     * ## step25 ステータス累計SELECT
     * ### CHARACTER_STATUS単体とCHARACTER_STATIC_INFOへのLEFT JOINでlevel最大値とvalue累計を取得する
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Test
    fun step25_selectCumulativeStatus() {
        val cumulativeCharacterPks = AndroidTestSeedData.cumulativeCharacterPks()
        val statusSelect = Select(Step25CharacterStatusAggregate::class)
            .where {
                Step25CharacterStatusAggregate::updateMethod eq CUMULATIVE_DATA_STEP
            }
            .having {
                Step25CharacterStatusAggregate::updateMethod eq CUMULATIVE_DATA_STEP
            }
            .order {
                Step25CharacterStatusAggregate::characterPk.asc
                Step25CharacterStatusAggregate::statusType.asc
            }
        val staticInfo = TableRef(Step25CharacterStaticInfo::class, "CSI23")
        val joinedStatus = TableRef(Step25JoinedCharacterStatusAggregate::class, "CS23J")
        val joinSelect = Select(staticInfo)
            .join(JoinType.LEFT, joinedStatus) {
                staticInfo[Step25CharacterStaticInfo::characterPk] eq
                        joinedStatus[Step25JoinedCharacterStatusAggregate::characterPk]
                joinedStatus[Step25JoinedCharacterStatusAggregate::updateMethod] eq CUMULATIVE_DATA_STEP
            }
            .where {
                staticInfo[Step25CharacterStaticInfo::characterPk] inList cumulativeCharacterPks
            }
            .order {
                Step25CharacterStaticInfo::characterPk.asc
                Step25JoinedCharacterStatusAggregate::statusType.asc
            }
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)

        databaseHelper.use { helper ->
            cumulativeStatusResult = helper.executeSelectAsMapList(statusSelect)
            cumulativeJoinResult = helper.executeSelectAsMapList(joinSelect)
            cumulativeStatusQuery = statusSelect.query
            cumulativeJoinQuery = joinSelect.query
            cumulativeStatusBindValues = statusSelect.bindValues.toList()
            cumulativeJoinBindValues = joinSelect.bindValues.toList()

            AndroidTestCsvExporter.exportSelectMapResultToDownload(
                context = context,
                stepName = testName.methodName,
                resultName = "select_cumulative_CHARACTER_STATUS",
                rows = cumulativeStatusResult,
            )
            AndroidTestCsvExporter.exportSelectMapResultToDownload(
                context = context,
                stepName = testName.methodName,
                resultName = "select_cumulative_CSI_CS",
                rows = cumulativeJoinResult,
            )
        }
    }

    /**
     * ## step26 ステータス累計検証
     * ### 単体集計とLEFT JOIN集計の結果、生成SQLおよびバインド値順を検証する
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Test
    fun step26_verifyCumulativeStatus() {
        val expectedStatusValues = expectedCumulativeStatusValues()
        val actualStatusValues = cumulativeStatusResult.associate { row ->
            CumulativeStatusKey(
                characterPk = (row.getValue("CS23_CHARACTER_PK") as Number).toInt(),
                statusType = row.getValue("CS23_STATUS_TYPE") as String,
            ) to CumulativeStatusValue(
                maxLevel = (row.getValue("CS23_MAX_LEVEL") as Number).toInt(),
                totalValue = (row.getValue("CS23_TOTAL_VALUE") as Number).toLong(),
            )
        }
        val characterNames = AndroidTestSeedData.characterStaticInfoList
            .take(CUMULATIVE_CHARACTER_COUNT)
            .associate { entity -> entity.characterPk to entity.characterName }
        val expectedJoinValues = expectedStatusValues.mapKeys { (key) ->
            CumulativeJoinKey(
                characterPk = key.characterPk,
                characterName = characterNames.getValue(key.characterPk),
                statusType = key.statusType,
            )
        }
        val actualJoinValues = cumulativeJoinResult.associate { row ->
            CumulativeJoinKey(
                characterPk = (row.getValue("CSI23_CHARACTER_PK") as Number).toInt(),
                characterName = row.getValue("CSI23_CHARACTER_NAME") as String,
                statusType = row.getValue("CS23J_STATUS_TYPE") as String,
            ) to CumulativeStatusValue(
                maxLevel = (row.getValue("CS23J_MAX_LEVEL") as Number).toInt(),
                totalValue = (row.getValue("CS23J_TOTAL_VALUE") as Number).toLong(),
            )
        }
        val expectedCharacterPks = AndroidTestSeedData.cumulativeCharacterPks()

        assertEquals("CHARACTER_STATUS単体の累計件数が不正です。", 50, actualStatusValues.size)
        assertEquals(
            "CHARACTER_STATUS単体の累計が不正です。",
            expectedStatusValues,
            actualStatusValues
        )
        assertEquals("LEFT JOINの累計件数が不正です。", 50, actualJoinValues.size)
        assertEquals("LEFT JOINの累計が不正です。", expectedJoinValues, actualJoinValues)
        assertEquals(
            "select CS23.CHARACTER_PK as CS23_CHARACTER_PK, " +
                    "CS23.STATUS_TYPE as CS23_STATUS_TYPE, " +
                    "max(CS23.LEVEL) as CS23_MAX_LEVEL, " +
                    "sum(CS23.VALUE) as CS23_TOTAL_VALUE " +
                    "from CHARACTER_STATUS CS23 " +
                    "where CS23.UPDATE_METHOD = ? " +
                    "group by CS23.CHARACTER_PK, CS23.STATUS_TYPE " +
                    "having CS23.UPDATE_METHOD = ? " +
                    "order by CS23.CHARACTER_PK asc nulls first, CS23.STATUS_TYPE asc nulls first",
            cumulativeStatusQuery,
        )
        assertEquals(
            listOf(CUMULATIVE_DATA_STEP, CUMULATIVE_DATA_STEP),
            cumulativeStatusBindValues,
        )
        assertEquals(
            "select CSI23.CHARACTER_PK as CSI23_CHARACTER_PK, " +
                    "CSI23.CHARACTER_NAME as CSI23_CHARACTER_NAME, " +
                    "CS23J.STATUS_TYPE as CS23J_STATUS_TYPE, " +
                    "max(CS23J.LEVEL) as CS23J_MAX_LEVEL, " +
                    "sum(CS23J.VALUE) as CS23J_TOTAL_VALUE " +
                    "from CHARACTER_STATIC_INFO CSI23 " +
                    "left join CHARACTER_STATUS CS23J on " +
                    "CSI23.CHARACTER_PK = CS23J.CHARACTER_PK AND CS23J.UPDATE_METHOD = ? " +
                    "where CSI23.CHARACTER_PK in (?, ?, ?, ?, ?) " +
                    "group by CSI23.CHARACTER_PK, CSI23.CHARACTER_NAME, CS23J.STATUS_TYPE " +
                    "order by CSI23.CHARACTER_PK asc nulls first, CS23J.STATUS_TYPE asc nulls first",
            cumulativeJoinQuery,
        )
        assertEquals(
            listOf(CUMULATIVE_DATA_STEP) + expectedCharacterPks,
            cumulativeJoinBindValues,
        )
    }

    /**
     * ## step27 BLOBテーブルとView作成
     * ### BLOBデータを保持するテーブルを作成し、データ登録後にBLOB列を含むViewを作成する
     * @author Masahiro Inoue
     * @since 2026-09-04
     */
    @Test
    fun step27_createBlobTableAndView() {
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)

        databaseHelper.use { helper ->
            helper.writableDatabase.execSQL(Create(Step27BlobTable::class).build())
            helper.executeDml(
                Insert(Step27BlobTable::class).addEntity(
                    Step27BlobTable(id = 2701, payload = step27BlobPayload)
                )
            )
            helper.writableDatabase.execSQL(
                CreateView(
                    Step27BlobViewDefinition::class,
                    ViewSelect(Step27BlobTable::class),
                ).build()
            )
        }
    }

    /**
     * ## step28 View検証
     * ### step27で作成したViewからBLOB値を読み出し、内容が一致することを検証する
     * @author Masahiro Inoue
     * @since 2026-09-04
     */
    @Test
    fun step28_verifyBlobView() {
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)

        databaseHelper.use { helper ->
            step28ViewResult = helper.executeSelectAsMapList(
                Select(Step27BlobViewSelect::class)
            )
        }

        assertEquals(1, step28ViewResult.size)
        assertEquals(2701L, (step28ViewResult.single().getValue("S27V_ID") as Number).toLong())
        assertArrayEquals(
            step27BlobPayload,
            step28ViewResult.single().getValue("S27V_PAYLOAD") as ByteArray,
        )
    }

    /**
     * ## step29 BLOBデータ検索
     * ### ByteArrayを検索条件へバインドして一致するBLOB行を取得する
     * @author Masahiro Inoue
     * @since 2026-09-04
     */
    @Test
    fun step29_searchBlobData() {
        val query = Select(Step27BlobTable::class).where {
            Step27BlobTable::payload eq step27BlobPayload
        }
        val databaseHelper = createDatabaseHelper(version, *currentTableDefinitions)

        databaseHelper.use { helper ->
            step29BlobSearchResult = helper.executeSelectAsMapList(query)
            step29BlobSearchQuery = query.query
            step29BlobSearchBindValues = query.bindValues.toList()
        }
    }

    /**
     * ## step30 BLOBデータ検索検証
     * ### step29の検索結果、SQLおよびBLOBバインド値を検証する
     * @author Masahiro Inoue
     * @since 2026-09-04
     */
    @Test
    fun step30_verifyBlobSearch() {
        assertEquals(1, step29BlobSearchResult.size)
        assertEquals(2701L, (step29BlobSearchResult.single().getValue("S27_ID") as Number).toLong())
        assertArrayEquals(
            step27BlobPayload,
            step29BlobSearchResult.single().getValue("S27_PAYLOAD") as ByteArray,
        )
        assertEquals(
            "select S27.ID as S27_ID, S27.PAYLOAD as S27_PAYLOAD " +
                    "from STEP27_BLOB_TABLE S27 where S27.PAYLOAD = ?",
            step29BlobSearchQuery,
        )
        assertEquals(listOf(step27BlobPayload), step29BlobSearchBindValues)
    }

    /**
     * ## SeedData投入
     * ### SeedDataが生成したEntityからInsertを構築し、9テーブルへ投入する
     * @param databaseHelper DB操作ヘルパー
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun insertSeedData(databaseHelper: AndrOrmDatabaseHelper) {
        val data = AndroidTestSeedData.createInsertData(testStep)
        insertAll(databaseHelper, CharacterStaticInfoV1Insert::class, data.staticInfoEntities)
        insertAll(databaseHelper, ItemMasterInsert::class, data.itemEntities)
        insertAll(databaseHelper, SpellsMasterInsert::class, data.spellMasterEntities)
        insertAll(databaseHelper, CharacterStatusV1Insert::class, data.statusEntities)
        insertAll(databaseHelper, CharacterPossessionsInsert::class, data.possessionEntities)
        insertAll(databaseHelper, CharacterEquipInsert::class, data.equipEntities)
        insertAll(databaseHelper, WeaponMasteryInsert::class, data.weaponMasteryEntities)
        insertAll(databaseHelper, MagicTypeMasteryInsert::class, data.magicMasteryEntities)
        insertAll(databaseHelper, CharacterSpellsInsert::class, data.characterSpellEntities)
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
        val data = AndroidTestSeedData.createUpdateData(testStep, updateTime)
        val characterStatus = TableRef(CharacterStatusV1Update::class, "CS")
        val updateList: List<Update<*>> = listOf(
            Update(CharacterStaticInfoV1UpdateAudit::class).set {
                CharacterStaticInfoV1UpdateAudit::updateMethod assign testStep
                CharacterStaticInfoV1UpdateAudit::updateTime assign updateTime
            }.where {
                CharacterStaticInfoV1UpdateAudit::characterPk inList data.characterPks
            },
            Update(characterStatus).set {
                CharacterStatusV1Update::value becomes
                        characterStatus[CharacterStatusV1Update::value] + 5
                CharacterStatusV1Update::updateMethod assign testStep
                CharacterStatusV1Update::updateTime assign updateTime
            }.where { characterStatus[CharacterStatusV1Update::statusType] eq "INT" },
            Update(characterStatus).set {
                CharacterStatusV1Update::value becomes
                        characterStatus[CharacterStatusV1Update::value] + 3
                CharacterStatusV1Update::updateMethod assign testStep
                CharacterStatusV1Update::updateTime assign updateTime
            }.where { characterStatus[CharacterStatusV1Update::statusType] eq "VIT" },
            Update(ItemMasterUpdateEffect::class).set(data.itemEffect).where {
                ItemMasterUpdateEffect::mainEffect eq "MP"
                ItemMasterUpdateEffect::subEffect.isNull(Unit)
            },
            Update(ItemMasterUpdateEquip::class)
                .set(data.itemEquip)
                .where { ItemMasterUpdateEquip::itemType eq 6 },
            Update(ItemMasterUpdateAudit::class).set {
                ItemMasterUpdateAudit::updateMethod assign testStep
                ItemMasterUpdateAudit::updateTime assign updateTime
            }.where { ItemMasterUpdateAudit::itemPk inList data.additionalItemPks },
            Select(SpellsMasterId::class).where {
                SpellsMasterId::magicTypeId between 3 and 5
                SpellsMasterId::subEffect isNull Unit
            }.let { subQuery ->
                Update(SpellsMasterUpdate::class).set {
                    SpellsMasterUpdate::mainEffect assign "nihil"
                    SpellsMasterUpdate::subEffect assign "all status"
                    SpellsMasterUpdate::updateMethod assign testStep
                    SpellsMasterUpdate::updateTime assign updateTime
                }.where { SpellsMasterUpdate::magicId inSelect subQuery }
            },
            Update(SpellsMasterUpdate::class).set {
                SpellsMasterUpdate::updateMethod assign testStep
                SpellsMasterUpdate::updateTime assign updateTime
            }.where { SpellsMasterUpdate::magicId inList data.additionalMagicIds },
            Update(CharacterPossessionsUpdateAudit::class).set {
                CharacterPossessionsUpdateAudit::updateMethod assign testStep
                CharacterPossessionsUpdateAudit::updateTime assign updateTime
            }.where {
                CharacterPossessionsUpdateAudit::characterPk inList data.characterPks
            },
            run {
                val itemPk = "11567"
                val possessions = TableRef(CharacterPossessionsBase::class, "CPB")
                Update(CharacterEquipUpdateAudit::class)
                    .set {
                        CharacterEquipUpdateAudit::itemPk becomes itemPk
                        CharacterEquipUpdateAudit::updateMethod assign testStep
                        CharacterEquipUpdateAudit::updateTime assign updateTime
                    }.where {
                        CharacterEquipUpdateAudit::characterPk inList data.characterPks
                        CharacterEquipUpdateAudit::equipSlot eq 2
                        exists(possessions) {
                            possessions[CharacterPossessionsBase::itemPk] eq itemPk
                        }
                    }
            },
            Update(WeaponMasteryUpdateAudit::class).set {
                WeaponMasteryUpdateAudit::updateMethod assign testStep
                WeaponMasteryUpdateAudit::updateTime assign updateTime
            }.where {
                WeaponMasteryUpdateAudit::characterPk inList data.weaponMasteryCharacterPks
            },
            Update(MagicTypeMasteryUpdateAudit::class).set {
                MagicTypeMasteryUpdateAudit::updateMethod assign testStep
                MagicTypeMasteryUpdateAudit::updateTime assign updateTime
            }.where {
                MagicTypeMasteryUpdateAudit::characterPk inList data.magicTypeMasteryCharacterPks
            },
            Update(CharacterSpellsUpdateAudit::class).set {
                CharacterSpellsUpdateAudit::updateMethod assign testStep
                CharacterSpellsUpdateAudit::updateTime assign updateTime
            }.where {
                CharacterSpellsUpdateAudit::characterPk inList data.characterPks
            },
        )
        return databaseHelper.transaction {
            updateList.sumOf { update ->
                databaseHelper.executeDml(update).also {
                    Log.d(testStep, update.query)
                    Log.d(testStep, update.bindValues.toString())
                }
            }
        }
    }

    /**
     * ## UPSERTデータ実行
     * ### SeedDataが生成したEntityからUpsertを構築し、9テーブルへ投入する
     * @param databaseHelper DB操作ヘルパー
     * @param testStep 更新元として記録するテストStep
     * @return 全UPSERTの影響行数
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun upsertData(
        databaseHelper: AndrOrmDatabaseHelper,
        testStep: String,
    ): Int {
        val data = AndroidTestSeedData.createUpsertData(testStep, LocalDateTime.now())
        return databaseHelper.transaction {
            executeUpsertData(databaseHelper, data)
        }
    }

    /**
     * ## 全テーブルUPSERT実行
     * ### 生成済みの9テーブル分のEntityを順番にUPSERTする
     * @param databaseHelper DB操作ヘルパー
     * @param data 9テーブル分のUPSERT Entity
     * @return 全UPSERTの影響行数
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun executeUpsertData(
        databaseHelper: AndrOrmDatabaseHelper,
        data: SeedUpsertData,
    ): Int {
        val upsertList = listOf(
            Upsert(CharacterStaticInfoV1Upsert::class)
                .addEntities(data.staticInfoEntities)
                .onConflict {
                    key(CharacterStaticInfoV1Upsert::characterPk)
                }
                .set {
                    CharacterStaticInfoV1Upsert::userId assign excluded(CharacterStaticInfoV1Upsert::userId)
                    CharacterStaticInfoV1Upsert::characterNo assign
                            excluded(CharacterStaticInfoV1Upsert::characterNo)
                    CharacterStaticInfoV1Upsert::characterName assign
                            excluded(CharacterStaticInfoV1Upsert::characterName)
                    CharacterStaticInfoV1Upsert::updateMethod assign
                            excluded(CharacterStaticInfoV1Upsert::updateMethod)
                    CharacterStaticInfoV1Upsert::updateTime assign
                            excluded(CharacterStaticInfoV1Upsert::updateTime)
                },
            Upsert(ItemMasterUpsert::class)
                .addEntities(data.itemEntities)
                .onConflict {
                    key(ItemMasterUpsert::itemPk)
                }
                .set {
                    ItemMasterUpsert::itemType assign excluded(ItemMasterUpsert::itemType)
                    ItemMasterUpsert::itemName assign excluded(ItemMasterUpsert::itemName)
                    ItemMasterUpsert::mainEffect assign excluded(ItemMasterUpsert::mainEffect)
                    ItemMasterUpsert::subEffect assign excluded(ItemMasterUpsert::subEffect)
                    ItemMasterUpsert::equipableSlot assign excluded(ItemMasterUpsert::equipableSlot)
                    ItemMasterUpsert::updateMethod assign excluded(ItemMasterUpsert::updateMethod)
                    ItemMasterUpsert::updateTime assign excluded(ItemMasterUpsert::updateTime)
                },
            Upsert(SpellsMasterUpsert::class)
                .addEntities(data.spellMasterEntities)
                .onConflict {
                    key(SpellsMasterUpsert::magicId)
                }
                .set {
                    SpellsMasterUpsert::magicTypeId assign excluded(SpellsMasterUpsert::magicTypeId)
                    SpellsMasterUpsert::magicName assign excluded(SpellsMasterUpsert::magicName)
                    SpellsMasterUpsert::mainEffect assign excluded(SpellsMasterUpsert::mainEffect)
                    SpellsMasterUpsert::subEffect assign excluded(SpellsMasterUpsert::subEffect)
                    SpellsMasterUpsert::updateMethod assign excluded(SpellsMasterUpsert::updateMethod)
                    SpellsMasterUpsert::updateTime assign excluded(SpellsMasterUpsert::updateTime)
                },
            Upsert(CharacterStatusV1Upsert::class)
                .addEntities(data.statusEntities)
                .onConflict {
                    key(CharacterStatusV1Upsert::characterPk)
                    key(CharacterStatusV1Upsert::statusType)
                }
                .set {
                    CharacterStatusV1Upsert::value assign excluded(CharacterStatusV1Upsert::value)
                    CharacterStatusV1Upsert::updateMethod assign excluded(CharacterStatusV1Upsert::updateMethod)
                    CharacterStatusV1Upsert::updateTime assign excluded(CharacterStatusV1Upsert::updateTime)
                },
            Upsert(CharacterPossessionsUpsert::class)
                .addEntities(data.possessionEntities)
                .onConflict {
                    key(CharacterPossessionsUpsert::characterPk)
                    key(CharacterPossessionsUpsert::itemPk)
                }
                .set {
                    CharacterPossessionsUpsert::itemStatus assign
                            excluded(CharacterPossessionsUpsert::itemStatus)
                    CharacterPossessionsUpsert::updateMethod assign
                            excluded(CharacterPossessionsUpsert::updateMethod)
                    CharacterPossessionsUpsert::updateTime assign
                            excluded(CharacterPossessionsUpsert::updateTime)
                },
            Upsert(CharacterEquipUpsert::class)
                .addEntities(data.equipEntities)
                .onConflict {
                    key(CharacterEquipUpsert::characterPk)
                    key(CharacterEquipUpsert::equipSlot)
                }
                .set {
                    CharacterEquipUpsert::itemPk assign excluded(CharacterEquipUpsert::itemPk)
                    CharacterEquipUpsert::updateMethod assign excluded(CharacterEquipUpsert::updateMethod)
                    CharacterEquipUpsert::updateTime assign excluded(CharacterEquipUpsert::updateTime)
                },
            Upsert(WeaponMasteryUpsert::class)
                .addEntities(data.weaponMasteryEntities)
                .onConflict {
                    key(WeaponMasteryUpsert::characterPk)
                    key(WeaponMasteryUpsert::weaponTypeId)
                }
                .set {
                    WeaponMasteryUpsert::mastery assign excluded(WeaponMasteryUpsert::mastery)
                    WeaponMasteryUpsert::updateMethod assign excluded(WeaponMasteryUpsert::updateMethod)
                    WeaponMasteryUpsert::updateTime assign excluded(WeaponMasteryUpsert::updateTime)
                },
            Upsert(MagicTypeMasteryUpsert::class)
                .addEntities(data.magicMasteryEntities)
                .onConflict {
                    key(MagicTypeMasteryUpsert::characterPk)
                    key(MagicTypeMasteryUpsert::magicTypeMastery)
                }
                .set {
                    MagicTypeMasteryUpsert::mastery assign excluded(MagicTypeMasteryUpsert::mastery)
                    MagicTypeMasteryUpsert::updateMethod assign excluded(MagicTypeMasteryUpsert::updateMethod)
                    MagicTypeMasteryUpsert::updateTime assign excluded(MagicTypeMasteryUpsert::updateTime)
                },
            Upsert(CharacterSpellsUpsert::class)
                .addEntities(data.characterSpellEntities)
                .onConflict {
                    key(CharacterSpellsUpsert::characterPk)
                    key(CharacterSpellsUpsert::magicId)
                }
                .set {
                    CharacterSpellsUpsert::updateMethod assign excluded(CharacterSpellsUpsert::updateMethod)
                    CharacterSpellsUpsert::updateTime assign excluded(CharacterSpellsUpsert::updateTime)
                },
        )
        return databaseHelper.transaction {
            upsertList.sumOf { upsert ->
                databaseHelper.executeDml(upsert).also {
                    Log.d(testStep, upsert.query)
                    Log.d(testStep, upsert.bindValues.toString())
                }
            }
        }
    }

    /**
     * ## ABSERTデータ実行
     * ### 装備データを複合競合キー付きでABSERTする
     * @param databaseHelper DB操作ヘルパー
     * @param testStep 作成元として記録するテストStep
     * @return ABSERTの影響行数
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun absertData(
        databaseHelper: AndrOrmDatabaseHelper,
        testStep: String,
    ): Int {
        val entities = AndroidTestSeedData.createAbsertData(testStep)
        return databaseHelper.transaction {
            entities.sumOf { entity ->
                val absert =
                    Absert(CharacterEquipAbsert::class)
                        .onConflict {
                            key(CharacterEquipAbsert::characterPk)
                            key(CharacterEquipAbsert::equipSlot)
                        }.addEntity(entity)
                databaseHelper.executeDml(absert).also {
                    Log.d(Companion.testStep, absert.query)
                    Log.d(Companion.testStep, absert.bindValues.toString())
                }
            }
        }
    }

    /**
     * ## DELETEデータ実行
     * ### 全対象テーブルから指定Stepで更新された行を削除する
     * @param databaseHelper DB操作ヘルパー
     * @param targetStep 削除対象の更新元Step
     * @return 全DELETEの影響行数
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    private fun deleteData(
        databaseHelper: AndrOrmDatabaseHelper,
        targetStep: String,
    ): Long {
        val deleteStatements = listOf(
            Delete(CharacterEquipDelete::class).where {
                CharacterEquipDelete::updateMethod eq targetStep
            },
            Delete(CharacterPossessionsDelete::class).where {
                CharacterPossessionsDelete::updateMethod eq targetStep
            },
            Delete(CharacterSpellsDelete::class).where {
                CharacterSpellsDelete::updateMethod eq targetStep
            },
            Delete(CharacterStaticInfoV1Delete::class).where {
                CharacterStaticInfoV1Delete::updateMethod eq targetStep
            },
            Delete(CharacterStatusV1Delete::class).where {
                CharacterStatusV1Delete::updateMethod eq targetStep
            },
            Delete(ItemMasterDelete::class).where {
                ItemMasterDelete::updateMethod eq targetStep
            },
            Delete(MagicTypeMasteryDelete::class).where {
                MagicTypeMasteryDelete::updateMethod eq targetStep
            },
            Delete(SpellsMasterDelete::class).where {
                SpellsMasterDelete::updateMethod eq targetStep
            },
            Delete(WeaponMasteryDelete::class).where {
                WeaponMasteryDelete::updateMethod eq targetStep
            },
        )
        return databaseHelper.transaction {
            deleteStatements.sumOf { delete ->
                databaseHelper.executeDml(delete).also {
                    Log.d(testStep, delete.query)
                    Log.d(testStep, delete.bindValues.joinToString())
                }
            }
        }.toLong()
    }

    /**
     * ## Insert一括実行
     * ### InsertEntityのリストをまとめて投入する
     * @param databaseHelper DB操作ヘルパー
     * @param entityClass InsertEntityクラス
     * @param entities 投入Entityリスト
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
            .also {
                Log.d(testStep, insert.query)
                Log.d(testStep, insert.bindValues.toString())
            }
    }

    /**
     * ## Absert一括実行
     * ### 結合済みEntity一覧を1つのAbsert文として実行する
     * @param databaseHelper DB操作ヘルパー
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
    ): Long = databaseHelper.executeDml(
        Absert(entityClass).onConflict(onConflict).addEntities(entities),
    ).toLong()

    /**
     * ## 累計期待値生成
     * ### step23投入データからcharacterPk・statusType単位の最大levelとvalue合計を算出する
     * @return 累計キー別の期待値
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    private fun expectedCumulativeStatusValues(): Map<CumulativeStatusKey, CumulativeStatusValue> =
        cumulativeStatusData
            .groupBy { entity -> CumulativeStatusKey(entity.characterPk, entity.statusType) }
            .mapValues { (_, entities) ->
                CumulativeStatusValue(
                    maxLevel = entities.maxOf { entity -> entity.level },
                    totalValue = entities.sumOf { entity -> entity.value.toLong() },
                )
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
        CharacterStatusV2::class.getTableName() to listOf("CHARACTER_PK", "LEVEL", "STATUS_TYPE"),
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
        CharacterStatusV2::class.getTableName() to data.characterStatusList.map { entity ->
            listOf(entity.characterPk.toString(), entity.level.toString(), entity.statusType)
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
        val keyColumnIndexes =
            keyColumns.map { columnName -> cursor.getColumnIndexOrThrow(columnName) }
        buildMap {
            while (cursor.moveToNext()) {
                val key = keyColumnIndexes.map { columnIndex -> cursor.getString(columnIndex) }
                val row =
                    (0 until cursor.columnCount).map { columnIndex -> cursor.getString(columnIndex) }
                put(key, row)
            }
        }
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
            Log.d(testStep, selectJoin.query)
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
            Log.d(testStep, selectJoin.query)
            Log.d(testStep, selectJoin.bindValues.joinToString())
        }
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
            val deleteCount = getTableRows(
                databaseHelper.readableDatabase,
                it.getTableName(),
                "UPDATE_METHOD = '$step'"
            )
            val tableName = it.getTableName()
            deleteTableRowCounts[tableName] =
                deleteTableRowCounts.getOrDefault(tableName, 0L) + deleteCount
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
     * ## step23ステータス明細SELECT Entity
     * ### step23でUPSERTした複合キーと値を取得する
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Table("CHARACTER_STATUS", "CS23D")
    data class Step23CharacterStatusDetail(
        val characterPk: Int,
        val level: Int,
        val statusType: String,
        val value: Int,
        val updateMethod: String,
    ) : SelectEntity

    /**
     * ## CHARACTER_STATUS単体累計SELECT Entity
     * ### characterPk・statusType単位の最大levelとvalue合計を取得する
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Table("CHARACTER_STATUS", "CS23")
    data class Step25CharacterStatusAggregate(
        val characterPk: Int,
        val statusType: String,
        @Function(columnFunction = ColumnFunction.MAX, args = ["level"], alias = "MAX_LEVEL")
        val maxLevel: Int,
        @Function(columnFunction = ColumnFunction.SUM, args = ["value"], alias = "TOTAL_VALUE")
        val totalValue: Long,
        @Column(hideFromSelect = true) val level: Int = 0,
        @Column(hideFromSelect = true) val value: Int = 0,
        @Column(hideFromSelect = true) val updateMethod: String = EMPTY_STRING,
    ) : SelectEntity

    /**
     * ## LEFT JOIN元キャラクターSELECT Entity
     * ### 累計結果へcharacterPkとcharacterNameを付加する
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Table("CHARACTER_STATIC_INFO", "CSI23")
    data class Step25CharacterStaticInfo(
        val characterPk: Int,
        val characterName: String,
    ) : SelectEntity

    /**
     * ## LEFT JOIN先ステータス累計SELECT Entity
     * ### statusType単位の最大levelとvalue合計を取得する
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    @Table("CHARACTER_STATUS", "CS23J")
    data class Step25JoinedCharacterStatusAggregate(
        val statusType: String,
        @Function(columnFunction = ColumnFunction.MAX, args = ["level"], alias = "MAX_LEVEL")
        val maxLevel: Int,
        @Function(columnFunction = ColumnFunction.SUM, args = ["value"], alias = "TOTAL_VALUE")
        val totalValue: Long,
        @Column(hideFromSelect = true) val characterPk: Int = 0,
        @Column(hideFromSelect = true) val level: Int = 0,
        @Column(hideFromSelect = true) val value: Int = 0,
        @Column(hideFromSelect = true) val updateMethod: String = EMPTY_STRING,
    ) : SelectEntity

    /**
     * ## step27 BLOBテーブルEntity
     * ### BLOB検索とView作成の元となるテーブル定義を表す
     * @author Masahiro Inoue
     * @since 2026-09-04
     */
    @Table(name = "STEP27_BLOB_TABLE", alias = "S27")
    data class Step27BlobTable(
        val id: Int,
        val payload: ByteArray,
    ) : SelectEntity, InsertEntity

    /**
     * ## step27 BLOB View定義Entity
     * ### BLOB列を含むSQLite VIEWの定義を表す
     * @author Masahiro Inoue
     * @since 2026-09-04
     */
    @View(name = "STEP27_BLOB_VIEW", alias = "S27V")
    data class Step27BlobViewDefinition(
        @Column(name = "ID") val id: Int,
        @Column(name = "PAYLOAD") val payload: ByteArray,
    ) : ViewDefinitionEntity

    /**
     * ## step28 BLOB ViewSelect Entity
     * ### step27で作成したVIEWを通常SELECTで読み出す結果定義を表す
     * @author Masahiro Inoue
     * @since 2026-09-04
     */
    @View(name = "STEP27_BLOB_VIEW", alias = "S27V")
    data class Step27BlobViewSelect(
        @Column(name = "ID") val id: Int,
        @Column(name = "PAYLOAD") val payload: ByteArray,
    ) : SelectEntity

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
