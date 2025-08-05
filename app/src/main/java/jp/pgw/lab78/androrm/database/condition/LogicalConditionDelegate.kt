package jp.pgw.lab78.androrm.database.condition

import jp.pgw.lab78.androrm.database.condition.sealed.Condition
import jp.pgw.lab78.androrm.database.condition.sealed.LogicalCondition
import jp.pgw.lab78.androrm.database.interfaces.ConditionBuilderLike
import jp.pgw.lab78.androrm.database.interfaces.LogicalConditionSupport
import kotlin.reflect.KProperty

/**
 * ## 論理条件生成移譲クラス
 * ### LogicalConditionSupport の実装クラス
 * ### 「join」・「where」・「having by」の
 * ### 論理積・論理和の外観を整えるための実装
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
class LogicalConditionDelegate<T : ConditionBuilderLike>(
    private val ownerFactory: () -> T,
    private val targetList:  MutableList<Condition>
) : LogicalConditionSupport<T> {
    /**
     * ## 委譲プロパティ
     * ### 論理条件生成移譲クラスのインスタンスを移譲元に渡す
     * @param thisRef オーナーオブジェクト：システムで設定
     * @param property プロパティ情報：システムで設定
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = this

    /**
     * ## 論理積メソッド
     * @param block 検索条件の記述
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    override fun and(block: T.() -> Unit) {
        val inner = ownerFactory().apply(block)
        targetList += LogicalCondition("AND", inner.buildList())
    }

    /**
     * ## 論理和メソッド
     * @param block 検索条件の記述
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    override fun or(block: T.() -> Unit) {
        val inner = ownerFactory().apply(block)
        targetList += LogicalCondition("OR", inner.buildList())
    }
}
