package jp.pgw.lab78.androrm.database.condition.interfaces

/**
 * ## バインド値条件生成インターフェース
 * ### バインド値を使用する条件生成のためのインターフェース
 * @author Masahiro Inoue
 * @since 2026-01-12
 */
abstract class QueryWithBindValues {
    /** バインド変数リスト（実体） */
    private val _bindValues: MutableList<Any?> = mutableListOf()
    val bindValues: List<Any?>
        get() = _bindValues + additionalBindValues()

    /**
     * ## 追加バインド値取得
     * ### 後置追加したいバインド値を返す
     * @return 追加バインド値のリスト
     * @author Masahiro Inoue
     * @since 2026-05-20
     */
    protected open fun additionalBindValues(): List<Any?> = emptyList()

    /**
     * ## バインド変数リスト（追加）
     * ### バインド変数と追加する
     * @param value 追加する値
     * @author Masahiro Inoue
     * @since 2026-01-12
     */
    internal fun addBindValue(value: Any?) {
        _bindValues += value
    }

    /**
     *  ## バインド変数リスト（データ群追加）
     * ### バインド変数の集合を追加する
     * @param values 追加する値
     * @author Masahiro Inoue
     * @since 2026-01-12
     */
    internal fun addBindValues(values: Iterator<Any?>) {
        values.forEach { value ->
            _bindValues += value
        }
    }

    /**
     * ## バインド変数リスト（クリア）
     * ### バインド変数を全て削除する
     * @author Masahiro Inoue
     * @since 2026-01-12
     */
    protected fun clearBindValues() {
        _bindValues.clear()
    }
}