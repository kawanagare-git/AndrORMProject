package jp.pgw.lab78.androrm.database.condition.sealed

import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumn
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator
import jp.pgw.lab78.androrm.database.function.AggregateFunction
import jp.pgw.lab78.androrm.utility.Functions.extractClassFromProperty
import jp.pgw.lab78.androrm.utility.Functions.formatValue
import jp.pgw.lab78.androrm.utility.Functions.getAlias
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * ## SQL 条件基底クラス
 * ### SQL で使用する結合・検索条件を生成するための基底クラス
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
sealed class Condition {
    /**
     * ## 条件生成メソッド
     * ### 定義された条件から文字列を生成する
     * @return 生成された文字列
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    abstract fun build(): String
}

/**
 * ## 条件定義クラス
 * ### join や where で使用する条件の基底クラス
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
sealed class Compare : Condition() {

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
            val mainAlias = getAlias(extractClassFromProperty(mainProperty) as KClass<out Entity>)
            val mainColumn = mainProperty.getColumn()
            val joinedAlias = getAlias(extractClassFromProperty(joinedProperty) as KClass<out Entity>)
            val joinedColumn = mainProperty.getColumn()
            return "$mainAlias$mainColumn ${operator.symbol} $joinedAlias$joinedColumn"
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
            val alias = getAlias(extractClassFromProperty(lhsProperty) as KClass<out Entity>)
            val column = lhsProperty.getColumn()
            return "$alias$column ${operator.symbol} ${formatValue(value)}"
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
        override fun build(): String = "${leftFunction.create(leftProperty)} " +
                                        "${operator.symbol} ${rightFunction.create(rightProperty)}"
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
        override fun build(): String = "${function.create(property)} " +
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
        override fun build(): String = "${function.create(property)} " +
                "${operator.symbol} ${expression.joinToString(" ")}"
    }
}

/**
 * ## 条件定義クラス
 * ### join や where で使用する条件の基底クラス
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
sealed class FreeText : Condition() {
    /**
     * ## 結合条件記述用クラス
     * ### join に使用する単一条件を指定
     * @param text 自由記述した検索条件
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    data class FreeCondition(
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
        return conditions.joinToString(" $operator ", "(", ")") { it.build() }
    }

}