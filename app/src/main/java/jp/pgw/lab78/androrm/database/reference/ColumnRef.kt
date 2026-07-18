package jp.pgw.lab78.androrm.database.reference

import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumnName
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryStructureLike
import kotlin.reflect.KProperty1

/**
 * ## カラム参照
 * ### SQL 上の alias.column を表す
 *
 * ### 仕様
 * #### テーブル参照と型付きプロパティを保持し、SQL ではテーブル別名付きのカラム名として構築する。
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
        "${tableRef.alias}.${tableRef.entityClass.getColumnName(property.name)}"
}
