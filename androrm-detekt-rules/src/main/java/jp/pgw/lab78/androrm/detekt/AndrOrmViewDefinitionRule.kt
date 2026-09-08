package jp.pgw.lab78.androrm.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import java.util.Locale

/**
 * ## AndrORM VIEW 定義検査ルール
 * ### @View と ViewDefinitionEntity の対応、@Table 併用、VIEW 名重複、テーブル名衝突を検出する
 * @param config Detekt ルール設定情報
 * @author Masahiro Inoue
 * @since 2026-08-31
 */
class AndrOrmViewDefinitionRule(config: Config = Config.empty) : Rule(config) {
    /** ルール定義情報 */
    override val issue = Issue(
        id = "AndrOrmViewDefinitionRule",
        severity = Severity.Defect,
        description = "Checks AndrORM VIEW definitions and relation name collisions.",
        debt = Debt.FIVE_MINS,
    )

    /** 既に確認した明示 relation 名 */
    private val relationByName = mutableMapOf<String, RelationDefinition>()

    /** package/import を解決する継承リゾルバ */
    private val typeResolver = SourceSetTypeResolver()

    /**
     * ## Kotlin ファイル訪問
     * @param file 解析対象ファイル
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    override fun visitKtFile(file: KtFile) {
        file.collectDescendantsOfType<KtClass>().forEach { declaration ->
            validateDeclaration(declaration)
            registerRelation(declaration)
        }
        super.visitKtFile(file)
    }

    /**
     * ## VIEW 宣言構造検証
     * @param declaration クラス宣言
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    private fun validateDeclaration(declaration: KtClass) {
        if (declaration.isInterface()) return
        val hasView = declaration.hasAnnotation(VIEW)
        val hasTable = declaration.hasAnnotation(TABLE)
        val hasViewMarker = typeResolver.hasSuperType(declaration, VIEW_DEFINITION_ENTITY)
        if (hasView && !hasViewMarker) {
            report(declaration, "@View requires ViewDefinitionEntity: ${declaration.name}")
        }
        if (hasViewMarker && !hasView) {
            report(declaration, "ViewDefinitionEntity requires @View: ${declaration.name}")
        }
        if (hasView && hasTable) {
            report(declaration, "A class cannot have both @Table and @View: ${declaration.name}")
        }
    }

    /**
     * ## relation 名登録
     * @param declaration クラス宣言
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    private fun registerRelation(declaration: KtClass) {
        val kind = when {
            declaration.hasAnnotation(VIEW) -> VIEW
            declaration.hasAnnotation(TABLE) && typeResolver.hasSuperType(
                declaration,
                TABLE_DEFINITION_ENTITY,
            ) -> TABLE
            else -> return
        }
        val annotation = declaration.annotationEntries.first { it.shortName?.asString() == kind }
        val name = annotation.explicitName()?.takeIf { it.isNotBlank() } ?: return
        val normalized = name.uppercase(Locale.ROOT)
        val current = RelationDefinition(name, kind, declaration.name.orEmpty())
        val previous = relationByName.putIfAbsent(normalized, current)
        if (previous != null && previous.className != current.className) {
            report(
                declaration,
                "SQLite relation name `$name` is duplicated by " +
                        "${previous.kind} `${previous.className}` and $kind `${current.className}`.",
            )
        }
    }

    /** 指定アノテーションを持つ場合 true を返す。 */
    private fun KtClass.hasAnnotation(name: String): Boolean =
        annotationEntries.any { it.shortName?.asString() == name }

    /** @Table/@View の明示 name を取得する。 */
    private fun KtAnnotationEntry.explicitName(): String? {
        val argument = valueArguments
            .firstOrNull { it.getArgumentName()?.asName?.identifier == "name" }
            ?: valueArguments.firstOrNull()
        val expression = argument?.getArgumentExpression() as? KtStringTemplateExpression
            ?: return null
        if (expression.entries.any { it.text.startsWith("\$") }) return null
        return expression.entries.joinToString("") { it.text }.trim('"')
    }

    /** finding を報告する。 */
    private fun report(declaration: KtClass, message: String) {
        report(CodeSmell(issue, Entity.from(declaration), message))
    }

    /** relation 定義情報 */
    private data class RelationDefinition(
        val name: String,
        val kind: String,
        val className: String,
    )

    private companion object {
        const val VIEW = "View"
        const val TABLE = "Table"
        const val VIEW_DEFINITION_ENTITY = "ViewDefinitionEntity"
        const val TABLE_DEFINITION_ENTITY = "TableDefinitionEntity"
    }
}
