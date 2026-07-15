package jp.pgw.lab78.androrm.database.condition

import jp.pgw.lab78.androrm.database.condition.annotation.ConditionDslMarker
import jp.pgw.lab78.androrm.database.condition.base.BaseConditionBuilder
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues

/**
 * ## SQL 条件外観調整クラス
 * ### 「where」「 join」に記述するときの
 * ### 外観を分かりやすくするクラス
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
@ConditionDslMarker
class ConditionBuilder(
    private val valueHolder: QueryWithBindValues,
    private val enableAlias: Boolean = true
) :
    BaseConditionBuilder<ConditionBuilder>(valueHolder, enableAlias) {
    /** Self インスタンス生成関数（自クラスを生成する） */
    override fun createSelf() = ConditionBuilder(valueHolder, enableAlias)
}
