package jp.pgw.lab78.androrm.ksp.factory

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import jp.pgw.lab78.androrm.common.database.SupportFunction.toSnakeCase
import jp.pgw.lab78.androrm.common.database.annotation.View
import jp.pgw.lab78.androrm.ksp.Constants.TABLE_ALIAS
import jp.pgw.lab78.androrm.ksp.Constants.TABLE_NAME
import jp.pgw.lab78.androrm.ksp.Constants.VIEW

/**
 * ## View アノテーション生成
 * ### ViewDefinitionEntity から生成する Entity へ同じ VIEW 名と派生エイリアスを伝播する
 * @author Masahiro Inoue
 * @since 2026-08-31
 */
class ViewAnnotationFactory {
    /**
     * ## View アノテーション生成
     * @param classDecl VIEW 定義クラス
     * @param aliasExtend 派生エイリアス
     * @return 生成 Entity 用 View アノテーション
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    fun create(classDecl: KSClassDeclaration, aliasExtend: String): AnnotationSpec {
        val annotation = classDecl.annotations.firstOrNull { it.shortName.asString() == VIEW }
        val viewName = annotation?.stringArgument(TABLE_NAME)
            ?.takeIf { it.isNotBlank() }
            ?: classDecl.simpleName.asString().toSnakeCase()
        val baseAlias = annotation?.stringArgument(TABLE_ALIAS)
            ?.takeIf { it.isNotBlank() }
            ?: viewName
        val viewAlias = if (aliasExtend.isBlank()) baseAlias else "${baseAlias}_${aliasExtend}"
        return AnnotationSpec.builder(View::class).apply {
            addMember("$TABLE_NAME = %S", viewName)
            addMember("$TABLE_ALIAS = %S", viewAlias)
        }.build()
    }

    /** アノテーションの文字列引数を取得する。 */
    private fun KSAnnotation.stringArgument(name: String): String? =
        arguments.firstOrNull { it.name?.asString() == name }?.value as? String
}
