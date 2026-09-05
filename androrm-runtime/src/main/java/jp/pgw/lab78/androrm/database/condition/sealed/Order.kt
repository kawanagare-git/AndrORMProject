package jp.pgw.lab78.androrm.database.condition.sealed

import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumnName
import jp.pgw.lab78.androrm.common.database.SupportFunction.getRelationAlias
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryStructureLike
import jp.pgw.lab78.androrm.database.utility.EntityManager.extractClassFromProperty
import kotlin.reflect.KProperty1

/**
 * ## Order クラス
 * ### ORDER BY 句を構築するためのデータ構造
 *
 * ### 仕様
 * #### Entity プロパティ、昇降順、NULL の配置を保持し、テーブル別名付きカラムの ORDER BY 要素へ変換する。
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
            "${column.extractClassFromProperty().getRelationAlias()}.${column.getColumnName()}"
        return build(columnName)
    }

    /**
     * ## 任意列式による SQL 文字列生成
     * ### 並び順と NULL 配置を維持したまま、呼び出し元が解決した列式で ORDER BY 要素を生成する
     * @param columnExpression ORDER BY に使用する列式
     * @return ORDER BY 句の SQL 文字列
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    internal fun build(columnExpression: String): String {
        val orderDir = if (ascending) "asc" else "desc"
        val nullsClause = if (nullsLast) "nulls last" else "nulls first"
        return "$columnExpression $orderDir $nullsClause"
    }
    }
