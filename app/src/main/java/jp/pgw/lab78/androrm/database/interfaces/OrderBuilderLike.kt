package jp.pgw.lab78.androrm.database.interfaces

import jp.pgw.lab78.androrm.database.condition.sealed.Order

interface OrderBuilderLike {
    fun buildList(): List<Order>
}