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
 * KSPの不正なSELECT Entity検証に使用するTestDuplicateAliasMixEntity。
 * @author Masahiro Inoue
 * @since 2026-05-01
 */
@Table(name = "TEST_DUP_ALIAS_MIX", alias = "TDAM")
@Projection(
    entityNameExtend = "Select",
    properties = [
        ColumnProjection("id")
    ],
    functions = [
        FunctionProjection(
            function = ColumnFunction.COUNT,
            args = [],
            alias = "DUP"
        )
    ],
    commonInterface = [DMLInterfaceEnum.SELECT]
)
data class TestDuplicateAliasMixEntity(
    @Column(name = "ID", alias = "DUP")
    val id: Int
) : SelectEntity