package jp.pgw.lab78.androrm.database.reference

import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * ## テーブル参照
 * ### SQL 上のテーブルエイリアスを再定義する
 *
 * ### 仕様
 * #### Entity 型と SQL 上の別名を一組で保持し、指定プロパティを同じテーブルに属する `ColumnRef` へ変換する。
 * @param entityClass 再定義対象のエンティティ
 * @param alias 再定義するテーブルエイリアス
 * @author Masahiro Inoue
 * @since 2026-05-12
 */
data class TableRef<E : Entity>(
    val entityClass: KClass<out E>,
    val alias: String,
) {
    /**
     * ## カラム参照生成
     * ### tableRef[property] の形式で alias 付きカラム参照を生成する
     * @return 取得したカラム参照
     * @author Masahiro Inoue
     * @since 2026-05-12
     */
    operator fun <V> get(property: KProperty1<E, V>): ColumnRef<E, V> =
        column(property)

    /**
     * ## カラム参照生成
     * ### column(property) の形式で alias 付きカラム参照を生成する
     * @return 生成されたカラム参照
     * @author Masahiro Inoue
     * @since 2026-05-12
     */
    fun <V> column(property: KProperty1<E, V>): ColumnRef<E, V> =
        ColumnRef(this, property)
}
