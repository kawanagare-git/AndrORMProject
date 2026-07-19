package jp.pgw.lab78.androrm.ksp.validator

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import jp.pgw.lab78.androrm.common.database.validation.SqlDefaultValueType
import jp.pgw.lab78.androrm.common.database.validation.SqlDefaultValueValidator
import jp.pgw.lab78.androrm.common.logging.interfaces.LoggerLike
import jp.pgw.lab78.androrm.ksp.Constants.MIGRATION_DEFAULT
import jp.pgw.lab78.androrm.ksp.Constants.MIGRATION_DEFAULT_FQN
import jp.pgw.lab78.androrm.ksp.Constants.MIGRATION_DEFAULT_VALUE
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger.logger

/**
 * ## MigrationDefault値検証クラス
 * ### DBマイグレーションで使用する値が対象プロパティ型に対して妥当か検証する
 * @author Masahiro Inoue
 * @since 2026-07-18
 */
class MigrationDefaultValueValidator : LoggerLike by logger {

    /**
     * ## Entity内のMigrationDefault値検証
     * ### MigrationDefaultが付与された全プロパティを共通SQL既定値規則で検証する
     * @param classDecl 検証対象Entity
     * @return 検証OKの場合 true
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    fun validate(classDecl: KSClassDeclaration): Boolean {
        var hasError = false
        classDecl.getAllProperties().forEach { property ->
            val migrationDefault = property.migrationDefaultValue() ?: return@forEach
            val typeName = property.typeName()
            if (
                SqlDefaultValueValidator.isValid(
                    type = SqlDefaultValueType.fromQualifiedName(typeName),
                    nullable = property.type.resolve().isMarkedNullable,
                    value = migrationDefault,
                    allowBlank = false,
                )
            ) {
                return@forEach
            }
            hasError = true
            logError(
                "Invalid @MigrationDefault(\"$migrationDefault\"). " +
                        "entity='${classDecl.qualifiedName?.asString()}', " +
                        "property='${property.simpleName.asString()}', " +
                        "type='$typeName', nullable=${property.type.resolve().isMarkedNullable}."
            )
        }
        return !hasError
    }

    /**
     * ## MigrationDefault値取得
     * @return アノテーション値。未指定の場合は null
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private fun KSPropertyDeclaration.migrationDefaultValue(): String? =
        annotations.firstOrNull { annotation ->
            annotation.shortName.asString() == MIGRATION_DEFAULT &&
                    annotation.annotationType.resolve()
                        .declaration.qualifiedName?.asString() == MIGRATION_DEFAULT_FQN
        }?.arguments?.firstOrNull { argument ->
            argument.name?.asString() == MIGRATION_DEFAULT_VALUE
        }?.value as? String

    /**
     * ## プロパティ型名取得
     * @return nullableを除いた完全修飾型名
     * @author Masahiro Inoue
     * @since 2026-07-18
     */
    private fun KSPropertyDeclaration.typeName(): String =
        type.resolve().declaration.qualifiedName?.asString()
            ?: type.resolve().declaration.simpleName.asString()
}
