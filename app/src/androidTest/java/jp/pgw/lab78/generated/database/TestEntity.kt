package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.ksp.interfaces.entity.TableDefinitionEntity
import java.time.LocalDate

data class TestEntity(val id :Int,val name :String,val address: String,val birthday :LocalDate,) :
    TableDefinitionEntity
