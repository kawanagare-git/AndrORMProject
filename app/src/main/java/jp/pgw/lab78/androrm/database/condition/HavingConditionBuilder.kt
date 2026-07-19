package jp.pgw.lab78.androrm.database.condition

import jp.pgw.lab78.androrm.database.condition.annotation.HavingDslMarker
import jp.pgw.lab78.androrm.database.condition.base.BaseConditionBuilder
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues

/**
 * ## SQL 条件外観調整クラス
 * ### `having` に記述するときの
 * ### 外観を分かりやすくするクラス
 *
 * ### 仕様
 * #### HAVING 用の条件 DSL として集計関数を左辺に持つ比較・集合・範囲・論理条件を蓄積し、値を指定順にバインドする。
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
@HavingDslMarker
class HavingConditionBuilder(private val valueHolder: QueryWithBindValues) :
    BaseConditionBuilder<HavingConditionBuilder>(valueHolder) {
    /**
     * 同じバインド値保持先を共有するHAVING条件ビルダーを生成する。
     *
     * @return 新しいHAVING条件ビルダー
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    override fun createSelf() = HavingConditionBuilder(valueHolder)
}