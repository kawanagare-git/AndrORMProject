package jp.pgw.lab78.androrm.database.entities

import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.*
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import jp.pgw.lab78.androrm.common.database.annotation.MigrationDefault
import jp.pgw.lab78.androrm.common.database.annotation.PrimaryKey
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.annotation.Unique
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.database.entities.interfaces.ManagementColumns
import java.time.LocalDateTime

/**
 * ## キャラクタ固定情報V2テーブルEntity
 * ### DBバージョン2で属性情報を追加したキャラクタ識別情報を定義する
 * ### V1との移行検証では意図的に同じテーブル名を使用するため、テーブル名重複検査を抑制する
 * @author Masahiro Inoue
 * @since 2026-07-08
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
                ColumnProjection("mainElement"),
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
                ColumnProjection("userId"),
                ColumnProjection("characterNo"),
                ColumnProjection("characterName"),
                ColumnProjection("mainElement"),
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
            customInterface = ["getname"]
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
@Table(name = "CHARACTER_STATIC_INFO", alias = "CSI")
@Unique(properties = ["userId", "characterNo"])
@Suppress("AndrOrmDuplicateTableNameRule")
data class CharacterStaticInfoV2(
    @PrimaryKey
    val characterPk: Int,
    val userId: String,
    val characterNo: Int,
    val characterName: String,
    @MigrationDefault("1")
    val mainElement: Int,
    override val createMethod: String,
    override val createTime: LocalDateTime,
    override val updateMethod: String,
    override val updateTime: LocalDateTime,
) : TableDefinitionEntity, ManagementColumns
