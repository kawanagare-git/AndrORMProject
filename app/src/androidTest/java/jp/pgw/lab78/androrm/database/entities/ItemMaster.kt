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
 * ## アイテムマスタテーブルEntity
 * ### アイテムの種別、名称、効果および装備可能箇所を定義する
 * @author Masahiro Inoue
 * @since 2026-06-14
 */
@Projections(
    [
        Projection(
            entityNameExtend = "Insert",
            aliasExtend = "DTA",
            properties = [
                ColumnProjection("itemPk"),
                ColumnProjection("itemType"),
                ColumnProjection("itemName"),
                ColumnProjection("mainEffect"),
                ColumnProjection("subEffect"),
                ColumnProjection("equipableSlot"),
                ColumnProjection("createMethod"),
                ColumnProjection("updateMethod"),
            ],
            commonInterface = [INSERT, ABSERT],
        ),
        Projection(
            entityNameExtend = "Base",
            aliasExtend = "B",
            properties = [
                ColumnProjection("itemPk"),
                ColumnProjection("itemType"),
                ColumnProjection("itemName"),
                ColumnProjection("mainEffect"),
                ColumnProjection("subEffect"),
                ColumnProjection("equipableSlot"),
            ],
            commonInterface = [SELECT],
        ),
        Projection(
            entityNameExtend = "ItemName",
            aliasExtend = "IN",
            properties = [
                ColumnProjection("itemPk", true),
                ColumnProjection("itemName"),
            ],
            commonInterface = [SELECT],
        ),
        Projection(
            entityNameExtend = "ItemEffect",
            aliasExtend = "IE",
            properties = [
                ColumnProjection("itemPk", true),
                ColumnProjection("itemName"),
                ColumnProjection("mainEffect"),
                ColumnProjection("subEffect"),
            ],
            commonInterface = [SELECT],
        ),
        Projection(
            entityNameExtend = "Equip",
            aliasExtend = "EQ",
            properties = [
                ColumnProjection("itemPk", true),
                ColumnProjection("itemType"),
                ColumnProjection("itemName"),
                ColumnProjection("equipableSlot"),
            ],
            commonInterface = [SELECT],
        ),
        Projection(
            entityNameExtend = "UpdateEffect",
            aliasExtend = "UPS_EFF",
            properties = [
                ColumnProjection("mainEffect"),
                ColumnProjection("subEffect"),
                ColumnProjection("updateMethod"),
                ColumnProjection("updateTime"),
            ],
            commonInterface = [UPDATE],
        ),
        Projection(
            entityNameExtend = "UpdateEquip",
            aliasExtend = "UPS_EQP",
            properties = [
                ColumnProjection("itemType"),
                ColumnProjection("equipableSlot"),
                ColumnProjection("updateMethod"),
                ColumnProjection("updateTime"),
            ],
            commonInterface = [UPDATE],
        ),
        Projection(
            entityNameExtend = "UpdateAudit",
            aliasExtend = "UPD_AUDIT",
            properties = [
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
                ColumnProjection("itemPk"),
                ColumnProjection("itemType"),
                ColumnProjection("itemName"),
                ColumnProjection("mainEffect"),
                ColumnProjection("subEffect"),
                ColumnProjection("equipableSlot"),
                ColumnProjection("createMethod"),
                ColumnProjection("updateMethod"),
                ColumnProjection("updateTime"),
            ],
            commonInterface = [UPSERT],
        ),
        Projection(
            entityNameExtend = "Delete",
            properties = [
                ColumnProjection("itemPk"),
                ColumnProjection("updateMethod"),
            ],
            commonInterface = [DELETE],
        ),
    ]
)
@Table
@Index("ITEM_IDX", ["itemType", "equipableSlot"])
data class ItemMaster(
    @PrimaryKey
    val itemPk: Int,
    val itemType: Int,
    val itemName: String,
    val mainEffect: String,
    val subEffect: String?,
    val equipableSlot: Int,
    override val createMethod: String,
    override val createTime: LocalDateTime,
    override val updateMethod: String,
    override val updateTime: LocalDateTime,
) : TableDefinitionEntity, ManagementColumns
