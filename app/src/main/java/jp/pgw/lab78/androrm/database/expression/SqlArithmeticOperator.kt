package jp.pgw.lab78.androrm.database.expression

/**
 * ## SQL 算術演算子
 * ### SQL 式で使用する算術演算子を定義する
 *
 * ### 仕様
 * #### 二項 SQL 式で利用できる算術演算子を、SQL に出力する記号と対応付ける。
 * @param symbol SQL 上の演算子文字列
 * @author Masahiro Inoue
 * @since 2026-06-19
 */
enum class SqlArithmeticOperator(
    val symbol: String,
) {
    ADD("+"),
    SUBTRACT("-"),
    MULTIPLY("*"),
    DIVIDE("/"),
}