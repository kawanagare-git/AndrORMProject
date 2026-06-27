package jp.pgw.lab78.androrm.database.condition.base

import jp.pgw.lab78.androrm.common.MessageConstants.AE00002
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.database.Select
import jp.pgw.lab78.androrm.database.condition.LogicalConditionDelegate
import jp.pgw.lab78.androrm.database.condition.interfaces.ConditionBuilderLike
import jp.pgw.lab78.androrm.database.condition.interfaces.LogicalConditionSupportLike
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.condition.operator.ComparisonOperator.*
import jp.pgw.lab78.androrm.database.condition.sealed.Compare
import jp.pgw.lab78.androrm.database.condition.sealed.Condition
import jp.pgw.lab78.androrm.database.condition.sealed.FreeText
import jp.pgw.lab78.androrm.database.reference.ColumnRef
import jp.pgw.lab78.androrm.database.utility.EntityManager.formatValue
import jp.pgw.lab78.androrm.database.utility.EntityManager.toColumnString
import kotlin.reflect.KProperty1

abstract class BaseConditionBuilder<B : BaseConditionBuilder<B>>(
    /** バインド変数管理オブジェクト */
    private val valueHolder: QueryWithBindValues
) : ConditionBuilderLike, LogicalConditionSupportLike<B> {
    /** バインド変数管理オブジェクト */
    protected val list = mutableListOf<Condition>()
    private val delegate by LogicalConditionDelegate({ createSelf() }, list)

    /** ## 派生クラスで自分自身を返すファクトリ */
    protected abstract fun createSelf(): B

    // --- DSL演算子群 ---
    /**
     * ## 等価条件用関数
     * @receiver 比較対象プロパティ
     * @param value 比較対象値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.eq(value: Any) =
        list.add(Compare.Value(this.toColumnString(), EQ, formatValue(valueHolder, value)))

    /**
     * ## 等価条件用関数
     * @receiver 比較対象プロパティ
     * @param value 比較対象値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.equal(value: Any) = this.eq(value)

    /**
     * ## 等価条件用関数
     * @receiver 検索条件のカラム参照
     * @param value 検索値
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-12
     */
    infix fun <T : Entity> ColumnRef<out T, *>.eq(value: Any) =
        list.add(Compare.Value(this.build(), EQ, formatValue(valueHolder, value)))

    /**
     * ## 等価条件用関数
     * @receiver 検索条件のカラム参照
     * @param rhs 検索条件のカラム参照（右辺）
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-12
     */
    infix fun <T : Entity> ColumnRef<out T, *>.eq(rhs: ColumnRef<out T, *>) = this.eq(rhs as Any)

    /**
     * ## 等価条件用関数
     * @receiver 検索条件のカラム参照
     * @param rhs 検索条件のカラム参照（右辺）
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-12
     */
    infix fun <T : Entity> ColumnRef<out T, *>.equal(rhs: Any) = this.eq(rhs)

    /**
     * ## 等価条件用関数
     * @receiver 検索条件のカラム参照
     * @param rhs 検索条件のカラム参照（右辺）
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-12
     */
    infix fun <T : Entity> ColumnRef<out T, *>.equal(rhs: ColumnRef<out T, *>) = this.eq(rhs as Any)

    /**
     * ## 不等価条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.ne(value: Any) =
        list.add(Compare.Value(this.toColumnString(), NE, formatValue(valueHolder, value)))

    /**
     * ## 不等価条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.norEqual(value: Any) = this.ne(value)

    /**
     * ## 不等価条件用関数
     * @receiver 検索条件のカラム参照
     * @param value 検索値
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-12
     */
    infix fun <T : Entity> ColumnRef<out T, *>.ne(value: Any) =
        list.add(Compare.Value(this.build(), NE, formatValue(valueHolder, value)))

    /**
     * ## 不等条件用関数
     * @receiver 検索条件のカラム参照
     * @param rhs 検索条件のカラム参照（右辺）
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-12
     */
    infix fun <T : Entity> ColumnRef<out T, *>.ne(rhs: ColumnRef<out T, *>) = this.ne(rhs as Any)

    /**
     * ## 不等価条件用関数
     * @receiver 検索条件のカラム参照
     * @param value 検索値
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-12
     */
    infix fun <T : Entity> ColumnRef<out T, *>.norEqual(value: Any) = this.ne(value)

    /**
     * ## 不等条件用関数
     * @receiver 検索条件のカラム参照
     * @param rhs 検索条件のカラム参照（右辺）
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-12
     */
    infix fun <T : Entity> ColumnRef<out T, *>.norEqual(rhs: ColumnRef<out T, *>) =
        this.ne(rhs as Any)

    /**
     * ## 超過（含まない）条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.gt(value: Any) =
        list.add(Compare.Value(this.toColumnString(), GT, formatValue(valueHolder, value)))

    /**
     * ## 超過（含まない）条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.graterThan(value: Any) = this.gt(value)

    /**
     * ## 超過（含まない）条件用関数
     * @receiver 検索条件のカラム参照
     * @param value 検索値
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity> ColumnRef<out T, *>.gt(value: Any) =
        list.add(Compare.Value(this.build(), GT, formatValue(valueHolder, value)))

    /**
     * ## 超過（含まない）条件用関数
     * @receiver 検索条件のカラム参照
     * @param rhs 検索条件のカラム参照（右辺）
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity> ColumnRef<out T, *>.gt(rhs: ColumnRef<out T, *>) =
        this.gt(rhs as Any)

    /**
     * ## 超過（含まない）条件用関数
     * @receiver 検索条件のカラム参照
     * @param value 検索値
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity> ColumnRef<out T, *>.graterThan(value: Any) = this.gt(value)

    /**
     * ## 超過（含まない）条件用関数
     * @receiver 検索条件のカラム参照
     * @param rhs 検索条件のカラム参照（右辺）
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity> ColumnRef<out T, *>.graterThan(rhs: ColumnRef<out T, *>) =
        this.gt(rhs as Any)

    /**
     * ## 以上条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.ge(value: Any) =
        list.add(Compare.Value(this.toColumnString(), GE, formatValue(valueHolder, value)))

    /**
     * ## 以上条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.graterEqual(value: Any) = this.ge(value)

    /**
     * ## 以上条件用関数
     * @receiver 検索条件のカラム参照
     * @param value 検索値
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity> ColumnRef<out T, *>.ge(value: Any) =
        list.add(Compare.Value(this.build(), GE, formatValue(valueHolder, value)))

    /**
     * ## 以上条件用関数
     * @receiver 検索条件のカラム参照
     * @param rhs 検索条件のカラム参照（右辺）
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity> ColumnRef<out T, *>.ge(rhs: ColumnRef<out T, *>) = this.ge(rhs as Any)

    /**
     * ## 以上条件用関数
     * @receiver 検索条件のカラム参照
     * @param value 検索値
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity> ColumnRef<out T, *>.graterEqual(value: Any) = this.ge(value)

    /**
     * ## 以上条件用関数
     * @receiver 検索条件のカラム参照
     * @param rhs 検索条件のカラム参照（右辺）
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity> ColumnRef<out T, *>.graterEqual(rhs: ColumnRef<out T, *>) =
        this.ge(rhs as Any)

    /**
     * ## 未満条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.lt(value: Any) =
        list.add(Compare.Value(this.toColumnString(), LT, formatValue(valueHolder, value)))

    /**
     * ## 未満条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.lesserThan(value: Any) = this.lt(value)

    /**
     * ## 未満条件用関数
     * @receiver 検索条件のカラム参照
     * @param value 検索値
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity> ColumnRef<out T, *>.lt(value: Any) =
        list.add(Compare.Value(this.build(), LT, formatValue(valueHolder, value)))

    /**
     * ## 未満条件用関数
     * @receiver 検索条件のカラム参照
     * @param rhs 検索条件のカラム参照（右辺）
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity> ColumnRef<out T, *>.lt(rhs: ColumnRef<out T, *>) = this.lt(rhs as Any)

    /**
     * ## 未満条件用関数
     * @receiver 検索条件のカラム参照
     * @param value 検索値
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity> ColumnRef<out T, *>.lesserThan(value: Any) = this.lt(value)

    /**
     * ## 未満条件用関数
     * @receiver 検索条件のカラム参照
     * @param rhs 検索条件のカラム参照（右辺）
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-12
     */
    infix fun <T : Entity> ColumnRef<out T, *>.lesserThan(rhs: ColumnRef<out T, *>) =
        this.lt(rhs as Any)

    /**
     * ## 以下条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.le(value: Any) =
        list.add(Compare.Value(this.toColumnString(), LE, formatValue(valueHolder, value)))

    /**
     * ## 以下条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.lessEqual(value: Any) = this.le(value)

    /**
     * ## 以下条件用関数
     * @receiver 検索条件のカラム参照
     * @param value 検索値
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity> ColumnRef<out T, *>.le(value: Any) =
        list.add(Compare.Value(this.build(), LE, formatValue(valueHolder, value)))

    /**
     * ## 以下条件用関数
     * @receiver 検索条件のカラム参照（左辺）
     * @param rhs 検索条件のカラム参照（右辺）
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity> ColumnRef<out T, *>.le(rhs: ColumnRef<out T, *>) = this.le(rhs as Any)

    /**
     * ## 以下条件用関数
     * @receiver 検索条件のカラム参照
     * @param value 検索値
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity> ColumnRef<out T, *>.lessEqual(value: Any) = this.le(value)

    /**
     * ## 以下条件用関数
     * @receiver 検索条件のカラム参照（左辺）
     * @param rhs 検索条件のカラム参照（右辺）
     * @return 検索条件リスト
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity> ColumnRef<out T, *>.lessEqual(rhs: ColumnRef<out T, *>) =
        this.le(rhs as Any)

    /**
     * ## 包括検索条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.like(value: Any) =
        list.add(Compare.Value(this.toColumnString(), LIKE, formatValue(valueHolder, value)))

    /**
     * ## 包括検索条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity> ColumnRef<out T, *>.like(value: Any) =
        list.add(Compare.Value(this.build(), LIKE, formatValue(valueHolder, value)))

    /**
     * ## 除外検索条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.notLike(value: Any) =
        list.add(Compare.Value(this.toColumnString(), NOT_LIKE, formatValue(valueHolder, value)))

    /**
     * ## 包括検索条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity> ColumnRef<out T, *>.notLike(value: Any) =
        list.add(Compare.Value(this.build(), NOT_LIKE, formatValue(valueHolder, value)))

    /**
     * ## 包括パターン検索条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.glob(value: Any) {
        list += Compare.Value(this.toColumnString(), GLOB, formatValue(valueHolder, value))
    }

    /**
     * ## 包括パターン検索条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity> ColumnRef<out T, *>.glob(value: Any) {
        list += Compare.Value(this.build(), GLOB, formatValue(valueHolder, value))
    }

    /**
     * ## 除外パターン検索条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.notGlob(value: Any) {
        list += Compare.Value(this.toColumnString(), NOT_GLOB, formatValue(valueHolder, value))
    }

    /**
     * ## 除外パターン検索条件用関数
     * @receiver 検索条件のカラム
     * @param value 検索値
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity> ColumnRef<T, *>.notGlob(value: Any) {
        list += Compare.Value(this.build(), NOT_GLOB, formatValue(valueHolder, value))
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
                this.toColumnString(),
                IN,
                values.map { formatValue(valueHolder, it as Any) })
        )

    /**
     * ## メンバーシップ検索条件用関数
     * @receiver 検索条件のカラム
     * @param values 検索値
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity, V> ColumnRef<T, V>.inList(values: Collection<V>) =
        list.add(
            Compare.Value(
                this.build(),
                IN,
                values.map { formatValue(valueHolder, it as Any) })
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
                this.toColumnString(),
                NOT_IN,
                values.map { formatValue(valueHolder, it as Any) })
        )

    /**
     * ## メンバーシップ検索条件用関数
     * @receiver 検索条件のカラム
     * @param values 検索値
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity, V> ColumnRef<T, V>.notInList(values: Collection<V>) =
        list.add(
            Compare.Value(
                this.build(),
                NOT_IN,
                values.map { formatValue(valueHolder, it as Any) })
        )

    /**
     * ## メンバーシップ検索条件用関数
     * @receiver 検索条件のカラム
     * @param subQuery 検索値抽出クエリ
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity, V> KProperty1<T, V>.inSelect(subQuery: Select<out SelectEntity>) {
        valueHolder.addBindValues(subQuery.bindValues)
        list.add(Compare.InSelect(this.toColumnString(), subQuery))
    }

    /**
     * ## メンバーシップ検索条件用関数
     * @receiver 検索条件のカラム
     * @param subQuery 検索値抽出クエリ
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity, V> ColumnRef<T, V>.inSelect(subQuery: Select<out SelectEntity>) {
        valueHolder.addBindValues(subQuery.bindValues)
        list.add(Compare.InSelect(this.build(), subQuery))
    }

    /**
     * ## 非メンバーシップ検索条件用関数
     * @receiver 検索条件のカラム
     * @param subQuery 検索値抽出クエリ
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity, V> KProperty1<T, V>.notInSelect(subQuery: Select<out SelectEntity>) {
        valueHolder.addBindValues(subQuery.bindValues)
        list.add(Compare.NotInSelect(this.toColumnString(), subQuery))
    }

    /**
     * ## 非メンバーシップ検索条件用関数
     * @receiver 検索条件のカラム
     * @param subQuery 検索値抽出クエリ
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity, V> ColumnRef<T, V>.notInSelect(subQuery: Select<out SelectEntity>) {
        valueHolder.addBindValues(subQuery.bindValues)
        list.add(Compare.NotInSelect(this.build(), subQuery))
    }

    /**
     * ## 範囲検索条件用関数
     * @receiver 検索条件のカラム
     * @param start 下限値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> KProperty1<T, *>.between(start: Any): BetweenBuilder<T> {
        return BetweenBuilder(this, formatValue(valueHolder, start))
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
            list += Compare.Between(property.toColumnString(), start, formatValue(valueHolder, end))
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
        list.add(
            Compare.Between(
                this.toColumnString(),
                formatValue(valueHolder, pair.first),
                formatValue(valueHolder, pair.second)
            )
        )

    /**
     * ## 範囲検索条件用関数
     * @receiver 検索条件のカラム
     * @param start 下限値
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity> ColumnRef<out T, *>.between(start: Any): ColumnRefBetweenBuilder<T> {
        return ColumnRefBetweenBuilder(this, formatValue(valueHolder, start))
    }

    /**
     * ## 範囲検索条件ビルダークラス
     * @param column 検索条件のカラム
     * @param start 下限値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    inner class ColumnRefBetweenBuilder<T : Entity>(
        private val column: ColumnRef<out T, *>,
        private val start: Any,
    ) {
        infix fun and(end: Any) {
            list += Compare.Between(column.build(), start, formatValue(valueHolder, end))
        }
    }

    /**
     * ## 範囲検索条件用関数
     * @receiver 検索条件のカラム
     * @param pair 下限・上限セットの検索値
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    infix fun <T : Entity> ColumnRef<out T, *>.between(pair: Pair<Any, Any>) =
        list.add(
            Compare.Between(
                this.build(),
                formatValue(valueHolder, pair.first),
                formatValue(valueHolder, pair.second)
            )
        )

    /**
     * ## サブクエリ存在条件用関数（EXISTS）
     * @param subQuery サブクエリ
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    fun <T : SelectEntity> exists(subQuery: Select<T>) {
        valueHolder.addBindValues(subQuery.bindValues)
        list += Compare.Exists(subQuery)
    }

    /**
     * ## サブクエリ存在条件用関数（NOT EXISTS）
     * @param subQuery サブクエリ
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    fun <T : SelectEntity> notExists(subQuery: Select<T>) {
        valueHolder.addBindValues(subQuery.bindValues)
        list += Compare.NotExists(subQuery)
    }

    /**
     * ## null チェック条件用関数
     * @receiver 検索条件のカラム
     * @param dummy infix 用のダミー引数
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    @Suppress("UNUSED_PARAMETER")
    infix fun <T : Entity, V> KProperty1<T, V>.isNull(dummy: Unit) =
        list.add(Compare.IsNull(this.toColumnString()))

    /**
     * ## not null チェック条件用関数
     * @receiver 検索条件のカラム
     * @param dummy infix 用のダミー引数
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    @Suppress("UNUSED_PARAMETER")
    infix fun <T : Entity, V> KProperty1<T, V>.isNotNull(dummy: Unit) =
        list.add(Compare.IsNotNull(this.toColumnString()))

    /**
     * ## null チェック条件用関数
     * @receiver 検索条件のカラム
     * @param dummy infix 用のダミー引数
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    @Suppress("UNUSED_PARAMETER")
    infix fun <T : Entity> ColumnRef<out T, *>.isNull(dummy: Unit) =
        list.add(Compare.IsNull(this.build()))

    /**
     * ## not null チェック条件用関数
     * @receiver 検索条件のカラム
     * @param dummy infix 用のダミー引数
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    @Suppress("UNUSED_PARAMETER")
    infix fun <T : Entity> ColumnRef<out T, *>.isNotNull(dummy: Unit) =
        list.add(Compare.IsNotNull(this.build()))

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
        val column = this.toColumnString()
        list += when (marker) {
            NullMarker.IS_NULL -> Compare.IsNull(column)
            NullMarker.IS_NOT_NULL -> Compare.IsNotNull(column)
        }
    }

    /**
     * ## null / not null チェック条件用関数
     * @receiver 検索条件のカラム
     * @param marker null チェックの種類を指定するマーカー
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    infix fun <T : Entity> ColumnRef<out T, *>.checkForNull(marker: NullMarker) {
        val column = this.build()
        list += when (marker) {
            NullMarker.IS_NULL -> Compare.IsNull(column)
            NullMarker.IS_NOT_NULL -> Compare.IsNotNull(column)
        }
    }

    /**
     * ## 単一条件用関数
     * ### 条件式を自由記述するための関数
     * @param text 検索条件の自由記述
     * @param values プレースホルダに対応するバインド値
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    fun condition(text: String, vararg values: Any) {
        // プレースホルダとバインド値の個数を検査
        val placeholderCount = text.count { it == '?' }
        // プレースホルダの個数とバインド値の個数が一致しない場合は例外をスロー
        require(placeholderCount == values.size) {
            AE00002.format(text, placeholderCount, values.size)
        }
        // 条件式をリストに追加し、バインド値を管理オブジェクトに登録
        list.add(FreeText(text))
        values.forEach { value -> valueHolder.addBindValue(value) }
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
}
