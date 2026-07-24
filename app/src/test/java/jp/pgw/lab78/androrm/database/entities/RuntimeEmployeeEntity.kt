package jp.pgw.lab78.androrm.database.entities

import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.UpdateEntity

/**
 * runtimeのSELECT処理を検証するための手書きテストEntity。
 *
 * KSP生成Entityへ依存しない。
 *
 * @author Masahiro Inoue
 * @since 2026-07-24
 */
@Table(
    name = "EMPLOYEE",
    alias = "EMP",
)
data class RuntimeEmployeeEntity(
    @Column(
        name = "EMPLOYEE_ID",
        hideFromSelect = false,
    )
    val employeeId: String,

    @Column(
        name = "EMPLOYEE_SUB_ID",
        hideFromSelect = true,
    )
    val employeeSubId: String? = null,

    @Column(
        name = "NAME",
        hideFromSelect = false,
    )
    val name: String,

    @Column(
        name = "ADDRESS",
        hideFromSelect = false,
    )
    val address: String,

    @Column(
        name = "GENDER",
        hideFromSelect = false,
    )
    val gender: String,

    @Column(
        name = "POSITION",
        hideFromSelect = false,
    )
    val position: String,
) : SelectEntity, UpdateEntity