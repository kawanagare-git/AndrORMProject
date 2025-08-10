package jp.pgw.lab78.androrm.database.condition.interfaces

/**
 * ## AndrORM 論理条件生成インターフェース
 * ### 「join」・「where」・「having by」の
 * ### 論理積・論理和の外観を整える雛形
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
interface LogicalConditionSupportLike<T : ConditionBuilderLike> {
    /**
     * ## 論理積メソッド
     * @param block 検索条件の記述
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun and(block: T.() -> Unit)

    /**
     * ## 論理和メソッド
     * @param block 検索条件の記述
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun or(block: T.() -> Unit)
}
