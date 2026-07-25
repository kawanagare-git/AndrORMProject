package jp.pgw.lab78.androrm.database.entities.select

import jp.pgw.lab78.androrm.common.database.annotation.Function
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import java.time.LocalDate

/**
 * テスト用 data クラス
  * @author Masahiro Inoue
  * @since 2025-03-02
 */
@Table(alias = "TS")
data class TestSelectEntity(
    val id: Int,
    val name: String,
    val address: String,
    @Function(
        columnFunction = ColumnFunction.MAX,
        alias = "LATEST_BIRTHDAY",
        args = ["birthday"],
    )
    val newestBirthday: LocalDate,
) : SelectEntity, TableDefinitionEntity
