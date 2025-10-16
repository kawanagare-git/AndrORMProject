package jp.pgw.lab78.androrm.database.condition.operator

/**
 * ## 比較演算子列挙型
 * ### join や where 等で条件に使用する比較演算子定義
 * @param symbol 比較演算子を表す文字列
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
enum class ComparisonOperator(val symbol: String) {
    EQ("="),
    NE("<>"),
    GT(">"),
    GE(">="),
    LT("<"),
    LE("<="),
    LIKE("like"),
    NOT_LIKE("not like"),
    GLOB("glob"),
    NOT_GLOB("not glob"),
    IN("in"),
    NOT_IN("not in"),
    BETWEEN("between"),
    EXISTS("exists"),
    NOT_EXISTS("not exists"),
    IS_NULL("is null"),
    IS_NOT_NULL("is not null"),
}
