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
    val <T> KProperty1<T, *>.asc: OrderDsl
        get() = this@OrderDsl.also { orders += Order(this, ascending = true) }

    /**
     * ## 降順（DESC）
     * @author Masahiro Inoue
     * @since 2025-10-21
     */
    val <T> KProperty1<T, *>.desc: OrderDsl
        get() = this@OrderDsl.also { orders += Order(this, ascending = false) }

    /**
     * ## NULLS LAST（null 末尾専用修飾）
     * @author Masahiro Inoue
     * @since 2025-10-21
     */
    val <T> KProperty1<T, *>.nullsLast: OrderDsl
        get() = this@OrderDsl.also { orders += Order(this, ascending = true, nullsLast = true) }

    /**
     * ## NULLS FIRST（null 先頭専用修飾）
     * @author Masahiro Inoue
     * @since 2025-10-21
     */
    val <T> KProperty1<T, *>.nullsFirst: OrderDsl
        get() = this@OrderDsl.also { orders += Order(this, ascending = true, nullsLast = false) }

    /**
     *  ## 昇順（ASC）
     *  @author Masahiro Inoue
     *  @since 2025-10-21
     */
    val OrderDsl.asc: OrderDsl
        get() = this.also { orders += orders.removeAt(orders.lastIndex).copy(ascending = true) }

    /**
     * ## 降順（DESC）
     * @author Masahiro Inoue
     * @since 2025-10-21
     */
    val OrderDsl.desc: OrderDsl
        get() = this.also { orders += orders.removeAt(orders.lastIndex).copy(ascending = false) }

    /**
     * ## NULLS LAST（降順専用修飾）
     * @author Masahiro Inoue
     * @since 2025-10-21
     */
    val OrderDsl.nullsLast: OrderDsl
        get() = this.also { orders += orders.removeAt(orders.lastIndex).copy(nullsLast = true) }

    /**
     * ## NULLS FIRST（null 先頭専用修飾）
     * @author Masahiro Inoue
     * @since 2025-10-21
     */
    val OrderDsl.nullsFirst: OrderDsl
        get() = this.also { orders += orders.removeAt(orders.lastIndex).copy(nullsLast = false) }
}