package jp.pgw.lab78.shared.library

object Utils {
    /**
     * ## Null 判定拡張メソッド
     * ### オブジェクトが Null かどうかを判定する拡張メソッド
     * @receiver 判定対象のオブジェクト
     * @return Null 判定結果
     * @author Masahiro Inoue
     * @since 2024-10-19
     */
    fun Any?.isNull(): Boolean = this == null

    /**
     * ## Not Null 判定拡張メソッド
     * ### オブジェクトが Not Null かどうかを判定する拡張メソッド
     * @receiver 判定対象のオブジェクト
     * @return Not Null 判定結果
     * @author Masahiro Inoue
     * @since 2024-10-19
     */
    fun Any?.isNotNull(): Boolean = !this.isNull()

    /**
     * ## Unit 判定拡張メソッド
     * ### オブジェクトが Unit かどうかを判定する拡張メソッド
     * @receiver 判定対象のオブジェクト
     * @return Unit 判定結果
     * @author Masahiro Inoue
     * @since 2026-05-07
     */
    fun Any?.isUnit(): Boolean = this == Unit

    /**
     * ## Void 判定拡張メソッド
     * ### オブジェクト型が Void かどうかを判定する拡張メソッド
     * @receiver 判定対象のオブジェクト型
     * @return Void 判定結果
     * @author Masahiro Inoue
     * @since 2026-05-07
     */
    fun Class<*>.isVoid(): Boolean = this == Void.TYPE
}