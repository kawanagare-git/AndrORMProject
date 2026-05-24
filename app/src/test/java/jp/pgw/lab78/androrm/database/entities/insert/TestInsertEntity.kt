package jp.pgw.lab78.androrm.database.entities.insert

import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.InsertEntity
import java.time.LocalDate

@Table
data class TestInsertEntity(
    val name: String,
    val address: String,
    val birthday: LocalDate,
    val updateDate: LocalDate?,
    val insertDateTime: LocalDate?
) : InsertEntity
