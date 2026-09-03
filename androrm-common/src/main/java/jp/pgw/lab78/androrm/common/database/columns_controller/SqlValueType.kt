package jp.pgw.lab78.androrm.common.database.validation

import jp.pgw.lab78.androrm.common.database.validation.SqlValueTypeModel.SqlDefaultValueValidator.dateTimeSpaceFormatter
import jp.pgw.lab78.androrm.common.database.validation.SqlValueTypeModel.SqlDefaultValueValidator.validFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

interface SqlValueTypeModel {
    fun <T>toColumnValue(value: Any): T
}
/**
 * ## SQL既定値の対応型
 * ### AndrORMがDEFAULT値として検証できるKotlin型を表す
 * @property type KotlinまたはJava上の完全修飾型名
 * @author Masahiro Inoue
 * @since 2026-07-18
 */
enum class SqlValueType(val type: KClass<*>, vararg val regex: String): SqlValueTypeModel  {
    /** Int / number */
    INT(Int::class, "^[+-]?\\d+$"){
        override fun <Long>toColumnValue(value: Any): Long = LONG.toColumnValue(value)
    },

    /** Long / INTEGER */
    LONG(Long::class, "^[+-]?\\d+$") {
        @Suppress("UNCHECKED_CAST")
        override fun <Long>toColumnValue(value: Any): Long =
            type.safeCast(value) as? Long ?: value.toString().toLong()
    },

    /** Float / REAL */
    FLOAT(Float::class, "^[+-]?\\d+(\\.\\d+)?$") {
        override fun <Double>toColumnValue(value: Any): Double = DOUBLE.toColumnValue(value)
    },

    /** Double / REAL */
    DOUBLE(Double::class, "^[+-]?\\d+(\\.\\d+)?$") {
        @Suppress("UNCHECKED_CAST")
        override fun <Double>toColumnValue(value: Any): Double =
            type.safeCast(value) as? Double ?: value.toString().toDouble() as Double
    },

    /** Boolean / INTEGER */
    BOOLEAN(Boolean::class, "^[01]$") {
        override fun toColumnValue(value: Any): Long =
            if (type.safeCast(value) == true) 1 else 0
    },

    /** String / TEXT */
    STRING(String::class, "^'(?:''|[^'])*'$") {
        override fun toColumnValue(value: Any): String = value.toString()
    },

    /** LocalDate / DATETIME */
    LOCAL_DATE(LocalDate::class, "^CURRENT_DATE$", "^'\\d{4}-\\d{2}-\\d{2}'$") {
        override fun matches(candidate: Any): Boolean {
            val candidateUpper = candidate.toString().uppercase()
            if (regex[0].toRegex().matches(candidateUpper)) return true
            return candidateUpper.validFormat(regex[1].toRegex()) {
                runCatching {
                    LocalDate.parse(it.trim('\''), DateTimeFormatter.ISO_LOCAL_DATE)
                }.isSuccess
            }
        }

        override fun toColumnValue(value: Any): String =
            (if (regex[0].toRegex().matches(value.toString().uppercase())) {
                LocalDate.now()
            } else {
                value.toString().trim('\'')
            }).toString()

        override fun toEntity(value: Any): Any = LocalDate.parse(value.toString())
    },

    /** LocalTime / DATETIME */
    LOCAL_TIME(
        LocalTime::class,
        "^CURRENT_TIME$",
        "^'\\d{2}:\\d{2}:\\d{2}'$"
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

        override fun toColumnValue(value: Any): String =
            (if (regex[0].toRegex().matches(value.toString().uppercase())) {
                LocalTime.now()
            } else {
                value.toString().trim('\'')
            }).toString()

        override fun toEntity(value: Any): Any = LocalTime.parse(value.toString())
    },

    /** LocalDateTime / DATETIME */
    LOCAL_DATE_TIME(
        LocalDateTime::class,
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

        override fun toEntity(value: Any): Any = LocalDateTime.parse(value.toString())
        override fun toColumnValue(value: Any): String {
            val input = value.toString().uppercase()
            val matched = regex[0].toRegex().matchEntire(input)?.groupValues?.get(1)
            return when (matched) {
                "CURRENT_TIMESTAMP" -> LocalDateTime.now().format(dateTimeSpaceFormatter)
                "CURRENT_TIMESTAMP_ISO" -> LocalDateTime.now().toString()
                else -> input.trim('\'')
            }
        }
    },

    /** ByteArray / BLOB */
    BYTE_ARRAY(ByteArray::class, "^X'(?:[0-9A-F]{2})+'$") {
        override fun toEntity(value: Any): Any =
            value as? ByteArray
                ?: value.toString().removePrefix("X'").removeSuffix("'").chunked(2)
                    .map { it.toInt(16).toByte() }.toByteArray()

        override fun toColumnValue(value: Any): ByteArray = value as? ByteArray ?: ByteArray(0)
    },

    /** null / NULL */
    NULL_VALUE(Any::class, "^null$") {
        override fun matches(candidate: Any): Boolean = true
        override fun toColumnValue(value: Any): String? = null
    }, ;

    companion object {
        /**
         * ## 完全修飾型名から対応型を取得
         * @param qualifiedName KotlinまたはJava上の完全修飾型名
         * @return 対応型。未対応型の場合は null
         * @author Masahiro Inoue
         * @since 2026-07-18
         */
        fun fromQualifiedName(qualifiedName: String): SqlValueType? =
            SqlValueType.entries.firstOrNull { it.type.qualifiedName == qualifiedName }
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
    open fun toEntity(value: Any) = type.safeCast(value) ?: type.safeCast(value.toString())

//    /**
//     * ## カラム転送
//     * ### データを変換してカラム値へ転送する
//     * @param value 変換元の値
//     * @return 変換後の値
//     * @author Masahiro Inoue
//     * @since 2026-09-01
//     */
//    abstract fun toColumnValue(value: Any): Any?

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
