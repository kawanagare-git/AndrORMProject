package jp.pgw.lab78.androrm.database.entities.define

import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity

@Table(name = "TEST_DUP_ALIAS_HIDDEN", alias = "TDAH")
@Projection(
    entityNameExtend = "Select",
    properties = [
        ColumnProjection("id", hideFromSelect = true),
        ColumnProjection("employeeId")
    ],
    commonInterface = [DMLInterfaceEnum.SELECT]
)
data class TestDuplicateAliasHiddenEntity(
    @Column(name = "ID", alias = "DUP")
    val id: Int,

    @Column(name = "EMPLOYEE_ID", alias = "DUP")
    val employeeId: Int
) : SelectEntity