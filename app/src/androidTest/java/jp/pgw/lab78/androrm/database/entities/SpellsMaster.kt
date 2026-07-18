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

@Projections(
    [
        Projection(
            entityNameExtend = "Insert",
            aliasExtend = "DTA",
            properties = [
                ColumnProjection("magicId"),
                ColumnProjection("magicTypeId"),
                ColumnProjection("magicName"),
                ColumnProjection("mainEffect"),
                ColumnProjection("subEffect"),
                ColumnProjection("createMethod"),
                ColumnProjection("updateMethod"),
            ],
            commonInterface = [INSERT, ABSERT],
        ),
        Projection(
            entityNameExtend = "Base",
            aliasExtend = "B",
            properties = [
                ColumnProjection("magicId"),
                ColumnProjection("magicTypeId"),
                ColumnProjection("magicName"),
                ColumnProjection("mainEffect"),
                ColumnProjection("subEffect"),
            ],
            commonInterface = [SELECT],
        ),
        Projection(
            entityNameExtend = "Id",
            aliasExtend = "",
            properties = [
                ColumnProjection("magicId"),
                ColumnProjection("magicTypeId", true),
                ColumnProjection("subEffect", true),
            ],
            commonInterface = [SELECT],
        ),
        Projection(
            entityNameExtend = "Update",
            aliasExtend = "UPS",
            properties = [
                ColumnProjection("magicId"),
                ColumnProjection("mainEffect"),
                ColumnProjection("subEffect"),
                ColumnProjection("updateMethod"),
                ColumnProjection("updateTime"),
            ],
            commonInterface = [UPDATE],
        ),
        Projection(
            entityNameExtend = "Upsert",
            aliasExtend = "UPSERT",
            properties = [
                ColumnProjection("magicId"),
                ColumnProjection("magicTypeId"),
                ColumnProjection("magicName"),
                ColumnProjection("mainEffect"),
                ColumnProjection("subEffect"),
                ColumnProjection("createMethod"),
                ColumnProjection("updateMethod"),
                ColumnProjection("updateTime"),
            ],
            commonInterface = [UPSERT],
        ),
        Projection(
            entityNameExtend = "Delete",
            properties = [
                ColumnProjection("magicId"),
                ColumnProjection("updateMethod"),
            ],
            commonInterface = [DELETE],
        ),
    ]
)
@Table(alias = "SM")
@Index("MAGIC_UNIQ", ["magicId", "magicTypeId"])
data class SpellsMaster(
    @PrimaryKey
    val magicId: Int,
    val magicTypeId: Int,
    val magicName: String,
    val mainEffect: String,
    val subEffect: String?,
    val createMethod: String,
    @Column(name = "CREATE_DATETIME", default = "CURRENT_TIMESTAMP_ISO")
    val createTime: LocalDateTime,
    val updateMethod: String,
    @Column(name = "UPDATE_DATETIME", default = "CURRENT_TIMESTAMP_ISO")
    val updateTime: LocalDateTime,
) : TableDefinitionEntity
