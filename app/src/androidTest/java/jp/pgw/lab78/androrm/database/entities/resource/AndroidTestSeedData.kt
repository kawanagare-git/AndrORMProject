package jp.pgw.lab78.androrm.database.entities.resource

import jp.pgw.lab78.androrm.database.entities.*
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

    private const val CREATE_METHOD = "ANDROID_TEST_SEED"
    private const val UPDATE_METHOD = "ANDROID_TEST_SEED"

    private val statusTypes = arrayOf(
        "HP", "MP", "SP", "STR", "VIT", "DEX", "AGI", "INT", "WIS", "FTN",
    )

    private val itemTypes = arrayOf(1, 2, 3, 4, 5, 6, 7, 8)

    private val equipableSlots = arrayOf(1, 2, 3, 4, 5, 6)

    private val magicCategories = listOf(
        "frame", "water", "wind", "earth", "thunderbolt", "light", "dark"
    )

    private val effectCategories = arrayOf(
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

    /** プライマリキー管理 */
    enum class Keys {
        CHARACTER_PK,
        ITEM_PK,
        MAGIC_ID,
        ;

        val list = mutableListOf<Int>()
    }

    /** キャラクタ基本固定情報 */
    val characterStaticInfoList: List<CharacterStaticInfo> = (1..USER_COUNT).flatMap { userIndex ->
        (1..CHARACTER_COUNT).map { charIndex ->
            val userId = "U%04d".format(userIndex)
            CharacterStaticInfo(
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
    val characterStatusList: List<CharacterStatus> =
        Keys.CHARACTER_PK.list.flatMapIndexed { index, characterPk ->
            statusTypes.mapIndexed { stsIndex, statusName ->
                CharacterStatus(
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
                CharacterStatus(
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
        }

    /** キャラクタ装備 */
    val characterEquipList: List<CharacterEquip> =
        Keys.CHARACTER_PK.list.flatMap { characterPk ->
            equipableSlots.mapIndexed { index, slot ->
                val item = characterPossessionsList.filter {
                    it.characterPk == characterPk
                }[index % (MASTER_COUNT / 2)].itemPk
                CharacterEquip(
                    characterPk = characterPk,
                    equipSlot = slot,
                    itemPk = item,
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

    /** プライマリキー作成 */
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
        "CHARACTER_STATUS" to characterStatusList.size,
        "CHARACTER_POSSESSIONS" to characterPossessionsList.size,
        "CHARACTER_EQUIP" to characterEquipList.size,
        "WEAPON_MASTERY" to weaponMasteryList.size,
        "MAGIC_TYPE_MASTERY" to magicTypeMasteryList.size,
        "CHARACTER_SPELLS" to characterSpellsList.size,
    )
}
