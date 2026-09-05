package jp.pgw.lab78.androrm.common.database.columns.controller

import jp.pgw.lab78.androrm.common.MessageConstants.AE00009
import jp.pgw.lab78.androrm.common.database.columns.base.AndrOrmValueType
import jp.pgw.lab78.androrm.common.database.columns.controller.SqlDefaultValueValidator.dateTimeSpaceFormatter
import jp.pgw.lab78.androrm.common.database.columns.controller.SqlDefaultValueValidator.validFormat
import jp.pgw.lab78.androrm.common.database.columns.interfaces.ColumnControllerModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import kotlin.reflect.safeCast

/**
 * ## SQL既定値の対応型
 * ### AndrORMがDEFAULT値として検証できるKotlin型を表す
 * @param valueType AndrORM 対応型
 * @param regex 整合性確認の為の正規表現
 * @author Masahiro Inoue
 * @since 2026-07-18
 */
enum class SqlValueType(val valueType: AndrOrmValueType, vararg val regex: String) :
    ColumnControllerModel {
    /** Int / number */
    INT(AndrOrmValueType.INT, "^[+-]?\\d+$") {
        override fun <T> toColumnValue(value: Any): T = LONG.toColumnValue(value)
    },

    /** Long / INTEGER */
    LONG(AndrOrmValueType.LONG, "^[+-]?\\d+$") {
        @Suppress("UNCHECKED_CAST")
        override fun <T> toColumnValue(value: Any): T =
            (valueType.type?.safeCast(value) ?: value.toString().toLong()) as T
    },

    /** Float / REAL */
    FLOAT(AndrOrmValueType.FLOAT, "^[+-]?\\d+(\\.\\d+)?$") {
        override fun <T> toColumnValue(value: Any): T = DOUBLE.toColumnValue(value)
    },

    /** Double / REAL */
    DOUBLE(AndrOrmValueType.DOUBLE, "^[+-]?\\d+(\\.\\d+)?$") {
        @Suppress("UNCHECKED_CAST")
        override fun <T> toColumnValue(value: Any): T =
            (valueType.type?.safeCast(value) ?: value.toString().toDouble()) as T
    },

    /** Boolean / INTEGER */
    BOOLEAN(AndrOrmValueType.BOOLEAN, "^[01]$") {
        override fun toEntity(value: Any): Boolean = value != 0

        @Suppress("UNCHECKED_CAST")
        override fun <T> toColumnValue(value: Any): T =
            (when (valueType.type?.safeCast(value) as? Boolean) {
                null,
                false -> 0

                else -> 1
            }) as T
    },

    /** String / TEXT */
    STRING(AndrOrmValueType.STRING, "^'(?:''|[^'])*'$") {
        @Suppress("UNCHECKED_CAST")
        override fun <String> toColumnValue(value: Any): String = value.toString() as String
    },

    /** LocalDate / DATETIME */
    LOCAL_DATE(AndrOrmValueType.LOCAL_DATE, "^CURRENT_DATE$", "^'\\d{4}-\\d{2}-\\d{2}'$") {
        override fun matches(candidate: Any): Boolean {
            val candidateUpper = candidate.toString().uppercase()
            if (regex[0].toRegex().matches(candidateUpper)) return true
            return candidateUpper.validFormat(regex[1].toRegex()) {
                runCatching {
                    LocalDate.parse(it.trim('\''), DateTimeFormatter.ISO_LOCAL_DATE)
                }.isSuccess
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T> toColumnValue(value: Any): T =
            (if (regex[0].toRegex().matches(value.toString().uppercase())) {
                LocalDate.now()
            } else {
                value.toString().trim('\'')
            }).toString() as T

        override fun toEntity(value: Any): Any = LocalDate.parse(value.toString())
    },

    /** LocalTime / DATETIME */
    LOCAL_TIME(
        AndrOrmValueType.LOCAL_TIME, "^CURRENT_TIME$", "^'\\d{2}:\\d{2}:\\d{2}'$"
    ) {
        override fun matches(candidate: Any): Boolean {
            val candidateUpper = candidate.toString().uppercase()
            if (regex[0].toRegex().matches(candidateUpper)) return true
            return candidateUpper.validFormat(regex[1].toRegex()) {
                runCatching {
                    LocalTime.parse(it.trim('\''), DateTimeFormatter.ISO_LOCAL_TIME)
                }.isSuccess
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T> toColumnValue(value: Any): T =
            (if (regex[0].toRegex().matches(value.toString().uppercase())) {
                LocalTime.now()
            } else {
                value.toString().trim('\'')
            }).toString() as T

        override fun toEntity(value: Any): Any = LocalTime.parse(value.toString())
    },

    /** LocalDateTime / DATETIME */
    LOCAL_DATE_TIME(
        AndrOrmValueType.LOCAL_DATE_TIME,
        "^(CURRENT_TIMESTAMP|CURRENT_TIMESTAMP_ISO)$",
        "^'\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}'$"
    ) {
        override fun matches(candidate: Any): Boolean {
            val candidateUpper = candidate.toString().uppercase()
            if (regex[0].toRegex().matches(candidateUpper)) return true
            return candidateUpper.validFormat(regex[1].toRegex()) {
                val parseString = it.trim('\'')
                runCatching {
                    LocalDateTime.parse(parseString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                }.isSuccess ||
                        runCatching {
                            LocalDateTime.parse(parseString, dateTimeSpaceFormatter)
                        }.isSuccess
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T> toColumnValue(value: Any): T {
            val input = value.toString().uppercase()
            val matched = regex[0].toRegex().matchEntire(input)?.groupValues?.get(1)
            return when (matched) {
                "CURRENT_TIMESTAMP" -> LocalDateTime.now().format(dateTimeSpaceFormatter)
                "CURRENT_TIMESTAMP_ISO" -> LocalDateTime.now().toString()
                else -> input.trim('\'')
            } as T
        }

        override fun toEntity(value: Any): Any = LocalDateTime.parse(value.toString())
    },

    /** ByteArray / BLOB */
    BYTE_ARRAY(AndrOrmValueType.BYTE_ARRAY, "^X'(?:[0-9A-F]{2})*'$") {
        override fun matches(candidate: Any): Boolean =
            regex[0].toRegex().matches(candidate.toString().uppercase())

        override fun toEntity(value: Any): Any =
            value as? ByteArray
                ?: value.toString().uppercase().removePrefix("X'").removeSuffix("'").chunked(2)
                    .map { it.toInt(16).toByte() }.toByteArray()

        @Suppress("UNCHECKED_CAST")
        override fun <T> toColumnValue(value: Any): T =
            requireNotNull(value as? ByteArray) { AE00009.format(value::class.qualifiedName) } as T
    },
    ;

    companion object {
        /**
         * ## 完全修飾型名から対応型を取得
         * @param qualifiedName KotlinまたはJava上の完全修飾型名
         * @return 対応型。未対応型の場合は null
         * @author Masahiro Inoue
         * @since 2026-07-18
         */
        fun fromQualifiedName(qualifiedName: String): SqlValueType? =
            SqlValueType.entries.firstOrNull { it.valueType.type?.qualifiedName == qualifiedName }
    }

    /**
     * ## バインド変数型整合検査
     * ### バインド変数を文字列として扱い、正規表現で検査する
     * @param candidate 検査対象文字列
     * @return バインド変数型整合が取れている場合 true
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    open fun matches(candidate: Any): Boolean = regex[0].toRegex().matches(candidate.toString())

    /**
     * ## エンティティ転送
     * ### データを変換してエンティティへ転送する
     * @param value 変換元の値
     * @return 変換後の値
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    open fun toEntity(value: Any) =
        valueType.type?.safeCast(value) ?: valueType.type?.safeCast(value.toString())
}

/**
 * ## SQL既定値検証
 * ### CREATE TABLE用DEFAULTとDBマイグレーション用既定値に共通する型別検証と正規化を提供する
 * @author Masahiro Inoue
 * @since 2026-07-18
 */
object SqlDefaultValueValidator {
    const val NULL_VALUE = "NULL"
    const val CURRENT_TIMESTAMP_ISO_VALUE = "CURRENT_TIMESTAMP_ISO"

    val dateTimeSpaceFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
            .withResolverStyle(ResolverStyle.STRICT)

    /**
     * ## SQL既定値の妥当性判定
     * @param type 対象プロパティ型
     * @param nullable 対象プロパティがnullを許容するか
     * @param value 検証対象値
     * @param allowBlank 空文字を「既定値指定なし」として許可するか
     * @return 妥当な場合 true
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    fun isValid(
        type: SqlValueType?,
        nullable: Boolean,
        value: String,
        allowBlank: Boolean,
    ): Boolean {
        val candidate = value.trim()
        if (candidate.isBlank()) return allowBlank
        if (candidate.equals(NULL_VALUE, ignoreCase = true)) return nullable
        return type?.matches(candidate) ?: false
    }

    /**
     * ## SQLite向け既定値の正規化
     * ### AndrORM固有予約値をSQLiteで実行可能な式へ変換し、それ以外は検証済みリテラルを返す
     * @param value 検証済みのSQL既定値
     * @return SQLiteで使用するSQLリテラルまたは式
     * @author Masahiro Inoue
     * @since 2026-07-18
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
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    fun String.validFormat(
        regex: Regex,
        parser: (String) -> Boolean,
    ): Boolean = if (regex.matches(this)) {
        parser(this)
    } else false
}
