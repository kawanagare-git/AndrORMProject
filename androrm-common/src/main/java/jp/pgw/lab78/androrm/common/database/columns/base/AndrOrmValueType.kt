package jp.pgw.lab78.androrm.common.database.columns.base

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.reflect.KClass

/**
 * ## AndrORM 対応型
 * ### @FunctionProjection に指定する戻り値の型推論識別子
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
enum class AndrOrmValueType(val type: KClass<*>?) {
    AUTO(null),
    INT(Int::class),
    LONG(Long::class),
    FLOAT(Float::class),
    DOUBLE(Double::class),
    BOOLEAN(Boolean::class),
    STRING(String::class),
    LOCAL_DATE(LocalDate::class),
    LOCAL_TIME(LocalTime::class),
    LOCAL_DATE_TIME(LocalDateTime::class),
    BYTE_ARRAY(ByteArray::class),
}