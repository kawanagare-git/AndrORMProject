package jp.pgw.lab78.androrm.database.entities

import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.*
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import jp.pgw.lab78.androrm.common.database.annotation.Index
import jp.pgw.lab78.androrm.common.database.annotation.PrimaryKey
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.database.entities.interfaces.ManagementColumns
import java.time.LocalDateTime

/**
 * ## キャラクタ装備テーブルEntity
 * ### キャラクタごとの装備スロットと装備アイテムを定義する
 * @author Masahiro Inoue
 * @since 2026-06-14
 */
@Projections(
    [
        Projection(
            entityNameExtend = "Insert",
            aliasExtend = "DTA",
            properties = [
                ColumnProjection("characterPk"),
                ColumnProjection("equipSlot"),
                ColumnProjection("itemPk"),
                ColumnProjection("createMethod"),
                ColumnProjection("updateMethod"),
            ],
            commonInterface = [INSERT, ABSERT],
        ),
        Projection(
            entityNameExtend = "Base",
            aliasExtend = "B",
            properties = [
                ColumnProjection("characterPk"),
                ColumnProjection("equipSlot"),
                ColumnProjection("itemPk"),
            ],
            commonInterface = [SELECT],
        ),
        Projection(
            entityNameExtend = "Slot",
            aliasExtend = "S",
            properties = [
                ColumnProjection("characterPk", hideFromSelect = true),
                ColumnProjection("equipSlot"),
            ],
            commonInterface = [SELECT],
        ),
        Projection(
            entityNameExtend = "SlotItem",
            aliasExtend = "SI",
            properties = [
                ColumnProjection("characterPk", hideFromSelect = true),
                ColumnProjection("equipSlot"),
                ColumnProjection("itemPk", hideFromSelect = true),
            ],
            commonInterface = [SELECT],
        ),
        Projection(
            entityNameExtend = "UpdateAudit",
            aliasExtend = "UPD_AUDIT",
            properties = [
                ColumnProjection("characterPk"),
                ColumnProjection("equipSlot"),
                ColumnProjection("itemPk"),
                ColumnProjection("updateMethod"),
                ColumnProjection("updateTime"),
            ],
            commonInterface = [UPDATE],
        ),
        Projection(
            entityNameExtend = "Upsert",
            aliasExtend = "UPS",
            properties = [
                ColumnProjection("characterPk"),
                ColumnProjection("equipSlot"),
                ColumnProjection("itemPk"),
                ColumnProjection("createMethod"),
                ColumnProjection("updateMethod"),
                ColumnProjection("updateTime"),
            ],
            commonInterface = [UPSERT],
        ),
        Projection(
            entityNameExtend = "Delete",
            properties = [
                ColumnProjection("characterPk"),
                ColumnProjection("updateMethod"),
            ],
            commonInterface = [DELETE],
        ),
    ]
)
@Table(alias = "CE")
@Index("INDEX", ["characterPk", "itemPk"])
data class CharacterEquip(
    @PrimaryKey
    val characterPk: Int,
    @PrimaryKey
    val equipSlot: Int,
    val itemPk: Int?,
    override val createMethod: String,
    override val createTime: LocalDateTime,
    override val updateMethod: String,
    override val updateTime: LocalDateTime,
) : TableDefinitionEntity, ManagementColumns
