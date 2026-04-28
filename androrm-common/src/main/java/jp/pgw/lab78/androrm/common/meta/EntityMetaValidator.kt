package jp.pgw.lab78.androrm.common.meta

/**
 * ## Entity メタ情報検証クラス
 * ### KSP と Select 系クラスから共通利用する
 * ### 判定だけを担当し、例外送出や logger 出力は呼び出し元に委ねる
 *
 * 現時点では以下を検査する。
 * - ⑤ @Column と @Function の併用
 * - ⑥ 全列 hideFromSelect = true
 * - ⑦ alias 重複
 *
 * @author Masahiro Inoue
 * @since 2026-04-27
 */
class EntityMetaValidator {

    /**
     * ## Entity メタ情報検証
     * ### 共通ルールに基づいて EntityMeta を検証する
     *
     * @param entityMeta 検証対象 Entity メタ情報
     * @return 検証結果
     */
    fun validate(entityMeta: EntityMeta): EntityMetaValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        validateDualAnnotation(entityMeta, errors)
        validateNoSelectableProperties(entityMeta, errors)
        validateDuplicateAliases(entityMeta, errors)

        return EntityMetaValidationResult(
            errors = errors,
            warnings = warnings,
        )
    }

    /**
     * ## @Column と @Function の併用検査
     * ### 1 プロパティに @Column と @Function が同時付与されていないか検査する
     *
     * @param entityMeta 検証対象
     * @param errors エラー格納先
     */
    private fun validateDualAnnotation(
        entityMeta: EntityMeta,
        errors: MutableList<String>,
    ) {
        entityMeta.properties
            .filter { it.hasColumnAnnotation && it.hasFunctionAnnotation }
            .forEach { property ->
                errors += buildString {
                    append("Property '")
                    append(property.propertyName)
                    append("' in entity '")
                    append(entityMeta.entityName)
                    append("' cannot have both @Column and @Function.")
                }
            }
    }

    /**
     * ## SELECT 対象ゼロ件検査
     * ### 全プロパティが hideFromSelect = true になっていないか検査する
     *
     * @param entityMeta 検証対象
     * @param errors エラー格納先
     */
    private fun validateNoSelectableProperties(
        entityMeta: EntityMeta,
        errors: MutableList<String>,
    ) {
        val visibleProperties = entityMeta.properties.filterNot { it.hideFromSelect }
        if (visibleProperties.isEmpty()) {
            errors += buildString {
                append("Entity '")
                append(entityMeta.entityName)
                append("' has no selectable properties. ")
                append("All properties are hidden from SELECT.")
            }
        }
    }

    /**
     * ## alias 重複検査
     * ### SELECT 対象プロパティ同士で alias が重複していないか検査する
     *
     * @param entityMeta 検証対象
     * @param errors エラー格納先
     */
    private fun validateDuplicateAliases(
        entityMeta: EntityMeta,
        errors: MutableList<String>,
    ) {
        val duplicatedAliases = entityMeta.properties
            .filterNot { it.hideFromSelect }
            .groupBy { it.aliasName }
            .filter { (alias, properties) ->
                alias.isNotBlank() && properties.size >= 2
            }

        duplicatedAliases.forEach { (alias, properties) ->
            errors += buildString {
                append("Duplicate alias '")
                append(alias)
                append("' in entity '")
                append(entityMeta.entityName)
                append("'. Properties: ")
                append(properties.joinToString(", ") { it.propertyName })
            }
        }
    }
}