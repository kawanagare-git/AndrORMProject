package jp.pgw.lab78.androrm.database.entities.define

import jp.pgw.lab78.androrm.common.GenerateProps
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.database.annotation.Column
import jp.pgw.lab78.androrm.database.annotation.Table
import java.time.LocalDateTime


@GenerateProps
@Table("DEPARTMENT", alias = "DEP")
data class DepartmentEntity(
    @Column("ID")
    val employeeId: String,
    @Column("DEPARTMENT")
    val department: String,
    @Column("SECTION")
    val section: String,
    @Column("CREATE_DATE_TIME")
    val createdAt: LocalDateTime,   // 作成日時
    @Column("CREATED_BY_ID")
    val createdBy: String,          // 作成者（employeeId）
    @Column("UPDATE_DATE_TIME")
    val updatedAt: LocalDateTime,   // 更新日時
    @Column("UPDATE_BY_ID")
    val updatedBy: String           // 更新者（employeeId）
) : TableDefinitionEntity

