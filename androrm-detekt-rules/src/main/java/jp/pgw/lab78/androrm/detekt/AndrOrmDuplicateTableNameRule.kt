package jp.pgw.lab78.androrm.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import jp.pgw.lab78.androrm.detekt.AndrOrmDetektMessages.duplicateTableName
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import java.nio.file.Path
import java.util.Locale

/**
 * ## AndrORM テーブル名重複検査ルール
 * ### 同一ソースセット内の異なる TableDefinitionEntity（間接継承を含む）が、同じ明示指定の @Table.name を持つ場合に報告する
 * ### SELECT・DML用Entityなど、TableDefinitionEntity以外のクラスは検査対象外とする
 * @param config Detekt ルール設定情報
 * @author Masahiro Inoue
 * @since 2026-07-20
 */
class AndrOrmDuplicateTableNameRule(
    config: Config = Config.empty,
) : Rule(config) {

    /** package/import を解決する継承リゾルバ */
    private val typeResolver = SourceSetTypeResolver()

    /** ルール定義情報 */
    override val issue: Issue = Issue(
        id = "AndrOrmDuplicateTableNameRule",
        severity = Severity.Defect,
        description = "Checks duplicate explicit table names among TableDefinitionEntity classes.",
        debt = Debt.FIVE_MINS,
    )

    /**
     * ## Kotlinファイル訪問
     * ### 現在のファイルと同じソースセットにあるテーブル定義を比較し、重複側のEntityを報告する
     * @param file 解析対象のKotlinファイル
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    override fun visitKtFile(file: KtFile) {
        val currentSourcePath = normalizeSourcePath(file.virtualFilePath)
        val currentDefinitions = extractTableDefinitions(file, currentSourcePath)
        val allDefinitions = collectSourceSetDefinitions(
            currentFile = file,
            currentSourcePath = currentSourcePath,
            currentDefinitions = currentDefinitions,
        )

        currentDefinitions.forEach { currentDefinition ->
            val firstDefinition = allDefinitions
                .filter { definition ->
                    definition.normalizedTableName == currentDefinition.normalizedTableName &&
                            definition.identity != currentDefinition.identity
                }
                .minByOrNull { definition -> definition.identity }
            if (firstDefinition != null && firstDefinition.identity < currentDefinition.identity) {
                report(
                    CodeSmell(
                        issue = issue,
                        entity = Entity.from(currentDefinition.declaration),
                        message = duplicateTableName(
                            tableName = currentDefinition.tableName,
                            firstEntity = firstDefinition.className,
                            duplicateEntity = currentDefinition.className,
                        ),
                    )
                )
            }
        }

        super.visitKtFile(file)
    }

    /**
     * ## ソースセット内テーブル定義収集
     * ### javaまたはkotlinソースルート配下のKotlinファイルを解析し、テーブル定義を収集する
     * @param currentFile 現在解析中のKotlinファイル
     * @param currentSourcePath 現在解析中ファイルの正規化済みパス
     * @param currentDefinitions 現在解析中ファイルから抽出済みのテーブル定義
     * @return 同一ソースセット内のテーブル定義
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    private fun collectSourceSetDefinitions(
        currentFile: KtFile,
        currentSourcePath: String,
        currentDefinitions: List<TableDefinition>,
    ): List<TableDefinition> {
        val currentPath = Path.of(currentSourcePath)
        if (findSourceRoot(currentPath) == null) return currentDefinitions
        val definitions = mutableListOf<TableDefinition>()
        val index = typeResolver.indexFor(currentFile)
        index.declarations.map { it.containingKtFile }
            .distinctBy {
                index.pathOf(it)?.toString() ?: it.virtualFile?.path ?: it.virtualFilePath
            }
            .forEach { sourceFile ->
            val normalizedPath = normalizeSourcePath(
                index.pathOf(sourceFile)?.toString()
                    ?: sourceFile.virtualFile?.path
                    ?: sourceFile.virtualFilePath,
                    )
                    definitions.addAll(extractTableDefinitions(sourceFile, normalizedPath))
                }
        return definitions
    }

    /**
     * ## ソースルート取得
     * ### 解析対象ファイルから最も近いjavaまたはkotlinディレクトリを取得する
     * @param sourcePath 解析対象ファイルのパス
     * @return ソースルート。特定できない場合はnull
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    private fun findSourceRoot(sourcePath: Path): Path? =
        generateSequence(sourcePath.parent) { path -> path.parent }
            .firstOrNull { path -> path.fileName?.toString() in SOURCE_ROOT_NAMES }

    /**
     * ## テーブル定義抽出
     * ### TableDefinitionEntityを継承し、明示的な@Table.nameを持つクラスを抽出する
     * @param file 抽出対象のKotlinファイル
     * @param sourcePath 抽出対象ファイルの正規化済みパス
     * @return 抽出したテーブル定義
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    private fun extractTableDefinitions(
        file: KtFile,
        sourcePath: String,
    ): List<TableDefinition> {
        return file.collectDescendantsOfType<KtClass>()
            .filter { declaration -> typeResolver.hasSuperType(declaration, TABLE_DEFINITION_ENTITY) }
        .mapNotNull { declaration ->
            val tableName = declaration.annotationEntries
                .firstOrNull { annotation -> annotation.shortName?.asString() == TABLE_ANNOTATION }
                ?.explicitTableName()
                ?.takeIf { name -> name.isNotBlank() }
                ?: return@mapNotNull null
            val simpleClassName = declaration.name ?: return@mapNotNull null
            val enclosingClassNames = generateSequence(declaration.parent) { element -> element.parent }
                .filterIsInstance<KtClass>()
                .mapNotNull { enclosingClass -> enclosingClass.name }
                .toList()
                .asReversed()
            val className = listOf(file.packageFqName.asString())
                .plus(enclosingClassNames)
                .plus(simpleClassName)
                .filter { namePart -> namePart.isNotBlank() }
                .joinToString(".")
            TableDefinition(
                tableName = tableName,
                normalizedTableName = tableName.uppercase(Locale.ROOT),
                className = className,
                identity = "$sourcePath#$className",
                declaration = declaration,
            )
        }
    }

    /**
     * ## 明示テーブル名取得
     * ### @Tableのname名前付き引数、または第1位置引数から文字列リテラルを取得する
     * @receiver Tableアノテーション
     * @return 明示指定されたテーブル名。文字列リテラルでない場合はnull
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    private fun KtAnnotationEntry.explicitTableName(): String? {
        val nameArgument = valueArguments
            .firstOrNull { argument -> argument.getArgumentName()?.asName?.identifier == "name" }
            ?: valueArguments.firstOrNull()
        val expression = nameArgument?.getArgumentExpression() as? KtStringTemplateExpression
            ?: return null
        return expression.literalValue()
    }

    /**
     * ## 文字列リテラル値取得
     * ### 文字列テンプレートを除外し、通常文字列とエスケープ文字からリテラル値を復元する
     * @receiver Kotlin文字列式
     * @return 復元した文字列。式展開を含む場合はnull
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    private fun KtStringTemplateExpression.literalValue(): String? {
        if (entries.any { entry -> entry is KtStringTemplateEntryWithExpression }) return null
        val value = StringBuilder()
        entries.forEach { entry ->
            when (entry) {
                is KtLiteralStringTemplateEntry -> value.append(entry.text)
                is KtEscapeStringTemplateEntry -> value.append(entry.unescapedValue)
                else -> return null
            }
        }
        return value.toString()
    }

    /**
     * ## ソースパス正規化
     * ### DetektのWindows仮想ファイルパス先頭のスラッシュを除き、絶対・正規化済みパスへ変換する
     * @param sourcePath 変換対象パス
     * @return 正規化済みパス
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    private fun normalizeSourcePath(sourcePath: String): String {
        val systemPath = if (
            sourcePath.length >= WINDOWS_VIRTUAL_PATH_MIN_LENGTH &&
            sourcePath.first() == '/' &&
            sourcePath[WINDOWS_DRIVE_SEPARATOR_INDEX] == ':'
        ) {
            sourcePath.drop(1)
        } else {
            sourcePath
        }
        return Path.of(systemPath).toAbsolutePath().normalize().toString()
    }

    /**
     * ## テーブル定義情報
     * @property tableName 明示指定されたテーブル名
     * @property normalizedTableName 大文字へ正規化した比較用テーブル名
     * @property className Entityの完全修飾クラス名
     * @property identity ファイルパスとクラス名から成る一意識別子
     * @property declaration 報告対象のクラス宣言
     * @author Masahiro Inoue
     * @since 2026-07-20
     */
    private data class TableDefinition(
        val tableName: String,
        val normalizedTableName: String,
        val className: String,
        val identity: String,
        val declaration: KtClass,
    )

    private companion object {
        /** Tableアノテーションの短縮名 */
        const val TABLE_ANNOTATION = "Table"

        /** テーブル定義Entityインターフェースの短縮名 */
        const val TABLE_DEFINITION_ENTITY = "TableDefinitionEntity"

        /** Windows仮想ファイルパス判定に必要な最小文字数 */
        const val WINDOWS_VIRTUAL_PATH_MIN_LENGTH = 4

        /** Windowsドライブ文字直後のコロン位置 */
        const val WINDOWS_DRIVE_SEPARATOR_INDEX = 2

        /** Kotlinソースルートとして扱うディレクトリ名 */
        val SOURCE_ROOT_NAMES = setOf("java", "kotlin")
    }
}
