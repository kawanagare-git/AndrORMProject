package jp.pgw.lab78.androrm.database.function.interfaces

import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import kotlin.reflect.KProperty1

/**
 * ## 関数生成インターフェス
 * ### 関数定義クラスに付与する生成インターフェス
 * @author Masahiro Inoue
 * @since 2025-08-08
 */
interface FunctionBuilderLike {
    /**
     * ## 生成メソッド
     * ### 関数文字列を生成するときに呼び出す
     * @return 生成されたクエリ文字列
     * @author Masahiro Inoue
     * @since 2025-08-08
     */
    fun <T : Entity>build(column: KProperty1<T, *>, isDistinct: Boolean = false): String
}