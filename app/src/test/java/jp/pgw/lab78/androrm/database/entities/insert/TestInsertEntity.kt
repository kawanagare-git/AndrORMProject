package jp.pgw.lab78.androrm.database.entities.insert

import jp.pgw.lab78.androrm.database.interfaces.orm.entity.query.InsertEntity
import java.time.LocalDate
import java.time.LocalDateTime

data class TestInsertEntity(val name :String,val address: String,val birthday :LocalDate,val updateDate :LocalDateTime,val insertDateTime: LocalDateTime) :
    InsertEntity
