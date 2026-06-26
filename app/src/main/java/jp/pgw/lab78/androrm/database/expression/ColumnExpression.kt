package jp.pgw.lab78.androrm.database.expression

import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.interfaces.SqlExpression
import jp.pgw.lab78.androrm.database.reference.ColumnRef

/**
 * ## カラム式
 * ### ColumnRef を SQL 式として扱うためのラッパー
 * @param columnRef カラム参照
 * @author Masahiro Inoue
 * @since 2026-06-19
 */
data class ColumnExpression(
    val columnRef: ColumnRef<out Entity, *>,
) : SqlExpression {
    /**
     * ## SQL 文字列生成
     * @param valueHolder バインド値管理オブジェクト
     * @return SQL 文字列
     * @author Masahiro Inoue
     * @since 2026-06-19
     */
    override fun build(valueHolder: QueryWithBindValues): String =
        columnRef.build()
}