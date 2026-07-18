package jp.pgw.lab78.androrm.common.database.validation

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/**
 * ## SQL既定値の対応型
 * ### AndrORMがDEFAULT値として検証できるKotlin型を表す
 * @property qualifiedName KotlinまたはJava上の完全修飾型名
 * @author Masahiro Inoue
 * @since 2026-07-18
 */
enum class SqlDefaultValueType(val qualifiedName: String) {
    INT("kotlin.Int"),
    LONG("kotlin.Long"),
    FLOAT("kotlin.Float"),
    DOUBLE("kotlin.Double"),
    BOOLEAN("kotlin.Boolean"),
    STRING("kotlin.String"),
    LOCAL_DATE("java.time.LocalDate"),
    LOCAL_TIME("java.time.LocalTime"),
    LOCAL_DATE_TIME("java.time.LocalDateTime"),
    BYTE_ARRAY("kotlin.ByteArray");

    companion object {
        /**
         * ## 完全修飾型名から対応型を取得
         * @param qualifiedName KotlinまたはJava上の完全修飾型名
         * @return 対応型。未対応型の場合は null
         */
        fun fromQualifiedName(qualifiedName: String): SqlDefaultValueType? =
            entries.firstOrNull { type -> type.qualifiedName == qualifiedName }
    }
}

/**
 * ## SQL既定値検証
 * ### CREATE TABLE用DEFAULTとDBマイグレーション用既定値に共通する型別検証と正規化を提供する
 * @author Masahiro Inoue
 * @since 2026-07-18
 */
object SqlDefaultValueValidator {
    const val NULL_VALUE = "NULL"
    const val CURRENT_DATE_VALUE = "CURRENT_DATE"
    const val CURRENT_TIME_VALUE = "CURRENT_TIME"
    const val CURRENT_TIMESTAMP_VALUE = "CURRENT_TIMESTAMP"
    const val CURRENT_TIMESTAMP_ISO_VALUE = "CURRENT_TIMESTAMP_ISO"

    private val integerRegex = "^[+-]?\\d+$".toRegex()
    private val decimalRegex = "^[+-]?\\d+(\\.\\d+)?$".toRegex()
    private val sqlStringRegex = "^'(?:''|[^'])*'$".toRegex()
    private val dateRegex = "^\\d{4}-\\d{2}-\\d{2}$".toRegex()
    private val timeRegex = "^\\d{2}:\\d{2}:\\d{2}$".toRegex()
    private val dateTimeTRegex =
        "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$".toRegex()
    private val dateTimeSpaceRegex =
        "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$".toRegex()
    private val dateTimeSpaceFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
        .withResolverStyle(ResolverStyle.STRICT)

    /**
     * ## SQL既定値の妥当性判定
     * @param type 対象プロパティ型
     * @param nullable 対象プロパティがnullを許容するか
     * @param value 検証対象値
     * @param allowBlank 空文字を「既定値指定なし」として許可するか
     * @return 妥当な場合 true
     */
    fun isValid(
        type: SqlDefaultValueType?,
        nullable: Boolean,
        value: String,
        allowBlank: Boolean,
    ): Boolean {
        val candidate = value.trim()
        if (candidate.isBlank()) return allowBlank
        if (candidate.equals(NULL_VALUE, ignoreCase = true)) return nullable
        return when (type) {
            SqlDefaultValueType.INT,
            SqlDefaultValueType.LONG -> integerRegex.matches(candidate)
            SqlDefaultValueType.FLOAT,
            SqlDefaultValueType.DOUBLE -> decimalRegex.matches(candidate)
            SqlDefaultValueType.BOOLEAN -> candidate == "0" || candidate == "1"
            SqlDefaultValueType.STRING -> sqlStringRegex.matches(candidate)
            SqlDefaultValueType.LOCAL_DATE ->
                candidate.equals(CURRENT_DATE_VALUE, ignoreCase = true) ||
                        candidate.validQuotedLiteral(dateRegex) { literal ->
                            LocalDate.parse(literal, DateTimeFormatter.ISO_LOCAL_DATE)
                        }
            SqlDefaultValueType.LOCAL_TIME ->
                candidate.equals(CURRENT_TIME_VALUE, ignoreCase = true) ||
                        candidate.validQuotedLiteral(timeRegex) { literal ->
                            LocalTime.parse(literal, DateTimeFormatter.ISO_LOCAL_TIME)
                        }
            SqlDefaultValueType.LOCAL_DATE_TIME ->
                candidate.equals(CURRENT_TIMESTAMP_VALUE, ignoreCase = true) ||
                        candidate.equals(CURRENT_TIMESTAMP_ISO_VALUE, ignoreCase = true) ||
                        candidate.validQuotedLiteral(dateTimeTRegex) { literal ->
                            LocalDateTime.parse(literal, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        } ||
                        candidate.validQuotedLiteral(dateTimeSpaceRegex) { literal ->
                            LocalDateTime.parse(literal, dateTimeSpaceFormatter)
                        }
            SqlDefaultValueType.BYTE_ARRAY,
            null -> false
        }
    }

    /**
     * ## SQLite向け既定値の正規化
     * ### AndrORM固有予約値をSQLiteで実行可能な式へ変換し、それ以外は検証済みリテラルを返す
     * @param value 検証済みのSQL既定値
     * @return SQLiteで使用するSQLリテラルまたは式
     */
    fun normalize(value: String): String = when {
        value.trim().equals(NULL_VALUE, ignoreCase = true) -> NULL_VALUE
        value.trim().equals(CURRENT_TIMESTAMP_ISO_VALUE, ignoreCase = true) ->
            "(strftime('%Y-%m-%dT%H:%M:%f', 'now', 'localtime'))"
        else -> value.trim()
    }

    /**
     * ## クォート付き日時リテラルの妥当性判定
     * @param regex 許可する書式
     * @param parser 日付・時刻型の厳密な解析処理
     * @return 妥当な場合 true
     */
    private fun String.validQuotedLiteral(
        regex: Regex,
        parser: (String) -> Any,
    ): Boolean {
        if (!sqlStringRegex.matches(this)) return false
        val literal = substring(1, length - 1).replace("''", "'")
        if (!regex.matches(literal)) return false
        return runCatching { parser(literal) }.isSuccess
    }
}
