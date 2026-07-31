package jp.pgw.lab78.androrm.database.entities.interfaces

import jp.pgw.lab78.androrm.common.database.annotation.Column
import java.time.LocalDateTime

interface ManagementColumns {
    val createMethod: String
    @Column(name = "CREATE_DATETIME", default = "CURRENT_TIMESTAMP_ISO")
    val createTime: LocalDateTime
    val updateMethod: String
    @Column(name = "UPDATE_DATETIME", default = "CURRENT_TIMESTAMP_ISO")
    val updateTime: LocalDateTime
}