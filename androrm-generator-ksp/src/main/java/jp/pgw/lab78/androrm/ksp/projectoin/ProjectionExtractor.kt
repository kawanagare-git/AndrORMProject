package jp.pgw.lab78.androrm.ksp.projectoin

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger.logger
import jp.pgw.lab78.androrm.ksp.logging.LoggerLike

/**
 * ## プロジェクション抽出クラス
 * ### KSP の Resolver を使用して、@Projection および @Projections アノテーションが付与されたクラスを検索し、KSClassDeclaration オブジェクトのリストを返す
 * ### 各クラスから @Projection アノテーションを抽出し、KSAnnotation オブジェクトのリストを返す
 * @author Masahiro Inoue
 * @since 2026-04-17
 */
class ProjectionExtractor() : LoggerLike by logger {

    companion object {
        /** @Projection の完全修飾名 */
        private val PROJECTION_FQN = Projection::class.qualifiedName!!

        /** @Projectionsの完全修飾名 */
        private val PROJECTIONS_FQN = Projections::class.qualifiedName!!
    }

    /** ## プロジェクションクラス検索メソッド
     * ### KSP の Resolver を使用して、@Projection および @Projections アノテーションが
     * ### 付与されたクラスを検索し、KSClassDeclaration オブジェクトのリストを返す
     * @param resolver KSP の Resolver インスタンス
     * @return @Projection および @Projections アノテーションが付与されたクラスの KSClassDeclaration オブジェクトのリスト
     * @author Masahiro Inoue
     * @since 2026-04-17
     */
    @OptIn(KspExperimental::class)
    fun findProjectionClasses(resolver: Resolver): List<KSClassDeclaration> {
        traceEntered(resolver)
        // KSP の Resolver を使用して、@Projection および @Projections アノテーションが付与されたクラスを検索する
        val result = sequenceOf(
            resolver.getSymbolsWithAnnotation(PROJECTION_FQN, false),
            resolver.getSymbolsWithAnnotation(PROJECTIONS_FQN, false)
        ).flatten()
            .filterIsInstance<KSClassDeclaration>()
            .toList()
        traceExiting(result)
        return result
    }

    /**
     * ## アノテーション抽出メソッド
     * ### 各クラスから @Projection アノテーションを抽出し、KSAnnotation オブジェクトのリストを返す
     * @param classDecl 対象クラスの宣言
     * @return クラスから抽出された @Projection アノテーションの KSAnnotation オブジェクトのリスト
     * @author Masahiro Inoue
     * @since 2026-04-17
     */
    fun extractFromClass(classDecl: KSClassDeclaration): List<KSAnnotation> {
        traceEntered(classDecl)
        // クラスのアノテーションを走査し、@Projection および @Projections アノテーションを抽出する
        val result = classDecl.annotations.flatMap { annotation ->
            when (annotation.annotationType.resolve().declaration.qualifiedName?.asString()) {
                PROJECTION_FQN -> listOf(annotation)
                PROJECTIONS_FQN -> extractProjectionList(annotation)
                else -> emptyList()
            }
        }
        traceExiting(result)
        return result.toList()
    }

    /**
     * ## プロジェクションリスト抽出メソッド
     * ### @Projections アノテーションの引数から、@Projection アノテーションのリストを抽出する
     * @param annotation @Projections アノテーションの KSAnnotation オブジェクト
     * @return @Projections アノテーションの引数から抽出された @Projection アノテーションの KSAnnotation オブジェクトのリスト
     * @author Masahiro Inoue
     * @since 2026-04-17
     */
    private fun extractProjectionList(annotation: KSAnnotation): List<KSAnnotation> {
        traceEntered(annotation)
        // @Projections アノテーションの引数から、@Projection アノテーションのリストを抽出する
        val result = (annotation.arguments
            .firstOrNull { it.name?.asString() == "value" }
            ?.value as? List<*>)
            ?.filterIsInstance<KSAnnotation>()
            ?: emptyList()
        traceExiting(result)
        return result
    }
}