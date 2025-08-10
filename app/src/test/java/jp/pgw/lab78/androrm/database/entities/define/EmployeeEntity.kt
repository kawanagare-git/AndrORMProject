package jp.pgw.lab78.androrm.database.entities.define

import jp.pgw.lab78.androrm.common.GenerateProps
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import java.time.LocalDate
import java.time.LocalDateTime


@GenerateProps
@Projections(
    [
        Projection(
            entityNameExtend = "IdSelection"
            , aliasExtend = "ID"
            , properties = ["employeeId"]
            , commonInterface = DMLInterfaceEnum.SELECT
        ),
        Projection(
            entityNameExtend = ""
            , properties = [
                "employeeId",
                "name",
                "address",
                "gender",
                "position",
            ]
            , commonInterface = DMLInterfaceEnum.SELECT
        ),
        Projection(
            entityNameExtend = "Condition"
            , properties = [
                            "employeeId",
                            "name",
                            "address",
                            "gender",
                            "position",
                            ]
            , commonInterface = DMLInterfaceEnum.CONDITION
        )
    ]
)
@Table("EMPLOYEE",alias = "EMP")
data class EmployeeEntity(
    val employeeId: String,
    val name: String,
    val address: String,
    val birthDate: LocalDate,         // 生年月日
    val gender: String,               // 性別
    val hireDate: LocalDate,          // 入社日
    val phoneNumber: String,          // 電話番号
    val email: String,                // メールアドレス
    val position: String,             // 役職
    @Column("CREATE_DATE_TIME")
    val createdAt: LocalDateTime,   // 作成日時
    @Column("CREATED_BY_ID")
    val createdBy: String,          // 作成者（employeeId）
    @Column("UPDATE_DATE_TIME")
    val updatedAt: LocalDateTime,   // 更新日時
    @Column("UPDATE_BY_ID")
    val updatedBy: String           // 更新者（employeeId）
) : TableDefinitionEntity
