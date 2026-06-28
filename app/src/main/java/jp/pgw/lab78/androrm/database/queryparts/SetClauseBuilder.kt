package jp.pgw.lab78.androrm.database.queryparts

import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumnName
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.common.dml.interfaces.UpsertEntity
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.interfaces.SqlExpression
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * ## SET 句ビルダー
 * ### Update / Upsert の SET 句を構築する
 * @param targetEntityClass
 * @param valueHolder
 * @param valueFormatter
 * @author Masahiro Inoue
 * @since 2026-06-27
 */
open class SetClauseBuilder<T : Entity> internal constructor(
    protected val targetEntityClass: KClass<out T>,
    private val valueHolder: QueryWithBindValues,
    private val valueFormatter: (QueryWithBindValues, Any) -> String,
) {
    /** SET 句リスト */
    private val assignments = mutableListOf<Pair<String, String>>()

    /**
     * ## becomes メソッド
     * ### 左辺プロパティに右辺値を設定する
     * @receiver プロパティ
     * @param value 右辺式
     * @author Masahiro Inoue
     * @since 2026-06-27
     */
    infix fun KProperty1<T, *>.becomes(value: Any?) {
        assignments += targetEntityClass.getColumnName(this.name) to formatSetValue(value)
    }

    /**
     * ## assign メソッド
     * ### becomes の別名
     */
    infix fun KProperty1<T, *>.assign(value: Any?) {
        this becomes value
    }

    /**
     * ## SET 句リスト生成
     */
    fun buildList(): List<Pair<String, String>> = assignments

    /**
     * ## SET 値式生成
     */
    private fun formatSetValue(value: Any?): String {
        if (value == null) {
            valueHolder.addBindValue(null)
            return "?"
        }

        return valueFormatter(valueHolder, value)
    }
}

/**
 * ## Upsert 用 SET 句ビルダー
 * ### excluded 参照と既存行カラム参照を追加する
 */
class UpsertSetClauseBuilder<T : UpsertEntity> internal constructor(
    targetEntityClass: KClass<out T>,
    valueHolder: QueryWithBindValues,
    valueFormatter: (QueryWithBindValues, Any) -> String,
) : SetClauseBuilder<T>(
    targetEntityClass = targetEntityClass,
    valueHolder = valueHolder,
    valueFormatter = valueFormatter,
) {
    /**
     * ## excluded カラム参照
     * ### insert しようとした値を参照する
     */
    fun excluded(property: KProperty1<T, *>): SqlExpression =
        ExcludedColumnExpression(targetEntityClass, property)

    /**
     * ## 既存行カラム参照
     * ### 衝突した既存行の値を参照する
     */
    fun column(property: KProperty1<T, *>): SqlExpression =
        UpsertTargetColumnExpression(targetEntityClass, property)
}

/**
 * ## excluded カラム式
 */
private data class ExcludedColumnExpression<T : Entity>(
    private val entityClass: KClass<out T>,
    private val property: KProperty1<T, *>,
) : SqlExpression {
    override fun build(valueHolder: QueryWithBindValues): String =
        "excluded.${entityClass.getColumnName(property.name)}"
}

/**
 * ## Upsert 対象カラム式
 */
private data class UpsertTargetColumnExpression<T : Entity>(
    private val entityClass: KClass<out T>,
    private val property: KProperty1<T, *>,
) : SqlExpression {
    override fun build(valueHolder: QueryWithBindValues): String =
        entityClass.getColumnName(property.name)
}