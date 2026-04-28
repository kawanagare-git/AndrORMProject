package jp.pgw.lab78.androrm.common.meta

/**
 * ## Entity メタ情報検証結果
 * ### Validator が返す共通結果
 *
 * @property errors エラー一覧
 * @property warnings 警告一覧
 * @author Masahiro Inoue
 * @since 2026-04-27
 */
data class EntityMetaValidationResult(
    /** エラー一覧 */
    val errors: List<String> = emptyList(),
    /** 警告一覧 */
    val warnings: List<String> = emptyList(),
) {
    /** エラーを持つか */
    val hasErrors: Boolean
        get() = errors.isNotEmpty()

    /** 警告を持つか */
    val hasWarnings: Boolean
        get() = warnings.isNotEmpty()
}