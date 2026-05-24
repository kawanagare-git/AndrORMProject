package jp.pgw.lab78.androrm.database.entities.define

import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.dml.interfaces.ComprehensiveEntity
import java.time.LocalDate
import java.time.LocalDateTime

@Projections(
    [
        Projection(
            entityNameExtend = "IdOnly",
            aliasExtend = "ID",
            properties = [ColumnProjection("id")],
            commonInterface = [DMLInterfaceEnum.SELECT]
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
            commonInterface = [DMLInterfaceEnum.INSERT, DMLInterfaceEnum.UPSERT]
        ),
        Projection(
            entityNameExtend = "Update",
            properties = [
                ColumnProjection("name"),
                ColumnProjection("address"),
                ColumnProjection("updateDate"),
            ],
            commonInterface = [DMLInterfaceEnum.UPDATE]
        ),
        Projection(
            entityNameExtend = "Delete",
            properties = [
                ColumnProjection("id", hideFromSelect = true),
                ColumnProjection("address", hideFromSelect = true),
            ],
            commonInterface = [DMLInterfaceEnum.DELETE]
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
