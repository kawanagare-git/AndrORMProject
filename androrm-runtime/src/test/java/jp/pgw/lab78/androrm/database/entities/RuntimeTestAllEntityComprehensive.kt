package jp.pgw.lab78.androrm.database.entities

import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.AbsertEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.InsertEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.UpsertEntity
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * runtimeのUPSERT処理を検証するための手書きテストEntity。
 *
 * KSP生成Entityへ依存しない。
 *
 * @author Masahiro Inoue
 * @since 2026-07-24
 */
@Table(
    name = "TEST_ALL_ENTITY",
    alias = "",
)
data class RuntimeTestAllEntityComprehensive(
    @Column(
        name = "ID",
        hideFromSelect = false,
    )
    val id: Int,

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
        name = "BIRTHDAY",
        hideFromSelect = false,
    )
    val birthday: LocalDate,

    @Column(
        name = "UPDATE_DATE",
        hideFromSelect = false,
    )
    val updateDate: LocalDateTime,

    @Column(
        name = "INSERT_DATE_TIME",
        hideFromSelect = false,
    )
    val insertDateTime: LocalDateTime,
) : InsertEntity, UpsertEntity, AbsertEntity