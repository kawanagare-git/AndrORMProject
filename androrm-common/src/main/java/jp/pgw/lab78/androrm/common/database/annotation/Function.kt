package jp.pgw.lab78.androrm.common.database.annotation

import jp.pgw.lab78.androrm.common.database.function.ColumnFunction

/**
 * ## AndrORM 関数アノテーションクラス
 * ### 自動生成されたエンティティクラスに付与するアノテーション
 * @param columnFunction 関数名（Enum 型）
 * @param alias エイリアス
 * @param args 関数の引数リスト
 * @param raw 関数を生文字列で生成する場合に使用（優先定義）
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
annotation class Function(
    val columnFunction: ColumnFunction,
    val alias: String,
    val args: Array<String> = [],
    val raw: String = "",
    val hideFromSelect: Boolean = false,
)
