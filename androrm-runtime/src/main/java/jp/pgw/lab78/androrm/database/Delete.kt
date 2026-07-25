package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.Constants.SPACE
import jp.pgw.lab78.androrm.common.MessageConstants
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableName
import jp.pgw.lab78.androrm.common.dml.interfaces.DeleteEntity
import jp.pgw.lab78.androrm.common.logging.LogLevel.TRACE
import jp.pgw.lab78.androrm.common.logging.LogScope.APP
import jp.pgw.lab78.androrm.database.condition.ConditionBuilder
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.interfaces.QueryBuilderLike
import jp.pgw.lab78.androrm.database.queryparts.WhereClauseDelegate
import java.util.logging.Logger
import kotlin.reflect.KClass

/**
 * ## Delete 文生成クラス
 * ### 仕様
 * #### Entity のテーブルに対する DELETE 文を生成し、`where` で指定した条件値をバインド値として保持する。
 * #### 全件削除は `deleteAll` による明示的な許可を必要とし、条件未指定の誤操作を防止する。
 * @author Masahiro Inoue
 * @since 2026-05-24
 */
class Delete<T : DeleteEntity>(
    private val entityClass: KClass<out T>
) : QueryBuilderLike<T>, QueryWithBindValues() {
    /** ログ出力移譲 */
    private val logger: Logger by lazy { APP.create(minLogLevel = TRACE) }

    /** テーブル名：クラス名をテーブル名（大文字）に変換 */
    val tableName = entityClass.getTableName()

    /** 全件対象 */
    private var isAllRecords: Boolean = false

    /** ビルドフラグ */
    private var isBuild = false

    /** WHERE 句生成委譲 */
    private val whereDelegate =
        WhereClauseDelegate<Delete<T>>(
            owner = this,
            ownerName = this.javaClass.simpleName,
            enableAlias = false
        ) {
            isBuild = false
        }

    /**
     * DELETE文へ適用する検索条件を設定する。
     *
     * @param block 検索条件を構築する処理
     * @return 自身のインスタンス
     * @author Masahiro Inoue
     * @since 2026-05-24
     */
    fun where(block: ConditionBuilder.() -> Unit): Delete<T> =
        whereDelegate.where(block).also { isAllRecords = false }

    /**
     * WHERE句で保持されたバインド値を返す。
     *
     * @return WHERE句のバインド値
     * @author Masahiro Inoue
     * @since 2026-05-24
     */
    override fun additionalBindValues(): List<Any?> =
        if (isAllRecords) {
            emptyList()
        } else {
            whereDelegate.bindValues
        }

    /**
     * 条件を指定しない全件削除を明示的に許可する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-24
     */
    /**
     * ## 全件削除指定
     * ### WHERE条件を使用せず、全レコードを削除対象とする
     */
    fun deleteAll(): Delete<T> {
        isAllRecords = true
        // 更新対象範囲が変更されたため、生成済みSQLを無効化する
        isBuild = false
        return this
    }

    /**
     * ## 生成メソッド
     * ### クエリを生成するときに呼び出す
     * @return 生成されたクエリ文字列
     * @author Masahiro Inoue
     * @since 2026-05-24
     */
    override fun build(): String {
        return if (isBuild) {
            query
        } else {
            require(isAllRecords || whereDelegate.hasCondition) {
                MessageConstants.AE00038
            }
            clearBindValues()
            "delete from $tableName ${
                if (isAllRecords) EMPTY_STRING else whereDelegate.buildClause()
            }"
                .replace(DmlConstant.MULTI_SPACE_REGEX, SPACE)
                .trim()
                .also {
                    // queryを完成させてからビルド済みにする
                    query = it
                    isBuild = true
                }
        }
    }
}