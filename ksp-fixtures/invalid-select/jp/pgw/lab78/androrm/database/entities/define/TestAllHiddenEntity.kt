package jp.pgw.lab78.androrm.database.entities.define

import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity

/**
 * KSPの不正なSELECT Entity検証に使用するTestAllHiddenEntity。
 * @author Masahiro Inoue
 * @since 2026-05-01
 */
@Table(name = "TEST_ALL_HIDDEN", alias = "TAH")
@Projection(
    entityNameExtend = "Select",
    properties = [
        ColumnProjection("id", hideFromSelect = true),
        ColumnProjection("name", hideFromSelect = true)
    ],
    commonInterface = [DMLInterfaceEnum.SELECT]
)
data class TestAllHiddenEntity(
    @Column(name = "ID")
    val id: Int,

    @Column(name = "NAME")
    val name: String
) : SelectEntity