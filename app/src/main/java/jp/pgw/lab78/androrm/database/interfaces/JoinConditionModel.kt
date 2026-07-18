package jp.pgw.lab78.androrm.database.interfaces

import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.database.condition.ConditionBuilder
import jp.pgw.lab78.androrm.database.condition.interfaces.SelectBody

/**
 * ## JoinCondition
 * ### join メソッド内で使用する結合条件クラス
 * @author Masahiro Inoue
 * @since 2026-07-10
 */
interface JoinConditionModel {

    /** ## on メソッド
     * ### テーブル結合条件を指定する
     * @param block 条件を構築するための DSL ブロック。`ConditionBuilder` の拡張ラムダとして記述。
     * @return 結合元の検索クエリ
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    fun on(block: ConditionBuilder.() -> Unit): SelectBody<out SelectEntity, *>
}