package jp.pgw.lab78.androrm.database.condition

import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator
import jp.pgw.lab78.androrm.database.condition.sealed.Condition
import jp.pgw.lab78.androrm.database.condition.sealed.HavingCompare
import jp.pgw.lab78.androrm.database.function.AggregateFunction
import jp.pgw.lab78.androrm.database.interfaces.ConditionBuilderLike
import jp.pgw.lab78.androrm.database.interfaces.LogicalConditionSupport
import kotlin.reflect.KProperty1


/**
 * ## SQL 条件外観調整クラス
 * ### 「having by」に記述するときの
 * ### 外観を分かりやすくするクラス
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
class HavingConditionBuilder: ConditionBuilderLike,LogicalConditionSupport<ConditionBuilder>{

    /** 条件管理リスト */
    private val list  = mutableListOf<Condition>()

    /** LogicalConditionSupport インターフェースのデリゲート */
    private val delegate by LogicalConditionDelegate({ ConditionBuilder() }, list)

    /**
     * ## 単一条件用関数
     * @param function 検索条件の関数指定
     * @param property 関数内に指定するカラム
     * @param operator 検索演算子
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <T : Entity> condition(
        function: AggregateFunction,
        property: KProperty1<T, *>,
        operator: ComparisonOperator,
        value: Any
    ) {
        list +=  HavingCompare.Value(function ,property, operator, value)
    }

    /**
     * ## 単一条件用関数
     * @param leftFunction 左辺検索条件の関数指定
     * @param leftProperty 左辺関数内に指定するカラム
     * @param operator 検索演算子
     * @param rightFunction 右辺検索条件の関数指定
     * @param rightProperty 右辺関数内に指定するカラム
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <T1 : Entity,T2 : Entity> condition(
        leftFunction: AggregateFunction,
        leftProperty: KProperty1<T1, *>,
        operator: ComparisonOperator,
        rightFunction: AggregateFunction,
        rightProperty: KProperty1<T2, *>,
    ) {
        list +=  HavingCompare.Function(leftFunction, leftProperty, operator, rightFunction, rightProperty)
    }

    /**
     * ## 条件生成メソッド
     * @return 生成された条件
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    override fun buildList(): List<Condition> = list

    /**
     * ## 論理積メソッド
     * @param block 検索条件の記述
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    override fun and(block: ConditionBuilder.() -> Unit) {
        delegate.and(block)
    }

    /**
     * ## 論理和メソッド
     * @param block 検索条件の記述
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    override fun or(block: ConditionBuilder.() -> Unit) {
        delegate.or(block)
    }
}
