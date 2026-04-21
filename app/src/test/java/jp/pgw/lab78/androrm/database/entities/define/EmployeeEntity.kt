package jp.pgw.lab78.androrm.database.entities.define

import jp.pgw.lab78.androrm.common.GenerateProps
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * ## エンティティクラス（社員）
 * ### 社員テーブルの基になるエンティティクラス
 * @author Masahiro Inoue
 * @since 2025-09-06
 */
@GenerateProps
@Projections(
    [
        Projection(
            entityNameExtend = "IdSelection",
            aliasExtend = "ID",
            properties = [ColumnProjection("employeeId")],
            commonInterface = [DMLInterfaceEnum.SELECT]
        ),
        Projection(
            entityNameExtend = "",
            properties = [
                ColumnProjection("employeeId", hideFromSelect = true),
                ColumnProjection("name"),
                ColumnProjection("address"),
                ColumnProjection("gender"),
                ColumnProjection("position"),
            ],
            commonInterface = [DMLInterfaceEnum.SELECT, DMLInterfaceEnum.UPDATE]
        ),
    ]
)
@Table("EMPLOYEE", alias = "EMP")
data class EmployeeEntity(
    /** 社員ID */
    val employeeId: String,
    /** 氏名 */
    val name: String,
    /** 住所 */
    val address: String,
    /** 生年月日 */
    val birthDate: LocalDate,
    /** 性別 */
    val gender: String,
    /** 入社日 */
    val hireDate: LocalDate,
    /** 電話番号 */
    val phoneNumber: String,
    /** メールアドレス */
    val email: String,
    /** 役職 */
    val position: String,
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
