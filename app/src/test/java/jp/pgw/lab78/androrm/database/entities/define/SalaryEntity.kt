package jp.pgw.lab78.androrm.database.entities.define

import jp.pgw.lab78.androrm.common.GenerateProps
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import java.time.LocalDateTime

@GenerateProps
@Projections(
    [
        Projection(entityNameExtend = "Upsert"
        , properties = ["employeeId","payMonth","createdAt"]
        , commonInterface = DMLInterfaceEnum.UPSERT),
        Projection(entityNameExtend = "Insert"
        , properties = ["employeeId","payMonth","gross","updatedAt","updatedBy"]
        , commonInterface = DMLInterfaceEnum.INSERT)
    ]
)
@Table("SALARY",alias = "SAL")
data class SalaryEntity(
    val employeeId: String,
    val payMonth: String,
    val gross: Int,
    val deduction: Int,
    @Column("CREATE_DATE_TIME")
    val createdAt: LocalDateTime,   // 作成日時
    @Column("CREATED_BY_ID")
    val createdBy: String,          // 作成者（employeeId）
    @Column("UPDATE_DATE_TIME")
    val updatedAt: LocalDateTime,   // 更新日時
    @Column("UPDATE_BY_ID")
    val updatedBy: String           // 更新者（employeeId）
) : TableDefinitionEntity
