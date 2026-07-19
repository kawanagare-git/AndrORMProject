package jp.pgw.lab78.androrm.database.entities.absert

import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.PrimaryKey
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.AbsertEntity
import java.time.LocalDateTime

/**
 * ## キャラクタ装備ABSERT用Entity
 * ### キャラクタと装備スロットを競合キーとして未登録装備だけを登録する
 * @author Masahiro Inoue
 * @since 2026-07-05
 */
@Table(name = "CHARACTER_EQUIP", alias = "CEA")
data class CharacterEquipAbsert(
    @PrimaryKey
    @Column(name = "CHARACTER_PK")
    val characterPk: Int,

    @PrimaryKey
    @Column(name = "EQUIP_SLOT")
    val equipSlot: Int,

    @Column(name = "ITEM_PK")
    val itemPk: Int?,

    @Column(name = "CREATE_METHOD")
    val createMethod: String,

    @Column(name = "CREATE_DATETIME")
    val createTime: LocalDateTime,

    @Column(name = "UPDATE_METHOD")
    val updateMethod: String,

    @Column(name = "UPDATE_DATETIME")
    val updateTime: LocalDateTime,
) : AbsertEntity
