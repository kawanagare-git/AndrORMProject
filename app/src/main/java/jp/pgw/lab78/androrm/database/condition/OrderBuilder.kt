package jp.pgw.lab78.androrm.database.condition

import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.database.condition.interfaces.OrderBuilderLike
import jp.pgw.lab78.androrm.database.condition.sealed.ColumnOrder
import jp.pgw.lab78.androrm.database.condition.sealed.Order
import kotlin.reflect.KProperty1

/**
 * ## SQL ソート条件クラス
 * ### ソート条件を管理・生成する
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
class OrderBuilder : OrderBuilderLike {
    /** ソート条件のリスト */
    private val list = mutableListOf<Order>()
    /**
     * ## ソート条件指定
     * ### ソート条件に指定するカラム名を指定する
     * @param property ソート条件に指定するカラム
     * @param descending 昇順・降順の指定 / デフォルト（false）は昇順
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <T : Entity> column(
        property: KProperty1<T, *>,
        descending: Boolean = false
    ) {
        list += ColumnOrder(property,descending)
    }

    /**
     * ## 並び順生成リスト
     * @return ビルドに必要な並び順生成リスト
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    override fun buildList(): List<Order> = list
}