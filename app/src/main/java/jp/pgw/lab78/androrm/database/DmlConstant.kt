package jp.pgw.lab78.androrm.database

/**
 * DML文の生成に使用する正規表現定数を提供する。
 *
 * @author Masahiro Inoue
 * @since 2026-01-12
 */
object DmlConstant {
    /** 正規表現：半角括弧置換用 */
    val ANY_OPEN_BRACKET_REGEX = Regex("[\\[{]")
    val ANY_CLOSE_BRACKET_REGEX = Regex("[}\\]]")
    val MULTI_SPACE_REGEX = Regex("\\s{2,}")
}