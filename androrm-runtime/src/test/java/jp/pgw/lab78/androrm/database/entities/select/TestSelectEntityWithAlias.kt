package jp.pgw.lab78.androrm.database.entities.select

import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import java.time.LocalDate

/**
 * テスト用 data クラス
  * @author Masahiro Inoue
  * @since 2025-12-11
 */
@Table(alias = "TSEA")
data class TestSelectEntityWithAlias(
    val id: Int,
    val name: String,
    val address: String,
    val birthday: LocalDate,
) : SelectEntity
