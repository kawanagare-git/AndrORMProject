package jp.pgw.lab78.androrm.common.annotation

import jp.pgw.lab78.androrm.common.database.function.ColumnFunction

/**
 * ## AndrORM 関数プロジェクションアノテーションクラス
 * ### このアノテーションに指定された関数アノテーションを生成するためのアノテーション
 * @param function 関数名
 * @param args 引数
 * @param alias エイリアス
 * @param returnHint 戻り値の型推論 : 推論不能なときだけ任意指定
 * @param hideFromSelect SELECT 句の抽出カラムに含めない（条件にだけ使用する）
 * @param raw 関数定義の直書き
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
annotation class FunctionProjection(
    val function: ColumnFunction,
    val args: Array<String>,
    val alias: String,
    val returnHint: ReturnHint = ReturnHint.AUTO,
    val hideFromSelect: Boolean = false,
    val raw: String = ""
)

enum class ReturnHint {
    AUTO, STRING, INT, LONG, DOUBLE, BOOLEAN, DECIMAL, DATE, DATETIME
}
