package jp.pgw.lab78.androrm.database.expression

import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.interfaces.SqlExpression

/**
 * ## 値式
 * ### 通常値をバインド値として扱う SQL 式
 *
 * ### 仕様
 * #### 構築時に値をバインド値管理オブジェクトへ1件追加し、SQL 表現としてプレースホルダーを返す。
 * @param value 値
 * @author Masahiro Inoue
 * @since 2026-06-19
 */
data class ValueExpression(
    val value: Any?,
) : SqlExpression {
    /**
     * ## SQL 文字列生成
     * @param valueHolder バインド値管理オブジェクト
     * @return SQL 文字列
     * @author Masahiro Inoue
     * @since 2026-06-19
     */
    override fun build(valueHolder: QueryWithBindValues): String {
        valueHolder.addBindValue(value)
        return "?"
    }
}