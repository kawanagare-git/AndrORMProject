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

    /** ## バインド変数リスト（取得） */
    val bindValues: List<Any?>
        get() = _bindValues

    /** ## バインド変数リスト（追加） */
    internal fun addBindValue(value: Any?) {
        _bindValues += value
    }

    /** ## バインド変数リスト（データ郡追加） */
    internal fun addBindValues(values: Iterator<Any?>) {
        _bindValues += values
    }

    /** ## バインド変数リスト（データ郡追加） */
    protected fun clearBindValues() {
        _bindValues.clear()
    }
}
