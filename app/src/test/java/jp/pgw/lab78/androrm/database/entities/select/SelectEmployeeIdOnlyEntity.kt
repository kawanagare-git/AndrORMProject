package jp.pgw.lab78.androrm.database.entities.select

import jp.pgw.lab78.androrm.annotation.Table
import jp.pgw.lab78.androrm.database.interfaces.entity.SelectEntity

@Table("EMPLOYEE",alias = "EMO")
data class SelectEmployeeIdOnlyEntity(
    val employeeId: String,
) : SelectEntity
