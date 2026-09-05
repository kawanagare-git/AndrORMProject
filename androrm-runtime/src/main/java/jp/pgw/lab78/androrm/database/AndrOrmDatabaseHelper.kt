package jp.pgw.lab78.androrm.database

import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteCursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.SystemClock
import jp.pgw.lab78.androrm.common.Constants.D_QUOTE
import jp.pgw.lab78.androrm.common.Constants.NEW_TABLE_SUFFIX
import jp.pgw.lab78.androrm.common.MessageConstants.AE00007
import jp.pgw.lab78.androrm.common.MessageConstants.AE00021
import jp.pgw.lab78.androrm.common.MessageConstants.AE00022
import jp.pgw.lab78.androrm.common.MessageConstants.AE00025
import jp.pgw.lab78.androrm.common.MessageConstants.AE00028
import jp.pgw.lab78.androrm.common.MessageConstants.AE00029
import jp.pgw.lab78.androrm.common.MessageConstants.AE00030
import jp.pgw.lab78.androrm.common.MessageConstants.AE00031
import jp.pgw.lab78.androrm.common.MessageConstants.AE00032
import jp.pgw.lab78.androrm.common.MessageConstants.AE00033
import jp.pgw.lab78.androrm.common.MessageConstants.AE00034
import jp.pgw.lab78.androrm.common.MessageConstants.AE00035
import jp.pgw.lab78.androrm.common.MessageConstants.AE00036
import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumnName
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableName
import jp.pgw.lab78.androrm.common.database.annotation.ColumnOldName
import jp.pgw.lab78.androrm.common.database.annotation.MigrationDefault
import jp.pgw.lab78.androrm.common.database.columns.controller.SqlDefaultValueValidator
import jp.pgw.lab78.androrm.common.database.columns.controller.SqlValueType
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.ViewDefinitionEntity
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.condition.interfaces.SelectQuery
import jp.pgw.lab78.androrm.database.interfaces.QueryBuilderLike
import jp.pgw.lab78.androrm.database.meta.RuntimeEntityMetaFactory
import jp.pgw.lab78.androrm.database.reference.TableRef
import jp.pgw.lab78.androrm.database.utility.EntityManager.DataConvertedMap
import jp.pgw.lab78.androrm.database.utility.EntityManager.SelectColumnTarget
import jp.pgw.lab78.androrm.database.utility.EntityManager.columnToFieldMap
import jp.pgw.lab78.androrm.database.utility.EntityManager.getConstructorOrderedProperties
import jp.pgw.lab78.androrm.database.utility.EntityManager.getSelectColumnTargets
import jp.pgw.lab78.androrm.database.validation.ValueFromCursor
import jp.pgw.lab78.shared.library.Utils.isNull
import java.io.Closeable
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

/**
 * ## AndrORM データベースヘルパークラス
 * ### AndrORM とデータベースの接続。各クエリの実行
 *
 * ### 仕様
 * #### 登録された Entity からテーブルを作成し、バージョン更新時は旧テーブルから新テーブルへ互換カラムを移行する。
 * #### DML、Entity／Map／Cursor 形式の SELECT、トランザクション実行を提供し、プレースホルダー順に値をバインドする。
 * @param context android システムのコンテキスト
 * @param databaseName データベース名
 * @param version データベースのバージョン
 * @param entities 生成するテーブル
 * @author Masahiro Inoue
 * @since 2025-08-08
 */
open class AndrOrmDatabaseHelper(
    context: Context,
    databaseName: String? = "app.db",
    version: Int,
    private val entities: List<KClass<out TableDefinitionEntity>>,
) : SQLiteOpenHelper(context, databaseName, null, version) {
    /**
     * ## AndrORM データベースヘルパー生成
     * ### 可変長引数で指定された Entity を使用してデータベースヘルパーを生成する
     * @param context Android システムのコンテキスト
     * @param databaseName データベース名
     * @param version データベースのバージョン
     * @param entities 生成・移行対象テーブルを定義する Entity
     * @author Masahiro Inoue
     * @since 2025-08-08
     */
    constructor(
        context: Context,
        databaseName: String = "app.db",
        version: Int,
        vararg entities: KClass<out TableDefinitionEntity>,
    ) : this(context, databaseName, version, entities.asList())

    /**
     * ## VIEW 付き AndrORM データベースヘルパー生成
     * @param context Android システムのコンテキスト
     * @param databaseName データベース名
     * @param version データベースのバージョン
     * @param entities 生成・移行対象テーブルを定義する Entity
     * @param views 作成・再作成する VIEW
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    constructor(
        context: Context,
        databaseName: String? = "app.db",
        version: Int,
        entities: List<KClass<out TableDefinitionEntity>>,
        views: List<CreateView<out ViewDefinitionEntity>>,
    ) : this(context, databaseName, version, entities) {
        this.views = views
    }

    /**
     * ## VIEW 付き AndrORM データベースヘルパー生成
     * @param context Android システムのコンテキスト
     * @param databaseName データベース名
     * @param version データベースのバージョン
     * @param views 作成・再作成する VIEW
     * @param entities 生成・移行対象テーブルを定義する Entity
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    constructor(
        context: Context,
        databaseName: String = "app.db",
        version: Int,
        views: List<CreateView<out ViewDefinitionEntity>>,
        vararg entities: KClass<out TableDefinitionEntity>,
    ) : this(context, databaseName, version, entities.asList(), views)

    /** 参照元テーブル作成後に生成し、アップグレード時に再作成する VIEW */
    private var views: List<CreateView<out ViewDefinitionEntity>> = emptyList()

    /**
     * ## 標準カラムマッピング保持領域
     * ### onUpgrade 中に作成した「旧カラム名 to 新カラム名」の対応を保持する
     * ### resolveColumnMappings() の指定を反映した後、実際のデータ転送用マッピングの生成に使用する
     */
    private lateinit var preparedColumnMappings: MutableMap<KClass<out TableDefinitionEntity>, List<Pair<String, String>>>

    /** クエリ実行時間（ナノ秒単位） */
    var queryExecutionTime: Long = 0L
        private set

    /**
     * ## SAVEPOINT 実行結果
     * ### SAVEPOINT 内の処理結果、成否、および失敗原因を保持する
     * @param T SAVEPOINT 内で実行する処理の戻り値型
     * @property result 処理成功時の戻り値。処理失敗時は null
     * @property isSuccess 処理が成功した場合は true
     * @property failure 処理失敗時に発生した例外。処理成功時は null
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    data class SavepointResult<T>(
        val result: T,
        val isSuccess: Boolean = false,
        val failure: Exception?
    )

    /**
     * ## SAVEPOINT 作成失敗例外
     * ### SAVEPOINT 文を実行できなかった場合に送出する
     * @param message エラーメッセージ
     * @param cause SAVEPOINT 文の実行時に発生した例外
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    class NotCreatedSavepointException(message: String, cause: Throwable) :
        RuntimeException(message, cause)

    /**
     * ## SAVEPOINT ロールバック失敗例外
     * ### ROLLBACK TO SAVEPOINT 文を実行できなかった場合に送出する
     * @param message エラーメッセージ
     * @param cause ROLLBACK TO SAVEPOINT 文の実行時に発生した例外
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    class FailureRollbackException(message: String, cause: Throwable) :
        RuntimeException(message, cause)

    /**
     * ## SAVEPOINT 解放失敗例外
     * ### RELEASE SAVEPOINT 文を実行できなかった場合に送出する
     * @param message エラーメッセージ
     * @param cause RELEASE SAVEPOINT 文の実行時に発生した例外
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    class FailureReleaseException(message: String, cause: Throwable) :
        RuntimeException(message, cause)

    /**
     * ## データベース生成
     * ### データベースの生成と所属するテーブルを作成
     * ### データベースが新規作成される時だけ作成される
     * @param db SQLite データベース
     * @author Masahiro Inoue
     * @since 2025-08-08
     */
    override fun onCreate(db: SQLiteDatabase) {
        val usedIndexNames = mutableSetOf<String>()
        val createQueriesByEntity = entities.distinct().associateWith { entity ->
            val create = Create(entity)
            create.build() to create.buildIndexQueries(0, usedIndexNames)
        }
        val createViewQueries = views.map { view -> view.build() }
        // テーブル作成
        entities.forEach { entity ->
            val (createTableQuery, createIndexQueries) = createQueriesByEntity.getValue(entity)
            db.execSQL(createTableQuery)
            // テーブル毎のインデックス作成
            createIndexQueries.forEach { createIndexQuery ->
                db.execSQL(createIndexQuery)
            }
        }
        // VIEW作成
        executeQuery(db, createViewQueries)
    }

    /**
     * ## データベース更新
     * ### データベースバージョンを比較してテーブル追加・変更を実施
     * ### 移行開始時に各移行対象の `<テーブル名>_new` を削除するため、この接尾辞は移行専用として予約する
     * ### 利用者が同名テーブルを作成していた場合、そのテーブルとデータはアップグレード時に削除される
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
        val createViewQueries = views.map { view -> view.build() }
        val dropViewQueries = buildList {
            addAll(views.asReversed().map { view -> view.buildDropQuery() })
            addAll(
                obsoleteViewNames(oldVersion, newVersion)
                    .map { viewName -> CreateView.buildDropQuery(viewName) }
            )
        }.distinct()
        executeQuery(db, dropViewQueries)
        migrateDatabase(db, newVersion)
        executeQuery(db, createViewQueries)
    }

    /**
     * ## 廃止 VIEW 名解決
     * ### 現在の views へ登録されなくなった旧 VIEW をアップグレード時に削除するための拡張点
     * @param oldVersion 適用前データベースバージョン
     * @param newVersion 適用後データベースバージョン
     * @return DROP VIEW 対象名
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    protected open fun obsoleteViewNames(oldVersion: Int, newVersion: Int): List<String> =
        emptyList()

    /**
     * ## DB 移行処理
     * ### 新テーブルを生成し、旧テーブルから移行可能なカラムの値を転送してテーブルを置き換える
     * ### 旧テーブルに存在しない非nullableカラムは MigrationDefault の値で補完し、nullableカラムは転送対象外とする
     * ### 前回の移行失敗で残った可能性がある `<テーブル名>_new` は、新しい一時テーブルを作成する前に削除する
     * @param db SQLite データベース
     * @param newVersion 適用後データベースバージョン。新規インデックスの適用判定に使用する
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
        // 前回の移行失敗で残った可能性がある一時テーブルを削除
        val dropTemporaryTableQueryList = tableNames.values.map { tableNamePair ->
            "drop table if exists ${tableNamePair.second}"
        }
        executeQuery(db, dropTemporaryTableQueryList)
        // 新旧テーブルカラムのマッピング
        preparedColumnMappings = entities.associateWith { entity ->
            entity.getConstructorOrderedProperties()
                .map { property ->
                    val newColumn = property.getColumnName()
                    val oldColumn =
                        property.findAnnotation<ColumnOldName>()?.name?.takeIf { it.isNotBlank() }
                            ?: newColumn
                    oldColumn to newColumn
                }
        }.toMutableMap()
        // 新旧テーブルカラムのマッピングを更新
        customColumnMappingsUpdate()
        // 旧テーブルに存在しないカラムを null 許容性・MigrationDefault に従って解決
        val transferColumnMappings = entities.associateWith { entity ->
            resolveTransferColumnMappings(
                db = db,
                entity = entity,
                tableName = tableNames.getValue(entity).first,
                columnMappings = preparedColumnMappings.getValue(entity),
            )
        }
        // 新テーブルの create 文生成
        val usedIndexNames = mutableSetOf<String>()
        val createNewTableList = tableNames.map { (key, tableNamePair) ->
            val create = Create(key, tableNamePair.second)
            create.build() to create.buildIndexQueries(newVersion, usedIndexNames)
        }
        // データ転送用クエリ生成
        val insertSelectQueryList = entities.map { entity ->
            Insert.intoTableColumns(
                tableNames.getValue(entity).second,
                transferColumnMappings.getValue(entity).map { it.second },
                Select.tableColumns(
                    tableNames.getValue(entity).first,
                    transferColumnMappings.getValue(entity).map { it.first })
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
    fun executeDml(query: QueryBuilderLike<out TableDefinitionEntity>) =
        if (query is QueryWithBindValues) {
            executeDml(query.build(), query.bindValues)
        } else {
            executeDml(query.build())
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
    fun executeDml(query: String, bindValues: List<*> = emptyList<Any>()): Int {
        val statement = writableDatabase.compileStatement(query)
        bindValues.forEachIndexed { index, value ->
            val bindIndex = index + 1
            if (value.isNull()) {
                statement.bindNull(bindIndex)
            } else {
                DataConvertedMap[value::class]!!.toBind(statement, bindIndex, value)
            }
        }
        val start = SystemClock.elapsedRealtimeNanos()
        try {
            return statement.executeUpdateDelete()
        } finally {
            queryExecutionTime = SystemClock.elapsedRealtimeNanos() - start
        }
    }

    /**
     * ## SELECT 実行
     * ### Select クラスで生成した SQL を実行し、結果を Entity のリストで返す
     * @param query Select クエリのインスタンス
     * @return SELECT 結果 Entity リスト
     * @author Masahiro Inoue
     * @since 2026-06-05
     */
    fun <T : SelectEntity> executeSelectAsEntityList(query: Select<T>): List<Map<String, SelectEntity?>> {
        val columnTargetsTaskMap = query.usedEntityClasses.associateWith { usedEntity ->
            usedEntity.createSelectColumnTargetsTask()
        }
        // エンティティリストの生成
        val entityResultTargets = query.usedEntityClasses.map { usedEntity ->
            EntityResultTarget(
                tableRef = usedEntity,
                columnTargets = columnTargetsTaskMap.getValue(usedEntity).get(),
                nullableByJoin = query.isNullableByJoin(usedEntity),
            )
        }
        return executeSelectAsEntityList(query, entityResultTargets)
    }

    /**
     * ## UNION ALL SELECT 実行
     * ### UnionAllクラスで生成したSQLを実行し、resultEntityのEntityリストで返す
     * @param query UNION ALLクエリのインスタンス
     * @return SELECT結果Entityリスト
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    fun <T : SelectEntity> executeSelectAsEntityList(
        query: UnionAll<T>,
    ): List<Map<String, SelectEntity?>> {
        val resultEntityMeta = RuntimeEntityMetaFactory().create(query.resultEntity)
        val resultTableRef = TableRef(
            entityClass = query.resultEntity,
            alias = resultEntityMeta.tableAlias,
        )
        val entityResultTarget = EntityResultTarget(
            tableRef = resultTableRef,
            columnTargets = resultTableRef.createSelectColumnTargetsTask().get(),
            nullableByJoin = false,
        )
        return executeSelectAsEntityList(query, listOf(entityResultTarget))
    }

    /**
     * ## SELECT系クエリ実行結果変換
     * ### Map形式の各行を指定されたEntity復元対象へ変換する
     * @param query SELECT系クエリのインスタンス
     * @param entityResultTargets Entity復元対象
     * @return SELECT結果Entityリスト
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    private fun executeSelectAsEntityList(
        query: SelectQuery<SelectEntity>,
        entityResultTargets: List<EntityResultTarget>,
    ): List<Map<String, SelectEntity?>> {
        // データの取得 ※マップ形式
        val mapList = executeSelectAsMapList(query)
        // エンティティの生成
        return mapList.map { row ->
            entityResultTargets.associate { target ->
                target.alias to target.createSelectEntityOrNull(row)
            }
        }
    }

    /**
     * ## SELECT系クエリ実行
     * ### SELECT系クエリで生成したSQLを実行し、結果をMapのリストで返す
     * @param query SELECT系クエリのインスタンス
     * @return SELECT結果
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    fun executeSelectAsMapList(query: SelectQuery<SelectEntity>): List<Map<String, Any?>> =
        executeSelectQueryAsMapList(query)

    /**
     * ## SELECT 実行
     * ### Select クラスで生成した SQL を実行し、結果を Map のリストで返す
     * @param query Select クエリのインスタンス
     * @return SELECT 結果
     * @author Masahiro Inoue
     * @since 2026-06-04
     */
    fun executeSelectAsMapList(query: Select<out SelectEntity>): List<Map<String, Any?>> =
        executeSelectQueryAsMapList(query)

    /**
     * ## UNION ALL SELECT 実行
     * ### UnionAllクラスで生成したSQLを実行し、結果をMapのリストで返す
     * @param query UNION ALLクエリのインスタンス
     * @return SELECT結果
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    fun executeSelectAsMapList(query: UnionAll<out SelectEntity>): List<Map<String, Any?>> =
        executeSelectQueryAsMapList(query)

    /**
     * ## SELECT系Map取得共通処理
     * ### SELECT系クエリのSQLとバインド値を既存の文字列実行経路へ渡す
     * @param query SELECT系クエリのインスタンス
     * @return SELECT結果
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    private fun executeSelectQueryAsMapList(
        query: SelectQuery<SelectEntity>,
    ): List<Map<String, Any?>> = executeSelectAsMapList(query.build(), query.bindValues)

    /**
     * ## SELECT 実行
     * ### 生成済み SELECT 文字列を実行し、結果を Map のリストで返す
     * @param query 実行クエリ文字列
     * @param bindValues 実行クエリ用バインド変数
     * @return SELECT 結果
     * @author Masahiro Inoue
     * @since 2026-06-04
     */
    fun executeSelectAsMapList(
        query: String,
        bindValues: List<*> = emptyList<Any>(),
    ): List<Map<String, Any?>> {
        val start = SystemClock.elapsedRealtimeNanos()
        try {
            return executeSelectAsCursor(query, bindValues).use { cursor ->
                cursor.toMapList()
            }
        } finally {
            queryExecutionTime = SystemClock.elapsedRealtimeNanos() - start
        }
    }

    /**
     * ## SELECT 実行
     * ### Select クラスで生成した SQL を読み出し用 DB で実行し、Cursor を返す
     * @param query Select クエリのインスタンス
     * @return 検索結果 Cursor
     * @author Masahiro Inoue
     * @since 2026-06-03
     */
    fun executeSelectAsCursor(query: Select<out SelectEntity>): Cursor =
        executeSelectQueryAsCursor(query)

    /**
     * ## UNION ALL SELECT 実行
     * ### UnionAllクラスで生成したSQLを読み出し用DBで実行し、Cursorを返す
     * @param query UNION ALLクエリのインスタンス
     * @return 検索結果Cursor
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    fun executeSelectAsCursor(query: UnionAll<out SelectEntity>): Cursor =
        executeSelectQueryAsCursor(query)

    /**
     * ## SELECT系クエリ実行
     * ### SELECT系クエリで生成したSQLを読み出し用DBで実行し、Cursorを返す
     * @param query SELECT系クエリのインスタンス
     * @return 検索結果Cursor
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    fun executeSelectAsCursor(query: SelectQuery<SelectEntity>): Cursor =
        executeSelectQueryAsCursor(query)

    /**
     * ## SELECT系Cursor取得共通処理
     * ### SELECT系クエリのSQLとバインド値を既存の文字列実行経路へ渡す
     * @param query SELECT系クエリのインスタンス
     * @return 検索結果Cursor
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    private fun executeSelectQueryAsCursor(
        query: SelectQuery<SelectEntity>,
    ): Cursor = executeSelectAsCursor(query.build(), query.bindValues)

    /**
     * ## SELECT 実行
     * ### 生成済み SELECT 文字列を読み出し用 DB で実行し、Cursor を返す
     * @param queryString 実行クエリ文字列
     * @param bindValues 実行クエリ用バインド変数
     * @return 検索結果 Cursor
     * @author Masahiro Inoue
     * @since 2026-06-03
     */
    fun executeSelectAsCursor(queryString: String, bindValues: List<*> = emptyList<Any>()): Cursor {
        return readableDatabase.rawQueryWithFactory(
            { _, cursorDriver, editTable, query ->
                bindValues.forEachIndexed { index, value ->
                    val indexInc = index + 1
                    if (value.isNull()) {
                        query.bindNull(indexInc)
                    }else{
                       ValueFromCursor.identifyType(value).setBind(query,indexInc,value)
                    }
                }
                SQLiteCursor(cursorDriver, editTable, query)
            },
            queryString,
            emptyArray<String>(),
            null,
        )
    }

    /**
     * ## トランザクション実行
     * ### ブロック内の DB 更新を 1 トランザクションとして実行する
     * - executeSelectAsCursor を使用する場合は、Cursor は block 内で使用すること
     * - block 内で発生した例外は握りつぶさないこと
     * @param block トランザクション内で実行する処理
     * @return ブロックの戻り値
     * @author Masahiro Inoue
     * @since 2026-06-27
     */
    fun <R> transaction(block: AndrOrmDatabaseHelper.() -> R): R {
        val db = writableDatabase
        // トランザクション開始
        db.beginTransaction()
        try {
            val result = this.block()
            // block の実行が全て成功したらトランザクション成功のフラグを立てる
            db.setTransactionSuccessful()
            return result
        } finally {
            // block の実行状況に応じて、commit / rollback が実行される
            db.endTransaction()
        }
    }

    /**
     * ## SAVEPOINT 実行
     * ### SAVEPOINT を作成して block を実行し、失敗時は当該 SAVEPOINT までロールバックする
     * ### 成功・失敗のどちらの場合も SAVEPOINT を解放し、block 内の例外は failure として返す
     * @param marker SAVEPOINT 名。nullまたは空文字の場合は一意な名前を自動生成する
     * @param block SAVEPOINT 内で実行する処理
     * @return block の実行結果、成否、および失敗原因
     * @throws NotCreatedSavepointException SAVEPOINT の作成に失敗した場合
     * @throws FailureRollbackException block 失敗後のロールバックに失敗した場合
     * @throws FailureReleaseException SAVEPOINT の解放に失敗した場合
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    @Suppress("TooGenericExceptionCaught")
    fun <T> savepoint(
        marker: Any? = null,
        block: () -> T,
    ): SavepointResult<T?> {
        val savepointName = createSavepointName(marker)

        return Savepoint(
            database = writableDatabase,
            name = savepointName,
        ).use { savepoint ->
            try {
                val result = block()
                SavepointResult(result, true, null)
            } catch (e: Exception) {
                savepoint.rollback()
                SavepointResult(null, false, e)
            }
        }
    }

    /**
     * ## SAVEPOINT 管理
     * ### SAVEPOINT の作成、ロールバック、および解放を状態に従って管理する
     * @param database SQL を実行する SQLite データベース
     * @param name SAVEPOINT 名
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private class Savepoint(
        private val database: SQLiteDatabase,
        private val name: String,
    ) : Closeable {
        /** SAVEPOINT の現在の状態 */
        private var state = State.ACTIVE

        /**
         * ## イニシャライザ
         * ### SAVEPOINT を作成する
         * @throws NotCreatedSavepointException SAVEPOINT の作成に失敗した場合
         * @author Masahiro Inoue
         * @since 2026-07-18
         */
        init {
            try {
                database.execSQL("SAVEPOINT $name")
            } catch (e: SQLException) {
                throw NotCreatedSavepointException(
                    AE00034.format(name),
                    e,
                )
            }
        }

        /**
         * ## SAVEPOINTまでロールバック
         * ### SAVEPOINT 作成後の変更を取り消し、状態をロールバック済みに更新する
         * @throws FailureRollbackException SAVEPOINT へのロールバックに失敗した場合
         * @author Masahiro Inoue
         * @since 2026-07-18
         */
        fun rollback() {
            /*
             * SQL実行前にBROKENへ変更する。
             * ROLLBACKに失敗した場合、close()でRELEASEしない。
             */
            state = State.BROKEN
            try {
                database.execSQL("ROLLBACK TO SAVEPOINT $name")
                state = State.ROLLED_BACK
            } catch (e: SQLException) {
                throw FailureRollbackException(
                    AE00035.format(name),
                    e,
                )
            }
        }

        /**
         * ## SAVEPOINTを解放
         * ### 現在の状態に応じて SAVEPOINT の解放処理を実行する
         * @throws FailureReleaseException SAVEPOINT の解放に失敗した場合
         * @author Masahiro Inoue
         * @since 2026-07-18
         */
        override fun close() {
            state.execute(this)
        }

        /**
         * ## SAVEPOINT解放処理
         * ### RELEASE SAVEPOINT を実行して状態を解放済みに更新する
         * @throws FailureReleaseException SAVEPOINT の解放に失敗した場合
         * @author Masahiro Inoue
         * @since 2026-07-18
         */
        private fun release() {
            /*
             * RELEASEに失敗した場合も、再度RELEASEしないように
             * SQL実行前にBROKENへ変更する。
             */
            state = State.BROKEN
            try {
                database.execSQL("RELEASE SAVEPOINT $name")
                state = State.RELEASED
            } catch (e: SQLException) {
                throw FailureReleaseException(
                    AE00036.format(name),
                    e,
                )
            }
        }

        /**
         * ## SAVEPOINTの状態
         * @param executor 状態に応じて実行する SAVEPOINT 操作
         * @author Masahiro Inoue
         * @since 2026-07-18
         */
        private enum class State(private val executor: Savepoint.() -> Unit) {
            /** 作成済み */
            ACTIVE({ release() }),

            /** ROLLBACK TO実行済み */
            ROLLED_BACK({ release() }),

            /** SQL実行に失敗し、状態を保証できない */
            BROKEN({ }),

            /** RELEASE実行済み */
            RELEASED({ }),
            ;

            /**
             * ## 状態に対応した処理を実行
             * @param savepoint 操作対象の SAVEPOINT
             * @author Masahiro Inoue
             * @since 2026-07-18
             */
            fun execute(savepoint: Savepoint) {
                executor.invoke(savepoint)
            }
        }
    }

    /** 自動生成する SAVEPOINT 名の連番 */
    private var savepointSequence: AtomicLong = AtomicLong(0L)

    /**
     * ## SAVEPOINT 名生成
     * ### marker が指定されている場合は前後空白を除去して使用し、未指定の場合は連番から生成する
     * @param marker SAVEPOINT 名の生成元
     * @return SQL に使用する SAVEPOINT 名
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private fun createSavepointName(marker: Any?): String {
        val specifiedName = marker?.toString()?.trim()

        if (specifiedName.isNullOrEmpty()) {
            savepointSequence.addAndGet(1)
            return "androrm_savepoint_$savepointSequence"
        }

        return "$D_QUOTE${specifiedName}$D_QUOTE"
    }

    /**
     * ## 移行用カラムマッピング解決
     * ### 旧テーブルに存在しないnullableカラムは転送対象外とし、MigrationDefault指定列は検証済み値で補完する
     * @param db SQLite データベース
     * @param entity 移行先 Entity
     * @param tableName 移行元テーブル名
     * @param columnMappings 旧カラムまたは式と新カラムの対応
     * @return データ転送に使用するカラムマッピング
     * @throws IllegalStateException 移行先カラムに対応するプロパティが存在しない場合、または非nullable追加カラムに MigrationDefault がない場合
     * @throws IllegalArgumentException MigrationDefault の値が対象プロパティの型に対して不正な場合
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private fun resolveTransferColumnMappings(
        db: SQLiteDatabase,
        entity: KClass<out TableDefinitionEntity>,
        tableName: String,
        columnMappings: List<Pair<String, String>>,
    ): List<Pair<String, String>> {
        val oldColumnNames = findTableColumnNames(
            db = db,
            tableName = tableName,
            fallback = columnMappings.map { mapping -> mapping.first }.toSet(),
        ).map { columnName -> columnName.uppercase() }.toSet()
        val propertyByColumnName = entity.getConstructorOrderedProperties()
            .associateBy { property -> property.getColumnName().uppercase() }

        return columnMappings.mapNotNull { (oldColumn, newColumn) ->
            if (oldColumn.uppercase() in oldColumnNames) {
                return@mapNotNull oldColumn to newColumn
            }

            val property = propertyByColumnName[newColumn.uppercase()]
                ?: error(AE00031.format(newColumn))
            resolveMigrationDefaultSql(entity, property)?.let { sqlValue ->
                sqlValue to newColumn
            }
        }
    }

    /**
     * ## テーブルカラム名取得
     * ### 移行元テーブルを空検索して実カラム名を取得する
     * @param db SQLite データベース
     * @param tableName テーブル名
     * @param fallback Cursor を返さないテスト用 DB の代替カラム名
     * @return テーブルのカラム名
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private fun findTableColumnNames(
        db: SQLiteDatabase,
        tableName: String,
        fallback: Set<String>,
    ): Set<String> {
        val cursor = db.rawQuery("select * from $tableName limit 0", emptyArray<String>())
            ?: return fallback
        return cursor.use { it.columnNames.toSet() }
    }

    /**
     * ## マイグレーション既定値SQL解決
     * ### MigrationDefaultを型別に検証し、SQLiteへ渡せるSQL値へ正規化する
     * @param entity 対象 Entity
     * @param property 旧テーブルに存在しないプロパティ
     * @return SQLリテラルまたは式。MigrationDefault未指定のnullableプロパティはnull
     * @throws IllegalStateException 非nullableプロパティに MigrationDefault が指定されていない場合
     * @throws IllegalArgumentException MigrationDefault の値が対象プロパティの型に対して不正な場合
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private fun resolveMigrationDefaultSql(
        entity: KClass<out TableDefinitionEntity>,
        property: KProperty1<out TableDefinitionEntity, *>,
    ): String? {
        val migrationDefault = property.findAnnotation<MigrationDefault>()
        if (migrationDefault == null) {
            if (property.returnType.isMarkedNullable) return null
            error(AE00032.format(entity.qualifiedName, property.name))
        }
        val typeName = (property.returnType.classifier as? KClass<*>)?.qualifiedName.orEmpty()
        val isValid = SqlDefaultValueValidator.isValid(
            type = SqlValueType.fromQualifiedName(typeName),
            nullable = property.returnType.isMarkedNullable,
            value = migrationDefault.value,
            allowBlank = false,
        )
        require(isValid) {
            AE00033.format(entity.qualifiedName, property.name, migrationDefault.value, typeName)
        }
        return SqlDefaultValueValidator.normalize(migrationDefault.value)
    }

    /**
     * ## Entity 復元対象情報
     * ### SELECT 結果1行から alias 単位で Entity を復元するための情報
     * @param tableRef 復元対象テーブル参照
     * @param columnTargets プロパティ名と SELECT 結果カラム名の紐づけ
     * @param nullableByJoin OUTER JOIN により Entity 自体が null になり得る場合 true
     * @author Masahiro Inoue
     * @since 2026-06-11
     */
    private data class EntityResultTarget(
        val tableRef: TableRef<out SelectEntity>,
        val columnTargets: List<SelectColumnTarget>,
        val nullableByJoin: Boolean,
    ) {
        /** SELECT 結果 Map で Entity を識別するテーブルエイリアス */
        val alias: String = tableRef.alias

        /** 復元する Entity のクラス */
        val entityClass: KClass<out SelectEntity> = tableRef.entityClass
    }

    /**
     * ## SELECT 結果カラム紐づけ取得タスク生成
     * ### Entity メタ情報取得を別スレッドで開始する
     * @receiver SELECT 結果 Entity クラス
     * @return SELECT 結果カラム紐づけ取得タスク
     * @author Masahiro Inoue
     * @since 2026-06-05
     */
    private fun <T : SelectEntity> TableRef<out T>.createSelectColumnTargetsTask(): FutureTask<List<SelectColumnTarget>> {
        val task = FutureTask<List<SelectColumnTarget>> {
            this.getSelectColumnTargets()
        }
        Thread(task, "AndrORM-select-column-metadata").start()
        return task
    }

    /**
     * ## SELECT 結果 Entity null 判定・変換
     * ### OUTER JOIN により結合先が存在しない場合は Entity 自体を null とする
     * @receiver Entity 復元対象情報
     * @param row SELECT 結果1行分
     * @return 生成した Entity。結合先が存在しない場合は null
     * @author Masahiro Inoue
     * @since 2026-06-11
     */
    private fun EntityResultTarget.createSelectEntityOrNull(
        row: Map<String, Any?>,
    ): SelectEntity? {
        val selectedColumnTargets = columnTargets.filter { columnTarget ->
            row.containsKey(columnTarget.resultColumnName)
        }
        val isNullJoinedEntity = nullableByJoin &&
                selectedColumnTargets.isNotEmpty() &&
                selectedColumnTargets.all { columnTarget ->
                    row[columnTarget.resultColumnName].isNull()
                }

        return if (isNullJoinedEntity) {
            null
        } else {
            entityClass.createSelectEntity(
                row = row,
                columnTargets = columnTargets,
            )
        }
    }

    /**
     * ## SELECT 結果 Entity 変換
     * ### Map 化した SELECT 結果から Entity を生成する
     * @receiver SELECT 結果 Entity クラス
     * @param row SELECT 結果1行分
     * @param columnTargets プロパティ名と SELECT 結果カラム名の紐づけ
     * @return 生成した Entity
     * @author Masahiro Inoue
     * @since 2026-06-05
     */
    private fun <T : SelectEntity> KClass<out T>.createSelectEntity(
        row: Map<String, Any?>,
        columnTargets: List<SelectColumnTarget>,
    ): T {
        val constructor = this.primaryConstructor
            ?: error(AE00007.format(this.qualifiedName))
        val columnTargetMap = columnTargets.associateBy { it.propertyName }
        val args = mutableMapOf<KParameter, Any?>()
        // コンストラクタの検証（保険機能）
        constructor.parameters.forEach { parameter ->
            val propertyName = parameter.name
                ?: error(AE00007.format(this.qualifiedName))
            val columnTarget = columnTargetMap[propertyName]
                ?: error(AE00028.format(propertyName))
            // カラム名の検証
            if (!row.containsKey(columnTarget.resultColumnName)) {
                when {
                    parameter.isOptional -> Unit
                    parameter.type.isMarkedNullable -> args[parameter] = null
                    else -> error(AE00029.format(columnTarget.resultColumnName))
                }
            } else {
                args[parameter] = convertSelectValue(
                    row[columnTarget.resultColumnName],
                    parameter.type,
                )
            }
        }
        return constructor.callBy(args)
    }

    /**
     * ## SELECT 結果値変換
     * ### Cursor 由来の値を Entity コンストラクタの型へ変換する
     * @param value SELECT 結果値
     * @param targetType 変換先型
     * @return 変換後の値
     * @author Masahiro Inoue
     * @since 2026-06-05
     */
    private fun convertSelectValue(value: Any?, targetType: KType): Any? {
        if (value.isNull()) {
            require(targetType.isMarkedNullable) {
                AE00030.format(targetType)
            }
            return null
        }
        return DataConvertedMap[targetType.classifier]?.toProp(value) ?: value
    }

    /**
     * ## Cursor Map 変換
     * ### Cursor の全行を List<Map<String, Any?>> に変換する
     * @receiver SELECT 結果 Cursor
     * @return Cursor を基に構築した Map リスト
     * @author Masahiro Inoue
     * @since 2026-06-04
     */
    private fun Cursor.toMapList(): List<Map<String, Any?>> {
        val result = mutableListOf<Map<String, Any?>>()
        // カラム名の取得
        val columnNames = this.columnNames
        // カーソルの次行読み出し
        while (this.moveToNext()) {
            val row = linkedMapOf<String, Any?>()
            // カラム名を基に値の取得
            columnNames.forEachIndexed { index, columnName ->
                row[columnName] = this.getValue(index)
            }
            result.add(row)
        }
        return result
    }

    /**
     * ## Cursor 値取得
     * ### Cursor の列型に応じて値を取得する
     * @receiver SELECT 結果 Cursor
     * @param index カラム index
     * @return Cursor から取得した値
     * @author Masahiro Inoue
     * @since 2026-06-04
     */
    private fun Cursor.getValue(index: Int): Any? {
        val columnType = this.getType(index)
        val invokeGetter = columnToFieldMap[columnType]
            ?: error(AE00025.format(index, columnType))
        return invokeGetter(this, index)
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
     * @param db SQL を実行する SQLite データベース
     * @param queries 実行対象クエリ群
     * @author Masahiro Inoue
     * @since 2026-06-05
     */
    private fun executeQuery(
        db: SQLiteDatabase,
        queries: List<String>,
    ) {
        queries.forEach { query -> db.execSQL(query) }
    }
}
