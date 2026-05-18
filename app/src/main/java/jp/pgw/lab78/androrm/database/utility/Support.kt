package jp.pgw.lab78.androrm.database.utility

import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.database.reference.TableRef
import kotlin.reflect.KClass

object Support {
    /**
     * ## テーブル参照生成
     * ### SQL 上で使用する table alias を明示して TableRef を生成する
     * @param entityClass エンティティ参照用のクラス
     * @param alias テーブルエイリアス
     * @return 生成されたテーブル参照
     * @author Masahiro Inoue
     * @since 2026-05-12
     */
    fun <E : Entity> tableRef(entityClass: KClass<E>, alias: String): TableRef<E> =
        TableRef(entityClass, alias)
}
