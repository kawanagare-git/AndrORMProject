package jp.pgw.lab78.androrm.database.entities

import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.*
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import jp.pgw.lab78.androrm.common.database.annotation.PrimaryKey
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.annotation.Unique
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.database.entities.interfaces.ManagementColumns
import java.time.LocalDateTime

/**
 * ## キャラクタ固定情報V1テーブルEntity
 * ### DBバージョン1のキャラクタ識別情報を定義する
 * ### V2との移行検証では意図的に同じテーブル名を使用するため、テーブル名重複検査を抑制する
 * @author Masahiro Inoue
 * @since 2026-06-13
 */
@Projections(
    [
        Projection(
            entityNameExtend = "Insert",
            aliasExtend = "DTA",
            properties = [
                ColumnProjection("characterPk"),
                ColumnProjection("userId"),
                ColumnProjection("characterNo"),
                ColumnProjection("characterName"),
                ColumnProjection("createMethod"),
                ColumnProjection("updateMethod"),
            ],
            commonInterface = [INSERT],
        ),
        Projection(
            entityNameExtend = "Base",
            aliasExtend = "B",
            properties = [
                ColumnProjection("characterPk"),
                ColumnProjection("userId"),
                ColumnProjection("characterNo"),
                ColumnProjection("characterName"),
            ],
            commonInterface = [SELECT],
        ),
        Projection(
            entityNameExtend = "Name",
            aliasExtend = "N",
            properties = [
                ColumnProjection("characterPk"),
                ColumnProjection("characterName"),
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
                ColumnProjection("userId"),
                ColumnProjection("characterNo"),
                ColumnProjection("characterName"),
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
@Table(name = "CHARACTER_STATIC_INFO", alias = "CSI")
@Unique(properties = ["userId", "characterNo"])
@Suppress("AndrOrmDuplicateTableNameRule")
data class CharacterStaticInfoV1(
    @PrimaryKey
    val characterPk: Int,
    val userId: String,
    val characterNo: Int,
    val characterName: String,
    override val createMethod: String,
    override val createTime: LocalDateTime,
    override val updateMethod: String,
    override val updateTime: LocalDateTime,
) : TableDefinitionEntity, ManagementColumns
