package jp.pgw.lab78.shared.library

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * 共有ライブラリで利用する共通ユーティリティ。
 * @author Masahiro Inoue
 * @since 2024-10-19
 */
object Utils {
    /**
     * ## Null 判定拡張メソッド
     * ### オブジェクトが Null かどうかを判定する拡張メソッド
     * @receiver 判定対象のオブジェクト
     * @return Null 判定結果
     * @author Masahiro Inoue
     * @since 2024-10-19
     */
    @OptIn(ExperimentalContracts::class)
    fun Any?.isNull(): Boolean {
        contract {
            returns(true) implies (this@isNull == null)
            returns(false) implies (this@isNull != null)
        }
        return this == null
    }

    /**
     * ## Not Null 判定拡張メソッド
     * ### オブジェクトが Not Null かどうかを判定する拡張メソッド
     * @receiver 判定対象のオブジェクト
     * @return Not Null 判定結果
     * @author Masahiro Inoue
     * @since 2024-10-19
     */
    @OptIn(ExperimentalContracts::class)
    fun Any?.isNotNull(): Boolean {
        contract {
            returns(true) implies (this@isNotNull != null)
            returns(false) implies (this@isNotNull == null)
        }
        return !this.isNull()
    }

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