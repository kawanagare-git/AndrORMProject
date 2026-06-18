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

    private const val CHARACTER_COUNT = 40
    private const val MASTER_COUNT = 40
    private const val BROKEN_RELATION_COUNT = 4

    private const val CREATE_METHOD = "ANDROID_TEST_SEED"
    private const val UPDATE_METHOD = "ANDROID_TEST_SEED"

    private val baseDateTime: LocalDateTime = LocalDateTime.of(2026, 6, 14, 10, 0, 0)

    private val statusTypes = arrayOf(
        "HP",
        "MP",
        "SP",
        "STR",
        "VIT",
        "DEX",
        "AGI",
        "INT",
        "WIS",
        "FTN",
    )

    private val itemTypes = arrayOf(
        1,
        2,
        3,
        4,
        5,
        6,
        7,
        8,
    )

    private val equipableSlots = arrayOf(
        1,
        2,
        3,
        4,
        5,
        6,
    )

    private val effectCategories = arrayOf(
        "HP",
        "MP",
        "SP",
        "STR",
        "VIT",
        "DEX",
        "AGI",
        "INT",
        "WIS",
        "FTN",
    )

    /** キャラクタ基本固定情報 */
    val characterStaticInfoList: List<CharacterStaticInfo> =
        (1..CHARACTER_COUNT).map { index ->
            CharacterStaticInfo(
                characterPk = index,
                userId = "U%04d".format(index),
                characterNo = index,
                characterName = "CHAR%04d".format(index),
                createMethod = CREATE_METHOD,
                createTime = baseDateTime.plusMinutes(index.toLong()),
                updateMethod = UPDATE_METHOD,
                updateTime = baseDateTime.plusMinutes(index.toLong()),
            )
        }

    /** アイテムマスタ */
    val itemMasterList: List<ItemMaster> =
        (1..MASTER_COUNT).map { index ->
            val itemType = itemTypes[(index - 1) % itemTypes.size]
            val equipableSlot = equipableSlots[(index - 1) % equipableSlots.size]

            ItemMaster(
                itemPk = index,
                itemType = itemType,
                itemName = "ITEM%04d".format(index),
                mainEffect = effectCategories[(index - 1) % effectCategories.size],
                subEffect = effectCategories[index % effectCategories.size],
                equipableSlot = equipableSlot,
                createMethod = CREATE_METHOD,
                createTime = baseDateTime.plusMinutes(index.toLong()),
                updateMethod = UPDATE_METHOD,
                updateTime = baseDateTime.plusMinutes(index.toLong()),
            )
        }

    /** 魔法マスタ */
    val spellsMasterList: List<SpellsMaster> =
        (1..MASTER_COUNT).map { index ->
            SpellsMaster(
                magicId = index,
                magicTypeId = ((index - 1) % 8) + 1,
                magicName = "MAGIC%03d".format(index),
                mainEffect = effectCategories[(index - 1) % effectCategories.size],
                subEffect = effectCategories[index % effectCategories.size],
                createMethod = CREATE_METHOD,
                createTime = baseDateTime.plusMinutes(index.toLong()),
                updateMethod = UPDATE_METHOD,
                updateTime = baseDateTime.plusMinutes(index.toLong()),
            )
        }

    /** キャラクタ最大値ステータス */
    val characterStatusList: List<CharacterStatus> =
        (1..CHARACTER_COUNT).map { index ->
            CharacterStatus(
                characterPk = index,
                statusType = statusTypes[(index - 1) % statusTypes.size],
                value = 100 + (index * 5),
                createMethod = CREATE_METHOD,
                createTime = baseDateTime.plusMinutes(index.toLong()),
                updateMethod = UPDATE_METHOD,
                updateTime = baseDateTime.plusMinutes(index.toLong()),
            )
        } + (1..BROKEN_RELATION_COUNT).map { index ->
            CharacterStatus(
                characterPk = 9000 + index,
                statusType = statusTypes[(index - 1) % statusTypes.size],
                value = 999,
                createMethod = CREATE_METHOD,
                createTime = baseDateTime.plusMinutes((9000 + index).toLong()),
                updateMethod = UPDATE_METHOD,
                updateTime = baseDateTime.plusMinutes((9000 + index).toLong()),
            )
        }

    /** キャラクタ所有全アイテム */
    val characterPossessionsList: List<CharacterPossessions> =
        (1..CHARACTER_COUNT).map { index ->
            CharacterPossessions(
                characterPk = index,
                itemPk = index,
                itemStatus = "HOLD",
                createMethod = CREATE_METHOD,
                createTime = baseDateTime.plusMinutes(index.toLong()),
                updateMethod = UPDATE_METHOD,
                updateTime = baseDateTime.plusMinutes(index.toLong()),
            )
        } + (1..BROKEN_RELATION_COUNT).map { index ->
            val brokenCharacterPk = if (index <= 2) 9000 + index else index
            val brokenItemPk = if (index <= 2) index else 9000 + index

            CharacterPossessions(
                characterPk = brokenCharacterPk,
                itemPk = brokenItemPk,
                itemStatus = "BROKEN_RELATION",
                createMethod = CREATE_METHOD,
                createTime = baseDateTime.plusMinutes((9100 + index).toLong()),
                updateMethod = UPDATE_METHOD,
                updateTime = baseDateTime.plusMinutes((9100 + index).toLong()),
            )
        }

    /** キャラクタ装備 */
    val characterEquipList: List<CharacterEquip> =
        (1..CHARACTER_COUNT).map { index ->
            CharacterEquip(
                characterPk = index,
                equipSlot = equipableSlots[(index - 1) % equipableSlots.size],
                itemPk = index,
                createMethod = CREATE_METHOD,
                createTime = baseDateTime.plusMinutes(index.toLong()),
                updateMethod = UPDATE_METHOD,
                updateTime = baseDateTime.plusMinutes(index.toLong()),
            )
        } + (1..BROKEN_RELATION_COUNT).map { index ->
            val brokenCharacterPk = if (index <= 2) 9200 + index else index
            val brokenItemPk = if (index <= 2) index else 9200 + index

            CharacterEquip(
                characterPk = brokenCharacterPk,
                equipSlot = 100 + index,
                itemPk = brokenItemPk,
                createMethod = CREATE_METHOD,
                createTime = baseDateTime.plusMinutes((9200 + index).toLong()),
                updateMethod = UPDATE_METHOD,
                updateTime = baseDateTime.plusMinutes((9200 + index).toLong()),
            )
        }

    /** 武具キャラクタ熟練度 */
    val weaponMasteryList: List<WeaponMastery> =
        (1..CHARACTER_COUNT).map { index ->
            WeaponMastery(
                characterPk = index,
                weaponTypeId = ((index - 1) % 8) + 1,
                mastery = 1 + (index % 10),
                createMethod = CREATE_METHOD,
                createTime = baseDateTime.plusMinutes(index.toLong()),
                updateMethod = UPDATE_METHOD,
                updateTime = baseDateTime.plusMinutes(index.toLong()),
            )
        } + (1..BROKEN_RELATION_COUNT).map { index ->
            WeaponMastery(
                characterPk = 9300 + index,
                weaponTypeId = index,
                mastery = 1,
                createMethod = CREATE_METHOD,
                createTime = baseDateTime.plusMinutes((9300 + index).toLong()),
                updateMethod = UPDATE_METHOD,
                updateTime = baseDateTime.plusMinutes((9300 + index).toLong()),
            )
        }

    /** 魔法キャラクタ熟練度 */
    val magicTypeMasteryList: List<MagicTypeMastery> =
        (1..CHARACTER_COUNT).map { index ->
            MagicTypeMastery(
                characterPk = index,
                magicTypeMastery = ((index - 1) % 8) + 1,
                mastery = 1 + (index % 10),
                createMethod = CREATE_METHOD,
                createTime = baseDateTime.plusMinutes(index.toLong()),
                updateMethod = UPDATE_METHOD,
                updateTime = baseDateTime.plusMinutes(index.toLong()),
            )
        } + (1..BROKEN_RELATION_COUNT).map { index ->
            MagicTypeMastery(
                characterPk = 9400 + index,
                magicTypeMastery = index,
                mastery = 1,
                createMethod = CREATE_METHOD,
                createTime = baseDateTime.plusMinutes((9400 + index).toLong()),
                updateMethod = UPDATE_METHOD,
                updateTime = baseDateTime.plusMinutes((9400 + index).toLong()),
            )
        }

    /** キャラクタ所持魔法 */
    val characterSpellsList: List<CharacterSpells> =
        (1..CHARACTER_COUNT).map { index ->
            CharacterSpells(
                characterPk = index,
                magicId = index,
                createMethod = CREATE_METHOD,
                createTime = baseDateTime.plusMinutes(index.toLong()),
                updateMethod = UPDATE_METHOD,
                updateTime = baseDateTime.plusMinutes(index.toLong()),
            )
        } + (1..BROKEN_RELATION_COUNT).map { index ->
            val brokenCharacterPk = if (index <= 2) 9500 + index else index
            val brokenMagicId = if (index <= 2) index else 9500 + index

            CharacterSpells(
                characterPk = brokenCharacterPk,
                magicId = brokenMagicId,
                createMethod = CREATE_METHOD,
                createTime = baseDateTime.plusMinutes((9500 + index).toLong()),
                updateMethod = UPDATE_METHOD,
                updateTime = baseDateTime.plusMinutes((9500 + index).toLong()),
            )
        }

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
