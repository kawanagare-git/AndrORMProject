package jp.pgw.lab78.androrm.database.condition.interfaces

import jp.pgw.lab78.androrm.common.dml.interfaces.BindValuesEntity
import kotlin.reflect.KClass

/**
 * ## バインド値条件生成インターフェース
 * ### バインド値を使用する条件生成のためのインターフェース
 * @author Masahiro Inoue
 * @since 2026-01-12
 */
interface QueryWithBindValues<T : BindValuesEntity> : QueryStructureLike {
    /** バインド変数リスト */
    val bindValues: MutableList<Any>

    /**
     * ## バインド変数定義メソッド
     * ### 定義されたバインド値からプレースホルダに設定する
     * @param bindValuesEntity バインド値エンティティのクラス型
     * @return 実装した型を返す
     * @author Masahiro Inoue
     * @since 2026-01-12
     */
    fun bindValue(bindValuesEntity: KClass<out T>): QueryWithBindValues<T>
}