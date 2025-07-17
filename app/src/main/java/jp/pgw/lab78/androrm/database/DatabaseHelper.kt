package jp.pgw.lab78.androrm.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import jp.pgw.lab78.androrm.annotation.Table
import jp.pgw.lab78.androrm.common.SupportFunction.toSnakeCase
import jp.pgw.lab78.androrm.database.interfaces.entity.TableDefinitionEntity
import jp.pgw.lab78.androrm.utility.Functions.generateTableCreationQuery
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * ## AndrORM データベースヘルパークラス
 * ### SQLiteOpenHelper の作業を代行するヘルパークラス
 * @param context アプリケーションコンテキスト（通常は Android framework が生成）
 * @param name データベースファイル名（省略時 app.db）
 * @param version データベース改変バージョン
 * @param entities データベースにテーブルとして配置する Entity クラス
 * @see SQLiteOpenHelper
 */
class DatabaseHelper(
    context: Context,
    val name: String = "app.db",
    version: Int,
    private vararg val entities: KClass<out TableDefinitionEntity>
) : SQLiteOpenHelper(context, name, null, version) {

    /**
     * override
     * スーパークラスの onCreate メソッドをオーバーライドします。
     * 設定された entities を基に Create 文を生成し、テーブルを作成する
     * @see android.database.sqlite.SQLiteDatabase
     */
    override fun onCreate(db: SQLiteDatabase) {
        // 全エンティティに対してテーブル生成クエリを実行
        entities.forEach { entity ->
            val tableQuery = generateTableCreationQuery(entity)
            db.execSQL(tableQuery)
        }
    }

    /**
     * override
     * スーパークラスの onUpgrade メソッドをオーバーライドします。
     * 設定された entities を基に Drop 文を作成し、テーブルを削除する
     * @see android.database.sqlite.SQLiteDatabase
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 各エンティティに対して DROP TABLE 文を実行
        entities.forEach { entity ->
            val tableAnnotation = entity.findAnnotation<Table>()
            val tableName = if (tableAnnotation != null && tableAnnotation.name.isNotBlank())
                tableAnnotation.name
            else
                entity.simpleName?.toSnakeCase() ?: error("Unable to determine table name")
            db.execSQL("DROP TABLE IF EXISTS $tableName")
        }
        onCreate(db)
    }
}