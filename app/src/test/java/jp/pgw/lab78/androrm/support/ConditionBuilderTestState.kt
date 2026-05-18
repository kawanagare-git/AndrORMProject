package jp.pgw.lab78.androrm.support

import jp.pgw.lab78.androrm.database.condition.ConditionBuilder
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues

/**
 * ## テスト状態管理クラス
 * ### ConditionBuilder と bindValues 保持先をまとめて扱う
 * @param builder ConditionBuilder
 * @param valueHolder QueryWithBindValues
 */
internal data class ConditionBuilderTestState(
    val builder: ConditionBuilder,
    val valueHolder: QueryWithBindValues,
) {

    /**
     * ## 単一 SQL 取得
     * ### 生成された条件が1件である前提で SQL 文字列を取得する
     * @return SQL 文字列
     */
    fun singleSql(): String =
        builder.buildList().single().build()
}

/**
 * ## ConditionBuilder テスト状態生成
 * ### ConditionBuilder のテスト状態を生成する
 * @return ConditionBuilder のテスト状態
 */
internal fun createConditionBuilderTestState(): ConditionBuilderTestState {
    val valueHolder = object : QueryWithBindValues() {}

    return ConditionBuilderTestState(
        builder = ConditionBuilder(valueHolder),
        valueHolder = valueHolder,
    )
}
