package jp.pgw.lab78.androrm.common.meta

/**
 * ## Entity メタ情報検証クラス
 * ### KSP と Select 系クラスから共通利用する
 * ### 判定だけを担当し、例外送出や logger 出力は呼び出し元に委ねる
 * #### 現時点では以下を検査する。
 * - @Column と @Function の併用
 * - 全列 hideFromSelect = true
 * - alias 重複
 * @author Masahiro Inoue
 * @since 2026-04-27
 */
class EntityMetaValidator {
    /** エラーを保持するためのプロパティ */
    private val errors = mutableListOf<String>()

    /** ワーニングを保持するためのプロパティ */
    private val warnings = mutableListOf<String>()

    /**
     * ## Entity メタ情報検証
     * ### 共通ルールに基づいて EntityMeta を検証する
     * @param entityMeta 検証対象 Entity メタ情報
     * @return 検証結果
     * @author Masahiro Inoue
     * @since 2026-04-27
     */
    fun validate(entityMeta: EntityMeta): EntityMetaValidationResult {
        // 各検査を実行
        validateDualAnnotation(entityMeta)
        validateNoSelectableProperties(entityMeta)
        validateDuplicateAliases(entityMeta)
        return EntityMetaValidationResult(
            errors = errors,
            warnings = warnings,
        )
    }

    /**
     * ## @Column と @Function の併用検査
     * ### 1 プロパティに @Column と @Function が同時付与されていないか検査する
     * @param entityMeta 検証対象
     * @author Masahiro Inoue
     * @since 2026-04-27
     */
    private fun validateDualAnnotation(
        entityMeta: EntityMeta,
    ) {
        // プロパティの取得
        entityMeta.properties
            // @Column と @Function の併用検査
            .filter { it.hasColumnAnnotation && it.hasFunctionAnnotation }
            .forEach { property ->
                errors += "Property '${property.propertyName}' in entity '${entityMeta.entityName}' " +
                        "cannot have both @Column and @Function. " +
                        "Source entity: '${entityMeta.defineEntityQualifiedName}'."
            }
    }

    /**
     * ## SELECT 対象ゼロ件検査
     * ### 全プロパティが hideFromSelect = true になっていないか検査する
     * @param entityMeta 検証対象
     * @author Masahiro Inoue
     * @since 2026-04-27
     */
    private fun validateNoSelectableProperties(
        entityMeta: EntityMeta,
    ) {
        // SELECT 対象プロパティのリストを取得
        val visibleProperties = entityMeta.properties.filterNot { it.hideFromSelect }
        // 全プロパティが hideFromSelect = true になっていないか検査する
        if (visibleProperties.isEmpty()) {
            errors += "Entity '${entityMeta.entityName}' derived from '${entityMeta.defineEntityQualifiedName}' " +
                    "has no selectable properties. All properties are hidden from SELECT."
        }
    }

    /**
     * ## alias 重複検査
     * ### SELECT 対象プロパティ同士で alias が重複していないか検査する
     * @param entityMeta 検証対象
     * @author Masahiro Inoue
     * @since 2026-04-27
     */
    private fun validateDuplicateAliases(
        entityMeta: EntityMeta,
    ) {
        val groupedByAlias = entityMeta.properties
            .filter { it.aliasName.isNotBlank() }
            .groupBy { it.aliasName }
            .filterValues { it.size >= 2 }
        // alias ごとにプロパティをグループ化し、重複している alias を抽出する
        groupedByAlias.forEach { (alias, properties) ->
            val visibleCount = properties.count { !it.hideFromSelect }
            // 重複している alias を error と warning に振り分ける
            when {
                // SELECT 対象プロパティに重複 alias が含まれている場合はエラーとする
                visibleCount >= 2 -> {
                    errors += "Duplicate alias '$alias' in entity '${entityMeta.entityName}' " +
                            "defined in '${entityMeta.defineEntityQualifiedName}'."
                }
                // SELECT 対象プロパティに重複 alias が含まれていない場合は警告とする
                else -> {
                    warnings +=
                        "Query construction is not affected, but alias '$alias' " +
                                "appears multiple times in entity '${entityMeta.entityName}' " +
                                "defined in '${entityMeta.defineEntityQualifiedName}'."
                }
            }
        }
    }
}