package jp.pgw.lab78.androrm.database.entities.define

import jp.pgw.lab78.androrm.common.GenerateProps
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.database.annotation.Column
import jp.pgw.lab78.androrm.database.annotation.Table
import jp.pgw.lab78.generated.ksp.interfaces.entity.TableDefinitionEntity
import java.time.LocalDateTime

@GenerateProps
@Projection(entityNameExtend = "IdSelection"
    , fields = ["employeeId","payMonth"]
    , implementsInterface = "jp.pgw.lab78.androrm.database.interfaces.entity.SelectEntity")
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
