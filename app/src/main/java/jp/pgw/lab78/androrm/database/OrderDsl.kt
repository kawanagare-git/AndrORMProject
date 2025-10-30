package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.database.condition.annotation.OrderDslMarker
import jp.pgw.lab78.androrm.database.condition.sealed.Order
import kotlin.reflect.KProperty1

/**
 * ## 並び替え DSL クラス
 * ### 並び替え条件を DSL 形式で指定するためのクラス
 * @author Masahiro Inoue
 * @since 2025-10-21
 */
@OrderDslMarker
class OrderDsl {
    internal val orders = mutableListOf<Order>()

    /**
     *  ## 昇順（ASC）
     *  @author Masahiro Inoue
     *  @since 2025-10-21
     */
    val <T> KProperty1<T, *>.asc: Order
        get() = Order(this, ascending = true).also { orders += it }

    /**
     * ## 降順（DESC）
     * @author Masahiro Inoue
     * @since 2025-10-21
     */
    val <T> KProperty1<T, *>.desc: Order
        get() = Order(this, ascending = false).also { orders += it }

    /**
     * ## NULLS LAST（降順専用修飾）
     * @author Masahiro Inoue
     * @since 2025-10-21
     */
    val Order.nullsLast: Order
        get() = this.copy(nullsLast = true)

    /**
     * ## NULLS FIRST（昇順専用修飾）
     * @author Masahiro Inoue
     * @since 2025-10-21
     */
    val Order.nullsFirst: Order
        get() = this.copy(nullsLast = false)
}