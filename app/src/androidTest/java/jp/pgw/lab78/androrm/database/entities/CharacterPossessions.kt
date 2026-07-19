package jp.pgw.lab78.androrm.database.entities

import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.*
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Index
import jp.pgw.lab78.androrm.common.database.annotation.PrimaryKey
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import java.time.LocalDateTime

/**
 * ## キャラクタ所持品テーブルEntity
 * ### キャラクタが所持するアイテムとその状態を定義する
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
                ColumnProjection("itemPk"),
                ColumnProjection("itemStatus"),
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
                ColumnProjection("itemPk"),
                ColumnProjection("itemStatus"),
            ],
            commonInterface = [SELECT],
        ),
        Projection(
            entityNameExtend = "UpdateAudit",
            aliasExtend = "UPD_AUDIT",
            properties = [
                ColumnProjection("characterPk"),
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
                ColumnProjection("itemPk"),
                ColumnProjection("itemStatus"),
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
@Table(alias = "CP")
@Index("ITEM_UNIQ", ["characterPk", "itemPk"])
data class CharacterPossessions(
    @PrimaryKey
    val characterPk: Int,
    @PrimaryKey
    val itemPk: Int,
    val itemStatus: String,
    val createMethod: String,
    @Column(name = "CREATE_DATETIME", default = "CURRENT_TIMESTAMP_ISO")
    val createTime: LocalDateTime,
    val updateMethod: String,
    @Column(name = "UPDATE_DATETIME", default = "CURRENT_TIMESTAMP_ISO")
    val updateTime: LocalDateTime,
) : TableDefinitionEntity
