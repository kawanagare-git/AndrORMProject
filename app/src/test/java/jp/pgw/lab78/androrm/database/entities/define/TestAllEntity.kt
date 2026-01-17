package jp.pgw.lab78.androrm.database.entities.define

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
            properties = ["id"],
            commonInterface = [DMLInterfaceEnum.SELECT]
        ),
        Projection(
            entityNameExtend = "Comprehensive",
            properties = [
                "id",
                "name",
                "address",
                "birthday",
                "updateDate",
                "insertDateTime"
            ],
            commonInterface = [DMLInterfaceEnum.INSERT, DMLInterfaceEnum.UPSERT]
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
