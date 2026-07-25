package jp.pgw.lab78.androrm.database.entities

import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Function
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity

/**
 * runtimeの集約関数を含むSELECT処理を検証するための手書きテストEntity。
 *
 * KSP生成Entityへ依存しない。
 *
 * @author Masahiro Inoue
 * @since 2026-07-24
 */
@Table(
    name = "SALARY",
    alias = "SAL",
)
data class RuntimeSalaryEntitySelective(
    @Column(
        name = "EMPLOYEE_ID",
        hideFromSelect = false,
    )
    val employeeId: String,

    @Column(
        name = "PAY_MONTH",
        hideFromSelect = false,
    )
    val payMonth: String,

    @Column(
        name = "GROSS",
        hideFromSelect = false,
    )
    val gross: Int,

    @Function(
        columnFunction = ColumnFunction.SUM,
        alias = "TOTAL_GROSS",
        args = ["gross"],
        hideFromSelect = false,
    )
    val totalGross: Long,

    @Function(
        columnFunction = ColumnFunction.MAX,
        alias = "MAX_GROSS",
        args = ["gross"],
        hideFromSelect = false,
    )
    val maxGross: Int,

    @Function(
        columnFunction = ColumnFunction.AVG,
        alias = "AVG_GROSS",
        args = ["gross"],
        hideFromSelect = false,
    )
    val avgGross: Double,

    @Function(
        columnFunction = ColumnFunction.MAX,
        alias = "MAX_DEDUCTION",
        args = ["deduction"],
        hideFromSelect = false,
    )
    val maxDeduction: Int,

    @Function(
        columnFunction = ColumnFunction.AVG,
        alias = "AVG_DEDUCTION",
        args = ["deduction"],
        hideFromSelect = false,
    )
    val avgDeduction: Double,
) : SelectEntity