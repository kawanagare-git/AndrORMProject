package jp.pgw.lab78.androrm.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import jp.pgw.lab78.androrm.annotation.Table
import jp.pgw.lab78.androrm.database.SupportFunction.generateTableCreationQuery
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

class DatabaseHelper(context: Context, private vararg val entities: KClass<*>) : SQLiteOpenHelper(context, "app.db", null, 1) {

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
