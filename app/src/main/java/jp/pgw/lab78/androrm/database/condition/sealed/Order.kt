package jp.pgw.lab78.androrm.database.condition.sealed

import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumnName
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableAlias
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryStructureLike
import jp.pgw.lab78.androrm.database.utility.EntityManager.extractClassFromProperty
import kotlin.reflect.KProperty1

/**
 * ## Order クラス
 * ### ORDER BY 句を構築するためのデータ構造
 * @param column 並び替え対象カラム
 * @param ascending 並び順（ASC / DESC）
 * @param nullsLast NULL の並び順（NULLS FIRST / LAST）
 * @author Masahiro Inoue
 * @since 2025-10-19
 */
data class Order(
    val column: KProperty1<out Entity, *>,
    val ascending: Boolean = true,
    val nullsLast: Boolean = false
) : QueryStructureLike {

    /**
     * ## SQL 文字列生成
     * ### ORDER BY 句の SQL 文字列を生成する
     * @return ORDER BY 句の SQL 文字列
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    override fun build(): String {
        val columnName =
            "${column.extractClassFromProperty().getTableAlias()}.${column.getColumnName()}"
        val orderDir = if (ascending) "asc" else "desc"
        val nullsClause = if (nullsLast) "nulls last" else "nulls first"
        return "$columnName $orderDir $nullsClause"
    }
}