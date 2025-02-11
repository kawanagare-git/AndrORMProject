package jp.pgw.lab78.androrm.database.entities.select

import jp.pgw.lab78.androrm.annotation.Table
import jp.pgw.lab78.androrm.database.interfaces.SelectEntity
import java.time.LocalDate

/**
 * テスト用 data クラス
 */
@Table(alias = "TSEA")
data class TestSelectEntityWithAlias(val id :Int, val name :String, val address: String, val birthday :LocalDate,) : SelectEntity
