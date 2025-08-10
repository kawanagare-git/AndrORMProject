package jp.pgw.lab78.androrm.database.condition.interfaces

import jp.pgw.lab78.androrm.database.condition.sealed.Condition

/**
 * ## 条件生成リスト抽出インターフェース
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
interface ConditionBuilderLike {
    /**
     * ## 条件生成リスト抽出メソッド
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun buildList(): List<Condition>
}