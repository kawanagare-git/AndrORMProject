package jp.pgw.lab78.androrm.database.entities.define

import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.*
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.ComprehensiveEntity
import java.time.LocalDate
import java.time.LocalDateTime

@Projections(
    [
        Projection(
            entityNameExtend = "IdOnly",
            aliasExtend = "ID",
            properties = [ColumnProjection("id")],
            commonInterface = [SELECT]
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
            commonInterface = [INSERT, UPSERT, ABSERT]
        ),
        Projection(
            entityNameExtend = "Comprehensive",
            properties = [
                ColumnProjection("id"),
                ColumnProjection("name"),
                ColumnProjection("address"),
                ColumnProjection("birthday"),
            ],
            commonInterface = [ABSERT]
        ),
        Projection(
            entityNameExtend = "Update",
            properties = [
                ColumnProjection("name"),
                ColumnProjection("address"),
                ColumnProjection("updateDate"),
            ],
            commonInterface = [UPDATE]
        ),
        Projection(
            entityNameExtend = "Delete",
            properties = [
                ColumnProjection("id", hideFromSelect = true),
                ColumnProjection("address", hideFromSelect = true),
            ],
            commonInterface = [DELETE]
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
