package jp.pgw.lab78.androrm.support

import jp.pgw.lab78.androrm.database.condition.ConditionBuilder
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues

/**
 * ## テスト準備用部材クラス
 * ###
 */
object TestTools

private fun newFixture(): Fixture {
    val valueHolder = object : QueryWithBindValues() {}
    return Fixture(
        builder = ConditionBuilder(valueHolder),
        valueHolder = valueHolder,
    )

    private fun Fixture.singleSql(): String =
        builder.buildList().single().build()
}
