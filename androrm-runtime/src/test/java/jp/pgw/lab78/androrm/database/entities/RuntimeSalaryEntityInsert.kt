package jp.pgw.lab78.androrm.database.entities

import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.InsertEntity
import java.time.LocalDateTime

/**
 * runtimeのINSERT処理を検証するための手書きテストEntity。
 *
 * KSP生成Entityへ依存しない。
 *
 * @author Masahiro Inoue
 * @since 2026-07-24
 */
@Table(
    name = "SALARY",
    alias = "SAL",
)
data class RuntimeSalaryEntityInsert(
    @Column(
        name = "EMPLOYEE_ID",
        hideFromSelect = false,
    )
    val employeeId: String,

    @Column(
        name = "PAY_MONTH",
        hideFromSelect = false,
    )
    val payMonth: String,

    @Column(
        name = "GROSS",
        hideFromSelect = false,
    )
    val gross: Int,

    @Column(
        name = "UPDATE_DATE_TIME",
        alias = "",
        hideFromSelect = false,
    )
    val updatedAt: LocalDateTime,

    @Column(
        name = "UPDATE_BY_ID",
        alias = "",
        hideFromSelect = false,
    )
    val updatedBy: String,
) : InsertEntity