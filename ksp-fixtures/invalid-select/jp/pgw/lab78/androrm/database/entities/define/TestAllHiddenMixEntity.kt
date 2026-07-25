package jp.pgw.lab78.androrm.database.entities.define

import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.FunctionProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction
import jp.pgw.lab78.androrm.common.dml.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity

/**
 * KSPの不正なSELECT Entity検証に使用するTestAllHiddenMixEntity。
 * @author Masahiro Inoue
 * @since 2026-05-01
 */
@Table(name = "TEST_ALL_HIDDEN_MIX", alias = "TAHM")
@Projection(
    entityNameExtend = "Select",
    properties = [
        ColumnProjection("id", hideFromSelect = true)
    ],
    functions = [
        FunctionProjection(
            function = ColumnFunction.COUNT,
            args = [],
            alias = "ALL_COUNT",
            hideFromSelect = true
        )
    ],
    commonInterface = [DMLInterfaceEnum.SELECT]
)
data class TestAllHiddenMixEntity(
    @Column(name = "ID")
    val id: Int
) : SelectEntity