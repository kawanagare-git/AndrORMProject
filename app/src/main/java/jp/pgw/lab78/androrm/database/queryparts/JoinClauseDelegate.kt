package jp.pgw.lab78.androrm.database.queryparts

import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableAlias
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableName
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.database.condition.ConditionBuilder
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.reference.TableRef
import kotlin.reflect.KClass

/**
 * ## JOIN 句生成委譲クラス
 * ### Select / Update で共通利用する JOIN 句を管理する
 *
 * @param owner join 呼び出し後に返す所有クラス
 * @param onChanged JOIN 条件変更時の処理
 * @param onTableJoined JOIN 対象テーブル登録時の処理
 *
 * @author Masahiro Inoue
 * @since 2026-05-24
 */
class JoinClauseDelegate<O, E : Entity>(
    private val owner: O,
    private val onChanged: () -> Unit = {},
    private val onTableJoined: (TableRef<out E>) -> Unit = {},
) {
    /** JOIN 句リスト */
    private val joinClauses = mutableListOf<String>()

    /** JOIN 句用バインド値リスト */
    private val joinBindValues = mutableListOf<Any?>()

    /** JOIN 句 */
    val clauses: List<String>
        get() = joinClauses

    /** JOIN 用バインド値 */
    val bindValues: List<Any?>
        get() = joinBindValues

    /**
     * ## join メソッド
     * ### Entity クラスを指定して JOIN 句を追加する
     */
    fun join(
        joinType: JoinType,
        joinedEntity: KClass<out E>,
        on: ConditionBuilder.() -> Unit,
    ): O =
        join(
            joinType = joinType,
            joinedTable = TableRef(
                entityClass = joinedEntity,
                alias = joinedEntity.getTableAlias(),
            ),
            on = on,
        )

    /**
     * ## join メソッド
     * ### TableRef を指定して JOIN 句を追加する
     */
    fun join(
        joinType: JoinType,
        joinedTable: TableRef<out E>,
        on: ConditionBuilder.() -> Unit,
    ): O {
        onChanged()
        onTableJoined(joinedTable)

        val joinedTableName = joinedTable.entityClass.getTableName()
        val joinedTableAlias = joinedTable.alias

        val valueHolder = object : QueryWithBindValues() {}
        val joinConditions = ConditionBuilder(valueHolder).apply(on).buildList()

        joinBindValues.addAll(valueHolder.bindValues)

        joinClauses += "${joinType.sql} join $joinedTableName $joinedTableAlias" +
                " on ${joinConditions.joinToString(" AND ") { it.build() }}"

        return owner
    }

    /**
     * ## join メソッド
     * ### on を後続指定するための中間オブジェクトを返す
     */
    fun join(
        joinType: JoinType,
        joinedEntity: KClass<out E>,
    ) =
        JoinCondition(
            joinType = joinType,
            joinedTable = TableRef(
                entityClass = joinedEntity,
                alias = joinedEntity.getTableAlias(),
            ),
        )

    /**
     * ## join メソッド
     * ### on を後続指定するための中間オブジェクトを返す
     */
    fun join(
        joinType: JoinType,
        joinedTable: TableRef<out E>,
    ) =
        JoinCondition(
            joinType = joinType,
            joinedTable = joinedTable,
        )

    /**
     * ## JOIN 条件指定用中間クラス
     */
    inner class JoinCondition internal constructor(
        private val joinType: JoinType,
        private val joinedTable: TableRef<out E>,
    ) {
        /**
         * ## on メソッド
         * ### JOIN 条件を指定する
         */
        fun on(block: ConditionBuilder.() -> Unit): O =
            this@JoinClauseDelegate.join(
                joinType = joinType,
                joinedTable = joinedTable,
                on = block,
            )
    }

    /**
     * ## JOIN 句生成
     * ### JOIN 句がない場合は空文字を返す
     */
    fun buildClause(): String =
        joinClauses.joinToString(" ")
}

/**
 * ## JOIN 種別
 * ### Select / Update で共通利用する JOIN 種別
 *
 * @author Masahiro Inoue
 * @since 2026-05-24
 */
enum class JoinType(val sql: String, val nullableByJoin: Boolean = false) {
    INNER("inner"),
    LEFT("left", true),
    CROSS("cross"),
    NATURAL("natural"),
    // RIGHT("right", true),
    // FULL("full", true)
}