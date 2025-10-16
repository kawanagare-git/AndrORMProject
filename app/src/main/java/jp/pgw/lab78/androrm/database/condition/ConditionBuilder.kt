package jp.pgw.lab78.androrm.database.condition

import jp.pgw.lab78.androrm.common.dml.interfaces.ConditionEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.database.Select
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
class ConditionBuilder : ConditionBuilderLike, LogicalConditionSupportLike<ConditionBuilder> {
    /** 条件管理リスト */
    private val list = mutableListOf<Condition>()

    /** LogicalConditionSupport インターフェースのデリゲート */
    private val delegate by LogicalConditionDelegate({ ConditionBuilder() }, list)

    /**
     * ## 等価条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    infix fun <T : Entity> KProperty1<T, *>.eq(value: Any) {
        list += Compare.Value(this, ComparisonOperator.EQ, value.toConditionValue())
    }

    /**
     * ## 等価条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    infix fun <T : Entity> KProperty1<T, *>.equal(value: Any) = this eq value

    /**
     * ## 不等価条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    infix fun <T : Entity> KProperty1<T, *>.ne(value: Any) {
        list += Compare.Value(this, ComparisonOperator.NE, value.toConditionValue())
    }

    /**
     * ## 不等価条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    infix fun <T : Entity> KProperty1<T, *>.notEqual(value: Any) = this ne value

    /**
     * ## 超過（含まない）条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    infix fun <T : Entity> KProperty1<T, *>.gt(value: Any) {
        list += Compare.Value(this, ComparisonOperator.GT, value.toConditionValue())
    }

    /**
     * ## 超過（含まない）条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    infix fun <T : Entity> KProperty1<T, *>.greater(value: Any) = this gt value

    /**
     * ## 以上条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    infix fun <T : Entity> KProperty1<T, *>.ge(value: Any) {
        list += Compare.Value(this, ComparisonOperator.GE, value.toConditionValue())
    }

    /**
     * ## 以上条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    infix fun <T : Entity> KProperty1<T, *>.greaterEqual(value: Any) = this ge value

    /**
     * ## 未満条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    infix fun <T : Entity> KProperty1<T, *>.lt(value: Any) {
        list += Compare.Value(this, ComparisonOperator.LT, value.toConditionValue())
    }

    /**
     * ## 未満条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    infix fun <T : Entity> KProperty1<T, *>.less(value: Any) = this lt value

    /**
     * ## 以下条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    infix fun <T : Entity> KProperty1<T, *>.le(value: Any) {
        list += Compare.Value(this, ComparisonOperator.LE, value.toConditionValue())
    }

    /**
     * ## 以下条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    infix fun <T : Entity> KProperty1<T, *>.lessEqual(value: Any) = this le value

    /**
     * ## 包括検索条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    infix fun <T : Entity> KProperty1<T, *>.like(value: Any) {
        list += Compare.Value(this, ComparisonOperator.LIKE, value.toConditionValue())
    }

    /**
     * ## 除外検索条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    infix fun <T : Entity> KProperty1<T, *>.notLike(value: Any) {
        list += Compare.Value(this, ComparisonOperator.NOT_LIKE, value.toConditionValue())
    }

    /**
     * ## 包括パターン検索条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    infix fun <T : Entity> KProperty1<T, *>.glob(value: Any) {
        list += Compare.Value(this, ComparisonOperator.GLOB, value.toConditionValue())
    }

    /**
     * ## 除外パターン検索条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    infix fun <T : Entity> KProperty1<T, *>.notGlob(value: Any) {
        list += Compare.Value(this, ComparisonOperator.NOT_GLOB, value.toConditionValue())
    }

    /**
     * ## メンバーシップ検索条件用関数
     * @receiver 検索条件のカラム
     * @param values 検索値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    infix fun <T : Entity, V> KProperty1<T, V>.inList(values: Collection<V>) {
        list += Compare.Value(this, ComparisonOperator.IN, values.map {
            (it as Any).toConditionValue()
        })
    }

    /**
     * ## 非メンバーシップ検索条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    infix fun <T : Entity, V> KProperty1<T, V>.notInList(value: Collection<V>) {
        list += Compare.Value(this, ComparisonOperator.NOT_IN, value.map {
            (it as Any).toConditionValue()
        })
    }

    /**
     * ## 範囲検索条件用関数
     * @receiver 検索条件のカラム
     * @param start 下限値
     * @author Masahiro Inoue
     * @since 2025-09-19
     */
    infix fun <T : Entity> KProperty1<T, *>.between(start: Any) {
        BetweenBuilder(this, start)
    }

    /**
     * ## 範囲検索条件ビルダークラス
     * @param property 検索条件のカラム
     * @param start 下限値
     * @author Masahiro Inoue
     * @since 2025-09-19
     */
    inner class BetweenBuilder<T : Entity>(
        private val property: KProperty1<T, *>,
        private val start: Any
    ) {
        /**
         * ## 範囲条件生成メソッド
         * ### 定義された条件から文字列を生成する
         * @param end 上限検索値
         * @author Masahiro Inoue
         * @since 2025-09-19
         */
        infix fun and(end: Any) {
            list += Compare.Between(property, start, end)
        }
    }

    /**
     * ## 範囲検索条件用関数
     * @receiver 検索条件のカラム
     * @param pair 下限・上限セットの検索値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    infix fun <T : Entity> KProperty1<T, *>.between(pair: Pair<Any, Any>) {
        list += Compare.Value(this, ComparisonOperator.BETWEEN, pair)
    }

    fun <T : SelectEntity> exists(subQuery: Select<T>) {
        list += Compare.Exists(subQuery)
    }

    /**
     * ## null チェック条件用関数
     * @receiver 検索条件のカラム
     * @param dummy infix 用のダミー引数
     * @author Masahiro Inoue
     * @since 2025-10-03
     */
    infix fun <T : Entity, V> KProperty1<T, V>.isNull(dummy: Unit) {
        list += Compare.IsNull(this)
    }

    /**
     * ## not null チェック条件用関数
     * @receiver 検索条件のカラム
     * @param dummy infix 用のダミー引数
     * @author Masahiro Inoue
     * @since 2025-10-03
     */
    infix fun <T : Entity, V> KProperty1<T, V>.isNotNull(dummy: Unit) {
        list += Compare.IsNotNull(this)
    }

    /**
     * ## null チェック条件用マーカー列挙型
     * @author Masahiro Inoue
     * @since 2025-10-03
     */
    enum class NullMarker { IS_NULL, IS_NOT_NULL }

    /**
     * ## null / not null チェック条件用関数
     * @receiver 検索条件のカラム
     * @param marker null チェックの種類を指定するマーカー
     * @author Masahiro Inoue
     * @since 2025-10-03
     */
    infix fun <T : Entity, V> KProperty1<T, V>.checkForNull(marker: NullMarker) {
        when (marker) {
            NullMarker.IS_NULL -> list += Compare.IsNull(this)
            NullMarker.IS_NOT_NULL -> list += Compare.IsNotNull(this)
        }
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

    /**
     * ## 条件値変換関数
     * @receiver 変換前の値
     * @return 変換後の値
     * @author Masahiro Inoue
     * @since 2025-09-13
     */
    private fun Any.toConditionValue(): Any =
        when (this) {
            is KProperty1<*, *> -> {
                when (this.getter.call(null)) {
                    is ConditionEntity -> ":${this.name}"
                    else -> this
                }
            }

            else -> this
        }
}

