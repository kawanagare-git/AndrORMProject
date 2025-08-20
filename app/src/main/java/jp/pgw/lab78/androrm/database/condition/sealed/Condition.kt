package jp.pgw.lab78.androrm.database.condition.sealed

import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumn
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableAlias
import jp.pgw.lab78.androrm.common.dml.DMLInterfaceEnum.CONDITION
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryStructureLike
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator
import jp.pgw.lab78.androrm.database.function.AggregateFunction
import jp.pgw.lab78.androrm.database.utility.EntityManager.extractClassFromProperty
import jp.pgw.lab78.androrm.database.utility.EntityManager.formatValue
import kotlin.reflect.KProperty1
import kotlin.reflect.full.superclasses

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

    companion object {
        /** 条件エンティティのクラス型 */
        private val CONDITION_CLASS = CONDITION.kClass
    }
    /**
     * ## 結合条件記述用クラス
     * ### join に使用する単一条件を指定
     * @param mainProperty 検索条件の主カラム
     * @param operator 検索演算子
     * @param joinedProperty 検索条件の結合カラム
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    data class Column<T1 : Entity,T2 : Entity>(
        val mainProperty: KProperty1<T1, *>,
        val operator: ComparisonOperator,
        val joinedProperty: KProperty1<T2, *>,
    ) : Condition() {

        /**
         * ## 単一条件生成メソッド
         * ### 定義された条件から文字列を生成する
         * @return 生成された文字列
         * @author Masahiro Inoue
         * @since 2025-08-01
         */
        override fun build(): String {
            val mainAlias = mainProperty.extractClassFromProperty().getTableAlias()
            val mainColumn = mainProperty.getColumn()
            val declaringClass = joinedProperty.extractClassFromProperty()
            val joined = if (declaringClass.superclasses.contains(CONDITION_CLASS)) {
                ":${joinedProperty.getColumn()}"
            } else {
                val joinedAlias = joinedProperty.extractClassFromProperty().getTableAlias()
                val joinedColumn = joinedProperty.getColumn()
                "${joinedAlias}.$joinedColumn"
            }
            return "${mainAlias}.$mainColumn ${operator.symbol} $joined"
        }
    }

    /**
     * ## 検索条件記述用クラス
     * ### where に使用する単一条件を指定
     * @param lhsProperty 検索条件のカラム
     * @param operator 検索演算子
     * @param value 検索値
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
            val alias = lhsProperty.extractClassFromProperty().getTableAlias()
            val column = lhsProperty.getColumn()
            return "${alias}.$column ${operator.symbol} ${formatValue(value)}"
        }
    }
}

/**
 * ## having 条件定義クラス
 * ### 関数を用いた検索条件を記述する
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
sealed class HavingCompare : Condition() {
    /**
     * ## 結合条件記述用クラス
     * ### having by に使用する単一条件を指定
     * @param leftFunction 検索条件の左辺関数
     * @param leftProperty 検索条件の左辺カラム
     * @param operator 検索演算子
     * @param leftFunction 検索条件の右辺関数
     * @param leftProperty 検索条件の右辺カラム
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    data class Function<T1 : Entity,T2 : Entity>(
        val leftFunction: AggregateFunction,
        val leftProperty: KProperty1<T1, *>,
        val operator: ComparisonOperator,
        val rightFunction: AggregateFunction,
        val rightProperty: KProperty1<T2, *>,
    ) : Condition() {

        /**
         * ## 単一条件生成メソッド
         * ### 定義された条件から文字列を生成する
         * @return 生成された文字列
         * @author Masahiro Inoue
         * @since 2025-08-01
         */
        override fun build(): String = "${leftFunction.build(leftProperty)} " +
                                        "${operator.symbol} ${rightFunction.build(rightProperty)}"
    }

    /**
     * ## 検索条件記述用クラス
     * ### having by に使用する単一条件を指定
     * @param function 検索条件の関数
     * @param property 検索条件のカラム
     * @param operator 検索演算子
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    data class Value<T : Entity>(
        val function: AggregateFunction,
        val property: KProperty1<T, *>,
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
        override fun build(): String = "${function.build(property)} " +
                                        "${operator.symbol} ${formatValue(value)}"
    }

    /**
     * ## 結合条件記述用クラス
     * ### having by に使用する単一条件を指定
     * @param function 検索条件の関数
     * @param property 検索条件のカラム
     * @param operator 検索演算子
     * @param expression 検索条件のカラム
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    data class Expression<T : Entity>(
        val function: AggregateFunction,
        val property: KProperty1<T, *>,
        val operator: ComparisonOperator,
        val expression: List<String>,
    ) : Condition() {

        /**
         * ## 単一条件生成メソッド
         * ### 定義された条件から文字列を生成する
         * @return 生成された文字列
         * @author Masahiro Inoue
         * @since 2025-08-01
         */
        override fun build(): String = "${function.build(property)} " +
                "${operator.symbol} ${expression.joinToString(" ")}"
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