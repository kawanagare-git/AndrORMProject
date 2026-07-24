package jp.pgw.lab78.androrm.database.entities

import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.DeleteEntity

/**
 * runtimeのDELETE処理を検証するための手書きテストEntity。
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
data class RuntimeTestAllEntityDelete(
    @Column(
        name = "ID",
        hideFromSelect = true,
    )
    val id: Int? = null,

    @Column(
        name = "ADDRESS",
        hideFromSelect = true,
    )
    val address: String? = null,
) : DeleteEntity
