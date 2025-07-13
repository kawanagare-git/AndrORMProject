package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.database.interfaces.ConditionBuilderLike
import jp.pgw.lab78.androrm.database.interfaces.Entity
import jp.pgw.lab78.androrm.database.sealed.Compare
import jp.pgw.lab78.androrm.database.sealed.Condition
import jp.pgw.lab78.androrm.database.sealed.HavingCompare
import jp.pgw.lab78.androrm.database.sealed.LogicalCondition
import kotlin.reflect.KProperty1

class HavingConditionBuilder : ConditionBuilderLike {
    private val list = mutableListOf<HavingConditionBuilder>()

    fun <T : Entity> condition(
        function: AggregateFunction,
        property: KProperty1<T, *>,
        operator: ComparisonOperator,
        value: Any
    ) {
        list +=  HavingCompare.Value(function ,property, operator, value)
    }

    fun <T1 : Entity ,T2 : Entity> condition(
        left: KProperty1<T1, *>,
        operator: ComparisonOperator,
        right: KProperty1<T2, *>,
    ) {
        list +=  HavingCompare.Column(left, operator, right)
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
