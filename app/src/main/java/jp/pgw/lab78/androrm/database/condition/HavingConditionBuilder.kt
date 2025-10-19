package jp.pgw.lab78.androrm.database.condition

import jp.pgw.lab78.androrm.database.condition.base.BaseConditionBuilder

/**
 * ## SQL 条件外観調整クラス
 * ### 「having by」に記述するときの
 * ### 外観を分かりやすくするクラス
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
class HavingConditionBuilder : BaseConditionBuilder<HavingConditionBuilder>() {
    /** Self インスタンス生成関数 */
    override fun createSelf() = HavingConditionBuilder()
}