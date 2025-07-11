package jp.pgw.lab78.androrm.utility

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * UtilityFunction オブジェクトクラス
 */
object Functions {
    /**
     * ## toSnakeCase 関数
     * ### キャメルケースの文字列をスネークケースに変換
     */
    fun String.toSnakeCase(): String {
        return this.replace(Regex("([a-z])([A-Z])")) { "${it.groupValues[1]}_${it.groupValues[2]}" }
            .uppercase(Locale.ROOT)
    }

    /** ## 型変換用マップ */
    private val fieldToColumnMap = mapOf(
        Int::class to "INTEGER"
        ,Long::class to "INTEGER"
        ,Float::class to "REAL"
        ,Double::class to "REAL"
        ,Boolean::class to "INTEGER"
        ,String::class to "TEXT"
        ,LocalDate::class to "DATETIME"
        ,LocalTime::class to "DATETIME"
        ,LocalDateTime::class to "DATETIME"
    )

    /**
     * ## mapKotlinTypeToSqlType 関数
     * ### クラスのフィールド型をデータベースのカラム型に変換
     * @param field 変換対象のフィールドを指定
     */
    fun mapKotlinTypeToSqlType(field : Any): String {
        val valueForJudgment = when (field) {
            // arg が既に KType の場合、KClass<*> にキャスト
            is KType -> field.classifier as? KClass<*>
            // それ以外は、KClass<*> を取得
            else -> field::class
        }
        if (valueForJudgment == null) {
            throw IllegalArgumentException("Unsupported type: $field")
        }else{
            return fieldToColumnMap[valueForJudgment] ?: throw IllegalArgumentException("Unsupported type: $valueForJudgment")
        }
    }
}