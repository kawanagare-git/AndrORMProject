package jp.pgw.lab78.androrm.database.condition

import jp.pgw.lab78.androrm.database.condition.sealed.Condition
import jp.pgw.lab78.androrm.database.condition.sealed.HavingCompare
import jp.pgw.lab78.androrm.database.condition.sealed.LogicalCondition
import jp.pgw.lab78.androrm.database.function.AggregateFunction
import jp.pgw.lab78.androrm.database.interfaces.ConditionBuilderLike
import jp.pgw.lab78.androrm.database.interfaces.entity.Entity
import jp.pgw.lab78.androrm.database.operator.ComparisonOperator
import kotlin.reflect.KProperty1

class HavingConditionBuilder : ConditionBuilderLike {
    private val list = mutableListOf<Condition>()

    fun <T : Entity> condition(
        function: AggregateFunction,
        property: KProperty1<T, *>,
        operator: ComparisonOperator,
        value: Any
    ) {
        list +=  HavingCompare.Value(function ,property, operator, value)
    }

    fun <T1 : Entity,T2 : Entity> condition(
        leftFunction: AggregateFunction,
        leftProperty: KProperty1<T1, *>,
        operator: ComparisonOperator,
        rightFunction: AggregateFunction,
        rightProperty: KProperty1<T2, *>,
    ) {
        list +=  HavingCompare.Function(leftFunction, leftProperty, operator, rightFunction, rightProperty)
    }

    fun and(block: HavingConditionBuilder.() -> Unit) {
        val inner = HavingConditionBuilder().apply(block)
        list += LogicalCondition("AND", inner.buildList())
    }

    fun or(block: HavingConditionBuilder.() -> Unit) {
        val inner = HavingConditionBuilder().apply(block)
        list += LogicalCondition("OR", inner.buildList())
    }

    override fun buildList(): List<Condition> = list
}
