package jp.pgw.lab78.androrm.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import jp.pgw.lab78.androrm.annotation.Table
import jp.pgw.lab78.androrm.database.SupportFunction.generateTableCreationQuery
import jp.pgw.lab78.androrm.database.definitions.Entity
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
class DatabaseHelper(context: Context,val name : String = "app.db",
                     version: Int, private vararg val entities: KClass<out Entity>)
        : SQLiteOpenHelper(context, name, null, version) {

    override fun onCreate(db: SQLiteDatabase) {
        entities.filter { entity ->
            // @Table アノテーションが付与されているかを判定
            entity.findAnnotation<Table>() != null
        }.forEach { entity ->
            val tableQuery = generateTableCreationQuery(entity)
            db.execSQL(tableQuery)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        entities.filter { entity ->
            // @Table アノテーションが付与されているかを判定
            entity.findAnnotation<Table>() != null
        }.forEach { entity ->
            val tableName = entity.findAnnotation<Table>()!!.name
            db.execSQL("DROP TABLE IF EXISTS $tableName")
        }
        onCreate(db)
    }
}
