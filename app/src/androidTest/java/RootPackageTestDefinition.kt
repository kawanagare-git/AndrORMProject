import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import jp.pgw.lab78.androrm.common.database.annotation.PrimaryKey
import jp.pgw.lab78.androrm.common.database.annotation.Table

@Projections(
    [
        Projection(
            entityNameExtend = "Select",
            properties = [
                ColumnProjection("id"),
            ],
            commonInterface = [DMLInterfaceEnum.SELECT],
        ),
    ],
)
@Table(name = "ROOT_PACKAGE_TEST")
data class RootPackageTestDefinition(
    @PrimaryKey
    val id: Int,
)