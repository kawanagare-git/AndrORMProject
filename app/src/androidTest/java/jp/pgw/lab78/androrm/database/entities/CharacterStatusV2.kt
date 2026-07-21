package jp.pgw.lab78.androrm.database.entities

import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.*
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import jp.pgw.lab78.androrm.common.database.annotation.*
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import java.time.LocalDateTime

/**
 * ## キャラクタステータステーブルEntity
 * ### キャラクタごとのステータス種別と値を定義する
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
                ColumnProjection("level"),
                ColumnProjection("statusType"),
                ColumnProjection("value"),
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
                ColumnProjection("statusType"),
                ColumnProjection("value"),
            ],
            commonInterface = [SELECT],
        ),
        Projection(
            entityNameExtend = "Value",
            aliasExtend = "V",
            properties = [
                ColumnProjection("characterPk", true),
                ColumnProjection("statusType"),
                ColumnProjection("value"),
            ],
            commonInterface = [SELECT],
        ),
        Projection(
            entityNameExtend = "Update",
            aliasExtend = "Upd",
            properties = [
                ColumnProjection("characterPk"),
                ColumnProjection("statusType"),
                ColumnProjection("value"),
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
                ColumnProjection("level"),
                ColumnProjection("statusType"),
                ColumnProjection("value"),
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
@Table(name = "CHARACTER_STATUS")
@Index(properties = ["characterPk"])
data class CharacterStatusV2(
    @PrimaryKey
    val characterPk: Int,
    @PrimaryKey
    @MigrationDefault("1")
    val level: Int,
    @PrimaryKey
    val statusType: String,
    val value: Int,
    val createMethod: String,
    @Column(name = "CREATE_DATETIME", default = "CURRENT_TIMESTAMP_ISO")
    val createTime: LocalDateTime,
    val updateMethod: String,
    @Column(name = "UPDATE_DATETIME", default = "CURRENT_TIMESTAMP_ISO")
    val updateTime: LocalDateTime,
) : TableDefinitionEntity
