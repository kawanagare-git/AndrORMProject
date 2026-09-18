package jp.pgw.lab78.androrm.database.entities.define

import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.*
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.ComprehensiveEntity
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Entityメタ情報またはSQL生成の検証に使用するテスト用TestAllEntity。
 * @author Masahiro Inoue
 * @since 2025-12-11
 */
@Projections(
    [
        Projection(
            entityNameExtend = "IdOnly",
            aliasExtend = "ID",
            properties = [ColumnProjection("id")],
            andrOrmSubPackage = [SELECT]
        ),
        Projection(
            entityNameExtend = "Comprehensive",
            properties = [
                ColumnProjection("id"),
                ColumnProjection("name"),
                ColumnProjection("address"),
                ColumnProjection("birthday"),
                ColumnProjection("updateDate"),
                ColumnProjection("insertDateTime")
            ],
            andrOrmSubPackage = [INSERT, UPSERT, ABSERT]
        ),
        Projection(
            entityNameExtend = "Comprehensive",
            properties = [
                ColumnProjection("id"),
                ColumnProjection("name"),
                ColumnProjection("address"),
                ColumnProjection("birthday"),
            ],
            andrOrmSubPackage = [ABSERT]
        ),
        Projection(
            entityNameExtend = "Update",
            properties = [
                ColumnProjection("name"),
                ColumnProjection("address"),
                ColumnProjection("updateDate"),
            ],
            andrOrmSubPackage = [UPDATE]
        ),
        Projection(
            entityNameExtend = "Delete",
            properties = [
                ColumnProjection("id", hideFromSelect = true),
                ColumnProjection("address", hideFromSelect = true),
            ],
            andrOrmSubPackage = [DELETE]
        ),
    ]
)
@Table
data class TestAllEntity(
    val id: Int,
    val name: String,
    val address: String,
    val birthday: LocalDate,
    val subId: Int? = null,
    val updateDate: LocalDateTime = LocalDateTime.now(),
    val insertDateTime: LocalDateTime = LocalDateTime.now()
) :
    ComprehensiveEntity
