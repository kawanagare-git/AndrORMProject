package jp.pgw.lab78.androrm.database.condition.interfaces

import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.logging.aop.InfoLog
import jp.pgw.lab78.androrm.common.logging.aop.TraceLog
import jp.pgw.lab78.androrm.database.condition.ConditionBuilder
import jp.pgw.lab78.androrm.database.condition.HavingConditionBuilder
import jp.pgw.lab78.androrm.database.interfaces.JoinConditionModel
import jp.pgw.lab78.androrm.database.queryparts.JoinType
import jp.pgw.lab78.androrm.database.reference.TableRef

/**
 * ## select 基底クラス
 * ### 通常の SELECT だけではなくサブクエリにも対応できるようにするため必要最低限のメソッド・変数を定義
 * @author Masahiro Inoue
 * @since 2026-07-10
 */
interface SelectBody<T : SelectEntity, R : SelectBody<T, R>> : QueryStructureLike {
    /**
     * ## join メソッド
     * ### テーブル結合を指定する
     * @param joinType 結合方法（LEFT RIGHT CROSS等）を指定
     * @param joinedTable 結合するエンティティクラス（副クラス）
     * @param on 条件を構築するための DSL ブロック。`ConditionBuilder` の拡張ラムダとして記述。
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    @InfoLog
    @TraceLog
    fun join(
        joinType: JoinType,
        joinedTable: TableRef<out SelectEntity>,
        on: ConditionBuilder.() -> Unit,
    ): R

    /**
     * ## join メソッド
     * ### テーブル結合を指定する
     * @param joinType 結合方法（LEFT RIGHT CROSS等）を指定
     * @param joinedTable 結合するエンティティクラス（副クラス）
     * @author Masahiro Inoue
     * @since 2026-07-10
     * @since 2026-05-12
     */
    @InfoLog
    fun join(
        joinType: JoinType,
        joinedTable: TableRef<out SelectEntity>,
    ): JoinConditionModel

    /**
     * ## where メソッド
     * ### テーブル検索条件を指定する
     * @param block 条件を構築するための DSL ブロック。`ConditionBuilder` の拡張ラムダとして記述。
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    @InfoLog
    fun where(block: ConditionBuilder.() -> Unit): R

    /**
     * ## having メソッド
     * ### 集計結果検索条件を指定する
     * @param block 条件を構築するための DSL ブロック。`HavingBuilder` の拡張ラムダとして記述。
     * @return 自身のインスタンス(this)
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    @InfoLog
    fun having(block: HavingConditionBuilder.() -> Unit): R

    /**
     * ## SELECT 文文字列生成関数
     * ### 最終的な Select 文を生成する
     * @return  生成された SQL 文字列
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    @InfoLog
    override fun build(): String
}
