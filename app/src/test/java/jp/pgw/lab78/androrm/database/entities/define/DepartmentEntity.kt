package jp.pgw.lab78.androrm.database.entities.define

import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.FunctionProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.PrimaryKey
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction.COUNT
import jp.pgw.lab78.androrm.common.dml.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import java.time.LocalDateTime

/**
 * ## エンティティクラス（部署情報）
 * ### 部署情報テーブルの基になるエンティティクラス
 * @author Masahiro Inoue
 * @since 2025-09-06
 */
@Projections(
    [
        Projection(
            entityNameExtend = "Info",
            aliasExtend = "INF",
            properties = [
                ColumnProjection("employeeId"),
                ColumnProjection("department"),
                ColumnProjection("section")
            ],
            functions = [
                FunctionProjection(function = COUNT, args = [], alias = "ALL_LINE")
            ],
            commonInterface = [DMLInterfaceEnum.SELECT, DMLInterfaceEnum.UPSERT]
        ),
        Projection(
            entityNameExtend = "Insert",
            properties = [
                ColumnProjection("employeeId"),
                ColumnProjection("department"),
                ColumnProjection("section"),
                ColumnProjection("createdAt"),
                ColumnProjection("createdBy"),
                ColumnProjection("updatedAt"),
                ColumnProjection("updatedBy")
            ],
            commonInterface = [DMLInterfaceEnum.INSERT]
        ),
    ]
)
@Table("DEPARTMENT", alias = "DEP")
data class DepartmentEntity(
    /** 社員ID */
    @PrimaryKey
    @Column("ID")
    val employeeId: String,
    /** 部署名 */
    @PrimaryKey
    @Column("DEPARTMENT", alias = "DEPARTMENT_NAME")
    val department: String,
    /** 課名 */
    @PrimaryKey
    @Column("SECTION", alias = "SECTION_NAME")
    val section: String,
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
