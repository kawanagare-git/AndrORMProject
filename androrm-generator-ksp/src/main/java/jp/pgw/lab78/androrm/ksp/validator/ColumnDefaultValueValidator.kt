package jp.pgw.lab78.androrm.ksp.validator

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.database.columns_controller.SqlDefaultValueValidator
import jp.pgw.lab78.androrm.common.database.columns_controller.SqlValueType
import jp.pgw.lab78.androrm.common.logging.interfaces.LoggerLike
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_DEFAULT_VALUE
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger.logger
import jp.pgw.lab78.androrm.ksp.projectoin.ProjectionDefinition
import jp.pgw.lab78.androrm.ksp.resolver.KspColumnAnnotationResolver

/**
 * ## @Column defaultValue 検証クラス
 * ### CREATE TABLE の DEFAULT 句に使用する値が、対象プロパティ型に対して妥当か検証する
 * @param columnAnnotationResolver
 * @author Masahiro Inoue
 * @since 2026-06-13
 */
class ColumnDefaultValueValidator(private val columnAnnotationResolver: KspColumnAnnotationResolver) :
    LoggerLike by logger {
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
            val defaultValue = property.defaultValue(ownerClass = classDecl).trim()
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
        return (!hasError).also { logTraceExiting(it) }
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
    ): Boolean = SqlDefaultValueValidator.isValid(
        type = SqlValueType.fromQualifiedName(typeName()),
        nullable = isNullable(),
        value = defaultValue,
        allowBlank = false,
    )

    /**
     * ## @Column defaultValue 取得
     * ### 対象プロパティの @Column から defaultValue を取得する
     * @receiver 対象プロパティ
     * @return defaultValue
     * @author Masahiro Inoue
     * @since 2026-06-13
     */
    private fun KSPropertyDeclaration.defaultValue(
        ownerClass: KSClassDeclaration,
    ): String =
        columnAnnotationResolver
            .find(
                ownerClass = ownerClass,
                property = this,
            )
            ?.stringArgument(
                argumentName = COLUMN_DEFAULT_VALUE,
            )
            ?: EMPTY_STRING

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

    /**
     * ## アノテーション文字列引数取得
     * @author Masahiro Inoue
     * @since 2026-07-31
     */
    private fun KSAnnotation.stringArgument(argumentName: String): String? =
        arguments.firstOrNull { argument -> argument.name?.asString() == argumentName }
            ?.value as? String
}