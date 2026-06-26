package jp.pgw.lab78.androrm.database.interfaces

import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.expression.*
import jp.pgw.lab78.androrm.database.reference.ColumnRef

/**
 * ## SQL 式
 * ### SET 句や WHERE 句の右辺で使用する SQL 式を表す
 * @author Masahiro Inoue
 * @since 2026-06-19
 */
interface SqlExpression {
    /**
     * ## SQL 文字列生成
     * ### 必要に応じてバインド値を保持しながら SQL 文字列を生成する
     * @param valueHolder バインド値管理オブジェクト
     * @return SQL 文字列
     * @author Masahiro Inoue
     * @since 2026-06-19
     */
    fun build(valueHolder: QueryWithBindValues): String
}

/**
 * ## SQL 式変換
 * ### 任意の値を SQL 式に変換する
 * @receiver 変換対象
 * @return SQL 式
 * @author Masahiro Inoue
 * @since 2026-06-19
 */
@Suppress("UNCHECKED_CAST")
fun Any?.toSqlExpression(): SqlExpression =
    when (this) {
        is SqlExpression -> this
        is ColumnRef<*, *> -> ColumnExpression(this as ColumnRef<out Entity, *>)
        else -> ValueExpression(this)
    }

/**
 * ## SQL 式変換
 * ### ColumnRef を ColumnExpression に変換する
 * @receiver カラム参照
 * @return SQL 式
 * @author Masahiro Inoue
 * @since 2026-06-19
 */
fun ColumnRef<out Entity, *>.toSqlExpression(): SqlExpression =
    ColumnExpression(this)

/**
 * ## 生 SQL 式生成
 * ### 任意 SQL を SQL 式として扱う
 * @param expression SQL 式
 * @param bindValues バインド値
 * @return SQL 式
 * @author Masahiro Inoue
 * @since 2026-06-19
 */
fun rawExpression(
    expression: String,
    vararg bindValues: Any?,
): SqlExpression =
    RawExpression(
        expression = expression,
        bindValues = bindValues.toList(),
    )

operator fun ColumnRef<out Entity, *>.plus(rhs: Any?): SqlExpression =
    this.toSqlExpression() + rhs

operator fun ColumnRef<out Entity, *>.minus(rhs: Any?): SqlExpression =
    this.toSqlExpression() - rhs

operator fun ColumnRef<out Entity, *>.times(rhs: Any?): SqlExpression =
    this.toSqlExpression() * rhs

operator fun ColumnRef<out Entity, *>.div(rhs: Any?): SqlExpression =
    this.toSqlExpression() / rhs

operator fun SqlExpression.plus(rhs: Any?): SqlExpression =
    BinaryExpression(
        lhs = this,
        operator = SqlArithmeticOperator.ADD,
        rhs = rhs.toSqlExpression(),
    )

operator fun SqlExpression.minus(rhs: Any?): SqlExpression =
    BinaryExpression(
        lhs = this,
        operator = SqlArithmeticOperator.SUBTRACT,
        rhs = rhs.toSqlExpression(),
    )

operator fun SqlExpression.times(rhs: Any?): SqlExpression =
    BinaryExpression(
        lhs = this,
        operator = SqlArithmeticOperator.MULTIPLY,
        rhs = rhs.toSqlExpression(),
    )

operator fun SqlExpression.div(rhs: Any?): SqlExpression =
    BinaryExpression(
        lhs = this,
        operator = SqlArithmeticOperator.DIVIDE,
        rhs = rhs.toSqlExpression(),
    )