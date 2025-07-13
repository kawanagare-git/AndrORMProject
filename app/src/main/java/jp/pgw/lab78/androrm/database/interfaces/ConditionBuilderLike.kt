package jp.pgw.lab78.androrm.database.interfaces

import jp.pgw.lab78.androrm.database.sealed.Condition

interface ConditionBuilderLike {
    fun buildList(): List<Condition>
}