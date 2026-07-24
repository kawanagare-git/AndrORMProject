package jp.pgw.lab78.androrm.database.entities

import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.AbsertEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.UpsertEntity
import java.time.LocalDateTime

/**
 * runtimeのUPSERT／ABSERT処理を検証するための手書きテストEntity。
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
data class RuntimeSalaryEntityUpsert(
    @Column(
        name = "EMPLOYEE_ID",
        hideFromSelect = true,
    )
    val employeeId: String? = null,

    @Column(
        name = "PAY_MONTH",
        hideFromSelect = false,
    )
    val payMonth: String,

    @Column(
        name = "CREATE_DATE_TIME",
        alias = "",
        hideFromSelect = false,
    )
    val createdAt: LocalDateTime,
) : UpsertEntity, AbsertEntity