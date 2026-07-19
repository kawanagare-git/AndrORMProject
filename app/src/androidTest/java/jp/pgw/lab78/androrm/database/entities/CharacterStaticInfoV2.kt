package jp.pgw.lab78.androrm.database.entities

import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.*
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import jp.pgw.lab78.androrm.common.database.annotation.*
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
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
            commonInterface = [SELECT],
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
    val createMethod: String,
    @Column(name = "CREATE_DATETIME", default = "CURRENT_TIMESTAMP_ISO")
    val createTime: LocalDateTime,
    val updateMethod: String,
    @Column(name = "UPDATE_DATETIME", default = "CURRENT_TIMESTAMP_ISO")
    val updateTime: LocalDateTime,
) : TableDefinitionEntity
