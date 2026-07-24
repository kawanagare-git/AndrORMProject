package jp.pgw.lab78.androrm.database.function

import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumnName
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.database.function.interfaces.FunctionBuilderLike
import jp.pgw.lab78.androrm.database.utility.EntityManager.extractClassFromProperty
import jp.pgw.lab78.androrm.database.utility.EntityManager.getAlias
import kotlin.reflect.KProperty1

/**
 * ## 関数生成クラス
 * ### 関数定義クラスに付与する生成クラス
 *
 * ### 仕様
 * #### Entity プロパティをテーブル別名付きカラムへ変換し、必要に応じて `DISTINCT` を付けて関数呼び出しを生成する。
 * @param query クエリに使用される文字列
 * @author Masahiro Inoue
 * @since 2025-08-08
 */
class FunctionBuilder(private val query: String) : FunctionBuilderLike {
    /**
     * ## 生成メソッド
     * ### 関数文字列を生成するときに呼び出す
     * @return 生成されたクエリ文字列
     * @author Masahiro Inoue
     * @since 2025-08-08
     */
    override fun <T : Entity> build(column: KProperty1<T, *>, isDistinct: Boolean): String {
        val distinct = if (isDistinct) "DISTINCT " else ""
        val alias = column.extractClassFromProperty().getAlias()
        return "${this.query}($distinct${alias}.${column.getColumnName()})"
    }
}