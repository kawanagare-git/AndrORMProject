package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.Constants
import jp.pgw.lab78.androrm.common.MessageConstants
import jp.pgw.lab78.androrm.common.database.SupportFunction
import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumn
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableAlias
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableName
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.common.dml.interfaces.UpdateEntity
import jp.pgw.lab78.androrm.database.condition.ConditionBuilder
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.interfaces.QueryBuilderLike
import jp.pgw.lab78.androrm.database.queryparts.JoinClauseDelegate
import jp.pgw.lab78.androrm.database.queryparts.JoinType
import jp.pgw.lab78.androrm.database.queryparts.WhereClauseDelegate
import jp.pgw.lab78.androrm.database.reference.TableRef
import jp.pgw.lab78.androrm.database.utility.EntityManager
import jp.pgw.lab78.androrm.database.utility.EntityManager.getDmlTargets
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * ## Update 文生成クラス
 * ### update 文を生成します
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

    /** クエリ */
    private lateinit var query: String

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
     *
     * @param block SET 句定義
     * @return 自身のインスタンス
     */
    fun set(block: SetClauseBuilder.() -> Unit): Update<T> {
        isBuild = false
        setAssignments.clear()
        setBindValues.clear()
        val valueHolder = object : QueryWithBindValues() {}
        val builder = SetClauseBuilder(valueHolder).apply(block)
        setAssignments.addAll(builder.buildList())
        setBindValues.addAll(valueHolder.bindValues)
        return this
    }

    /**
     * ## from メソッド
     * ### UPDATE FROM の FROM 句を指定する
     *
     * @param fromEntity 参照元 Entity クラス
     * @return 自身のインスタンス
     */
    fun <E : Entity> from(fromEntity: KClass<E>): Update<T> =
        from(TableRef(entityClass = fromEntity, alias = fromEntity.getTableAlias()))

    /**
     * ## from メソッド
     * ### UPDATE FROM の FROM 句を指定する
     * @param fromTable 参照元テーブル
     * @return 自身のインスタンス
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
     */
    fun join(
        joinType: JoinType,
        joinedEntity: KClass<out Entity>,
        on: ConditionBuilder.() -> Unit,
    ): Update<T> = joinDelegate.join(joinType = joinType, joinedEntity = joinedEntity, on = on)

    /**
     * ## join メソッド
     * ### UPDATE FROM の JOIN 句を指定する
     */
    fun join(
        joinType: JoinType,
        joinedTable: TableRef<out Entity>,
        on: ConditionBuilder.() -> Unit,
    ): Update<T> = joinDelegate.join(joinType = joinType, joinedTable = joinedTable, on = on)

    /**
     * ## join メソッド
     * ### on を後続指定するための中間オブジェクトを返す
     */
    fun join(
        joinType: JoinType,
        joinedEntity: KClass<out Entity>,
    ) = joinDelegate.join(joinType = joinType, joinedEntity = joinedEntity)

    /**
     * ## join メソッド
     * ### on を後続指定するための中間オブジェクトを返す
     */
    fun join(
        joinType: JoinType,
        joinedTable: TableRef<out Entity>,
    ) = joinDelegate.join(joinType = joinType, joinedTable = joinedTable)

    /**
     * ## where
     * ### 更新対象条件を指定する
     */
    fun where(block: ConditionBuilder.() -> Unit): Update<T> =
        whereDelegate.where(block)

    /**
     * ## 全件更新を明示的に許可する
     */
    fun updateAll(): Update<T> {
        isBuild = true
        require(whereDelegate.hasCondition) { MessageConstants.AE00013 }
        return this
    }

    /**
     * ## 追加バインド値取得
     * ### SQL 句の出現順に合わせて bindValues を合成する
     */
    protected override fun additionalBindValues(): List<Any?> = buildList {
        addAll(joinDelegate.bindValues)
        addAll(whereDelegate.bindValues)
    }

    /**
     * ## update 文生成
     * ### SQLite 用 UPDATE 文を生成する
     */
    override fun build(): String {
        if (!isBuild) {
            require(setAssignments.isNotEmpty()) { MessageConstants.AE00014 }
            require(whereDelegate.hasCondition) { MessageConstants.AE00013 }
            clearBindValues()
            addBindValues(setBindValues)
            val targetTableExpression =
                if (tableAlias.isBlank()) {
                    tableName
                } else {
                    "$tableName as $tableAlias"
                }
            val setClause = setAssignments.joinToString(", ") { (columnName, expression) ->
                "$columnName = $expression"
            }
            query = buildString {
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
                val whereClause = whereDelegate.buildClause()
                if (whereClause.isNotBlank()) {
                    append(" ")
                    append(whereClause)
                }
            }.replace(DmlConstant.MULTI_SPACE_REGEX, Constants.SPACE)
                .trim()
            isBuild = true
        }
        return query
    }

    /**
     * ## テーブルエイリアス登録
     * ### 同一 Update 内で同じ alias が再利用されないよう検証する
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

    /**
     * ## SET 句ビルダー
     * ### Update.set DSL で使用する
     */
    inner class SetClauseBuilder internal constructor(
        private val valueHolder: QueryWithBindValues,
    ) {
        /** SET 句リスト */
        private val assignments = mutableListOf<Pair<String, String>>()

        /**
         * ## setTo メソッド
         * ### 左辺プロパティに右辺値を設定する
         *
         * 左辺は SQLite の仕様に合わせて alias を付けず、カラム名のみを出力する。
         */
        infix fun KProperty1<T, *>.setTo(value: Any?) {
            assignments += this.getColumn() to formatSetValue(value)
        }

        /**
         * ## assign メソッド
         * ### setTo の別名
         */
        infix fun KProperty1<T, *>.assign(value: Any?) {
            this setTo value
        }

        /**
         * ## SET 句リスト生成
         */
        fun buildList(): List<Pair<String, String>> = assignments

        /**
         * ## SET 値式生成
         * ### ColumnRef なら alias.column、それ以外は bind 値として扱う
         */
        private fun formatSetValue(value: Any?): String {
            if (value == null) {
                valueHolder.addBindValue(null)
                return "?"
            }

            return EntityManager.formatValue(valueHolder, value)
        }
    }
}