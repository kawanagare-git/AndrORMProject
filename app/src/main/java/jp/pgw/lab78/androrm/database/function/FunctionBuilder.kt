package jp.pgw.lab78.androrm.database.function

import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumn
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.database.function.interfaces.FunctionBuilderLike
import jp.pgw.lab78.androrm.database.utility.EntityManager.extractClassFromProperty
import jp.pgw.lab78.androrm.database.utility.EntityManager.getAlias
import kotlin.reflect.KProperty1

/**
 * ## 関数生成クラス
 * ### 関数定義クラスに付与する生成クラス
 * @author Masahiro Inoue
 * @since 2025-08-08
 */
class FunctionBuilder(val sql: String) : FunctionBuilderLike {
    /**
     * ## 生成メソッド
     * ### 関数文字列を生成するときに呼び出す
     * @return 生成されたクエリ文字列
     * @author Masahiro Inoue
     * @since 2025-08-08
     */
    override fun <T : Entity>build(column: KProperty1<T, *>, isDistinct: Boolean): String {
        val distinct = if (isDistinct) "DISTINCT " else ""
        val alias = getAlias(column.extractClassFromProperty())
        return "${this.sql}($distinct${alias}.${column.getColumn()})"
    }
}