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
                ColumnProjection("characterPk"),
                ColumnProjection("statusType"),
                ColumnProjection("value"),
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
            entityNameExtend = "Delete",
            properties = [
                ColumnProjection("characterPk"),
            ],
            commonInterface = [DELETE],
        ),
    ]
)
@Table
@Index("CHAR_UNIQ", ["characterPk"])
data class CharacterStatus(
    @PrimaryKey
    val characterPk: Int,
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
