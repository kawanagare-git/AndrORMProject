package jp.pgw.lab78.androrm.database.entities.define

import jp.pgw.lab78.androrm.common.dml.interfaces.ComprehensiveEntity
import java.time.LocalDate
import java.time.LocalDateTime

data class TestAllEntity(val id :Int, val name :String, val address: String, val birthday :LocalDate, val updateDate :LocalDateTime, val insertDateTime: LocalDateTime) :
    ComprehensiveEntity
