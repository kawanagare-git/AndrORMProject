package jp.pgw.lab78.androrm.common.database.annotation

import jp.pgw.lab78.androrm.common.database.function.ColumnFunction

/**
 * ## AndrORM 関数アノテーションクラス
 * ### 自動生成されたエンティティクラスに付与するアノテーション
 * @param columnExpression 関数名（生成される関数の情報を格納）
 * @param alias エイリアス
 */
annotation class Function(
    val columnExpression: ColumnFunction,
    val alias: String
)
