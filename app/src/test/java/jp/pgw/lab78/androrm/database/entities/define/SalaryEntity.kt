package jp.pgw.lab78.androrm.database.entities.define

import jp.pgw.lab78.androrm.annotation.Column
import jp.pgw.lab78.androrm.annotation.Table
import jp.pgw.lab78.androrm.database.interfaces.orm.entity.query.TableDefinitionEntity
import jp.pgw.lab78.androrm.ksp.annotation.GenerateProps
import java.time.LocalDateTime

@GenerateProps
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
