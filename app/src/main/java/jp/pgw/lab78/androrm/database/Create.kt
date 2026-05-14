package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableName
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.database.interfaces.QueryBuilderLike
import jp.pgw.lab78.androrm.database.utility.EntityManager.getColumnDefinitions
import kotlin.reflect.KClass

/**
 * ## Create 文生成クラス
 * ### クエリ生成クラスに付与する生成インターフェス
 * @param T [TableDefinitionEntity] インターフェースを実装するクラスの型。
 * @param entityClass TableDefinitionEntity クラスのインスタンスを指定
 * @author Masahiro Inoue
 * @since 2025-08-08
 */
class Create<T : TableDefinitionEntity>(
    private val entityClass: KClass<out T>
) : QueryBuilderLike<Any?> {
    /**
     * ## CREATE 文文字列生成関数
     * ### テーブルを作成するクエリを生成する
     * @author Masahiro Inoue
     * @since 2025-08-08
     */
    override fun build(): String {
        /** テーブル名の生成 */
        val tableName = entityClass.getTableName()

        /** カラム定義の生成 */
        val columnList = entityClass.getColumnDefinitions()
        return "CREATE TABLE $tableName (${
            columnList.joinToString(", ") { (columnName, sqlType) ->
                "$columnName $sqlType"
            }
        })"
    }
}