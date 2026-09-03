package jp.pgw.lab78.androrm.database.condition.interfaces

import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity

/**
 * ## SELECT系クエリインターフェース
 * ### SELECTとして実行できるSQLとバインド値を公開する
 *
 * ### 仕様
 * #### 通常のSelectと複合SELECTが共通のMap・Cursor実行経路を利用するための契約を表す。
 * @param T SELECT結果を表すEntity
 * @author Masahiro Inoue
 * @since 2026-09-01
 */
interface SelectQuery<out T : SelectEntity> : QueryStructureLike {
    /** SQLのプレースホルダー順に並んだバインド値 */
    val bindValues: List<Any?>
}
