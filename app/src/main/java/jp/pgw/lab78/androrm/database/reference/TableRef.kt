package jp.pgw.lab78.androrm.database.reference

import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumn
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryStructureLike
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * ## テーブル参照
 * ### SQL 上のテーブルエイリアスを再定義する
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

/**
 * ## カラム参照
 * ### SQL 上の alias.column を表す
 * @param tableRef カラム参照を生成するテーブル参照
 * @param property カラム参照を生成するプロパティ
 * @author Masahiro Inoue
 * @since 2026-05-12
 */
data class ColumnRef<E : Entity, V>(
    val tableRef: TableRef<E>,
    val property: KProperty1<E, V>,
) : QueryStructureLike {
    /**
     * ## 生成メソッド
     * ### エイリアスを再定義したテーブル参照用文字列の生成
     * @return 生成されたテーブル参照用文字列
     * @author Masahiro Inoue
     * @since 2026-05-12
     */
    override fun build(): String =
        "${tableRef.alias}.${property.getColumn()}"
}

/**
 * ## テーブル参照生成
 * ### SQL 上で使用する table alias を明示して TableRef を生成する
 * @param entityClass エンティティ参照用のクラス
 * @param alias テーブルエイリアス
 * @return 生成されたテーブル参照
 * @author Masahiro Inoue
 * @since 2026-05-12
 */
fun <E : SelectEntity> table(entityClass: KClass<E>, alias: String): TableRef<E> =
    TableRef(entityClass, alias)