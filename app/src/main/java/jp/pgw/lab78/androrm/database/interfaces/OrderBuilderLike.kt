package jp.pgw.lab78.androrm.database.interfaces

import jp.pgw.lab78.androrm.database.condition.sealed.Order

/**
 * ## 並び順生成リスト抽出インターフェース
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
interface OrderBuilderLike {
    /**
     * ## 並び順生成リスト抽出メソッド
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun buildList(): List<Order>
}