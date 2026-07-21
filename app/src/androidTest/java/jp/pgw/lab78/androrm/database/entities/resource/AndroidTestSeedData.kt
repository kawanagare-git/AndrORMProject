package jp.pgw.lab78.androrm.database.entities.resource

import jp.pgw.lab78.androrm.database.entities.*
import jp.pgw.lab78.androrm.database.entities.absert.CharacterEquipAbsert
import jp.pgw.lab78.androrm.database.entities.insert.*
import jp.pgw.lab78.androrm.database.entities.update.*
import jp.pgw.lab78.androrm.database.entities.upsert.*
import java.time.LocalDateTime

/**
 * ## AndroidTest 用初期データ
 * ### ER 図確認用に、各テーブル 30～50 件のデータを作成する
 * ### 子テーブルには、親子関係が壊れているデータを約 10% 含める
 * @author Masahiro Inoue
 * @since 2026-06-14
 */
object AndroidTestSeedData {

    private const val USER_COUNT = 10
    private const val CHARACTER_COUNT = 4
    private const val MASTER_COUNT = 40
    private const val BROKEN_RELATION_COUNT = 4
    private const val UPDATE_TARGET_PERCENT = 30
    private const val UNREGISTERED_ITEM_PK_BASE = 200_000
    private const val SAVEPOINT_ITEM_PK_BASE = 300_000
    private const val SAVEPOINT_MAGIC_ID_BASE = 400_000
    private const val SAVEPOINT_CHARACTER_PK_BASE = 500_000
    private val CUMULATIVE_STATUS_OFFSETS = listOf(0, 3, 6)

    private const val CREATE_METHOD = "ANDROID_TEST_SEED"
    private const val UPDATE_METHOD = "ANDROID_TEST_SEED"

    /** step19で各テーブルへ追加を試行する件数 */
    const val SAVEPOINT_INSERT_COUNT = 10

    /** step23で使用する既存キャラクター数 */
    const val CUMULATIVE_CHARACTER_COUNT = 5

    /** step23で作成する最大レベル */
    const val CUMULATIVE_MAX_LEVEL = 10

    /** step23でレベルごとに選択するステータス数 */
    const val CUMULATIVE_STATUS_COUNT_PER_LEVEL = 3

    /** 累計対象データを識別する更新メソッド */
    const val CUMULATIVE_DATA_STEP = "step23"

    /**
     * ## step19投入データ
     * ### savepoint前後へ投入する8テーブル分のInsertEntityを保持する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    data class Step19InsertData(
        val itemMasterList: List<ItemMasterInsert>,
        val magicTypeMasteryList: List<MagicTypeMasteryInsert>,
        val spellsMasterList: List<SpellsMasterInsert>,
        val characterEquipList: List<CharacterEquipInsert>,
        val characterSpellsList: List<CharacterSpellsInsert>,
        val characterStaticInfoList: List<CharacterStaticInfoV2Insert>,
        val characterStatusList: List<CharacterStatusV2Insert>,
        val characterPossessionsList: List<CharacterPossessionsInsert>,
    )

    /**
     * ## step21投入データ
     * ### step03由来とstep19由来を結合した9テーブル分のAbsert対象を保持する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    data class Step21AbsertData(
        val characterStaticInfoList: List<CharacterStaticInfoV2Insert>,
        val itemMasterList: List<ItemMasterInsert>,
        val spellsMasterList: List<SpellsMasterInsert>,
        val characterStatusList: List<CharacterStatusV2Insert>,
        val characterPossessionsList: List<CharacterPossessionsInsert>,
        val characterEquipList: List<CharacterEquipInsert>,
        val weaponMasteryList: List<WeaponMasteryInsert>,
        val magicTypeMasteryList: List<MagicTypeMasteryInsert>,
        val characterSpellsList: List<CharacterSpellsInsert>,
    )

    /**
     * ## SeedData用INSERTデータ
     * ### 9テーブル分のINSERT Entityを保持する
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    data class SeedInsertData(
        val staticInfoEntities: List<CharacterStaticInfoV1Insert>,
        val itemEntities: List<ItemMasterInsert>,
        val spellMasterEntities: List<SpellsMasterInsert>,
        val statusEntities: List<CharacterStatusV1Insert>,
        val possessionEntities: List<CharacterPossessionsInsert>,
        val equipEntities: List<CharacterEquipInsert>,
        val weaponMasteryEntities: List<WeaponMasteryInsert>,
        val magicMasteryEntities: List<MagicTypeMasteryInsert>,
        val characterSpellEntities: List<CharacterSpellsInsert>,
    )

    /**
     * ## SeedData用UPDATEデータ
     * ### UPDATE文へ設定するEntityと対象キーを保持する
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    data class SeedUpdateData(
        val characterPks: List<Int>,
        val weaponMasteryCharacterPks: List<Int>,
        val magicTypeMasteryCharacterPks: List<Int>,
        val itemEffect: ItemMasterUpdateEffect,
        val itemEquip: ItemMasterUpdateEquip,
        val additionalItemPks: List<Int>,
        val additionalMagicIds: List<Int>,
    )

    /**
     * ## SeedData用UPSERTデータ
     * ### 9テーブル分のUPSERT Entityを保持する
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    data class SeedUpsertData(
        val staticInfoEntities: List<CharacterStaticInfoV1Upsert>,
        val itemEntities: List<ItemMasterUpsert>,
        val spellMasterEntities: List<SpellsMasterUpsert>,
        val statusEntities: List<CharacterStatusV1Upsert>,
        val possessionEntities: List<CharacterPossessionsUpsert>,
        val equipEntities: List<CharacterEquipUpsert>,
        val weaponMasteryEntities: List<WeaponMasteryUpsert>,
        val magicMasteryEntities: List<MagicTypeMasteryUpsert>,
        val characterSpellEntities: List<CharacterSpellsUpsert>,
    )

    val statusTypes = arrayOf(
        "HP", "MP", "SP", "STR", "VIT", "DEX", "AGI", "INT", "WIS", "FTN",
    )

    val itemTypes = arrayOf(1, 2, 3, 4, 5, 6, 7, 8)

    val equipableSlots = arrayOf(1, 2, 3, 4, 5, 6)

    val magicCategories = listOf(
        "frame", "water", "wind", "earth", "thunderbolt", "light", "dark"
    )

    val effectCategories = arrayOf(
        null,
        "HP",
        null,
        "MP",
        null,
        "SP",
        null,
        "STR",
        null,
        "VIT",
        null,
        "DEX",
        null,
        "AGI",
        null,
        "INT",
        null,
        "WIS",
        null,
        "FTN",
    )

    /**
     * ## プライマリキー管理列挙型
     * ### データ種別ごとに生成済みプライマリキーを保持する
     * @author Masahiro Inoue
     * @since 2026-06-14
     */
    enum class Keys {
        CHARACTER_PK,
        ITEM_PK,
        MAGIC_ID,
        ;

        val list = mutableListOf<Int>()
    }

    /** キャラクタ基本固定情報 */
    val characterStaticInfoList: List<CharacterStaticInfoV1> =
        (1..USER_COUNT).flatMap { userIndex ->
            (1..CHARACTER_COUNT).map { charIndex ->
                val userId = "U%04d".format(userIndex)
                CharacterStaticInfoV1(
                    characterPk = createPk(Keys.CHARACTER_PK, userId, charIndex),
                    userId = userId,
                    characterNo = charIndex,
                    characterName = "CHAR%04d-%01d".format(userIndex, charIndex),
                    createMethod = CREATE_METHOD,
                    createTime = LocalDateTime.now(),
                    updateMethod = UPDATE_METHOD,
                    updateTime = LocalDateTime.now(),
                )
            }
        }

    /** アイテムマスタ */
    val itemMasterList: List<ItemMaster> = (1..MASTER_COUNT).map { index ->
        val itemType = itemTypes[(index) % itemTypes.size]
        val equipableSlot = equipableSlots[(index) % equipableSlots.size]
        val effectCategory = effectCategories[itemType % effectCategories.size]
        val mainEffectCategory =
            effectCategory ?: effectCategories[(itemType + 1) % effectCategories.size]!!
        ItemMaster(
            itemPk = createPk(Keys.ITEM_PK, index),
            itemType = itemType,
            itemName = "ITEM%04d".format(index),
            mainEffect = mainEffectCategory,
            subEffect = effectCategory,
            equipableSlot = equipableSlot,
            createMethod = CREATE_METHOD,
            createTime = LocalDateTime.now(),
            updateMethod = UPDATE_METHOD,
            updateTime = LocalDateTime.now(),
        )
    }

    /** ITEM_MASTER に存在しない所持品（偶数 ITEM_PK） */
    private val unregisteredPossessions: List<Pair<Int, Int>> =
        characterStaticInfoList
            .drop(characterStaticInfoList.size * UPDATE_TARGET_PERCENT / 100)
            .take(5)
            .mapIndexed { index, character ->
                character.characterPk to UNREGISTERED_ITEM_PK_BASE + (index + 1) * 2
            }

    /** ITEM_MASTER に存在しない装備（奇数 ITEM_PK） */
    private val unregisteredEquips: List<Triple<Int, Int, Int>> =
        characterStaticInfoList
            .drop(characterStaticInfoList.size * UPDATE_TARGET_PERCENT / 100)
            .take(5)
            .mapIndexed { index, character ->
                Triple(
                    character.characterPk,
                    2 + index % 4,
                    UNREGISTERED_ITEM_PK_BASE + index * 2 + 1,
                )
            }

    /** 魔法マスタ */
    val spellsMasterList: List<SpellsMaster> =
        magicCategories.flatMapIndexed { effectIndex, effect ->
            (1..MASTER_COUNT).map { index ->
                val subEffect =
                    effectCategories[(index + 1) % effectCategories.size]
                SpellsMaster(
                    magicId = createPk(Keys.MAGIC_ID, effectIndex * 10_0000, index * 10),
                    magicTypeId = index % 12,
                    magicName = "MAGIC%03d-%01d".format(effectIndex, index),
                    mainEffect = effect,
                    subEffect = subEffect,
                    createMethod = CREATE_METHOD,
                    createTime = LocalDateTime.now(),
                    updateMethod = UPDATE_METHOD,
                    updateTime = LocalDateTime.now(),
                )
            }
        }

    /** キャラクタ最大値ステータス */
    val characterStatusV1Lists: List<CharacterStatusV1> =
        Keys.CHARACTER_PK.list.flatMapIndexed { index, characterPk ->
            statusTypes.mapIndexed { stsIndex, statusName ->
                CharacterStatusV1(
                    characterPk = characterPk,
                    statusType = statusName,
                    value = 100 * index + stsIndex,
                    createMethod = CREATE_METHOD,
                    createTime = LocalDateTime.now(),
                    updateMethod = UPDATE_METHOD,
                    updateTime = LocalDateTime.now(),
                )
            }
        } + (1..BROKEN_RELATION_COUNT).flatMap { index ->
            statusTypes.map { statusName ->
                CharacterStatusV1(
                    characterPk = 9000 + index,
                    statusType = statusName,
                    value = 999,
                    createMethod = CREATE_METHOD,
                    createTime = LocalDateTime.now(),
                    updateMethod = UPDATE_METHOD,
                    updateTime = LocalDateTime.now(),
                )
            }
        }

    /** キャラクタ所有全アイテム */
    val characterPossessionsList: List<CharacterPossessions> =
        Keys.CHARACTER_PK.list.flatMap { characterPk ->
            Keys.ITEM_PK.list.map { itemPk ->
                CharacterPossessions(
                    characterPk = characterPk,
                    itemPk = itemPk,
                    itemStatus = "HOLD",
                    createMethod = CREATE_METHOD,
                    createTime = LocalDateTime.now(),
                    updateMethod = UPDATE_METHOD,
                    updateTime = LocalDateTime.now(),
                )
            }
        } + (1..BROKEN_RELATION_COUNT).map { index ->
            val brokenCharacterPk = if (index <= 2) 9000 + index else index
            val brokenItemPk = if (index <= 2) index else 9000 + index

            CharacterPossessions(
                characterPk = brokenCharacterPk,
                itemPk = brokenItemPk,
                itemStatus = "BROKEN_RELATION",
                createMethod = CREATE_METHOD,
                createTime = LocalDateTime.now(),
                updateMethod = UPDATE_METHOD,
                updateTime = LocalDateTime.now(),
            )
        } + unregisteredPossessions.map { (characterPk, itemPk) ->
            CharacterPossessions(
                characterPk = characterPk,
                itemPk = itemPk,
                itemStatus = "ITEM_MASTER_NOT_FOUND",
                createMethod = CREATE_METHOD,
                createTime = LocalDateTime.now(),
                updateMethod = UPDATE_METHOD,
                updateTime = LocalDateTime.now(),
            )
        }

    /** キャラクタ装備 */
    val characterEquipList: List<CharacterEquip> =
        Keys.CHARACTER_PK.list.flatMap { characterPk ->
            equipableSlots.mapIndexed { index, slot ->
                val unregisteredItemPk = unregisteredEquips
                    .firstOrNull { (targetCharacterPk, targetSlot) ->
                        targetCharacterPk == characterPk && targetSlot == slot
                    }?.third
                val itemPk = unregisteredItemPk ?: characterPossessionsList.filter {
                    it.characterPk == characterPk
                }[index % (MASTER_COUNT / 2)].itemPk
                CharacterEquip(
                    characterPk = characterPk,
                    equipSlot = slot,
                    itemPk = itemPk,
                    createMethod = CREATE_METHOD,
                    createTime = LocalDateTime.now(),
                    updateMethod = UPDATE_METHOD,
                    updateTime = LocalDateTime.now(),
                )
            }
        } + (1..BROKEN_RELATION_COUNT).map { index ->
            val brokenCharacterPk = if (index <= 2) 9200 + index else index
            val brokenItemPk = if (index <= 2) index else 9200 + index

            CharacterEquip(
                characterPk = brokenCharacterPk,
                equipSlot = 100 + index,
                itemPk = brokenItemPk,
                createMethod = CREATE_METHOD,
                createTime = LocalDateTime.now(),
                updateMethod = UPDATE_METHOD,
                updateTime = LocalDateTime.now(),
            )
        }

    /** 武具キャラクタ熟練度 */
    val weaponMasteryList: List<WeaponMastery> =
        Keys.CHARACTER_PK.list.mapIndexed { index, characterPk ->
            WeaponMastery(
                characterPk = characterPk,
                weaponTypeId = ((index - 1) % 8) + 1,
                mastery = 1 + (index % 10),
                createMethod = CREATE_METHOD,
                createTime = LocalDateTime.now(),
                updateMethod = UPDATE_METHOD,
                updateTime = LocalDateTime.now(),
            )
        } + (1..BROKEN_RELATION_COUNT).map { index ->
            WeaponMastery(
                characterPk = 9300 + index,
                weaponTypeId = index,
                mastery = 1,
                createMethod = CREATE_METHOD,
                createTime = LocalDateTime.now(),
                updateMethod = UPDATE_METHOD,
                updateTime = LocalDateTime.now(),
            )
        }

    /** 魔法キャラクタ熟練度 */
    val magicTypeMasteryList: List<MagicTypeMastery> =
        Keys.CHARACTER_PK.list.mapIndexed { index, characterPk ->
            MagicTypeMastery(
                characterPk = characterPk,
                magicTypeMastery = ((index - 1) % 8) + 1,
                mastery = 1 + (index % 10),
                createMethod = CREATE_METHOD,
                createTime = LocalDateTime.now(),
                updateMethod = UPDATE_METHOD,
                updateTime = LocalDateTime.now(),
            )
        } + (1..BROKEN_RELATION_COUNT).map { index ->
            MagicTypeMastery(
                characterPk = 9400 + index,
                magicTypeMastery = index,
                mastery = 1,
                createMethod = CREATE_METHOD,
                createTime = LocalDateTime.now(),
                updateMethod = UPDATE_METHOD,
                updateTime = LocalDateTime.now(),
            )
        }

    /** キャラクタ所持魔法 */
    val characterSpellsList: List<CharacterSpells> =
        Keys.CHARACTER_PK.list.flatMapIndexed { index, characterPk ->
            val picker = index % 10
            Keys.MAGIC_ID.list.filterIndexed { magicIndex, _ ->
                magicIndex % 10 == picker
            }.map { magicId ->
                CharacterSpells(
                    characterPk = characterPk,
                    magicId = magicId,
                    createMethod = CREATE_METHOD,
                    createTime = LocalDateTime.now(),
                    updateMethod = UPDATE_METHOD,
                    updateTime = LocalDateTime.now(),
                )
            }
        } + (1..BROKEN_RELATION_COUNT).map { index ->
            val brokenCharacterPk = if (index <= 2) 9500 + index else index
            val brokenMagicId = if (index <= 2) index else 9500 + index

            CharacterSpells(
                characterPk = brokenCharacterPk,
                magicId = brokenMagicId,
                createMethod = CREATE_METHOD,
                createTime = LocalDateTime.now(),
                updateMethod = UPDATE_METHOD,
                updateTime = LocalDateTime.now(),
            )
        }

    /**
     * ## step23累計対象キャラクターキー
     * ### 初期データから既存5キャラクターのcharacterPkを取得する
     * @return 累計対象characterPk
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    fun cumulativeCharacterPks(): List<Int> =
        characterStaticInfoList
            .take(CUMULATIVE_CHARACTER_COUNT)
            .map { entity -> entity.characterPk }

    /**
     * ## step23累計対象データ生成
     * ### levelごとに添字を循環させて3種類のstatusTypeを規則的に分散する
     * @param updateTime UPSERT更新日時
     * @return 既存5キャラクターへUPSERTする150件
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    fun createCumulativeStatusData(
        updateTime: LocalDateTime,
    ): List<CharacterStatusV2Upsert> =
        cumulativeCharacterPks().flatMapIndexed { characterIndex, characterPk ->
            (1..CUMULATIVE_MAX_LEVEL).flatMap { level ->
                CUMULATIVE_STATUS_OFFSETS.map { statusOffset ->
                    val statusIndex = (level - 1 + statusOffset) % statusTypes.size
                    CharacterStatusV2Upsert(
                        characterPk = characterPk,
                        level = level,
                        statusType = statusTypes[statusIndex],
                        value = (characterIndex + 1) * 10_000 + level * 100 + statusIndex,
                        createMethod = CUMULATIVE_DATA_STEP,
                        updateMethod = CUMULATIVE_DATA_STEP,
                        updateTime = updateTime,
                    )
                }
            }
        }

    /**
     * ## SAVEPOINT用投入データ生成
     * ### 既存キー範囲と重複しない10件を各テーブル用に生成し、所持品の10件目だけ複合キーを1件目と重複させる
     * @param testStep 作成元として記録するテストStep
     * @return savepoint前後に投入するInsertEntity一覧
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    fun createSavepointInsertData(testStep: String): Step19InsertData {
        val existingCharacterPks = characterStaticInfoList
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
            CharacterStatusV2Insert(
                characterPk = SAVEPOINT_CHARACTER_PK_BASE + index,
                level = index / statusTypes.size + 1,
                statusType = statusTypes[index % statusTypes.size],
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
     * @param testStep step19由来データへ設定するテストStep
     * @return テーブル別の一括Absert対象
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    fun createStep21AbsertData(testStep: String): Step21AbsertData {
        val savepointData = createSavepointInsertData(testStep)
        return Step21AbsertData(
            characterStaticInfoList = characterStaticInfoList.map { entity ->
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
            itemMasterList = itemMasterList.map { entity ->
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
            spellsMasterList = spellsMasterList.map { entity ->
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
            characterStatusList = characterStatusV1Lists.map { entity ->
                CharacterStatusV2Insert(
                    characterPk = entity.characterPk,
                    level = 1,
                    statusType = entity.statusType,
                    value = entity.value,
                    createMethod = "step03",
                    updateMethod = "step03",
                )
            } + savepointData.characterStatusList,
            characterPossessionsList = characterPossessionsList.map { entity ->
                CharacterPossessionsInsert(
                    characterPk = entity.characterPk,
                    itemPk = entity.itemPk,
                    itemStatus = entity.itemStatus,
                    createMethod = "step03",
                    updateMethod = "step03",
                )
            } + savepointData.characterPossessionsList,
            characterEquipList = characterEquipList.map { entity ->
                CharacterEquipInsert(
                    characterPk = entity.characterPk,
                    equipSlot = entity.equipSlot,
                    itemPk = entity.itemPk!!,
                    createMethod = "step03",
                    updateMethod = "step03",
                )
            } + savepointData.characterEquipList,
            weaponMasteryList = weaponMasteryList.map { entity ->
                WeaponMasteryInsert(
                    characterPk = entity.characterPk,
                    weaponTypeId = entity.weaponTypeId,
                    createMethod = "step03",
                    updateMethod = "step03",
                )
            },
            magicTypeMasteryList = magicTypeMasteryList.map { entity ->
                MagicTypeMasteryInsert(
                    characterPk = entity.characterPk,
                    magicTypeMastery = entity.magicTypeMastery,
                    mastery = entity.mastery,
                    createMethod = "step03",
                    updateMethod = "step03",
                )
            } + savepointData.magicTypeMasteryList,
            characterSpellsList = characterSpellsList.map { entity ->
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
     * ## SeedData用INSERT Entity生成
     * ### 初期データを9テーブル分のInsertEntityへ変換する
     * @param testStep 作成元として記録するテストStep
     * @return 9テーブル分のINSERT Entity
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    fun createInsertData(testStep: String): SeedInsertData = SeedInsertData(
        staticInfoEntities = characterStaticInfoList.map { entity ->
                CharacterStaticInfoV1Insert(
                    characterPk = entity.characterPk,
                    userId = entity.userId,
                    characterNo = entity.characterNo,
                    characterName = entity.characterName,
                    createMethod = testStep,
                    updateMethod = testStep,
                )
            },
        itemEntities = itemMasterList.map { entity ->
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
        spellMasterEntities = spellsMasterList.map { entity ->
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
        statusEntities = characterStatusV1Lists.map { entity ->
                CharacterStatusV1Insert(
                    characterPk = entity.characterPk,
                    statusType = entity.statusType,
                    value = entity.value,
                    createMethod = testStep,
                    updateMethod = testStep,
                )
            },
        possessionEntities = characterPossessionsList.map { entity ->
                CharacterPossessionsInsert(
                    characterPk = entity.characterPk,
                    itemPk = entity.itemPk,
                    itemStatus = entity.itemStatus,
                    createMethod = testStep,
                    updateMethod = testStep,
                )
            },
        equipEntities = characterEquipList.map { entity ->
                CharacterEquipInsert(
                    characterPk = entity.characterPk,
                    equipSlot = entity.equipSlot,
                    itemPk = entity.itemPk!!,
                    createMethod = testStep,
                    updateMethod = testStep,
                )
            },
        weaponMasteryEntities = weaponMasteryList.map { entity ->
                WeaponMasteryInsert(
                    characterPk = entity.characterPk,
                    weaponTypeId = entity.weaponTypeId,
                    createMethod = testStep,
                    updateMethod = testStep,
                )
            },
        magicMasteryEntities = magicTypeMasteryList.map { entity ->
                MagicTypeMasteryInsert(
                    characterPk = entity.characterPk,
                    magicTypeMastery = entity.magicTypeMastery,
                    mastery = entity.mastery,
                    createMethod = testStep,
                    updateMethod = testStep,
                )
            },
        characterSpellEntities = characterSpellsList.map { entity ->
                CharacterSpellsInsert(
                    characterPk = entity.characterPk,
                    magicId = entity.magicId,
                    createMethod = testStep,
                    updateMethod = testStep,
                )
            },
        )

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
        val targetCount = itemMasterList.size * UPDATE_TARGET_PERCENT / 100
        val existingTargets = itemMasterList.filter { entity ->
            (entity.mainEffect == "MP" && entity.subEffect == null) || entity.itemType == 6
        }
        val existingItemPks = existingTargets.map { entity -> entity.itemPk }.toSet()
        val additionalTargets = itemMasterList
            .filter { entity -> entity.itemPk !in existingItemPks }
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
        val targetCount = spellsMasterList.size * UPDATE_TARGET_PERCENT / 100
        val existingTargets = spellsMasterList.filter { entity ->
            entity.magicTypeId in 3..5 && entity.subEffect == null
        }
        val existingMagicIds = existingTargets.map { entity -> entity.magicId }.toSet()
        val additionalTargets = spellsMasterList
            .filter { entity -> entity.magicId !in existingMagicIds }
            .take((targetCount - existingTargets.size).coerceAtLeast(0))
        return existingTargets + additionalTargets
    }

    /**
     * ## SeedData用UPDATE Entity生成
     * ### UPDATE文へ設定するEntityと対象キーを生成する
     * @param testStep 更新元として記録するテストStep
     * @param updateTime 更新日時
     * @return UPDATE文へ設定するEntityと対象キー
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    fun createUpdateData(
        testStep: String,
        updateTime: LocalDateTime,
    ): SeedUpdateData {
        val characterPks = characterStaticInfoList.targetSegment(0)
            .map { entity -> entity.characterPk }
        val weaponMasteryCharacterPks = weaponMasteryList.targetSegment(0)
            .map { entity -> entity.characterPk }
        val magicTypeMasteryCharacterPks = magicTypeMasteryList.targetSegment(0)
            .map { entity -> entity.characterPk }
        val existingUpdateItemPks = itemMasterList.filter { entity ->
            (entity.mainEffect == "MP" && entity.subEffect == null) || entity.itemType == 6
        }.map { entity -> entity.itemPk }.toSet()
        val additionalItemPks = itemMasterUpdateTargets()
            .filter { entity -> entity.itemPk !in existingUpdateItemPks }
            .map { entity -> entity.itemPk }
        val existingUpdateMagicIds = spellsMasterList.filter { entity ->
            entity.magicTypeId in 3..5 && entity.subEffect == null
        }.map { entity -> entity.magicId }.toSet()
        val additionalMagicIds = spellsMasterUpdateTargets()
            .filter { entity -> entity.magicId !in existingUpdateMagicIds }
            .map { entity -> entity.magicId }

        return SeedUpdateData(
            characterPks = characterPks,
            weaponMasteryCharacterPks = weaponMasteryCharacterPks,
            magicTypeMasteryCharacterPks = magicTypeMasteryCharacterPks,
            itemEffect = ItemMasterUpdateEffect(
                mainEffect = "STR",
                subEffect = "HP",
                updateMethod = testStep,
                updateTime = updateTime,
            ),
            itemEquip = ItemMasterUpdateEquip(
                itemType = 3,
                equipableSlot = 4,
                updateMethod = testStep,
                updateTime = updateTime,
            ),
            additionalItemPks = additionalItemPks,
            additionalMagicIds = additionalMagicIds,
        )
    }

    /**
     * ## SeedData用UPSERT Entity生成
     * ### 9テーブル分のUPSERT Entityを生成する
     * @param testStep 更新元として記録するテストStep
     * @param updateTime 更新日時
     * @return 9テーブル分のUPSERT Entity
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    fun createUpsertData(
        testStep: String,
        updateTime: LocalDateTime,
    ): SeedUpsertData {
        val upsertCharacterPks = characterStaticInfoList.targetSegment(1)
            .map { entity -> entity.characterPk }.toSet()
        val updatedItemPks = itemMasterUpdateTargets().map { entity -> entity.itemPk }.toSet()
        val upsertItemTargets = itemMasterList
            .filter { entity -> entity.itemPk !in updatedItemPks }
            .take(itemMasterList.size * UPDATE_TARGET_PERCENT / 100)
        val updatedMagicIds = spellsMasterUpdateTargets().map { entity -> entity.magicId }.toSet()
        val upsertSpellsMasterTargets = spellsMasterList
            .filter { entity -> entity.magicId !in updatedMagicIds }
            .take(spellsMasterList.size * UPDATE_TARGET_PERCENT / 100)
        val upsertStatusTypes = setOf("HP", "MP", "SP")

        val staticInfoEntities = characterStaticInfoList
            .filter { entity -> entity.characterPk in upsertCharacterPks }
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
        val itemEntities = upsertItemTargets.map { entity ->
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
        val spellMasterEntities = upsertSpellsMasterTargets.map { entity ->
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
        val statusEntities = characterStatusV1Lists
            .filter { entity -> entity.statusType in upsertStatusTypes }
            .map { entity ->
                CharacterStatusV1Upsert(
                    characterPk = entity.characterPk,
                    statusType = entity.statusType,
                    value = entity.value,
                    createMethod = testStep,
                    updateMethod = testStep,
                    updateTime = updateTime,
                )
            }
        val possessionEntities = characterPossessionsList
            .filter { entity -> entity.characterPk in upsertCharacterPks }
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
        val equipEntities = characterEquipList
            .filter { entity -> entity.characterPk in upsertCharacterPks }
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
        val weaponMasteryEntities = weaponMasteryList.targetSegment(1).map { entity ->
            WeaponMasteryUpsert(
                characterPk = entity.characterPk,
                weaponTypeId = entity.weaponTypeId,
                mastery = entity.mastery,
                createMethod = testStep,
                updateMethod = testStep,
                updateTime = updateTime,
            )
        }
        val magicMasteryEntities = magicTypeMasteryList.targetSegment(1).map { entity ->
            MagicTypeMasteryUpsert(
                characterPk = entity.characterPk,
                magicTypeMastery = entity.magicTypeMastery,
                mastery = entity.mastery,
                createMethod = testStep,
                updateMethod = testStep,
                updateTime = updateTime,
            )
        }
        val characterSpellEntities = characterSpellsList
            .filter { entity -> entity.characterPk in upsertCharacterPks }
            .map { entity ->
                CharacterSpellsUpsert(
                    characterPk = entity.characterPk,
                    magicId = entity.magicId,
                    createMethod = testStep,
                    updateMethod = testStep,
                    updateTime = updateTime,
                )
            }

        return SeedUpsertData(
            staticInfoEntities = staticInfoEntities,
            itemEntities = itemEntities,
            spellMasterEntities = spellMasterEntities,
            statusEntities = statusEntities,
            possessionEntities = possessionEntities,
            equipEntities = equipEntities,
            weaponMasteryEntities = weaponMasteryEntities,
            magicMasteryEntities = magicMasteryEntities,
            characterSpellEntities = characterSpellEntities,
            )
        }


    /**
     * ## SeedData用ABSERT Entity生成
     * ### 装備データのABSERT対象を生成する
     * @param testStep 作成元として記録するテストStep
     * @return ABSERT対象Entity
     * @author Masahiro Inoue
     * @since 2026-06-16
     */
    fun createAbsertData(testStep: String): List<CharacterEquipAbsert> =
        listOf(
            CharacterEquipAbsert(
                characterPk = 77274,
                equipSlot = 4,
                itemPk = 11637,
                createMethod = testStep,
                createTime = LocalDateTime.now(),
                updateMethod = testStep,
                updateTime = LocalDateTime.now(),
            ),
            CharacterEquipAbsert(
                characterPk = 77274,
                equipSlot = 5,
                itemPk = 11638,
                createMethod = testStep,
                createTime = LocalDateTime.now(),
                updateMethod = testStep,
                updateTime = LocalDateTime.now(),
            ),
            CharacterEquipAbsert(
                characterPk = 77274,
                equipSlot = 6,
                itemPk = 11639,
                createMethod = testStep,
                createTime = LocalDateTime.now(),
                updateMethod = testStep,
                updateTime = LocalDateTime.now(),
            ),
            CharacterEquipAbsert(
                characterPk = 77274,
                equipSlot = 7,
                itemPk = null,
                createMethod = testStep,
                createTime = LocalDateTime.now(),
                updateMethod = testStep,
                updateTime = LocalDateTime.now(),
            ),
            CharacterEquipAbsert(
                characterPk = 77274,
                equipSlot = 8,
                itemPk = 10060,
                createMethod = testStep,
                createTime = LocalDateTime.now(),
                updateMethod = testStep,
                updateTime = LocalDateTime.now(),
            ),
        )


    /**
     * ## プライマリキー作成
     * ### キー構成値からテスト用プライマリキーを生成して管理一覧へ追加する
     * @param keys 生成キーの種別
     * @param keyParts キー生成に使用する値
     * @return 生成したプライマリキー
     * @author Masahiro Inoue
     * @since 2026-06-14
     */
    private fun createPk(keys: Keys, vararg keyParts: Any): Int =
        (10_000 + Math.floorMod(
            keyParts.joinToString(separator = "").hashCode(),
            90_000,
        )).also { keys.list.add(it) }

    /** テーブル別件数 */
    val rowCountByTable: Map<String, Int> = mapOf(
        "CHARACTER_STATIC_INFO" to characterStaticInfoList.size,
        "ITEM_MASTER" to itemMasterList.size,
        "SPELLS_MASTER" to spellsMasterList.size,
        "CHARACTER_STATUS" to characterStatusV1Lists.size,
        "CHARACTER_POSSESSIONS" to characterPossessionsList.size,
        "CHARACTER_EQUIP" to characterEquipList.size,
        "WEAPON_MASTERY" to weaponMasteryList.size,
        "MAGIC_TYPE_MASTERY" to magicTypeMasteryList.size,
        "CHARACTER_SPELLS" to characterSpellsList.size,
    )
}
