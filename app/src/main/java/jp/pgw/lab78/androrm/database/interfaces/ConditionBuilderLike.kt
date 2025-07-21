package jp.pgw.lab78.generated.database.interfaces

import jp.pgw.lab78.generated.database.condition.sealed.Condition

interface ConditionBuilderLike {
    fun buildList(): List<Condition>
}