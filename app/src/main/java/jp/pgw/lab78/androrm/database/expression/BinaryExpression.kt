package jp.pgw.lab78.androrm.database.expression

import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.interfaces.SqlExpression

/**
 * ## 二項演算式
 * ### 左辺・演算子・右辺から構成される SQL 式
 *
 * ### 仕様
 * #### 左辺、演算子、右辺の順に式を構築して括弧で囲み、各子式が追加するバインド値の順序を維持する。
 * @param lhs 左辺式
 * @param operator 演算子
 * @param rhs 右辺式
 * @author Masahiro Inoue
 * @since 2026-06-19
 */
data class BinaryExpression(
    val lhs: SqlExpression,
    val operator: SqlArithmeticOperator,
    val rhs: SqlExpression,
) : SqlExpression {
    /**
     * ## SQL 文字列生成
     * @param valueHolder バインド値管理オブジェクト
     * @return SQL 文字列
     * @author Masahiro Inoue
     * @since 2026-06-19
     */
    override fun build(valueHolder: QueryWithBindValues): String =
        "(${lhs.build(valueHolder)} ${operator.symbol} ${rhs.build(valueHolder)})"
}
