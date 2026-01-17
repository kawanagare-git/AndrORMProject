package jp.pgw.lab78.androrm.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import jp.pgw.lab78.androrm.common.database.SupportFunction.toSnakeCase
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.database.utility.EntityManager.convertToEntity
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation

/**
 * ## AndrORM データベースヘルパークラス
 * ### SQLiteOpenHelper の作業を代行するヘルパークラス
 * @param context アプリケーションコンテキスト（通常は Android framework が生成）
 * @param name データベースファイル名（省略時 app.db）
 * @param version データベース改変バージョン
 * @param entities データベースにテーブルとして配置する Entity クラス
 * @author Masahiro Inoue
 * @since 2025-08-01
 * @see SQLiteOpenHelper
 */
class AndrOrmDatabaseHelper(
    context: Context,
    val name: String = "app.db",
    version: Int,
    private vararg val entities: KClass<out TableDefinitionEntity>
) : SQLiteOpenHelper(context, name, null, version) {

    /**
     * ## AndrORM データベーステーブル生成メソッド
     * ### スーパークラスの onCreate メソッドをオーバーライドします。
     * ### 設定された entities を基に Create 文を生成し、テーブルを作成する
     * @author Masahiro Inoue
     * @since 2025-08-08
     * @see android.database.sqlite.SQLiteDatabase
     */
    override fun onCreate(db: SQLiteDatabase) {
        // 全エンティティに対してテーブル生成クエリを実行
        entities.forEach { entity ->
            val tableQuery = Create(entity).build()
            db.execSQL(tableQuery)
        }
    }

    /**
     * ## AndrORM データベースアップグレードメソッド
     * ### スーパークラスの onUpgrade メソッドをオーバーライドします。
     * ### 設定された entities を基に Drop 文を作成し、テーブルを削除する
     * @author Masahiro Inoue
     * @since 2025-08-08
     * @see android.database.sqlite.SQLiteDatabase
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 各エンティティに対して DROP TABLE 文を実行
        entities.forEach { entity ->
            val tableAnnotation = entity.findAnnotation<Table>()
            val tableName = if (tableAnnotation != null && tableAnnotation.name.isNotBlank()) {
                tableAnnotation.name
            } else {
                entity.simpleName?.toSnakeCase() ?: error("Unable to determine table name")
            }
            db.execSQL("DROP TABLE IF EXISTS $tableName")
        }
        onCreate(db)
    }

    /**
     * ## AndrORM データベースヘルパ取得メソッド
     * @return AndrORM データベースヘルパを返す
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun getDatabase(): SQLiteOpenHelper = this

    /**
     * ## AndrORM データベースヘルパ取得メソッド（移譲用）
     * ### AndrORM データベースヘルパークラスのインスタンスを移譲元に渡す
     * @param thisRef オーナーオブジェクト：システムで設定
     * @param property プロパティ情報：システムで設定
     * @return AndrORM データベースヘルパを返す
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = this

    /**
     * ## AndrORM データベース実行メソッド
     * ### 引数で渡されたクエリを実行する（select 文用）
     * @param query 実行する Select クラスのインスタンス
     * @param entities 条件エンティティクラスのインスタンス
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    inline fun <reified T : SelectEntity, reified> queryList(
        query: Select<out T>,
    ): List<T> {
        // select の実行（values.firstOrNull() は、values が空の時は null を返す）
        val cursor =
            readableDatabase.rawQuery(
                query.build(),
                query.bindValues.map { it.toString() }.toTypedArray()
            )
        val result = mutableListOf<T>()
        cursor.use {
            while (it.moveToFirst()) {
                val propertiesMap = convertToEntity(it.columnNames, T::class)
                result += T::class.constructors.first().callBy( /* Cursor から map */ emptyMap())
            }
        }
        return result
    }

//    fun <T : InsertEntity> execInsert(entity: T, vararg entities: T) {
//        val allEntities = listOf(entity) + entities
//
//        val insertResult = Insert.build(allEntities) // insertResult.query: String, insertResult.binds: List<Map<String, Any>>
//
//        writableDatabase.beginTransaction()
//        try {
//            val stmt = writableDatabase.compileStatement(insertResult.query)
//            for (bindMap in insertResult.binds) {
//                bindMap.entries.forEachIndexed { index, (_, value) ->
//                    stmt.bindObject(index + 1, value)
//                }
//                stmt.executeInsert()
//                stmt.clearBindings()
//            }
//            writableDatabase.setTransactionSuccessful()
//        } finally {
//            writableDatabase.endTransaction()
//        }
//    }

    data class InsertResult(
        val query: String,  // 例: INSERT INTO EMPLOYEE (ID, NAME) VALUES (?, ?)
        val binds: List<Map<String, Any>> // 各行に対する値マップ
    )
}