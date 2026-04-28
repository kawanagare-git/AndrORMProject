package jp.pgw.lab78.androrm.common.meta

import jp.pgw.lab78.androrm.common.database.function.ColumnFunction

/**
 * ## プロパティメタ情報
 * ### Entity を構成する 1 プロパティ分の正規化済みメタ情報
 * ### Select / Delete / Update / Upsert などから共通利用する
 *
 * @property propertyName Kotlin 上のプロパティ名
 * @property columnName DB 上のカラム名
 * @property aliasName 出力時のエイリアス名
 * @property isFunction 関数列かどうか
 * @property hideFromSelect SELECT 句から除外するかどうか
 * @property hasColumnAnnotation 元定義に @Column が付いていたかどうか
 * @property hasFunctionAnnotation 元定義に @Function が付いていたかどうか
 * @property functionType 関数列の場合の関数種別
 * @property functionArgs 関数列の場合の引数一覧
 * @property rawFunction 関数列の場合の raw SQL
 * @author Masahiro Inoue
 * @since 2026-04-28
 */
data class PropertyMeta(
    /** Kotlin 上のプロパティ名 */
    val propertyName: String,
    /** DB 上のカラム名 */
    val columnName: String,
    /** 出力時のエイリアス名 */
    val aliasName: String,
    /** 関数列かどうか */
    val isFunction: Boolean,
    /** SELECT 句から除外するかどうか */
    val hideFromSelect: Boolean,
    /** 元定義に @Column が付いていたかどうか */
    val hasColumnAnnotation: Boolean,
    /** 元定義に @Function が付いていたかどうか */
    val hasFunctionAnnotation: Boolean,
    /** 関数列の場合の関数種別 */
    val functionType: ColumnFunction? = null,
    /** 関数列の場合の引数一覧 */
    val functionArgs: List<String> = emptyList(),
    /** 関数列の場合の raw SQL */
    val rawFunction: String = "",
)
