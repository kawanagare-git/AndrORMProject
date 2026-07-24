package jp.pgw.lab78.androrm.database.entities.define

import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity

/**
 * KSPの不正なSELECT Entity検証に使用するTestDuplicateAliasEntity。
 * @author Masahiro Inoue
 * @since 2026-05-01
 */
@Table(name = "TEST_DUP_ALIAS", alias = "TDA")
@Projection(
    entityNameExtend = "Select",
    properties = [
        ColumnProjection("id"),
        ColumnProjection("employeeId")
    ],
    commonInterface = [DMLInterfaceEnum.SELECT]
)
data class TestDuplicateAliasEntity(
    @Column(name = "ID", alias = "DUP")
    val id: Int,

    @Column(name = "EMPLOYEE_ID", alias = "DUP")
    val employeeId: Int
) : SelectEntity