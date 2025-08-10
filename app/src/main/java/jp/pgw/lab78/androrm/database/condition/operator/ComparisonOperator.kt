package jp.pgw.lab78.androrm.database.condition.operator

/**
 * ## 比較演算子列挙型
 * ### join や where 等で条件に使用する比較演算子定義
 * @param symbol 比較演算子を表す文字列
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
enum class ComparisonOperator(val symbol: String) {
    EQUALS("="),
    EQ("="),
    NOT_EQUALS("!="),
    NE("!="),
    GREATER_THAN(">"),
    GT(">"),
    GREATER_THAN_OR_EQUALS(">="),
    GE(">="),
    LESS_THAN("<"),
    LT("<"),
    LESS_THAN_OR_EQUALS("<="),
    LE("<="),
    LIKE("LIKE"),
    NOT_LIKE("NOT LIKE"),
    IN("IN"),
    NOT_IN("NOT IN"),
    BETWEEN("BETWEEN"),
    EXISTS("EXISTS"),
    NOT_EXISTS("NOT EXISTS"),
    IS_NULL("IS NULL"),
    IS_NOT_NULL("IS NOT NULL"),
}
