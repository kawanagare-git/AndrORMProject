package jp.pgw.lab78.androrm.database.expression

import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.interfaces.SqlExpression

/**
 * ## 生 SQL 式
 * ### 任意の SQL 断片を式として扱う
 *
 * ### 仕様
 * #### SQL 断片は加工せず返し、付随するバインド値を指定順にバインド値管理オブジェクトへ追加する。
 * @param expression SQL 断片
 * @param bindValues バインド値
 * @author Masahiro Inoue
 * @since 2026-06-19
 */
data class RawExpression(
    val expression: String,
    val bindValues: List<Any?> = emptyList(),
) : SqlExpression {
    /**
     * ## SQL 文字列生成
     * @param valueHolder バインド値管理オブジェクト
     * @return SQL 文字列
     * @author Masahiro Inoue
     * @since 2026-06-19
     */
    override fun build(valueHolder: QueryWithBindValues): String {
        valueHolder.addBindValues(bindValues)
        return expression
    }
}