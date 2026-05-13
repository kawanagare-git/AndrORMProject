package jp.pgw.lab78.androrm.database.condition.sealed

import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumn
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableAlias
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.database.Select
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryStructureLike
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator.*
import jp.pgw.lab78.androrm.database.reference.ColumnRef
import jp.pgw.lab78.androrm.database.utility.EntityManager.extractClassFromProperty
import kotlin.reflect.KProperty1

/**
 * ## SQL 条件基底クラス
 * ### SQL で使用する結合・検索条件を生成するための基底クラス
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
sealed class Condition : QueryStructureLike {
    /**
     * ## 条件生成メソッド
     * ### 定義された条件から文字列を生成する
     * @return 生成された文字列
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    abstract override fun build(): String
}

/**
 * ## 条件定義クラス
 * ### join や where で使用する条件の基底クラス
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
sealed class Compare : Condition() {

    /**
     * ## 検索条件記述用クラス
     * ### where に使用する単一条件を指定
     * @param lhsProperty 検索条件のカラム
     * @param operator 比較演算子
     * @param value 検索値
     * @return 条件インスタンスを返却（this）
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    data class Value<T : Entity>(
        val lhsProperty: KProperty1<T, *>,
        val operator: ComparisonOperator,
        val value: Any
    ) : Condition() {

        /**
         * ## 単一条件生成メソッド
         * ### 定義された条件から文字列を生成する
         * @return 生成された文字列
         * @author Masahiro Inoue
         * @since 2025-08-01
         */
        override fun build(): String {
            return "${lhsProperty.extractClassFromProperty().getTableAlias()}." +
                    "${lhsProperty.getColumn()} ${operator.symbol} ${value.toSqlConditionText()}"
        }
    }

    /**
     * ## 範囲条件記述用クラス
     * ### BETWEEN 範囲条件を指定
     * @param lhsProperty 検索条件のカラム
     * @param start 下限検索値
     * @param end 上限検索値
     * @return 条件インスタンスを返却（this）
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    data class Between<T : Entity>(
        val lhsProperty: KProperty1<T, *>,
        val start: Any,
        val end: Any
    ) : Condition() {

        /**
         * ## 条件生成メソッド
         * ### 定義された条件から文字列を生成する
         * @return 生成された文字列
         * @author Masahiro Inoue
         * @since 2025-09-13
         */
        override fun build(): String {
            return "${lhsProperty.extractClassFromProperty().getTableAlias()}." +
                    "${lhsProperty.getColumn()} between $start and $end"
        }
    }

    /**
     * ## 範囲条件ビルダークラス
     * ### BETWEEN 範囲条件を構築するためのビルダークラス
     * @param property 検索条件のカラム
     * @param start 下限検索値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    class BetweenBuilder<T : Entity>(
        private val property: KProperty1<T, *>,
        private val start: Any
    ) {
        /**
         * ## 範囲条件生成メソッド
         * ### 定義された条件から文字列を生成する
         * @param end 上限検索値
         * @return 生成された文字列
         * @author Masahiro Inoue
         * @since 2025-09-13
         */
        infix fun and(end: Any): Condition {
            return Between(property, start, end)
        }
    }

    /**
     * ## EXISTS 条件記述用クラス
     * ### where に使用する単一条件を指定
     * @param subQuery サブクエリ
     * @return 条件インスタンスを返却（this）
     * @author Masahiro Inoue
     * @since 2025-10-03
     */
    class Exists<T : SelectEntity>(
        private val subQuery: Select<T>,
    ) : Condition() {

        /**
         * ## 単一条件生成メソッド
         * ### 定義された条件から文字列を生成する
         * @return 生成された文字列
         * @author Masahiro Inoue
         * @since 2025-10-03
         */
        override fun build(): String {
            return "${EXISTS.symbol} (${subQuery.build()})"
        }
    }

    /**
     * ## NOT EXISTS 条件記述用クラス
     * ### where に使用する単一条件を指定
     * @param subQuery サブクエリ
     * @return 条件インスタンスを返却（this）
     * @author Masahiro Inoue
     * @since 2025-10-03
     */
    class NotExists<T : SelectEntity>(
        private val subQuery: Select<T>,
    ) : Condition() {

        /**
         * ## 単一条件生成メソッド
         * ### 定義された条件から文字列を生成する
         * @return 生成された文字列
         * @author Masahiro Inoue
         * @since 2025-10-03
         */
        override fun build(): String {
            return "${NOT_EXISTS.symbol} (${subQuery.build()})"
        }
    }

    /**
     * ## null 条件記述用クラス
     * ### where に使用する単一条件を指定
     * @param lhsProperty 検索条件のカラム
     * @return 条件インスタンスを返却（this）
     * @author Masahiro Inoue
     * @since 2025-10-03
     */
    data class IsNull<T : Entity>(
        val lhsProperty: KProperty1<T, *>,
    ) : Condition() {

        /**
         * ## 単一条件生成メソッド
         * ### 定義された条件から文字列を生成する
         * @return 生成された文字列
         * @author Masahiro Inoue
         * @since 2025-10-03
         */
        override fun build(): String {
            val alias = lhsProperty.extractClassFromProperty().getTableAlias()
            val column = lhsProperty.getColumn()
            return "${alias}.$column ${IS_NULL.symbol}"
        }
    }

    /**
     * ## not null 条件記述用クラス
     * ### where に使用する単一条件を指定
     * @param lhsProperty 検索条件のカラム
     * @return 条件インスタンスを返却（this）
     * @author Masahiro Inoue
     * @since 2025-10-03
     */
    data class IsNotNull<T : Entity>(
        val lhsProperty: KProperty1<T, *>,
    ) : Condition() {

        /**
         * ## 単一条件生成メソッド
         * ### 定義された条件から文字列を生成する
         * @return 生成された文字列
         * @author Masahiro Inoue
         * @since 2025-10-03
         */
        override fun build(): String {
            val alias = lhsProperty.extractClassFromProperty().getTableAlias()
            val column = lhsProperty.getColumn()
            return "${alias}.$column ${IS_NOT_NULL.symbol}"
        }
    }

    /**
     * ## alias 明示カラムと値の比較条件
     * ### TableRef から生成した ColumnRef を使用する
     * @param lhsColumn 検索条件のカラム参照情報
     * @param operator 比較条件
     * @param value 検索値
     * @return 条件インスタンスを返却（this）
     * @author Masahiro Inoue
     * @since 2026-05-12
     */
    data class ColumnValue(
        val lhsColumn: ColumnRef<out Entity, *>,
        val operator: ComparisonOperator,
        val value: Any
    ) : Condition() {

        override fun build(): String {
            return "${lhsColumn.build()} ${operator.symbol} ${value.toSqlConditionText()}"
        }
    }

    /**
     * ## alias 明示カラム同士の比較条件
     * ### 同一 Entity 複数 join 時に alias を明示して比較する
     * @param lhsColumn 検索条件のカラム参照情報（左辺）
     * @param operator 比較条件
     * @param rhsColumn 検索条件のカラム参照情報（右辺）
     * @return 条件インスタンスを返却（this）
     * @author Masahiro Inoue
     * @since 2026-05-12
     */
    data class ColumnColumn(
        val lhsColumn: ColumnRef<out Entity, *>,
        val operator: ComparisonOperator,
        val rhsColumn: ColumnRef<out Entity, *>,
    ) : Condition() {

        override fun build(): String {
            return "${lhsColumn.build()} ${operator.symbol} ${rhsColumn.build()}"
        }
    }

    /**
     * ## alias 明示カラムの BETWEEN 条件
     * @author Masahiro Inoue
     * @since 2026-05-12
     */
    data class ColumnBetween(
        val column: ColumnRef<out Entity, *>,
        val start: Any,
        val end: Any,
    ) : Condition() {

        override fun build(): String {
            return "${column.build()} between $start and $end"
        }
    }

    /**
     * ## alias 明示カラムの IS NULL 条件
     * @author Masahiro Inoue
     * @since 2026-05-12
     */
    data class ColumnIsNull(
        val column: ColumnRef<out Entity, *>,
    ) : Condition() {

        override fun build(): String {
            return "${column.build()} ${IS_NULL.symbol}"
        }
    }

    /**
     * ## alias 明示カラムの IS NOT NULL 条件
     * ### where に使用する単一条件を指定
     * @param column 検索条件のカラム
     * @author Masahiro Inoue
     * @since 2026-05-12
     */
    data class ColumnIsNotNull(
        val column: ColumnRef<out Entity, *>,
    ) : Condition() {

        override fun build(): String {
            return "${column.build()} ${IS_NOT_NULL.symbol}"
        }
    }
}

/**
 * ## SQL 条件文字列生成メソッド
 * ### 右辺に来る文字列を生成
 * @receiver 右辺に来るクラスのインスタンス
 * @return 生成された文字列
 * @author Masahiro Inoue
 * @since 2026-05-12
 */
private fun Any.toSqlConditionText(): String {
    return when (this) {
        is Collection<*> -> this.joinToString(", ", "(", ")")
        is ColumnRef<*, *> -> this.build()
        else -> this.toString()
    }
}

/**
 * ## 条件定義クラス
 * ### join や where で使用する条件の基底クラス
 * @param text 自由記述した検索条件
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
data class FreeText(
    val text: String
) : Condition() {

    /**
     * ## 単一条件生成メソッド
     * ### 定義された条件から文字列を生成する
     * @return 生成された文字列
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    override fun build(): String = text
}

/**
 * ## GROUP BY 条件定義クラス
 * ### GROUP BY で使用する条件の基底クラス
 * @param column 検索条件のカラム
 * @author Masahiro Inoue
 * @since 2026-01-20
 */
data class GroupByColumn(
    val column: KProperty1<out Entity, *>
) : Condition() {
    /**
     * ## 単一条件生成メソッド
     * ### 定義された条件から文字列を生成する
     * @return 生成された文字列
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    override fun build(): String {
        return column.let {
            "${it.extractClassFromProperty().getTableAlias()}.${it.getColumn()}"
        }
    }
}

/**
 * ## 複数条件記述用クラス
 * ### 複数の条件を束ねる論理条件（AND/OR）
 * @param operator "AND" または "OR"
 * @param conditions 検索条件のカラム
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
data class LogicalCondition(
    val operator: String = "AND", // "AND" または "OR"
    val conditions: List<Condition>
) : Condition() {

    /**
     * ## 複数条件生成メソッド
     * ### 定義された条件から文字列を生成する
     * @return 生成された文字列
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    override fun build(): String {
        return build(mutableSetOf())
    }

    /**
     * ## 循環検出メソッド
     * ### 循環参照を検出し問題がなければ、条件文字列を生成
     * @param visited 循環検出リスト
     * @return 生成された文字列 / 循環参照を検出した場合 "<cycle>"
     * @author Masahiro Inoue
     * @since 2025-08-13
     */
    private fun build(visited: MutableSet<LogicalCondition>): String {
        if (!visited.add(this)) return "<cycle>"   // 循環検出
        val body = conditions.joinToString(" $operator ") {
            when (it) {
                is LogicalCondition -> it.build(visited)
                else -> it.build()
            }
        }
        return "($body)"
    }
}

