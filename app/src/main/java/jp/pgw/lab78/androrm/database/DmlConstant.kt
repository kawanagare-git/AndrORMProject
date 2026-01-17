package jp.pgw.lab78.androrm.database

object DmlConstant {
    /** 正規表現：半角括弧置換用 */
    val ANY_OPEN_BRACKET_REGEX = Regex("[\\[{]")
    val ANY_CLOSE_BRACKET_REGEX = Regex("[}\\]]")
    val MULTI_SPACE_REGEX = Regex("\\s{2,}")
}