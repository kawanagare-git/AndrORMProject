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
import jp.pgw.lab78.androrm.database.queryparts.UpsertSetClauseBuilder
import jp.pgw.lab78.androrm.database.utility.EntityManager
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

    /** SET 句リスト */
    private val setAssignments = mutableListOf<Pair<String, String>>()

    /** SET 句用バインド値 */
    private val setBindValues = mutableListOf<Any?>()

    /**
     * DML 対象リスト
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
     * @param entity 追加する Entity
     * @author Masahiro Inoue
     * @since 2026-05-24
     */
    fun addEntity(entity: T): Upsert<T> {
        addEntities(listOf(entity))
        return this
    }

    /**
     * ## エンティティ追加
     * ### Upsert 対象 Entity を複数件追加する
     * @param entities 追加する Entity リスト
     * @author Masahiro Inoue
     * @since 2026-05-24
     */
    fun addEntities(vararg entities: T): Upsert<T> {
        addEntities(entities.asList())
        return this
    }

    /**
     * ## エンティティ追加
     * ### Upsert 対象 Entity を複数件追加する
     * @param entities 追加する Entity リスト
     * @author Masahiro Inoue
     * @since 2026-05-24
     */
    fun addEntities(entities: List<T>): Upsert<T> {
        isBuild = false
        this.entities.addAll(entities)
        return this
    }

    /**
     * ## 衝突判定カラム指定
     * ### ON CONFLICT に指定するカラムを設定する
     * @param properties 衝突判定に使用するプロパティ
     * @return 自身のインスタンス
     * @author Masahiro Inoue
     * @since 2026-05-24
     */
    @TraceLog
    fun onConflict(
        block: ConflictClauseBuilder.() -> Unit,
    ): Upsert<T> {
        isBuild = false
        conflictColumns.clear()

        val builder = ConflictClauseBuilder().apply(block)
        conflictColumns.addAll(builder.buildList())

        return this
    }

    /**
     * ## 更新カラム指定
     * ### DO UPDATE SET に指定するカラムを設定する
     * @param block 衝突時に更新するプロパティを指定
     * @return 自身のインスタンス
     * @author Masahiro Inoue
     * @since 2026-05-24
     */
    @TraceLog
    fun set(block: UpsertSetClauseBuilder<T>.() -> Unit): Upsert<T> {
        isBuild = false
        setAssignments.clear()
        setBindValues.clear()

        val valueHolder = object : QueryWithBindValues() {}
        val builder =
            UpsertSetClauseBuilder(
                targetEntityClass = entityClass,
                valueHolder = valueHolder,
                valueFormatter = this::formatSetValue,
            ).apply(block)

        setAssignments.addAll(builder.buildList())
        setBindValues.addAll(valueHolder.bindValues)

        return this
    }

    /**
     * ## Upsert 文生成
     * ### addEntity / addEntities で追加済みの Entity を基に Upsert 文を生成する
     * @return 生成された Upsert 文
     * @author Masahiro Inoue
     * @since 2026-05-24
     */
    @TraceLog
    override fun build(): String {
        require(entities.isNotEmpty()) { AE00016 }
        require(conflictColumns.isNotEmpty()) { AE00017 }
        require(setAssignments.isNotEmpty()) { AE00018 }
        this.entities = entities.toMutableList()
        clearBindValues()
        val valuesClause = placeholders(
            rowCount = entities.size,
            columnCount = columnList.size,
        )
        val conflictClause = conflictColumns.joinToString(", ", "on conflict(", ")")
        val updateClause = setAssignments.joinToString(", ") { (columnName, expression) ->
            "$columnName = $expression"
        }
        query = if (isBuild) {
            query
        } else {
            isBuild = true
            "insert into $tableName $columnDefine values$valuesClause " +
                    "$conflictClause do update set $updateClause"
        }
        val insertBindValues = entities.flatMap { entity ->
            columnList.map { (propertyName, _) ->
                getPropertyValue(entity, propertyName)
            }
        }
        val bindValues = insertBindValues + setBindValues
        addBindValues(bindValues)
        return query
    }

    /**
     * ## プレースホルダー生成
     * ### Upsert 対象行数に応じて VALUES 句のプレースホルダーを生成する
     * @param rowCount 行数
     * @param columnCount カラム数
     * @return 生成されたプレースホルダー文字列
     * @author Masahiro Inoue
     * @since 2026-05-24
     */
    @TraceLog
    private fun placeholders(
        rowCount: Int,
        columnCount: Int,
    ): String {
        val oneRow = List(columnCount) { "?" }.joinToString(", ", "(", ")")
        return List(rowCount) { oneRow }.joinToString(", ")
    }

    /**
     * ## 値書式設定
     * ### SET 句の右辺に指定された内容の書式を成形する
     * @param valueHolder 保存領域
     * @param value 右辺式
     * @return 整形した文字列
     * @author Masahiro Inoue
     * @since 2026-06-27
     */
    @Suppress("UNCHECKED_CAST")
    private fun formatSetValue(
        valueHolder: QueryWithBindValues,
        value: Any,
    ): String =
        when (value) {
            is KProperty1<*, *> -> {
                entityClass.getColumnName(value.name)
            }

            else -> {
                EntityManager.formatValue(valueHolder, value)
            }
        }

    /**
     * ## 衝突条件の生成クラス
     * @author Masahiro Inoue
     * @since 2026-06-27
     */
    inner class ConflictClauseBuilder internal constructor() {
        private val columns = mutableListOf<String>()

        /**
         * ## 衝突判定プロパティ指定
         * ### 衝突判定に使用するプロパティを指定する
         * @param property 衝突判定に使用するプロパティ
         * @author Masahiro Inoue
         * @since 2026-06-27
         */
        fun key(property: KProperty1<T, *>) {
            column(property)
        }

        /**
         * ## 衝突判定プロパティ指定
         * ### 衝突判定に使用するプロパティを指定する
         * @param property 衝突判定に使用するプロパティ
         * @author Masahiro Inoue
         * @since 2026-06-27
         */
        fun column(property: KProperty1<T, *>) {
            columns += entityClass.getColumnName(property.name)
        }

        /**
         * ## 衝突判定プロパティリスト取得
         * @author Masahiro Inoue
         * @since 2026-06-27
         */
        fun buildList(): List<String> = columns
    }
}