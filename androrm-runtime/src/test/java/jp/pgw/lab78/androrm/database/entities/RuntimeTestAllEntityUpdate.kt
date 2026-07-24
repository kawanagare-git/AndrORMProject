package jp.pgw.lab78.androrm.database.entities

import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.UpdateEntity
import java.time.LocalDateTime

/**
 * runtimeのUPDATE処理を検証するための手書きテストEntity。
 * KSP生成Entityへ依存しない。
 * @author Masahiro Inoue
 * @since 2026-07-24
 */
@Table(
    name = "TEST_ALL_ENTITY",
    alias = "",
)
data class RuntimeTestAllEntityUpdate(
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
        name = "UPDATE_DATE",
        hideFromSelect = false,
    )
    val updateDate: LocalDateTime,
) : UpdateEntity