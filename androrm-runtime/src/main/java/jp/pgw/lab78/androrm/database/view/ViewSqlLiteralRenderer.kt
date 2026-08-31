package jp.pgw.lab78.androrm.database.view

import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.expression.BinaryExpression
import jp.pgw.lab78.androrm.database.expression.ColumnExpression
import jp.pgw.lab78.androrm.database.expression.RawExpression
import jp.pgw.lab78.androrm.database.expression.ValueExpression
import jp.pgw.lab78.androrm.database.interfaces.SqlExpression
import jp.pgw.lab78.androrm.database.reference.ColumnRef
import jp.pgw.lab78.androrm.database.utility.EntityManager.toColumnString
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale
import kotlin.reflect.KProperty1

/**
 * ## SQL 値表現提供インターフェース
 * ### 条件 DSL が値を bind 以外の方法で表現する場合の委譲先を表す
 * @author Masahiro Inoue
 * @since 2026-08-31
 */
internal interface SqlValueRendererOwner {
    /**
     * ## SQL 値表現
     * @param value SQL に配置する値
     * @param enableAlias カラム参照へエイリアスを付ける場合 true
     * @return SQL 断片
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    fun renderSqlValue(value: Any, enableAlias: Boolean): String
}

/**
 * ## VIEW 定義用値保持領域
 * ### バインド値を保持せず、値を SQLite SQL リテラルへ変換する
 * @author Masahiro Inoue
 * @since 2026-08-31
 */
internal class ViewLiteralValueHolder : QueryWithBindValues(), SqlValueRendererOwner {
    /**
     * ## VIEW 定義用 SQL 値表現
     * @param value SQL に配置する値
     * @param enableAlias カラム参照へエイリアスを付ける場合 true
     * @return SQL リテラルまたはカラム参照
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    override fun renderSqlValue(value: Any, enableAlias: Boolean): String =
        ViewSqlLiteralRenderer.renderValue(value, enableAlias)
}

/**
 * ## VIEW 定義用 SQL リテラル生成
 * ### SQLite の CREATE VIEW で bind parameter を使用しないため値を安全なリテラルへ変換する
 * @author Masahiro Inoue
 * @since 2026-08-31
 */
internal object ViewSqlLiteralRenderer {
    /**
     * ## 値の SQL 表現生成
     * @param value 変換対象値
     * @param enableAlias カラム参照へエイリアスを付ける場合 true
     * @return SQLite SQL リテラルまたは式
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    @Suppress("UNCHECKED_CAST")
    fun renderValue(value: Any?, enableAlias: Boolean = true): String = when (value) {
        null -> "NULL"
        is KProperty1<*, *> ->
            (value as KProperty1<out Entity, *>).toColumnString(enableAlias)
        is ColumnRef<*, *> -> value.build()
        is SqlExpression -> renderExpression(value, enableAlias)
        is Boolean -> if (value) "1" else "0"
        is Byte, is Short, is Int, is Long -> value.toString()
        is Float -> renderFloatingPoint(value.toDouble(), value.toString())
        is Double -> renderFloatingPoint(value, value.toString())
        is String -> quoteText(value)
        is LocalDate, is LocalTime, is LocalDateTime -> quoteText(value.toString())
        is ByteArray -> "X'${value.joinToString("") { byte -> "%02X".format(Locale.ROOT, byte.toInt() and 0xFF) }}'"
        else -> throw IllegalArgumentException(
            "Unsupported VIEW SQL literal type: ${value::class.qualifiedName}"
        )
    }

    /**
     * ## SQL 式のリテラル展開
     * @param expression 展開対象式
     * @param enableAlias カラム参照へエイリアスを付ける場合 true
     * @return bind parameter を含まない SQL 式
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    private fun renderExpression(expression: SqlExpression, enableAlias: Boolean): String =
        when (expression) {
            is ValueExpression -> renderValue(expression.value, enableAlias)
            is ColumnExpression -> expression.columnRef.build()
            is BinaryExpression ->
                "(${renderExpression(expression.lhs, enableAlias)} ${expression.operator.symbol} " +
                        "${renderExpression(expression.rhs, enableAlias)})"
            is RawExpression -> expandAnonymousParameters(expression.expression, expression.bindValues)
            else -> throw IllegalArgumentException(
                "Custom SqlExpression is not supported in ViewSelect: ${expression::class.qualifiedName}"
            )
        }

    /**
     * ## 文字列リテラル生成
     * @param value 文字列値
     * @return シングルクォート済み SQL 文字列
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    private fun quoteText(value: String): String {
        require('\u0000' !in value) {
            "NUL character is not supported in a VIEW SQL string literal."
        }
        return "'${value.replace("'", "''")}'"
    }

    /**
     * ## 浮動小数リテラル生成
     * @param numericValue 有限性検査用の値
     * @param text SQL へ出力する文字列
     * @return 有限な浮動小数リテラル
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    private fun renderFloatingPoint(numericValue: Double, text: String): String {
        require(numericValue.isFinite()) {
            "NaN and infinite values are not supported in a VIEW SQL literal."
        }
        return text
    }

    /**
     * ## 無名パラメータ展開
     * ### 文字列・識別子・コメント内を除く `?` だけを値の指定順で置換する
     * @param sql SQL 断片
     * @param values 展開値
     * @return SQL リテラルへ展開した SQL 断片
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    fun expandAnonymousParameters(sql: String, values: List<Any?>): String {
        var valueIndex = 0
        val result = scanSql(sql) { token ->
            if (token == "?") {
                require(valueIndex < values.size) {
                    "VIEW SQL has more anonymous parameters than values: $sql"
                }
                renderValue(values[valueIndex++])
            } else {
                token
            }
        }
        require(valueIndex == values.size) {
            "VIEW SQL has fewer anonymous parameters than values: $sql"
        }
        return result
    }

    /**
     * ## SQL パラメータ検査
     * @param sql 検査対象 SQL
     * @return bind parameter が残っている場合 true
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    fun hasSqlParameter(sql: String): Boolean {
        var found = false
        scanSql(sql) { token ->
            if (token.startsWith("?") || token.startsWith(":") ||
                token.startsWith("@") || token.startsWith("\$")
            ) {
                found = true
            }
            token
        }
        return found
    }

    /**
     * ## SQL 字句走査
     * ### クォートおよびコメントの外側にあるパラメータ字句だけをコールバックへ渡す
     * @param sql 走査対象 SQL
     * @param parameterTransform パラメータ字句の変換処理
     * @return 変換後 SQL
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    private fun scanSql(sql: String, parameterTransform: (String) -> String): String {
        val result = StringBuilder()
        var index = 0
        while (index < sql.length) {
            val current = sql[index]
            when {
                current == '\'' || current == '"' || current == '`' -> {
                    index = appendQuoted(sql, index, current, result)
                }
                current == '[' -> index = appendBracketQuoted(sql, index, result)
                current == '-' && sql.getOrNull(index + 1) == '-' ->
                    index = appendLineComment(sql, index, result)
                current == '/' && sql.getOrNull(index + 1) == '*' ->
                    index = appendBlockComment(sql, index, result)
                current == '?' -> {
                    val end = readWhile(sql, index + 1) { it.isDigit() }
                    result.append(parameterTransform(sql.substring(index, end)))
                    index = end
                }
                current == ':' || current == '@' || current == '$' -> {
                    val end = readWhile(sql, index + 1) { it.isLetterOrDigit() || it == '_' }
                    if (end == index + 1) {
                        result.append(current)
                        index++
                    } else {
                        result.append(parameterTransform(sql.substring(index, end)))
                        index = end
                    }
                }
                else -> {
                    result.append(current)
                    index++
                }
            }
        }
        return result.toString()
    }

    /** クォート文字列を終端まで追加する。 */
    private fun appendQuoted(sql: String, start: Int, quote: Char, result: StringBuilder): Int {
        var index = start
        result.append(sql[index++])
        while (index < sql.length) {
            val current = sql[index]
            result.append(current)
            index++
            if (current == quote) {
                if (sql.getOrNull(index) == quote) {
                    result.append(sql[index++])
                } else {
                    break
                }
            }
        }
        return index
    }

    /** 角括弧識別子を終端まで追加する。 */
    private fun appendBracketQuoted(sql: String, start: Int, result: StringBuilder): Int {
        var index = start
        while (index < sql.length) {
            val current = sql[index++]
            result.append(current)
            if (current == ']') break
        }
        return index
    }

    /** 行コメントを改行まで追加する。 */
    private fun appendLineComment(sql: String, start: Int, result: StringBuilder): Int {
        var index = start
        while (index < sql.length) {
            val current = sql[index++]
            result.append(current)
            if (current == '\n' || current == '\r') break
        }
        return index
    }

    /** ブロックコメントを終端まで追加する。 */
    private fun appendBlockComment(sql: String, start: Int, result: StringBuilder): Int {
        var index = start
        while (index < sql.length) {
            val current = sql[index++]
            result.append(current)
            if (current == '*' && sql.getOrNull(index) == '/') {
                result.append(sql[index++])
                break
            }
        }
        return index
    }

    /** 条件を満たす間の終端位置を返す。 */
    private fun readWhile(sql: String, start: Int, predicate: (Char) -> Boolean): Int {
        var index = start
        while (index < sql.length && predicate(sql[index])) index++
        return index
    }
}
