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
}