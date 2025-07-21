package jp.pgw.lab78.androrm.database.condition.operator

/**
 * ## 比較演算子列挙型
 * ### join や where 等で条件に使用する
 */
enum class ComparisonOperator(val symbol: String) {
    EQUALS("="),
    NOT_EQUALS("!="),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUALS(">="),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUALS("<="),
    LIKE("LIKE"),
    IN("IN"),
    BETWEEN("BETWEEN")
}
