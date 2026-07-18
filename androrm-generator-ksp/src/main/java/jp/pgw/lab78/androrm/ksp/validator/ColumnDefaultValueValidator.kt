package jp.pgw.lab78.androrm.ksp.validator

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import jp.pgw.lab78.androrm.common.logging.interfaces.LoggerLike
import jp.pgw.lab78.androrm.common.database.validation.SqlDefaultValueType
import jp.pgw.lab78.androrm.common.database.validation.SqlDefaultValueValidator
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_DEFAULT_VALUE
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_FQN
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger.logger
import jp.pgw.lab78.androrm.ksp.projectoin.ProjectionDefinition

/**
 * ## @Column defaultValue 検証クラス
 * ### CREATE TABLE の DEFAULT 句に使用する値が、対象プロパティ型に対して妥当か検証する
 * @author Masahiro Inoue
 * @since 2026-06-13
 */
class ColumnDefaultValueValidator : LoggerLike by logger {

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
    ): Boolean = SqlDefaultValueValidator.isValid(
        type = SqlDefaultValueType.fromQualifiedName(typeName()),
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