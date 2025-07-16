package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.database.interfaces.ConditionBuilderLike
import jp.pgw.lab78.androrm.database.interfaces.orm.entity.query.Entity
import jp.pgw.lab78.androrm.database.sealed.Compare
import jp.pgw.lab78.androrm.database.sealed.Condition
import jp.pgw.lab78.androrm.database.sealed.LogicalCondition
import kotlin.reflect.KProperty1

class ConditionBuilder : ConditionBuilderLike {
    private val list = mutableListOf<Condition>()

    fun <T : Entity> condition(
        property: KProperty1<T, *>,
        operator: ComparisonOperator,
        value: Any
    ) {
        list +=  Compare.Value(property, operator, value)
    }

    fun <T1 : Entity,T2 : Entity> condition(
        left: KProperty1<T1, *>,
        operator: ComparisonOperator,
        right: KProperty1<T2, *>,
    ) {
        list +=  Compare.Column(left, operator, right)
    }

    fun and(block: ConditionBuilder.() -> Unit) {
        val inner = ConditionBuilder().apply(block)
        list += LogicalCondition("AND", inner.buildList())
    }

    fun or(block: ConditionBuilder.() -> Unit) {
        val inner = ConditionBuilder().apply(block)
        list += LogicalCondition("OR", inner.buildList())
    }

    override fun buildList(): List<Condition> = list
}
