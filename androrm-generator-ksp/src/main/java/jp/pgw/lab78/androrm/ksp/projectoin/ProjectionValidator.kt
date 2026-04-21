package jp.pgw.lab78.androrm.ksp.projectoin

import com.google.devtools.ksp.symbol.KSClassDeclaration
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger.logger
import jp.pgw.lab78.androrm.ksp.logging.LoggerLike

/**
 * ## プロジェクション定義の検証クラス
 * ### プロジェクション定義のプロパティが、対象クラスの宣言されたプロパティに存在するかを検証する
 * ### プロジェクション定義のプロパティと、集約関数のターゲット列が重複していないかを検証する
 * @param logger ロガーインスタンス
 * @author Masahiro Inoue
 * @since 2026-04-17
 */
class ProjectionValidator() : LoggerLike by logger {

    /**
     * ## プロジェクションのプロパティ検証メソッド
     * ### プロジェクション定義のプロパティが、対象クラスの宣言されたプロパティに存在するかを検証する
     * @param classDecl 対象クラスの宣言
     * @param definition プロジェクション定義
     * @param allClassProperties クラスの完全修飾名をキー、プロパティ名のリストを値とするマップ。キャッシュとして使用される
     * @author Masahiro Inoue
     * @since 2026-04-17
     */
    fun validateProperties(
        classDecl: KSClassDeclaration,
        definition: ProjectionDefinition,
        allClassProperties: MutableMap<String, List<String>>
    ) {
        traceEntered(classDecl, definition)
        val fqn = classDecl.qualifiedName?.asString() ?: run {
            error("Annotation target class is null.")
            return
        }
        val declaredProperties = allClassProperties[fqn]
            ?: classDecl.getAllProperties().map { it.simpleName.asString() }.toList()
                .also { allClassProperties[fqn] = it }
        definition.properties.map { it.property }.forEach { property ->
            if (!declaredProperties.contains(property)) {
                error(
                    "property '$property' is not declared in class",
                    classDecl.simpleName.asString()
                )
            }
        }
        traceExiting()
    }

    /**
     * ## 集約関数の競合検証メソッド
     * ### プロジェクション定義のプロパティと、集約関数のターゲット列が重複していないかを検証する
     * @param definition プロジェクション定義
     * @author Masahiro Inoue
     * @since 2026-04-17
     */
    fun validateAggregateConflicts(definition: ProjectionDefinition) {
        traceEntered(definition)
        val propertiesValues = definition.properties
            .map { it.property.trim().lowercase() }
            .toSet()
        val functionTargetColsNormalized = definition.functions
            .filter { it.function != ColumnFunction.CUSTOM }
            .flatMap { func ->
                func.args.map { arg ->
                    arg.trim().substringAfterLast('.').lowercase()
                }
            }
            .toSet()
        val duplicates = propertiesValues.intersect(functionTargetColsNormalized)
        if (duplicates.isNotEmpty()) {
            warning(
                "Projection contains column(s) that are both in properties and used as aggregate targets: " +
                        duplicates.joinToString(", ")
            )
        }
        traceExiting()
    }
}