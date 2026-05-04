package jp.pgw.lab78.androrm.ksp.helper

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import jp.pgw.lab78.androrm.common.MessageConstants.ERROR_COLUMN_FUNCTION_FULLY_QUALIFIED_NAME
import jp.pgw.lab78.androrm.common.MessageConstants.ERROR_COLUMN_FUNCTION_SIMPLE_NAME
import jp.pgw.lab78.androrm.common.database.annotation.Function
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger.logger
import jp.pgw.lab78.androrm.ksp.logging.LoggerLike

/**
 * ## アノテーションヘルパー
 * ### KSP のアノテーション関連の処理を補助するヘルパークラス
 * @param logger KSP のログ出力機能
 * @author Masahiro Inoue
 * @since 2026-02-27
 */
class AnnotationHelper() : LoggerLike by logger {
    /**
     * ## アノテーションの値取得
     * ### アノテーションのメンバーから値を取得する
     * @param annotationSpec アノテーションスペック
     * @return メンバーの値（存在しない場合は null）
     * @author Masahiro Inoue
     * @since 2026-02-26
     */
    fun getSimpleName(annotationSpec: AnnotationSpec): String {
        logTraceEntered(annotationSpec)
        val annTypeFqn = annotationSpec.typeName.toString()
        val annTypeSimple = (annotationSpec.typeName as? ClassName)?.simpleNames?.joinToString(".")
            ?: annTypeFqn.substringAfterLast(".")
        val result = when (annTypeSimple) {
            "Function" -> getFunctionAnnotation(annotationSpec)
                .replace("`", "")
                .replaceFirst(
                    "${Function::class.qualifiedName}",
                    "${Function::class.simpleName}"
                )

            else -> changeSimpleName(annotationSpec, annTypeFqn, annTypeSimple)
        }
        logTraceEntered(result)
        return result
    }

    /**
     * ## Function アノテーション以外の処理
     * ### FQN を シンプルネイムに置換する
     * @param annotationSpec アノテーションスペック
     * @param annTypeFqn アノテーションの型の完全修飾名
     * @param annTypeSimple アノテーションの型の単純名
     * @return 置換後のアノテーション文字列
     * @author Masahiro Inoue
     * @since 2026-03-09
     */
    private fun changeSimpleName(
        annotationSpec: AnnotationSpec,
        annTypeFqn: String,
        annTypeSimple: String
    ): String {
        logTraceEntered(annotationSpec, annTypeFqn, annTypeSimple)
        val result = annotationSpec.toString().replaceFirst("@$annTypeFqn", "@$annTypeSimple")
        logTraceEntered(result)
        return result
    }

    /**
     * ## Function アノテーションの特別処理
     * ### Function アノテーションは引数の FQN を シンプルネイムに置換する
     * ```
     * jp...ColumnFunction.COUNT → ColumnFunction.COUNT
     * ```
     * @return 置換後のアノテーション文字列
     * @author Masahiro Inoue
     * @since 2026-03-09
     */
    private fun getFunctionAnnotation(annotationSpec: AnnotationSpec): String {
        logTraceEntered(annotationSpec)
        val fqnClassName =
            checkNotNull(ColumnFunction::class.qualifiedName) { ERROR_COLUMN_FUNCTION_FULLY_QUALIFIED_NAME }
        val simpleClassName =
            checkNotNull(ColumnFunction::class.simpleName) { ERROR_COLUMN_FUNCTION_SIMPLE_NAME }
        logDebug("Replace string", "base = $annotationSpec", fqnClassName, "->", simpleClassName)
        val result = annotationSpec.toString().replaceFirst(fqnClassName, simpleClassName)
        logTraceExiting(result)
        return result
    }

}