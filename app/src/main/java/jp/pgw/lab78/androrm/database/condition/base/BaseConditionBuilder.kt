package jp.pgw.lab78.androrm.database.condition.base

import jp.pgw.lab78.androrm.common.dml.interfaces.ConditionEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.database.Select
import jp.pgw.lab78.androrm.database.condition.LogicalConditionDelegate
import jp.pgw.lab78.androrm.database.condition.interfaces.ConditionBuilderLike
import jp.pgw.lab78.androrm.database.condition.interfaces.LogicalConditionSupportLike
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator
import jp.pgw.lab78.androrm.database.condition.sealed.Compare
import jp.pgw.lab78.androrm.database.condition.sealed.Condition
import jp.pgw.lab78.androrm.database.condition.sealed.FreeText
import kotlin.reflect.KProperty1

abstract class BaseConditionBuilder<B : BaseConditionBuilder<B>> :
    ConditionBuilderLike, LogicalConditionSupportLike<B> {

    protected val list = mutableListOf<Condition>()
    private val delegate by LogicalConditionDelegate({ createSelf() }, list)

    /** ## 派生クラスで自分自身を返すファクトリ */
    protected abstract fun createSelf(): B

    // --- DSL演算子群 ---
    /**
     * ## 等価比較
     * @receiver 比較対象プロパティ
     * @param value 比較対象値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.eq(value: Any) =
        list.add(Compare.Value(this, ComparisonOperator.EQ, value.toConditionValue()))

    /**
     * ## 等価比較
     * @receiver 比較対象プロパティ
     * @param value 比較対象値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.equal(value: Any) = this.eq(value)

    /**
     * ## 不等価条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.ne(value: Any) =
        list.add(Compare.Value(this, ComparisonOperator.NE, value.toConditionValue()))

    /**
     * ## 不等価条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.norEqual(value: Any) = this.ne(value)

    /**
     * ## 超過（含まない）条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.gt(value: Any) =
        list.add(Compare.Value(this, ComparisonOperator.GT, value.toConditionValue()))

    /**
     * ## 超過（含まない）条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.graterThan(value: Any) = this.gt(value)

    /**
     * ## 以上条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.ge(value: Any) =
        list.add(Compare.Value(this, ComparisonOperator.GE, value.toConditionValue()))

    /**
     * ## 以上条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.graterEqual(value: Any) = this.ge(value)

    /**
     * ## 未満条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.lt(value: Any) =
        list.add(Compare.Value(this, ComparisonOperator.LT, value.toConditionValue()))

    /**
     * ## 未満条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.lesserThan(value: Any) = this.lt(value)

    /**
     * ## 以下条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.le(value: Any) =
        list.add(Compare.Value(this, ComparisonOperator.LE, value.toConditionValue()))

    /**
     * ## 以下条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.lessEqual(value: Any) = this.le(value)

    /**
     * ## 包括検索条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.like(value: Any) =
        list.add(Compare.Value(this, ComparisonOperator.LIKE, value.toConditionValue()))

    /**
     * ## 除外検索条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.notLike(value: Any) =
        list.add(Compare.Value(this, ComparisonOperator.NOT_LIKE, value.toConditionValue()))

    /**
     * ## 包括パターン検索条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.glob(value: Any) {
        list += Compare.Value(this, ComparisonOperator.GLOB, value.toConditionValue())
    }

    /**
     * ## 除外パターン検索条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.notGlob(value: Any) {
        list += Compare.Value(this, ComparisonOperator.NOT_GLOB, value.toConditionValue())
    }

    /**
     * ## メンバーシップ検索条件用関数
     * @receiver 検索条件のカラム
     * @param values 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity, V> KProperty1<T, V>.inList(values: Collection<V>) =
        list.add(
            Compare.Value(
                this,
                ComparisonOperator.IN,
                values.map { (it as Any).toConditionValue() })
        )

    /**
     * ## 非メンバーシップ検索条件用関数
     * @receiver 検索条件のカラム
     * @param values 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity, V> KProperty1<T, V>.notInList(values: Collection<V>) =
        list.add(
            Compare.Value(
                this,
                ComparisonOperator.NOT_IN,
                values.map { (it as Any).toConditionValue() })
        )

    /**
     * ## 範囲検索条件用関数
     * @receiver 検索条件のカラム
     * @param start 下限値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.between(start: Any) {
        BetweenBuilder(this, start)
    }

    /**
     * ## 範囲検索条件ビルダークラス
     * @param property 検索条件のカラム
     * @param start 下限値
     * @author Masahiro Inoue
     * @since 2025-10-19
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
         * @since 2025-10-19
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
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.between(pair: Pair<Any, Any>) =
        list.add(Compare.Value(this, ComparisonOperator.BETWEEN, pair))

    /**
     * ## サブクエリ存在条件用関数
     * @param subQuery サブクエリ
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    fun <T : SelectEntity> exists(subQuery: Select<T>) {
        list += Compare.Exists(subQuery)
    }

    /**
     * ## null チェック条件用関数
     * @receiver 検索条件のカラム
     * @param dummy infix 用のダミー引数
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity, V> KProperty1<T, V>.isNull(dummy: Unit) =
        list.add(Compare.IsNull(this))

    /**
     * ## not null チェック条件用関数
     * @receiver 検索条件のカラム
     * @param dummy infix 用のダミー引数
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity, V> KProperty1<T, V>.isNotNull(dummy: Unit) =
        list.add(Compare.IsNotNull(this))

    /**
     * ## null チェック条件用マーカー列挙型
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    enum class NullMarker { IS_NULL, IS_NOT_NULL }

    /**
     * ## null / not null チェック条件用関数
     * @receiver 検索条件のカラム
     * @param marker null チェックの種類を指定するマーカー
     * @author Masahiro Inoue
     * @since 2025-10-19
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
     * @since 2025-10-19
     */
    fun condition(text: String) {
        list.add(FreeText(text))
    }

    /**
     * ## 条件リスト生成メソッド
     * @return 生成された条件
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    override fun buildList(): List<Condition> = list

    /**
     * ## 論理積メソッド
     * @param block 検索条件の記述
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    override fun and(block: B.() -> Unit) = delegate.and(block)

    /**
     * ## 論理和メソッド
     * @param block 検索条件の記述
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    override fun or(block: B.() -> Unit) = delegate.or(block)

    /**
     * ##プレースホルダ変換（共通ロジック）
     * ### ConditionEntity の場合はプレースホルダ文字列に変換する
     * @receiver 任意のオブジェクト
     * @return ConditionEntityの場合はプレースホルダ文字列、そうでない場合はそのままのオブジェクト
     * @author Masahiro Inoue
     * @since 2025-10-21
     */
    private fun Any.toConditionValue(): Any = when (this) {
        is ConditionEntity -> ":${this::class.simpleName}"
        else -> this
    }
}
