package jp.pgw.lab78.androrm.detekt

import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import java.nio.file.Files
import java.nio.file.Path
import java.util.IdentityHashMap

/**
 * ## ソースセット内 Kotlin 型解決
 * ### package、import、完全修飾名を考慮して間接継承を解決する。
 * ### ソースルート単位で宣言をキャッシュし、クラスごとの再走査を防ぐ。
 */
internal class SourceSetTypeResolver {
    private val cache = mutableMapOf<Path, DeclarationIndex>()
    private var lastIndex: DeclarationIndex? = null

    /** 指定クラスが対象マーカーを継承するか判定する。 */
    fun hasSuperType(declaration: KtClass, targetSimpleName: String): Boolean {
        val index = indexFor(declaration.containingKtFile)
        return hasSuperType(declaration, targetSimpleName, index, emptySet())
    }

    /** ソースセット宣言を取得する。 */
    fun indexFor(file: KtFile): DeclarationIndex {
        val root = findSourceRoot(file)
        if (root == null || !Files.isDirectory(root)) {
            return lastIndex ?: DeclarationIndex(
                file.collectDescendantsOfType<KtClass>(),
                emptyMap(),
            )
        }
        val index = cache.getOrPut(root) {
            val psiFactory = KtPsiFactory(file.project, false)
            val declarations = mutableListOf<KtClass>()
            val pathsByFile = IdentityHashMap<KtFile, Path>()
            Files.walk(root).use { paths ->
                paths.filter { path ->
                    Files.isRegularFile(path) && path.fileName.toString().endsWith(".kt")
                }.forEach { path ->
                    val currentPath = path.toAbsolutePath().normalize()
                    val filePath = normalize(file.virtualFile?.path ?: file.virtualFilePath)
                    val parsed = if (currentPath == filePath) {
                        file
                    } else {
                        psiFactory.createFile(path.fileName.toString(), Files.readString(path))
                    }
                    pathsByFile[parsed] = currentPath
                    declarations += parsed.collectDescendantsOfType<KtClass>()
                }
            }
            DeclarationIndex(declarations, pathsByFile)
        }
        lastIndex = index
        return index
    }

    private fun hasSuperType(
        declaration: KtClass,
        targetSimpleName: String,
        index: DeclarationIndex,
        visited: Set<String>,
    ): Boolean {
        val identity = declaration.fqName?.asString() ?: declaration.name ?: return false
        if (identity in visited) return false
        val nextVisited = visited + identity
        return declaration.superTypeListEntries.any { entry ->
            val reference = entry.typeReference?.text?.substringBefore('<')?.trim() ?: return@any false
            index.isCanonicalMarkerReference(declaration, reference, targetSimpleName) ||
                    index.resolve(declaration, reference)?.let {
                        hasSuperType(it, targetSimpleName, index, nextVisited)
                    } == true
        }
    }

    private fun findSourceRoot(file: KtFile): Path? = runCatching {
        val path = file.virtualFile?.path ?: runCatching { file.virtualFilePath }.getOrNull()
            ?: return@runCatching null
        val sourcePath = normalize(path)
        generateSequence(sourcePath.parent) { it.parent }
            .firstOrNull { it.fileName?.toString() in SOURCE_ROOT_NAMES }
    }.getOrNull()

    private fun normalize(path: String): Path {
        val fixed = if (path.length >= 3 && path[0] == '/' && path[2] == ':') path.drop(1) else path
        return Path.of(fixed).toAbsolutePath().normalize()
    }

    /** ソースセットの型宣言一覧。 */
    internal class DeclarationIndex(
        val declarations: List<KtClass>,
        private val pathsByFile: Map<KtFile, Path>,
    ) {
        private val byFqn = declarations.mapNotNull { declaration ->
            declaration.fqName?.asString()?.let { it to declaration }
        }.toMap()
        private val bySimple = declarations.filter { it.name != null }.groupBy { it.name!! }

        /** PSIファイルに対応する実ファイルパスを取得する。 */
        fun pathOf(file: KtFile): Path? = pathsByFile[file]

        /** owner の package/import を基準に参照型を解決する。 */
        fun resolve(owner: KtClass, reference: String): KtClass? {
            byFqn[reference]?.let { return it }
            val file = owner.containingKtFile
            val imported = file.importDirectives.firstNotNullOfOrNull { directive ->
                val path = directive.importPath?.pathStr ?: return@firstNotNullOfOrNull null
                val alias = directive.aliasName
                if ((alias ?: path.substringAfterLast('.')) == reference.substringAfterLast('.')) {
                    byFqn[path]
                } else null
            }
            if (imported != null) return imported
            val samePackage = byFqn["${file.packageFqName.asString()}.$reference"]
            if (samePackage != null) return samePackage
            return bySimple[reference.substringAfterLast('.')].orEmpty().singleOrNull()
        }

        /** AndrORM正式マーカーのFQNとして参照されている場合だけ true を返す。 */
        fun isCanonicalMarkerReference(
            owner: KtClass,
            reference: String,
            targetSimpleName: String,
        ): Boolean {
            val canonical = CANONICAL_MARKERS[targetSimpleName] ?: return false
            if (reference == canonical) return true
            val referenceSimpleName = reference.substringAfterLast('.')
            val file = owner.containingKtFile
            val importedPath = file.importDirectives.firstOrNull { directive ->
                val path = directive.importPath?.pathStr ?: return@firstOrNull false
                val alias = directive.aliasName
                (alias ?: path.substringAfterLast('.')) == referenceSimpleName
            }?.importPath?.pathStr
            if (importedPath != null) return importedPath == canonical
            if (referenceSimpleName != targetSimpleName) return false
            if (reference.contains('.')) return false
            val localCandidates = bySimple[targetSimpleName].orEmpty()
            return localCandidates.isEmpty()
        }
    }

    private companion object {
        val SOURCE_ROOT_NAMES = setOf("java", "kotlin")
        val CANONICAL_MARKERS = mapOf(
            "ViewDefinitionEntity" to
                    "jp.pgw.lab78.androrm.common.dml.interfaces.ViewDefinitionEntity",
            "TableDefinitionEntity" to
                    "jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity",
        )
    }
}
