package jp.pgw.lab78.androrm.ksp.validator

import jp.pgw.lab78.androrm.common.database.SupportFunction.toSnakeCase

/**
 * ## View定義検証の共通計算
 * ### KSPのView検証とその境界値テストで同じ物理カラム名規則を利用する。
 */
internal object ViewDefinitionValidationSupport {
    /** 実効物理カラム名を解決する。 */
    fun effectivePhysicalName(propertyName: String, explicitName: String?): String =
        explicitName?.takeIf { it.isNotBlank() } ?: propertyName.toSnakeCase()

    /** 物理カラム名を大文字小文字無視で重複検証する。 */
    fun hasDuplicatePhysicalNames(names: Collection<String>): Boolean =
        names.groupingBy { it.uppercase() }.eachCount().any { it.value > 1 }
}
