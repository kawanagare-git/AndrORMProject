package jp.pgw.lab78.androrm.database.entities.insert

import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.InsertEntity
import java.time.LocalDate

/**
 * Entityメタ情報またはSQL生成の検証に使用するテスト用TestInsertEntity。
 * @author Masahiro Inoue
 * @since 2025-12-11
 */
@Table
data class TestInsertEntity(
    val name: String,
    val address: String,
    val birthday: LocalDate,
    val updateDate: LocalDate?,
    val insertDateTime: LocalDate?
) : InsertEntity
