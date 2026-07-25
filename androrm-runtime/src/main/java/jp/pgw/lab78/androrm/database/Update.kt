package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.Constants
import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.MessageConstants
import jp.pgw.lab78.androrm.common.database.SupportFunction
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableAlias
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableName
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.common.dml.interfaces.UpdateEntity
import jp.pgw.lab78.androrm.database.condition.ConditionBuilder
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.interfaces.QueryBuilderLike
import jp.pgw.lab78.androrm.database.queryparts.JoinClauseDelegate
import jp.pgw.lab78.androrm.database.queryparts.JoinType
import jp.pgw.lab78.androrm.database.queryparts.SetClauseBuilder
import jp.pgw.lab78.androrm.database.queryparts.WhereClauseDelegate
import jp.pgw.lab78.androrm.database.reference.TableRef
import jp.pgw.lab78.androrm.database.utility.EntityManager
import jp.pgw.lab78.androrm.database.utility.EntityManager.getDmlTargets
import kotlin.reflect.KClass

/**
 * ## Update 文生成クラス
 * ### update 文を生成します
 *
 * ### 仕様
 * #### Entity または SET DSL から更新値を構築し、FROM、JOIN、WHERE を組み合わせた SQLite UPDATE 文を生成する。
 * #### バインド値は SET、JOIN、WHERE の順で保持し、テーブル別名の重複と句の重複指定を検証する。
 * #### WHERE のない更新は `updateAll` による明示的な許可を必要とする。
 * @param targetTable 更新対象テーブル参照
 * @author Masahiro Inoue
 * @since 2026-05-25
 */
class Update<T : UpdateEntity>(
    private val targetTable: TableRef<out T>,
) : QueryBuilderLike<T>, QueryWithBindValues() {
    /**
     * ## コンストラクタ
     * ### Entity クラスから Update 文生成クラスを生成する
     */
    constructor(
        targetEntity: KClass<out T>,
    ) : this(
        TableRef(
            entityClass = targetEntity,
            alias = targetEntity.getTableAlias(),
        )
    )

    /** 全件対象 */
    private var isAllRecords: Boolean = false

    /** テーブル名 */
    private val tableName = targetTable.entityClass.getTableName()

    /** テーブルエイリアス */
    private val tableAlias = targetTable.alias

    /** DML 用プロパティ名・カラム名リスト */
    private val dmlTargets = targetTable.entityClass.getDmlTargets()

    /** SET 句リスト */
    private val setAssignments = mutableListOf<Pair<String, String>>()

    /** SET 句用バインド値 */
    private val setBindValues = mutableListOf<Any?>()

    /** FROM 句 */
    private var fromClause: String = ""

    /** 使用済みテーブルエイリアス */
    private val usedTableAliases = mutableSetOf<String>()

    /** ビルド済みフラグ */
    private var isBuild = false

    /** WHERE 句生成委譲 */
    private val whereDelegate =
        WhereClauseDelegate<Update<T>>(owner = this, ownerName = this.javaClass.simpleName) {
            isBuild = false
        }

    /** JOIN 句生成委譲 */
    private val joinDelegate =
        JoinClauseDelegate<Update<T>, Entity>(
            owner = this, onChanged = { isBuild = false },
            onTableJoined = { joinedTable ->
                check(fromClause.isNotBlank()) { MessageConstants.AE00015 }
                registerTableAlias(
                    tableName = joinedTable.entityClass.getTableName(),
                    tableAlias = joinedTable.alias,
                )
            },
        )

    init {
        registerTableAlias(
            tableName = tableName,
            tableAlias = tableAlias,
        )
    }

    /**
     * ## set メソッド
     * ### Entity インスタンスを基に SET 句を指定する
     * @param entity 更新値を保持する Entity
     * @return 自身のインスタンス
     * @author Masahiro Inoue
     * @since 2026-05-25
     */
    fun set(entity: T): Update<T> {
        isBuild = false
        setAssignments.clear()
        setBindValues.clear()
        dmlTargets.forEach { (propertyName, columnName) ->
            setAssignments += columnName to "?"
            setBindValues += SupportFunction.getPropertyValue(entity, propertyName)
        }
        return this
    }

    /**
     * ## set メソッド
     * ### DSL で SET 句を指定する
     * @param block SET 句定義
     * @return 自身のインスタンス
     * @author Masahiro Inoue
     * @since 2026-05-25
     */
    fun set(block: SetClauseBuilder<T>.() -> Unit): Update<T> {
        isBuild = false
        setAssignments.clear()
        setBindValues.clear()
        // 値の保存
        val valueHolder = object : QueryWithBindValues() {}
        val builder =
            SetClauseBuilder(
                targetEntityClass = targetTable.entityClass,
                valueHolder = valueHolder,
                valueFormatter = EntityManager::formatValue,
            ).apply(block)
        setAssignments.addAll(builder.buildList())
        setBindValues.addAll(valueHolder.bindValues)
        return this
    }

    /**
     * ## from メソッド
     * ### UPDATE FROM の FROM 句を指定する
     * @param fromEntity 参照元 Entity クラス
     * @return 自身のインスタンス
     * @author Masahiro Inoue
     * @since 2026-05-25
     */
    fun <E : Entity> from(fromEntity: KClass<E>): Update<T> =
        from(TableRef(entityClass = fromEntity, alias = fromEntity.getTableAlias()))

    /**
     * ## from メソッド
     * ### UPDATE FROM の FROM 句を指定する
     * @param fromTable 参照元テーブル
     * @return 自身のインスタンス
     * @author Masahiro Inoue
     * @since 2026-05-25
     */
    fun from(fromTable: TableRef<out Entity>): Update<T> {
        check(fromClause.isBlank()) {
            MessageConstants.AE00010.format(this.javaClass.simpleName, "from")
        }
        isBuild = false
        val fromTableName = fromTable.entityClass.getTableName()
        val fromTableAlias = fromTable.alias
        registerTableAlias(
            tableName = fromTableName,
            tableAlias = fromTableAlias,
        )
        fromClause = "from $fromTableName $fromTableAlias"
        return this
    }

    /**
     * ## join メソッド
     * ### UPDATE FROM の JOIN 句を指定する
     * @param joinType 結合方法
     * @param joinedEntity 結合エンティティ
     * @param on 結合条件
     * @author Masahiro Inoue
     * @since 2026-05-25
     */
    fun join(
        joinType: JoinType,
        joinedEntity: KClass<out Entity>,
        on: ConditionBuilder.() -> Unit,
    ): Update<T> = joinDelegate.join(joinType = joinType, joinedEntity = joinedEntity, on = on)

    /**
     * ## join メソッド
     * ### UPDATE FROM の JOIN 句を指定する
     * @param joinType 結合方法
     * @param joinedTable 結合テーブル参照
     * @param on 結合条件
     * @author Masahiro Inoue
     * @since 2026-05-25
     */
    fun join(
        joinType: JoinType,
        joinedTable: TableRef<out Entity>,
        on: ConditionBuilder.() -> Unit,
    ): Update<T> = joinDelegate.join(joinType = joinType, joinedTable = joinedTable, on = on)

    /**
     * ## join メソッド
     * ### on を後続指定するための中間オブジェクトを返す
     * @param joinType 結合方法
     * @param joinedEntity 結合エンティティ
     * @author Masahiro Inoue
     * @since 2026-05-25
     */
    fun join(
        joinType: JoinType,
        joinedEntity: KClass<out Entity>,
    ) = joinDelegate.join(joinType = joinType, joinedEntity = joinedEntity)

    /**
     * ## join メソッド
     * ### on を後続指定するための中間オブジェクトを返す
     * @param joinType 結合方法
     * @param joinedTable 結合テーブル参照
     * @author Masahiro Inoue
     * @since 2026-05-25
     */
    fun join(
        joinType: JoinType,
        joinedTable: TableRef<out Entity>,
    ) = joinDelegate.join(joinType = joinType, joinedTable = joinedTable)

    /**
     * ## where
     * ### 更新対象条件を指定する
     * @param block 検索条件
     * @author Masahiro Inoue
     * @since 2026-05-25
     */
    fun where(block: ConditionBuilder.() -> Unit): Update<T> =
        whereDelegate.where(block).also { isAllRecords = false }

    /**
     * ## WHERE 条件指定の検証
     * ### WHERE 条件が指定済みであることを検証する
     * @return 自身のインスタンス
     * @throws IllegalArgumentException WHERE 条件が指定されていない場合
     * @author Masahiro Inoue
     * @since 2026-05-25
     */
    fun updateAll(): Update<T> {
        isAllRecords = true
        isBuild = false
        return this
    }

    /**
     * ## 追加バインド値取得
     * ### SQL 句の出現順に合わせて bindValues を合成する
     * @author Masahiro Inoue
     * @since 2026-05-25
     */
    override fun additionalBindValues(): List<Any?> =
        if (isAllRecords) {
            joinDelegate.bindValues
        } else {
            joinDelegate.bindValues + whereDelegate.bindValues
        }

    /**
     * ## update 文生成
     * ### SQLite 用 UPDATE 文を生成する
     * @author Masahiro Inoue
     * @since 2026-05-25
     */
    override fun build(): String {
        return if (isBuild) {
            query
        } else {
            require(setAssignments.isNotEmpty()) { MessageConstants.AE00014 }
            require(isAllRecords || whereDelegate.hasCondition) { MessageConstants.AE00013 }
            val targetTableExpression =
                if (tableAlias.isBlank()) {
                    tableName
                } else {
                    "$tableName as $tableAlias"
                }
            val setClause =
                setAssignments.joinToString(", ") { (columnName, expression) ->
                    "$columnName = $expression"
                }
            buildString {
                append("update ")
                append(targetTableExpression)
                append(" set ")
                append(setClause)
                if (fromClause.isNotBlank()) {
                    append(" ")
                    append(fromClause)
                }
                val joinClause = joinDelegate.buildClause()
                if (joinClause.isNotBlank()) {
                    append(" ")
                    append(joinClause)
                }
                append(" ")
                append(
                    if (isAllRecords) {
                        EMPTY_STRING
                    } else {
                        whereDelegate.buildClause()
                    }
                )
            }.replace(
                DmlConstant.MULTI_SPACE_REGEX,
                Constants.SPACE,
            ).trim()
        }.also {
            clearBindValues()
            addBindValues(setBindValues)
            // queryを完成させてからビルド済みにする
            isBuild = true
            query = it
        }
    }

    /**
     * ## テーブルエイリアス登録
     * ### 同一 Update 内で同じ alias が再利用されないよう検証する
     * @param tableName テーブル名
     * @param tableAlias テーブルエイリアス
     * @author Masahiro Inoue
     * @since 2026-05-25
     */
    private fun registerTableAlias(
        tableName: String,
        tableAlias: String,
    ) {
        require(usedTableAliases.add(tableAlias)) {
            MessageConstants.AE00010.format(
                this.javaClass.simpleName,
                "alias[$tableAlias:$tableName]"
            )
        }
    }
}