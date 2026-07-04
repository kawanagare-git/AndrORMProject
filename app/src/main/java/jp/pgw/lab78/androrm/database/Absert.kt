package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.MessageConstants.AE00016
import jp.pgw.lab78.androrm.common.MessageConstants.AE00017
import jp.pgw.lab78.androrm.common.database.SupportFunction.getPropertyValue
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableName
import jp.pgw.lab78.androrm.common.dml.interfaces.AbsertEntity
import jp.pgw.lab78.androrm.common.logging.aop.TraceLog
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.interfaces.QueryBuilderLike
import jp.pgw.lab78.androrm.database.queryparts.OnConflictClause
import jp.pgw.lab78.androrm.database.queryparts.OnConflictClauseBuilder
import jp.pgw.lab78.androrm.database.queryparts.OnConflictClauseDelegate
import jp.pgw.lab78.androrm.database.utility.EntityManager.getDmlTargets
import kotlin.reflect.KClass

/**
 * ## Absert 文生成クラス
 * ### SQLite の INSERT ... ON CONFLICT ... DO NOTHING 文を生成します
 *
 * @param entityClass Absert 対象 Entity クラス
 * @author Masahiro Inoue
 * @since 2026-07-02
 */
class Absert<T : AbsertEntity>(
    private val entityClass: KClass<out T>,
) : QueryBuilderLike<T>, QueryWithBindValues(), OnConflictClause<T, Absert<T>> {

    /** テーブル名 */
    private val tableName = entityClass.getTableName()

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

    /** Absert 対象 Entity 一覧 */
    private var entities: MutableList<T> = mutableListOf()

    /** 衝突判定句委譲 */
    private val conflictDelegate = OnConflictClauseDelegate(
        owner = this, entityClass = entityClass, onChanged = { isBuild = false },
    )

    /** ビルドフラグ */
    private var isBuild: Boolean = false

    /** クエリ格納 */
    private lateinit var query: String

    /**
     * ## エンティティ追加
     * ### Absert 対象 Entity を1件追加する
     * @param entity 追加する Entity
     * @author Masahiro Inoue
     * @since 2026-07-02
     */
    fun addEntity(entity: T): Absert<T> {
        addEntities(entity)
        return this
    }

    /**
     * ## エンティティ追加
     * ### Absert 対象 Entity を複数件追加する
     * @param entities 追加する Entity リスト
     * @author Masahiro Inoue
     * @since 2026-07-02
     */
    fun addEntities(vararg entities: T): Absert<T> {
        addEntities(entities.asList())
        return this
    }

    /**
     * ## エンティティ追加
     * ### Absert 対象 Entity を複数件追加する
     * @param entities 追加する Entity リスト
     * @author Masahiro Inoue
     * @since 2026-07-02
     */
    fun addEntities(entities: List<T>): Absert<T> {
        isBuild = false
        this.entities.addAll(entities)
        return this
    }

    /**
     * ## 衝突判定カラム指定
     * ### ON CONFLICT に指定するカラムを設定する
     * @param block 衝突判定に使用するプロパティを列挙するブロック
     * @return 自身のインスタンス
     * @author Masahiro Inoue
     * @since 2026-07-02
     */
    @TraceLog
    override fun onConflict(
        block: OnConflictClauseBuilder<T>.() -> Unit,
    ): Absert<T> = conflictDelegate.onConflict(block)

    /**
     * ## Absert 文生成
     * ### addEntity / addEntities で追加済みの Entity を基に Absert 文を生成する
     * @return 生成された Absert 文
     * @author Masahiro Inoue
     * @since 2026-07-02
     */
    @TraceLog
    override fun build(): String {
        require(entities.isNotEmpty()) { AE00016 }
        require(conflictDelegate.hasColumns) { AE00017 }
        this.entities = entities.toMutableList()
        clearBindValues()
        val valuesClause = placeholders(
            rowCount = entities.size,
            columnCount = columnList.size,
        )
        val conflictClause = conflictDelegate.buildClause()
        query = if (isBuild) {
            query
        } else {
            isBuild = true
            "insert into $tableName $columnDefine values$valuesClause " +
                    "$conflictClause do nothing"
        }
        val bindValues = entities.flatMap { entity ->
            columnList.map { (propertyName, _) ->
                getPropertyValue(entity, propertyName)
            }
        }
        addBindValues(bindValues)
        return query
    }

    /**
     * ## プレースホルダー生成
     * ### Absert 対象行数に応じて VALUES 句のプレースホルダーを生成する
     * @param rowCount 行数
     * @param columnCount カラム数
     * @return 生成されたプレースホルダー文字列
     * @author Masahiro Inoue
     * @since 2026-07-02
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