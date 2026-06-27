package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.MessageConstants.AE00016
import jp.pgw.lab78.androrm.common.MessageConstants.AE00017
import jp.pgw.lab78.androrm.common.MessageConstants.AE00018
import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumnName
import jp.pgw.lab78.androrm.common.database.SupportFunction.getPropertyValue
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableName
import jp.pgw.lab78.androrm.common.dml.interfaces.UpsertEntity
import jp.pgw.lab78.androrm.common.logging.aop.TraceLog
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.interfaces.QueryBuilderLike
import jp.pgw.lab78.androrm.database.utility.EntityManager.getDmlTargets
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * ## Upsert 文生成クラス
 * ### SQLite の INSERT ... ON CONFLICT ... DO UPDATE 文を生成します
 *
 * @param entityClass Upsert 対象 Entity クラス
 * @author Masahiro Inoue
 * @since 2026-05-24
 */
class Upsert<T : UpsertEntity>(
    private val entityClass: KClass<out T>,
) : QueryBuilderLike<T>, QueryWithBindValues() {

    /** テーブル名 */
    private val tableName = entityClass.getTableName()

    /**
     * DML 対象リスト
     *
     * Pair の内容:
     * - first  : Kotlin プロパティ名
     * - second : DB カラム名
     */
    private val columnList = entityClass.getDmlTargets()

    /** カラム名の定義文字列 */
    private val columnDefine: String =
        columnList.joinToString(", ", "(", ")") { it.second }

    /** Upsert 対象 Entity 一覧 */
    private var entities: MutableList<T> = mutableListOf()

    /** 衝突判定カラム */
    private val conflictColumns: MutableList<String> = mutableListOf()

    /** 衝突時に更新するカラム */
    private val updateColumns: MutableList<String> = mutableListOf()

    /** ビルドフラグ */
    private var isBuild: Boolean = false

    /** クエリ格納 */
    private lateinit var query: String

    /**
     * ## エンティティ追加
     * ### Upsert 対象 Entity を1件追加する
     *
     * @param entity 追加する Entity
     */
    fun addEntity(entity: T) {
        entities.add(entity)
    }

    /**
     * ## エンティティ追加
     * ### Upsert 対象 Entity を複数件追加する
     *
     * @param entities 追加する Entity リスト
     */
    fun addEntities(entities: List<T>) {
        this.entities.addAll(entities)
    }

    /**
     * ## 衝突判定カラム指定
     * ### ON CONFLICT に指定するカラムを設定する
     *
     * @param properties 衝突判定に使用するプロパティ
     * @return 自身のインスタンス
     */
    @TraceLog
    fun onConflict(
        vararg properties: KProperty1<T, *>,
    ): Upsert<T> {
        isBuild = false
        conflictColumns.clear()
        conflictColumns.addAll(
            properties.map { property ->
                property.getColumnName()
            }
        )
        return this
    }

    /**
     * ## 更新カラム指定
     * ### DO UPDATE SET に指定するカラムを設定する
     *
     * @param properties 衝突時に更新するプロパティ
     * @return 自身のインスタンス
     */
    @TraceLog
    fun updateColumns(
        vararg properties: KProperty1<T, *>,
    ): Upsert<T> {
        isBuild = false
        updateColumns.clear()
        updateColumns.addAll(
            properties.map { property ->
                property.getColumnName()
            }
        )
        return this
    }

    /**
     * ## Upsert 文生成
     * ### addEntity / addEntities で追加済みの Entity を基に Upsert 文を生成する
     *
     * @return 生成された Upsert 文
     */
    override fun build(): String =
        build(entities).first

    /**
     * ## Upsert 文生成
     * ### Entity リストを基に Upsert 文と bindValues を生成する
     *
     * @param entities Upsert 対象 Entity リスト
     * @return Upsert 文と bindValues
     */
    @TraceLog
    fun build(entities: List<T>): Pair<String, List<Any?>> {
        require(entities.isNotEmpty()) {
            AE00016
        }
        require(conflictColumns.isNotEmpty()) {
            AE00017
        }
        require(updateColumns.isNotEmpty()) {
            AE00018
        }
        this.entities = entities.toMutableList()
        clearBindValues()
        val valuesClause = placeholders(
            rowCount = entities.size,
            columnCount = columnList.size,
        )
        val conflictClause = conflictColumns.joinToString(", ", "on conflict(", ")")
        val updateClause = updateColumns.joinToString(", ") { columnName ->
            "$columnName = excluded.$columnName"
        }
        query = if (isBuild) {
            query
        } else {
            isBuild = true
            "insert into $tableName $columnDefine values$valuesClause " +
                    "$conflictClause do update set $updateClause"
        }
        val bindValues = entities.flatMap { entity ->
            columnList.map { (propertyName, _) ->
                getPropertyValue(entity, propertyName)
            }
        }
        addBindValues(bindValues)
        return query to bindValues
    }

    /**
     * ## プレースホルダー生成
     * ### Upsert 対象行数に応じて VALUES 句のプレースホルダーを生成する
     *
     * @param rowCount 行数
     * @param columnCount カラム数
     * @return 生成されたプレースホルダー文字列
     */
    @TraceLog
    private fun placeholders(
        rowCount: Int,
        columnCount: Int,
    ): String {
        val oneRow = List(columnCount) { "?" }.joinToString(", ", "(", ")")
        return List(rowCount) { oneRow }.joinToString(", ")
    }
}