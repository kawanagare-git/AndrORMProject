package jp.pgw.lab78.androrm.database.entities.define

import jp.pgw.lab78.androrm.common.GenerateProps
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import java.time.LocalDateTime

@GenerateProps
@Projections(
    [
        Projection(entityNameExtend = "Info"
            , aliasExtend = "INF"
            , properties = [
                            "employeeId",
                            "department",
                            "section"
                           ]
            , commonInterface = DMLInterfaceEnum.SELECT
        ),
        Projection(entityNameExtend = "Insert"
            , properties = [
                            "employeeId",
                            "department",
                            "section",
                            "createdAt",
                            "createdBy",
                            "updatedAt",
                            "updatedBy"
                           ]
            , commonInterface = DMLInterfaceEnum.INSERT),
        Projection(entityNameExtend = "Condition"
            , properties = [
                            "employeeId",
                            "department",
                            "section",
                           ]
        , commonInterface = DMLInterfaceEnum.CONDITION)
    ]
)
@Table("DEPARTMENT", alias = "DEP")
data class DepartmentEntity(
    @Column("ID")
    val employeeId: String,
    @Column("DEPARTMENT", alias = "DEPARTMENT_NAME")
    val department: String,
    @Column("SECTION", alias = "SECTION_NAME")
    val section: String,
    @Column("CREATE_DATE_TIME")
    val createdAt: LocalDateTime,   // 作成日時
    @Column("CREATED_BY_ID")
    val createdBy: String,          // 作成者（employeeId）
    @Column("UPDATE_DATE_TIME")
    val updatedAt: LocalDateTime,   // 更新日時
    @Column("UPDATE_BY_ID")
    val updatedBy: String           // 更新者（employeeId）
) : TableDefinitionEntity
