package jp.pgw.lab78.androrm.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import jp.pgw.lab78.androrm.common.Constants.NEW_TABLE_SUFFIX
import jp.pgw.lab78.androrm.common.MessageConstants.AE00021
import jp.pgw.lab78.androrm.common.MessageConstants.AE00022
import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumn
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableName
import jp.pgw.lab78.androrm.common.database.annotation.ColumnOldName
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.interfaces.QueryBuilderLike
import jp.pgw.lab78.androrm.database.utility.EntityManager.fieldToColumnMap
import jp.pgw.lab78.androrm.database.utility.EntityManager.getConstructorOrderedProperties
import jp.pgw.lab78.shared.library.Utils.isNull
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * ## AndrORM データベースヘルパークラス
 * ### AndrORM とデータベースの接続。各クエリの実行
 * @param context android システムのコンテキスト
 * @param databaseName データベース名
 * @param version データベースのバージョン
 * @param entities 生成するテーブル
 * @author Masahiro Inoue
 * @since 2025-08-08
 */
open class AndrOrmDatabaseHelper(
    context: Context,
    databaseName: String = "app.db",
    version: Int,
    private vararg val entities: KClass<out TableDefinitionEntity>,
) : SQLiteOpenHelper(context, databaseName, null, version) {
    /**
     * ## 標準カラムマッピング保持領域
     * ### onUpgrade 中に作成した標準カラムマッピングを resolveColumnMappings() から参照する
     */
    private lateinit var preparedColumnMappings: MutableMap<KClass<out TableDefinitionEntity>, List<Pair<String, String>>>

    /**
     * ## データベース生成
     * ### データベースの生成と所属するテーブルを作成
     * ### データベースが新規作成される時だけ作成される
     * @param db SQLite データベース
     * @author Masahiro Inoue
     * @since 2025-08-08
     */
    override fun onCreate(db: SQLiteDatabase) {
        val createTableList = entities.map { entity ->
            val create = Create(entity)
            create.build() to create.buildIndexQueries(0)
        }
        // テーブル作成
        createTableList.forEach { (createTableQuery, createIndexQueries) ->
            db.execSQL(createTableQuery)
            // テーブル毎のインデックス作成
            createIndexQueries.forEach { createIndexQuery ->
                db.execSQL(createIndexQuery)
            }
        }
    }

    /**
     * ## データベース更新
     * ### データベースバージョンを比較してテーブル追加・変更を実施
     * @param db SQLite データベース
     * @param oldVersion 適用前データベースバージョン
     * @param newVersion 適用後データベースバージョン
     * @author Masahiro Inoue
     * @since 2025-08-08
     */
    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int,
    ) {
        migrateDatabase(db, newVersion)
    }

    /**
     * ## DB 移行処理
     * ### 標準では同名カラムの値を引き継ぐ
     * @param db SQLite データベース
     * @param newVersion 適用後データベースバージョン
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    protected open fun migrateDatabase(
        db: SQLiteDatabase,
        newVersion: Int = 0,
    ) {
        // 新旧テーブル名を作成
        val tableNames =
            entities.associateWith { entity -> entity.getTableName() to "${entity.getTableName()}$NEW_TABLE_SUFFIX" }
        // 新旧テーブルカラムのマッピング
        preparedColumnMappings = entities.associateWith { entity ->
            entity.getConstructorOrderedProperties()
                .map { property ->
                    val newColumn = property.getColumn()
                    val oldColumn =
                        property.findAnnotation<ColumnOldName>()?.name?.takeIf { it.isNotBlank() }
                            ?: newColumn
                    oldColumn to newColumn
                }
        }.toMutableMap()
        // 新旧テーブルカラムのマッピングを更新
        customColumnMappingsUpdate()
        // 新テーブルの create 文生成
        val createNewTableList = tableNames.map { (key, tableNamePair) ->
            val create = Create(key, tableNamePair.second)
            create.build() to create.buildIndexQueries(newVersion)
        }
        // データ転送用クエリ生成
        val insertSelectQueryList = entities.map { entity ->
            Insert.intoTableColumns(
                tableNames.getValue(entity).second,
                preparedColumnMappings.getValue(entity).map { it.second },
                Select.tableColumns(
                    tableNames.getValue(entity).first,
                    preparedColumnMappings.getValue(entity).map { it.first })
            )
        }
        // drop 文生成
        val dropOldTableQueryList = entities.map { entity ->
            "drop table if exists ${tableNames.getValue(entity).first}"
        }
        // alter 文生成(テーブル名変更)
        val renameTableQueryList = entities.map { entity ->
            "alter table ${
                tableNames.getValue(entity).second
            } rename to ${
                tableNames.getValue(entity).first
            }"
        }
        // 生成したクエリの実行
        executeQuery(db, createNewTableList.map { it.first })
        executeQuery(db, insertSelectQueryList)
        executeQuery(db, createNewTableList.flatMap { it.second })
        executeQuery(db, dropOldTableQueryList)
        executeQuery(db, renameTableQueryList)
    }

    /**
     * ## SQL 実行
     * ### insert / update / delete / upsert などの更新系SQLを実行する
     * @param query 実行クエリのインスタンス
     * @return 処理件数
     * @author Masahiro Inoue
     * @since 2026-05-31
     */
    fun execute(query: QueryBuilderLike<out TableDefinitionEntity>) =
        if (query is QueryWithBindValues) {
            execute(query.build(), query.bindValues)
        } else {
            execute(query.build())
        }

    /**
     * ## SQL 実行
     * ### insert / update / delete / upsert などの更新系SQLを実行する
     * @param query 実行クエリ文字列
     * @param bindValues 実行クエリ用バインド変数
     * @return 処理件数
     * @author Masahiro Inoue
     * @since 2026-06-03
     */
    fun execute(query: String, bindValues: List<*> = emptyList<Any>()): Int {
        val statement = writableDatabase.compileStatement(query)
        bindValues.forEachIndexed { index, value ->
            val bindIndex = index + 1
            if (value.isNull()) {
                statement.bindNull(bindIndex)
            } else {
                fieldToColumnMap[value!!::class]!!.bind(statement, bindIndex, value)
            }
        }
        return statement.executeUpdateDelete()
    }

    /**
     * ## カラムマッピング解決
     * ### onUpgrade 用のカラム移行マッピングを解決する
     * ### 標準では空マップを返す
     * @return エンティティとマッピングされたリスト
     * - key   : 対象エンティティ
     * - value : リスト(旧カラム名 to 新カラム名)
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    protected open fun resolveColumnMappings(): Map<KClass<out TableDefinitionEntity>, List<Pair<String, String>>> =
        emptyMap()

    /**
     * ## カラム名マッピング更新
     * ### 利用者がカスタムした resolveColumnMappings() メソッドの内容を基にカラム名のマッピングを更新
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    private fun customColumnMappingsUpdate() {
        // resolveColumnMappings() から取得した Map を基に preparedColumnMappings を変更
        resolveColumnMappings().forEach { (entity, customColumnList) ->
            // resolveColumnMappings() で指定されたエンティティのリストを取得
            val preparedColumnList =
                // preparedColumnMappings に存在しない場合、例外を投げる
                preparedColumnMappings[entity] ?: error(AE00021.format(entity))
            // 新旧テーブルカラムのマッピングから旧カラム名のセットを生成
            val preparedColumnKeySet = preparedColumnList.map { prepared -> prepared.first }.toSet()
            // カスタムされたカラム変更リストを基にマップを生成（キー：旧カラム名 / 値：Pair 旧カラム名 to 新カラム名）
            val customColumnMap = customColumnList.associateBy { custom ->
                custom.first
            }
            // customColumnMap.keys と preparedColumnKeySet の差分を取る
            val notFoundColumnList = customColumnMap.keys - preparedColumnKeySet
            // 差分が無いなら継続、差分があるなら例外
            require(notFoundColumnList.isEmpty()) {
                AE00022.format(notFoundColumnList.joinToString(", "))
            }
            // 新旧テーブルカラムのマッピングを更新。customColumnMap に変更用 Pair が無い場合元の Pair を使用
            preparedColumnMappings[entity] = preparedColumnList.map { prepared ->
                customColumnMap[prepared.first] ?: prepared
            }
        }
    }

    /**
     * ## SQLリスト実行
     * ### 生成済みSQLを順番に実行する
     *
     * @param db SQLiteDatabase
     * @param queries 実行対象クエリ郡
     */
    private fun executeQuery(
        db: SQLiteDatabase,
        queries: List<String>,
    ) {
        queries.forEach { query -> db.execSQL(query) }
    }
}
