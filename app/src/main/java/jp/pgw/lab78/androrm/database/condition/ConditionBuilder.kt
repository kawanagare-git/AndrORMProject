package jp.pgw.lab78.androrm.database.condition

import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.database.condition.interfaces.ConditionBuilderLike
import jp.pgw.lab78.androrm.database.condition.interfaces.LogicalConditionSupportLike
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator
import jp.pgw.lab78.androrm.database.condition.sealed.Compare
import jp.pgw.lab78.androrm.database.condition.sealed.Condition
import jp.pgw.lab78.androrm.database.condition.sealed.FreeText
import kotlin.reflect.KProperty1

/**
 * ## SQL 条件外観調整クラス
 * ### 「where」「 join」に記述するときの
 * ### 外観を分かりやすくするクラス
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
class ConditionBuilder: ConditionBuilderLike, LogicalConditionSupportLike<ConditionBuilder>
{
    /** 条件管理リスト */
    private val list  = mutableListOf<Condition>()

    /** LogicalConditionSupport インターフェースのデリゲート */
    private val delegate by LogicalConditionDelegate({ this }, list)

    /**
     * ## 単一条件用関数
     * @param property 検索条件のカラム
     * @param operator 検索演算子
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <T : Entity> condition(
        property: KProperty1<T, *>,
        operator: ComparisonOperator,
        value: Any
    ) {
        list += Compare.Value(property, operator, value)
    }

    /**
     * ## 単一条件用関数
     * @param left 左辺検索条件のカラム
     * @param operator 検索演算子
     * @param right 右辺検索条件のカラム
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <T1 : Entity,T2 : Entity> condition(
        left: KProperty1<T1, *>,
        operator: ComparisonOperator,
        right: KProperty1<T2, *>,
    ) {
        list += Compare.Column(left, operator, right)
    }

    /**
     * ## 単一条件用関数
     * @param text 検索条件の自由記述
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun condition(text: String) {
        list += FreeText(text)
    }

    /**
     * ## 条件生成メソッド
     * @return 生成された条件
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    override fun buildList(): List<Condition> = list

    /**
     * ## 論理積メソッド
     * @param block 検索条件の記述
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    override fun and(block: ConditionBuilder.() -> Unit) {
        delegate.and(block)
    }

    /**
     * ## 論理和メソッド
     * @param block 検索条件の記述
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    override fun or(block: ConditionBuilder.() -> Unit) {
        delegate.or(block)
    }
}
