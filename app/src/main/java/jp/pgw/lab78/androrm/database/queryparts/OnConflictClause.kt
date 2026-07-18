package jp.pgw.lab78.androrm.database.queryparts

import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumnName
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * ## ON CONFLICT 句インターフェース
 * ### Upsert / Absert で共通利用する
 *
 * @param T 対象 Entity 型
 * @param O owner 型
 * @author Masahiro Inoue
 * @since 2026-07-02
 */
interface OnConflictClause<T : Entity, O> {
    /**
     * ## 衝突判定カラム指定
     * ### ON CONFLICT に指定するカラムを設定する
     * @author Masahiro Inoue
     * @since 2026-07-02
     */
    fun onConflict(block: OnConflictClauseBuilder<T>.() -> Unit): O
}

/**
 * ## ON CONFLICT 句委譲クラス
 * ### 衝突判定カラムを管理する
 *
 * ### 仕様
 * #### DSL で指定された Entity プロパティをカラム名へ変換し、ON CONFLICT 句の指定順を保持する。
 * #### 再指定時は以前のカラムを置き換え、所有オブジェクトへ変更を通知する。
 *
 * @param owner onConflict 呼び出し後に返す所有クラス
 * @param entityClass 対象 Entity クラス
 * @param onChanged ON CONFLICT 条件変更時の処理
 * @author Masahiro Inoue
 * @since 2026-07-02
 */
class OnConflictClauseDelegate<O, T : Entity>(
    private val owner: O,
    private val entityClass: KClass<out T>,
    private val onChanged: () -> Unit = {},
) : OnConflictClause<T, O> {

    /** 衝突判定カラム */
    private val conflictColumns = mutableListOf<String>()

    /** 衝突判定カラム有無 */
    val hasColumns: Boolean
        get() = conflictColumns.isNotEmpty()

    /**
     * ## 衝突判定カラム指定
     * ### ON CONFLICT に指定するカラムを設定する
     * @param block 衝突判定を列挙するブロック
     * @return オーナー型のインスタンス
     * @author Masahiro Inoue
     * @since 2026-07-02
     */
    override fun onConflict(block: OnConflictClauseBuilder<T>.() -> Unit): O {
        onChanged()
        conflictColumns.clear()

        val builder = OnConflictClauseBuilder(entityClass).apply(block)
        conflictColumns.addAll(builder.buildList())

        return owner
    }

    /**
     * ## ON CONFLICT 句生成
     * ### `on conflict(...)` を生成する
     * @return 生成された ON CONFLICT 句
     * @author Masahiro Inoue
     * @since 2026-07-02
     */
    fun buildClause(): String =
        conflictColumns.joinToString(", ", "on conflict(", ")")
}

/**
 * ## ON CONFLICT 句ビルダー
 * ### 衝突判定に使用するプロパティを指定する
 *
 * ### 仕様
 * #### `key` と `column` で指定されたプロパティを対象 Entity のカラム名へ変換し、指定順の一覧として返す。
 *
 * @param entityClass 対象 Entity クラス
 * @author Masahiro Inoue
 * @since 2026-07-02
 */
class OnConflictClauseBuilder<T : Entity> internal constructor(
    private val entityClass: KClass<out T>,
) {
    /** 衝突判定カラム */
    private val columns = mutableListOf<String>()

    /**
     * ## 衝突判定プロパティ指定
     * ### column の別名
     * @param property 衝突判定に使用するプロパティ
     * @author Masahiro Inoue
     * @since 2026-07-02
     */
    fun key(property: KProperty1<T, *>) {
        column(property)
    }

    /**
     * ## 衝突判定プロパティ指定
     * @param property 衝突判定に使用するプロパティ
     * @author Masahiro Inoue
     * @since 2026-07-02
     */
    fun column(property: KProperty1<T, *>) {
        columns += entityClass.getColumnName(property.name)
    }

    /**
     * ## 衝突判定カラムリスト取得
     * @return 衝突判定に使用するカラムの一覧
     * @author Masahiro Inoue
     * @since 2026-07-02
     */
    fun buildList(): List<String> = columns
}