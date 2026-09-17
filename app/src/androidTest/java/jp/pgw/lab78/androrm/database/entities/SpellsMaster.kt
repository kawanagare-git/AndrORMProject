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
 * ## 魔法マスタテーブルEntity
 * ### 魔法の種別、名称および効果を定義する
 * @author Masahiro Inoue
 * @since 2026-06-14
 */
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
            andrOrmSubPackage = [INSERT, ABSERT],
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
            andrOrmSubPackage = [SELECT],
        ),
        Projection(
            entityNameExtend = "Id",
            aliasExtend = "",
            properties = [
                ColumnProjection("magicId"),
                ColumnProjection("magicTypeId", true),
                ColumnProjection("subEffect", true),
            ],
            andrOrmSubPackage = [SELECT],
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
            andrOrmSubPackage = [UPDATE],
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
            andrOrmSubPackage = [UPSERT],
        ),
        Projection(
            entityNameExtend = "Delete",
            properties = [
                ColumnProjection("magicId"),
                ColumnProjection("updateMethod"),
            ],
            andrOrmSubPackage = [DELETE],
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
    override val createMethod: String,
    override val createTime: LocalDateTime,
    override val updateMethod: String,
    override val updateTime: LocalDateTime,
) : TableDefinitionEntity, ManagementColumns
