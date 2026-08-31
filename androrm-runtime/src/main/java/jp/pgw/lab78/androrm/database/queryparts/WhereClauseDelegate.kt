package jp.pgw.lab78.androrm.database.queryparts

import jp.pgw.lab78.androrm.common.Constants.LogicalOperator.AND
import jp.pgw.lab78.androrm.common.MessageConstants.AE00010
import jp.pgw.lab78.androrm.database.condition.ConditionBuilder
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.condition.sealed.Condition

/**
 * ## WHERE 句生成委譲クラス
 * ### Select / Delete などで共通利用する WHERE 条件を管理する
 *
 * ### 仕様
 * #### DSL で構築した条件とバインド値を保持し、条件が存在する場合だけ WHERE 句を生成する。
 * #### 条件の指定時には所有オブジェクトへ変更を通知する。
 *
 * @param owner where 呼び出し後に返す所有クラス
 * @param ownerName 所有クラス名
 * @param enableAlias エイリアス有効 規定値：有効
 * @param onChanged WHERE 条件変更時の処理
 * @author Masahiro Inoue
 * @since 2026-05-24
 */
class WhereClauseDelegate<O>(
    private val owner: O,
    private val ownerName: String,
    private var enableAlias: Boolean = true,
    private val valueHolderFactory: () -> QueryWithBindValues = {
        object : QueryWithBindValues() {}
    },
    private val onChanged: () -> Unit = {},
) {
    /** WHERE 条件リスト */
    private val whereConditions = mutableListOf<Condition>()

    /** WHERE 句用バインド値リスト */
    private val whereBindValues = mutableListOf<Any?>()

    /** WHERE 指定済みフラグ */
    private var isSpecified = false

    /** WHERE 条件 */
    val conditions: List<Condition>
        get() = whereConditions

    /** WHERE 用バインド値 */
    val bindValues: List<Any?>
        get() = whereBindValues

    /** WHERE 条件有無 */
    val hasCondition: Boolean
        get() = whereConditions.isNotEmpty()

    /**
     * ## where メソッド
     * ### WHERE 条件を指定する
     *
     * @param block 条件を構築するための DSL ブロック
     * @return 所有クラス
     * @author Masahiro Inoue
     * @since 2026-05-24
     */
    fun where(block: ConditionBuilder.() -> Unit): O {
        check(!isSpecified) {
            AE00010.format(ownerName, "where")
        }

        onChanged()

        val valueHolder = valueHolderFactory()
        val builder = ConditionBuilder(valueHolder, enableAlias).apply(block)

        whereConditions += builder.buildList()
        whereBindValues.addAll(valueHolder.bindValues)
        isSpecified = true

        return owner
    }

    /**
     * ## WHERE 条件文字列生成
     * ### `where` は含めず、条件部分のみ生成する
     * @return WHEREキーワードを含まない条件文字列
     * @author Masahiro Inoue
     * @since 2026-05-24
     */
    fun buildCondition(): String =
        whereConditions.joinToString(AND.query) { it.build() }

    /**
     * ## WHERE 句生成
     * ### 条件がない場合は空文字を返す
     * @return 生成されたWHERE句
     * @author Masahiro Inoue
     * @since 2026-05-24
     */
    fun buildClause(): String =
        if (whereConditions.isEmpty()) {
            ""
        } else {
            "where ${buildCondition()}"
        }
}