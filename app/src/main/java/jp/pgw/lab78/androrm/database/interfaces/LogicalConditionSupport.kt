package jp.pgw.lab78.androrm.database.interfaces

/**
 * ## AndrORM 論理条件生成インターフェース
 * ### 「join」・「where」・「having by」の
 * ### 論理積・論理和の外観を整える雛形
 */
interface LogicalConditionSupport<T : ConditionBuilderLike> {
    /**
     * ## 論理積メソッド
     * @param block 検索条件の記述
     */
    fun and(block: T.() -> Unit)

    /**
     * ## 論理和メソッド
     * @param block 検索条件の記述
     */
    fun or(block: T.() -> Unit)
}
