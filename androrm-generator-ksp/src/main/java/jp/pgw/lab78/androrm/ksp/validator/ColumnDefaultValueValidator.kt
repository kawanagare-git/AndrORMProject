package jp.pgw.lab78.androrm.ksp.validator

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import jp.pgw.lab78.androrm.common.logging.interfaces.LoggerLike
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_DEFAULT_VALUE
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_FQN
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger.logger
import jp.pgw.lab78.androrm.ksp.projectoin.ProjectionDefinition
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/**
 * ## @Column defaultValue 検証クラス
 * ### CREATE TABLE の DEFAULT 句に使用する値が、対象プロパティ型に対して妥当か検証する
 * @author Masahiro Inoue
 * @since 2026-06-13
 */
class ColumnDefaultValueValidator : LoggerLike by logger {

    companion object {
        private const val NULL_VALUE = "NULL"
        private const val CURRENT_DATE_VALUE = "CURRENT_DATE"
        private const val CURRENT_TIME_VALUE = "CURRENT_TIME"
        private const val CURRENT_TIMESTAMP_VALUE = "CURRENT_TIMESTAMP"

        private const val TYPE_INT = "kotlin.Int"
        private const val TYPE_LONG = "kotlin.Long"
        private const val TYPE_FLOAT = "kotlin.Float"
        private const val TYPE_DOUBLE = "kotlin.Double"
        private const val TYPE_BOOLEAN = "kotlin.Boolean"
        private const val TYPE_STRING = "kotlin.String"
        private const val TYPE_BYTE_ARRAY = "kotlin.ByteArray"
        private const val TYPE_LOCAL_DATE = "java.time.LocalDate"
        private const val TYPE_LOCAL_TIME = "java.time.LocalTime"
        private const val TYPE_LOCAL_DATE_TIME = "java.time.LocalDateTime"
        private val integerRegex = "^[+-]?\\d+$".toRegex()
        private val decimalRegex = "^[+-]?\\d+(\\.\\d+)?$".toRegex()
        private val sqlStringRegex = "^'(?:''|[^'])*'$".toRegex()
        private val dateRegex = "^\\d{4}-\\d{2}-\\d{2}$".toRegex()
        private val timeRegex = "^\\d{2}:\\d{2}:\\d{2}$".toRegex()
        private val dateTimeTRegex = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$".toRegex()
        private val dateTimeSpaceRegex = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$".toRegex()
        private val dateTimeSpaceFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
            .withResolverStyle(ResolverStyle.STRICT)
    }

    private data class DefaultValueContext(
        val propertyName: String,
        val typeName: String,
        val nullable: Boolean,
        val value: String,
    )

    private data class DateTimeLiteralValidator(
        val regex: Regex,
        val formatChecker: (String) -> Boolean,
    )

    /** 型名をキーにした defaultValue 検証関数 */
    private val validatorsByType: Map<String, (DefaultValueContext) -> Boolean> = mapOf(
        TYPE_INT to { context -> integerRegex.matches(context.value) },
        TYPE_LONG to { context -> integerRegex.matches(context.value) },
        TYPE_FLOAT to { context -> decimalRegex.matches(context.value) },
        TYPE_DOUBLE to { context -> decimalRegex.matches(context.value) },
        TYPE_BOOLEAN to { context -> context.value == "0" || context.value == "1" },
        TYPE_STRING to { context -> sqlStringRegex.matches(context.value) },
        TYPE_LOCAL_DATE to { context ->
            isValidLocalDateTimeDefault(
                context, CURRENT_DATE_VALUE,
                localDateLiteralValidator,
            )
        },
        TYPE_LOCAL_TIME to { context ->
            isValidLocalDateTimeDefault(
                context, CURRENT_TIME_VALUE,
                localTimeLiteralValidator,
            )
        },
        TYPE_LOCAL_DATE_TIME to { context ->
            isValidLocalDateTimeDefault(
                context, CURRENT_TIMESTAMP_VALUE, *(localDateTimeLiteralValidators)
            )
        },
        TYPE_BYTE_ARRAY to { false },
    )

    /** LocalDate 文字列リテラル検証関数 */
    private val localDateLiteralValidator =
        DateTimeLiteralValidator(
            regex = dateRegex,
            formatChecker = { literal ->
                runCatching {
                    LocalDate.parse(literal, DateTimeFormatter.ISO_LOCAL_DATE)
                }.isSuccess
            },
        )

    /** LocalTime 文字列リテラル検証関数 */
    private val localTimeLiteralValidator =
        DateTimeLiteralValidator(
            regex = timeRegex,
            formatChecker = { literal ->
                runCatching {
                    LocalTime.parse(literal, DateTimeFormatter.ISO_LOCAL_TIME)
                }.isSuccess
            },
        )

    /** LocalDateTime 文字列リテラル検証関数 */
    private val localDateTimeLiteralValidators = arrayOf(
        DateTimeLiteralValidator(
            regex = dateTimeTRegex,
            formatChecker = { literal ->
                runCatching {
                    LocalDateTime.parse(literal, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                }.isSuccess
            },
        ),
        DateTimeLiteralValidator(
            regex = dateTimeSpaceRegex,
            formatChecker = { literal ->
                runCatching {
                    LocalDateTime.parse(literal, dateTimeSpaceFormatter)
                }.isSuccess
            },
        ),
    )

    /**
     * ## Projection 内の @Column defaultValue 検証
     * ### Projection で生成対象になっている通常列だけを検証する
     * @param classDecl Projection が付与された定義元クラス
     * @param definition Projection 定義
     * @return 検証OKの場合 true
     * @author Masahiro Inoue
     * @since 2026-06-13
     */
    fun validate(
        classDecl: KSClassDeclaration,
        definition: ProjectionDefinition,
    ): Boolean {
        logTraceEntered(classDecl, definition)
        // クラス定義に記録されている全てのプロパティをマップとして取得
        val sourcePropertiesByName = classDecl.getAllProperties()
            .associateBy { property -> property.simpleName.asString() }
        var hasError = false
        // 定義してある全プロパティを捜査する
        definition.properties.forEach { columnProjection ->
            val property = sourcePropertiesByName[columnProjection.property]
                ?: return@forEach
            val defaultValue = property.defaultValue().trim()
            if (defaultValue.isBlank()) {
                return@forEach
            }
            if (property.isValidDefaultValue(defaultValue)) {
                return@forEach
            }
            hasError = true
            logError(
                "Invalid @Column(defaultValue = \"$defaultValue\"). " +
                        "property='${property.simpleName.asString()}', " +
                        "type='${property.typeName()}', " +
                        "nullable=${property.isNullable()}, " +
                        "source='${classDecl.qualifiedName?.asString()}'."
            )
        }
        val result = !hasError
        logTraceExiting(result)
        return result
    }

    /**
     * ## defaultValue 妥当性判定
     * ### NULL 判定後、型名をキーにした Map で検証関数へ分岐する
     * @receiver 検証対象プロパティ
     * @param defaultValue @Column(defaultValue) の値
     * @return 妥当な場合 true
     * @author Masahiro Inoue
     * @since 2026-06-13
     */
    private fun KSPropertyDeclaration.isValidDefaultValue(
        defaultValue: String,
    ): Boolean {
        val context = DefaultValueContext(
            propertyName = simpleName.asString(),
            typeName = typeName(),
            nullable = isNullable(),
            value = defaultValue,
        )
        if (context.value.equals(NULL_VALUE, ignoreCase = true)) {
            return context.nullable
        }
        val validator = validatorsByType[context.typeName] ?: return false
        return validator(context)
    }

    /**
     * ## LocalDateTime defaultValue 検証
     * ### 'YYYY-MM-DDTHH:MM:SS' / 'YYYY-MM-DD HH:MM:SS' / CURRENT_TIMESTAMP を許可する
     * @param context defaultValue 検証コンテキスト
     * @return 妥当な場合 true
     * @author Masahiro Inoue
     * @since 2026-06-13
     */
    private fun isValidLocalDateTimeDefault(
        context: DefaultValueContext,
        dateType: String,
        vararg literalValidator: DateTimeLiteralValidator,
    ): Boolean {
        if (context.value.equals(dateType, ignoreCase = true)) {
            return true
        }
        val literal = context.value.toSqlStringLiteralValue() ?: return false
        return literalValidator.any { validator ->
            if (validator.regex.matches(literal)) {
                validator.formatChecker(literal)
            } else {
                false
            }
        }
    }

    /**
     * ## SQL 文字列リテラル値取得
     * ### 'text' 形式の SQL 文字列から中身を取得する
     * @receiver defaultValue
     * @return SQL 文字列リテラルの中身。不正な場合 null
     * @author Masahiro Inoue
     * @since 2026-06-13
     */
    private fun String.toSqlStringLiteralValue(): String? =
        if (sqlStringRegex.matches(this)) {
            substring(1, length - 1).replace("''", "'")
        } else {
            null
        }

    /**
     * ## @Column defaultValue 取得
     * ### 対象プロパティの @Column から defaultValue を取得する
     * @receiver 対象プロパティ
     * @return defaultValue
     * @author Masahiro Inoue
     * @since 2026-06-13
     */
    private fun KSPropertyDeclaration.defaultValue(): String =
        annotations.firstOrNull { annotation ->
            annotation.shortName.asString() == COLUMN &&
                    annotation.annotationType.resolve()
                        .declaration.qualifiedName?.asString() == COLUMN_FQN
        }?.arguments?.firstOrNull { argument -> argument.name?.asString() == COLUMN_DEFAULT_VALUE }
            ?.value as? String ?: ""

    /**
     * ## nullable 判定
     * ### 対象プロパティが null を許容するか判定する
     * @receiver 対象プロパティ
     * @return nullable の場合 true
     * @author Masahiro Inoue
     * @since 2026-06-13
     */
    private fun KSPropertyDeclaration.isNullable(): Boolean =
        type.resolve().isMarkedNullable

    /**
     * ## 型名取得
     * ### nullable を除いた完全修飾型名を取得する
     * @receiver 対象プロパティ
     * @return 完全修飾型名
     * @author Masahiro Inoue
     * @since 2026-06-13
     */
    private fun KSPropertyDeclaration.typeName(): String =
        type.resolve().declaration.qualifiedName?.asString()
            ?: type.resolve().declaration.simpleName.asString()
}