package jp.pgw.lab78.androrm.database.sealed

import jp.pgw.lab78.androrm.database.AggregateFunction
import jp.pgw.lab78.androrm.database.ComparisonOperator
import jp.pgw.lab78.androrm.database.SupportFunction.extractClassFromProperty
import jp.pgw.lab78.androrm.database.SupportFunction.formatValue
import jp.pgw.lab78.androrm.database.SupportFunction.getAlias
import jp.pgw.lab78.androrm.database.SupportFunction.simpleNameToSnakeCase
import jp.pgw.lab78.androrm.database.interfaces.orm.entity.query.Entity
import jp.pgw.lab78.androrm.utility.Functions.toSnakeCase
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * ## SQL 条件基底クラス
 * ### SQL で使用する結合・検索条件を生成するための着ていクラス
 */
sealed class Condition {
    /**
     * ## 条件生成メソッド
     * ### 定義された条件から文字列を生成する
     * @return 生成された文字列
     */
    abstract fun build(): String
}

/**
 * ## 条件定義クラス
 * ###
 */
sealed class Compare : Condition() {

    /**
     * ## 結合条件記述用クラス
     * ### JOIN に使用する単一条件を指定
     * @param mainProperty 検索条件の主カラム
     * @param operator 検索演算子
     * @param joinedProperty 検索条件の結合カラム
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
         */
        override fun build(): String {
            val mainAlias = getAlias(extractClassFromProperty(mainProperty) as KClass<out Entity>)
            val mainColumn = mainProperty.name.toSnakeCase()
            val joinedAlias = getAlias(extractClassFromProperty(joinedProperty) as KClass<out Entity>)
            val joinedColumn = mainProperty.name.toSnakeCase()
            return "$mainAlias$mainColumn ${operator.symbol} $joinedAlias$joinedColumn"
        }
    }

    /**
     * ## 検索条件記述用クラス
     * ### WHERE に使用する単一条件を指定
     * @param lhsProperty 検索条件のカラム
     * @param operator 検索演算子
     * @param value 検索値
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
         */
        override fun build(): String {
            val alias = getAlias(extractClassFromProperty(lhsProperty) as KClass<out Entity>)
            val column = lhsProperty.simpleNameToSnakeCase()
            return "$alias$column ${operator.symbol} ${formatValue(value)}"
        }
    }
}

/**
 * ## having 条件定義クラス
 * ###
 */
sealed class HavingCompare : Condition() {

    /**
     * ## 結合条件記述用クラス
     * ### JOIN に使用する単一条件を指定
     * @param leftFunction 検索条件の左辺関数
     * @param leftProperty 検索条件の左辺カラム
     * @param operator 検索演算子
     * @param leftFunction 検索条件の右辺関数
     * @param leftProperty 検索条件の右辺カラム
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
         */
        override fun build(): String = "${leftFunction.create(leftProperty)} " +
                                        "${operator.symbol} ${rightFunction.create(rightProperty)}"
    }

    /**
     * ## 検索条件記述用クラス
     * ### WHERE に使用する単一条件を指定
     * @param function 検索条件の関数
     * @param property 検索条件のカラム
     * @param operator 検索演算子
     * @param value 検索値
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
         */
        override fun build(): String = "${function.create(property)} " +
                                        "${operator.symbol} ${formatValue(value)}"
    }

    /**
     * ## 結合条件記述用クラス
     * ### JOIN に使用する単一条件を指定
     * @param function 検索条件の関数
     * @param property 検索条件のカラム
     * @param operator 検索演算子
     * @param expression 検索条件のカラム
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
         */
        override fun build(): String = "${function.create(property)} " +
                "${operator.symbol} ${expression.joinToString(" ")}"
    }
}

/**
 * ## 複数条件記述用クラス
 * ### 複数の条件を束ねる論理条件（AND/OR）
 * @param operator "AND" または "OR"
 * @param conditions 検索条件のカラム
 */
data class LogicalCondition(
    val operator: String = "AND", // "AND" または "OR"
    val conditions: List<Condition>
) : Condition() {

    /**
     * ## 複数条件生成メソッド
     * ### 定義された条件から文字列を生成する
     * @return 生成された文字列
     */
    override fun build(): String {
        return conditions.joinToString(" $operator ", "(", ")") { it.build() }
    }

}