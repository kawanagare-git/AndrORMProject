package jp.pgw.lab78.androrm.database.condition.sealed

import jp.pgw.lab78.androrm.common.Constants.LogicalOperator
import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumnName
import jp.pgw.lab78.androrm.common.database.SupportFunction.getRelationAlias
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryStructureLike
import jp.pgw.lab78.androrm.database.condition.interfaces.SelectBody
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator.*
import jp.pgw.lab78.androrm.database.reference.ColumnRef
import jp.pgw.lab78.androrm.database.utility.EntityManager.extractClassFromProperty
import jp.pgw.lab78.androrm.database.utility.EntityManager.toColumnString
import kotlin.reflect.KProperty1

/**
 * ## SQL 条件基底クラス
 * ### SQL で使用する結合・検索条件を生成するための基底クラス
 *
 * ### 仕様
 * #### WHERE、JOIN、HAVING などへ配置できる条件要素を共通の SQL 構築インターフェースとして表現する。
 * #### 具体的な比較条件、自由記述、論理グループはそれぞれが自身の SQL 断片を生成する。
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
 *
 * ### 仕様
 * #### 比較演算子と左辺を保持する条件群の基底となり、値比較、範囲、集合、サブクエリ、NULL 判定を型別に表現する。
 * #### 通常値を使用する具象条件はプレースホルダーを出力し、値自体は条件ビルダー側で管理する。
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
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    data class Value(
        val lhsProperty: String,
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
            return "$lhsProperty ${operator.symbol} ${value.toSqlConditionText()}"
        }
    }

    /**
     * ## 範囲条件記述用クラス
     * ### BETWEEN 範囲条件を指定
     * @param lhsProperty 検索条件のカラム
     * @param start 下限検索値
     * @param end 上限検索値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    data class Between(
        val lhsProperty: String,
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
            return "$lhsProperty between $start and $end"
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
    class BetweenBuilder(
        private val property: String,
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
     * ## メンバーシップ検索条件用関数
     * @param lhsProperty 検索対象
     * @param subQuery 検索値抽出クエリ
     * @author Masahiro Inoue
     * @since 2026-06-27
     */
    data class InSelect(
        val lhsProperty: String,
        val subQuery: SelectBody<out SelectEntity, *>,
    ) : Condition() {
        /**
         * IN句へサブクエリを組み込んだ条件文字列を生成する。
         *
         * @return 生成された条件文字列
         * @author Masahiro Inoue
         * @since 2026-06-27
         */
        override fun build(): String =
            "$lhsProperty in (${subQuery.build()})"
    }

    /**
     * ## メンバーシップ検索条件用関数
     * @param lhsProperty 検索対象
     * @param subQuery 検索値抽出クエリ
     * @author Masahiro Inoue
     * @since 2026-06-27
     */
    data class NotInSelect(
        val lhsProperty: String,
        val subQuery: SelectBody<out SelectEntity, *>,
    ) : Condition() {
        /**
         * NOT IN句へサブクエリを組み込んだ条件文字列を生成する。
         *
         * @return 生成された条件文字列
         * @author Masahiro Inoue
         * @since 2026-06-27
         */
        override fun build(): String =
            "$lhsProperty not in (${subQuery.build()})"
    }

    /**
     * ## EXISTS 条件記述用クラス
     * ### where に使用する単一条件を指定
     * @param subQuery サブクエリ
     * @author Masahiro Inoue
     * @since 2025-10-03
     */
    class Exists<T : SelectEntity>(
        private val subQuery: SelectBody<T, *>,
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
     * @author Masahiro Inoue
     * @since 2025-10-03
     */
    class NotExists<T : SelectEntity>(
        private val subQuery: SelectBody<T, *>,
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
     * @author Masahiro Inoue
     * @since 2025-10-03
     */
    data class IsNull(
        val lhsProperty: String,
    ) : Condition() {

        /**
         * ## 単一条件生成メソッド
         * ### 定義された条件から文字列を生成する
         * @return 生成された文字列
         * @author Masahiro Inoue
         * @since 2025-10-03
         */
        override fun build(): String {
            return "$lhsProperty ${IS_NULL.symbol}"
        }
    }

    /**
     * ## not null 条件記述用クラス
     * ### where に使用する単一条件を指定
     * @param lhsProperty 検索条件のカラム
     * @author Masahiro Inoue
     * @since 2025-10-03
     */
    data class IsNotNull(
        val lhsProperty: String,
    ) : Condition() {

        /**
         * ## 単一条件生成メソッド
         * ### 定義された条件から文字列を生成する
         * @return 生成された文字列
         * @author Masahiro Inoue
         * @since 2025-10-03
         */
        override fun build(): String {
            return "$lhsProperty ${IS_NOT_NULL.symbol}"
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
private fun Any.toSqlConditionText(): String =
    when (this) {
        is Collection<*> -> this.joinToString(", ", "(", ")")
        { it?.toSqlConditionText() ?: "null" }

        is KProperty1<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            (this as KProperty1<out Entity, *>).toColumnString(true)
        }

        is ColumnRef<*, *> -> this.build()
        else -> this.toString()
    }

/**
 * ## 条件定義クラス
 * ### join や where で使用する条件の基底クラス
 *
 * ### 仕様
 * #### 呼び出し元が指定した SQL 条件断片を加工せず保持し、そのまま条件要素として出力する。
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
 *
 * ### 仕様
 * #### Entity プロパティをテーブル別名付きカラム名へ変換し、GROUP BY の1要素として出力する。
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
            "${it.extractClassFromProperty().getRelationAlias()}.${it.getColumnName()}"
        }
    }
}

/**
 * ## 複数条件記述用クラス
 * ### 複数の条件を束ねる論理条件（AND/OR）
 *
 * ### 仕様
 * #### 子条件を保持した順に指定演算子で連結して括弧で囲む。子条件がない状態での SQL 構築は許可しない。
 * @param operator "AND" または "OR"
 * @param conditions 論理演算子で結合する検索条件のリスト
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
data class LogicalCondition(
    val operator: String = LogicalOperator.AND.query, // "AND" または "OR"
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
        val body = conditions.joinToString(" ${operator.trim()} ") {
            when (it) {
                is LogicalCondition -> it.build(visited)
                else -> it.build()
            }
        }
        return "($body)"
    }
}
