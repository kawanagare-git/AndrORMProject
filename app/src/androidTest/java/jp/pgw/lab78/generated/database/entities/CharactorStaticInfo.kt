package jp.pgw.lab78.generated.database.entities

import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.dml.DMLInterfaceEnum.INSERT
import java.time.LocalDateTime

@Projection(
    entityNameExtend = "Insert",
    aliasExtend = "FULL",
    properties = [
        ColumnProjection("characterPk"),
        ColumnProjection("userId"),
        ColumnProjection("characterNo"),
        ColumnProjection("characterName"),
        ColumnProjection("createMethod"),
        ColumnProjection("createTime"),
        ColumnProjection("updateMethod"),
        ColumnProjection("updateTime"),
    ],
    commonInterface = [INSERT],
)
data class CharacterStaticInfo(
    val characterPk: String,
    val userId: String,
    val characterNo: Int,
    val characterName: String,
    val createMethod: String,
    val createTime: LocalDateTime,
    val updateMethod: String,
    val updateTime: LocalDateTime,
)
