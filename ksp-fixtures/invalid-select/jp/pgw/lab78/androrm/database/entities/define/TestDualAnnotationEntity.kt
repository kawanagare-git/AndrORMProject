package jp.pgw.lab78.androrm.database.entities.define

import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.PrimaryKey
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction
import jp.pgw.lab78.androrm.common.dml.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity

@Table(name = "TEST_DUAL_ANNOTATION", alias = "TDA")
@Projection(
    entityNameExtend = "Select",
    properties = [
        ColumnProjection("id"),
        ColumnProjection("countValue")
    ],
    commonInterface = [DMLInterfaceEnum.SELECT]
)
data class TestDualAnnotationEntity(
    @PrimaryKey
    @Column(name = "ID")
    val id: Int,

    @Column(name = "COUNT_VALUE")
    @Function(columnFunction = ColumnFunction.COUNT, alias = "COUNT_VALUE")
    val countValue: Long
) : SelectEntity