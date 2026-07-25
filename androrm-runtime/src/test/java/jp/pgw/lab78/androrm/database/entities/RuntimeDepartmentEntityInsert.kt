package jp.pgw.lab78.androrm.database.entities

import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.PrimaryKey
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.AbsertEntity
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
    name = "DEPARTMENT",
    alias = "DEP",
)
data class RuntimeDepartmentEntityInsert(
    @PrimaryKey
    @Column(
        name = "ID",
        alias = "",
        hideFromSelect = false,
    )
    val employeeId: String,

    @PrimaryKey
    @Column(
        name = "DEPARTMENT",
        alias = "DEPARTMENT_NAME",
        hideFromSelect = false,
    )
    val department: String,

    @PrimaryKey
    @Column(
        name = "SECTION",
        alias = "SECTION_NAME",
        hideFromSelect = false,
    )
    val section: String,

    @Column(
        name = "CREATE_DATE_TIME",
        alias = "",
        hideFromSelect = false,
    )
    val createdAt: LocalDateTime,

    @Column(
        name = "CREATED_BY_ID",
        alias = "",
        hideFromSelect = false,
    )
    val createdBy: String,

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
) : InsertEntity, AbsertEntity