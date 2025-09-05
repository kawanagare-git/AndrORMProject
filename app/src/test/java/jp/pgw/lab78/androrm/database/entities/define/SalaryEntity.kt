package jp.pgw.lab78.androrm.database.entities.define

import jp.pgw.lab78.androrm.common.GenerateProps
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.annotation.FunctionProjection
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction.AVG
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction.MAX
import jp.pgw.lab78.androrm.common.dml.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import java.time.LocalDateTime

@GenerateProps
@Projections(
    [
        Projection(
            entityNameExtend = "Upsert",
            properties = [
                "employeeId",
                "payMonth",
                "createdAt"
            ],
            commonInterface = DMLInterfaceEnum.UPSERT),
        Projection(
            entityNameExtend = "Insert",
            properties = [
                "employeeId",
                "payMonth",
                "gross",
                "updatedAt",
                "updatedBy"
            ],
            commonInterface = DMLInterfaceEnum.INSERT),
        Projection(
            entityNameExtend = "Selective"
            , properties = [
                "employeeId",
                "payMonth",
                "gross",
            ]
            , functions = [
                FunctionProjection(
                    MAX
                    , args = ["gross"]
                    , alias = "MAX_GROSS"
                )
                , FunctionProjection(
                    AVG
                    , args = ["gross"]
                    , alias = "AVG_GROSS"
                )
                , FunctionProjection(
                    MAX
                    , args = ["deduction"]
                    , alias = "MAX_DEDUCTION"
                )
                , FunctionProjection(
                    AVG
                    , args = ["deduction"]
                    , alias = "AVG_DEDUCTION"
                )
            ]
            , commonInterface = DMLInterfaceEnum.SELECT),
        Projection(
            entityNameExtend = "Condition"
            , properties = [
                "employeeId",
                "payMonth",
                "gross",
            ]
            , commonInterface = DMLInterfaceEnum.CONDITION)
    ]
)
@Table("SALARY",alias = "SAL")
data class SalaryEntity(
    val employeeId: String,
    val payMonth: String,
    val gross: Int,
    val deduction: Int,
    @Column("CREATE_DATE_TIME")
    val createdAt: LocalDateTime,   // 作成日時
    @Column("CREATED_BY_ID")
    val createdBy: String,          // 作成者（employeeId）
    @Column("UPDATE_DATE_TIME")
    val updatedAt: LocalDateTime,   // 更新日時
    @Column("UPDATE_BY_ID")
    val updatedBy: String           // 更新者（employeeId）
) : TableDefinitionEntity
