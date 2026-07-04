package jp.pgw.lab78.androrm.database.entities.define

import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.FunctionProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction.*
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import java.time.LocalDateTime

/**
 * ## エンティティクラス（給与）
 * ### 給与テーブルの基になるエンティティクラス
 * @author Masahiro Inoue
 * @since 2025-09-06
 */
@Projections(
    [
        Projection(
            entityNameExtend = "Upsert",
            properties = [
                ColumnProjection("employeeId", hideFromSelect = true),
                ColumnProjection("payMonth"),
                ColumnProjection("createdAt")
            ],
            commonInterface = [DMLInterfaceEnum.UPSERT, DMLInterfaceEnum.ABSERT]
        ),
        Projection(
            entityNameExtend = "Insert",
            properties = [
                ColumnProjection("employeeId"),
                ColumnProjection("payMonth"),
                ColumnProjection("gross"),
                ColumnProjection("updatedAt"),
                ColumnProjection("updatedBy")
            ],
            commonInterface = [DMLInterfaceEnum.INSERT]
        ),
        Projection(
            entityNameExtend = "Selective",
            properties = [
                ColumnProjection("employeeId"),
                ColumnProjection("payMonth"),
                ColumnProjection("gross"),
            ],
            functions = [
                FunctionProjection(function = SUM, args = ["gross"], alias = "TOTAL_GROSS"),
                FunctionProjection(function = MAX, args = ["gross"], alias = "MAX_GROSS"),
                FunctionProjection(function = AVG, args = ["gross"], alias = "AVG_GROSS"),
                FunctionProjection(function = MAX, args = ["deduction"], alias = "MAX_DEDUCTION"),
                FunctionProjection(function = AVG, args = ["deduction"], alias = "AVG_DEDUCTION")
            ],
            commonInterface = [DMLInterfaceEnum.SELECT]
        ),
    ]
)
@Table("SALARY", alias = "SAL")
data class SalaryEntity(
    /** 社員ID */
    val employeeId: String,
    /** 支払月 */
    val payMonth: String,
    /** 支給額 */
    val gross: Int,
    /** 控除額 */
    val deduction: Int,
    /** 作成日時 */
    @Column("CREATE_DATE_TIME")
    val createdAt: LocalDateTime,
    /** 作成者（employeeId） */
    @Column("CREATED_BY_ID")
    val createdBy: String,
    /** 更新日時 */
    @Column("UPDATE_DATE_TIME")
    val updatedAt: LocalDateTime,
    /** 更新者（employeeId） */
    @Column("UPDATE_BY_ID")
    val updatedBy: String
) : TableDefinitionEntity
