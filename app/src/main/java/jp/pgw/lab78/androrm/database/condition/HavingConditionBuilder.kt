package jp.pgw.lab78.androrm.database.condition

import jp.pgw.lab78.androrm.database.condition.annotation.HavingDslMarker
import jp.pgw.lab78.androrm.database.condition.base.BaseConditionBuilder
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues

/**
 * ## SQL 条件外観調整クラス
 * ### 「having by」に記述するときの
 * ### 外観を分かりやすくするクラス
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
@HavingDslMarker
class HavingConditionBuilder(private val valueHolder: QueryWithBindValues) :
    BaseConditionBuilder<HavingConditionBuilder>(valueHolder) {
    /** Self インスタンス生成関数（自クラスを生成する） */
    override fun createSelf() = HavingConditionBuilder(valueHolder)
}