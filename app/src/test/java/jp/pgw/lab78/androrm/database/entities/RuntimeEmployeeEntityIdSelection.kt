package jp.pgw.lab78.androrm.database.entities

import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity

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
    alias = "EMP_ID",
)
data class RuntimeEmployeeEntityIdSelection(
    @Column(
        name = "EMPLOYEE_ID",
        hideFromSelect = false,
    )
    val employeeId: String,
) : SelectEntity