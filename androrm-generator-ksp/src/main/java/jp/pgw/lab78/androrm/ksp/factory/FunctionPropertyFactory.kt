package jp.pgw.lab78.androrm.ksp.factory

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import jp.pgw.lab78.androrm.common.annotation.FunctionProjection
import jp.pgw.lab78.androrm.common.database.SupportFunction.toCamelCase
import jp.pgw.lab78.androrm.common.database.annotation.Function
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction
import jp.pgw.lab78.androrm.common.logging.interfaces.LoggerLike
import jp.pgw.lab78.androrm.ksp.Constants.F_ALIAS
import jp.pgw.lab78.androrm.ksp.Constants.F_ARGS
import jp.pgw.lab78.androrm.ksp.Constants.F_COLUMN_FUNCTION
import jp.pgw.lab78.androrm.ksp.Constants.F_RAW
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger.logger


/**
 * ## Function プロパティファクトリークラス
 * ### FunctionProjection から @Function アノテーション付きプロパティを生成するためのクラス
 * ### 関数定義と元プロパティ情報から戻り値型を解決し、GeneratedProperty を生成する
 * @author Masahiro Inoue
 * @since 2026-04-21
 */
class FunctionPropertyFactory : LoggerLike by logger {

    /**
     * ## @Function リスト生成メソッド
     * ### FunctionProjection のリストから @Function アノテーション付きプロパティを生成する
     * @param functions 関数プロジェクションのリスト
     * @param propsByName プロパティ名をキー、KSPropertyDeclaration を値とするマップ
     * @return 生成されたプロパティと SELECT 非表示情報を保持する GeneratedProperty のリスト
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    fun createAll(
        functions: List<FunctionProjection>,
        propsByName: Map<String, KSPropertyDeclaration>
    ): List<GeneratedProperty> {
        logTraceEntered(functions, propsByName)
        // プロパティ名のセットを取得する
        val properties = propsByName.keys
        // 各 FunctionProjection について、引数の検査、戻り値の型の解決、@Function の生成を行い、PropertySpec を作成する
        val result = functions.map { func ->
            checkFunctionArgs(func, properties)
            val typeName = resolveReturnType(func, propsByName).let { resolvedType ->
                if (func.hideFromSelect) {
                    resolvedType.copy(nullable = true)
                } else {
                    resolvedType
                }
            }
            val annotation = buildFunctionAnnotation(func)
            // PropertySpec を作成する
            GeneratedProperty(
                propertySpec = PropertySpec.builder(func.alias.toCamelCase(), typeName)
                    .addAnnotation(annotation)
                    .build(),
                hideFromSelect = func.hideFromSelect,
            )
        }
        logTraceExiting(result)
        return result
    }

    /**
     * ## @Function 生成メソッド
     * ### FunctionProjection から @Function を生成するためのメソッド
     * ### @Function の引数（function、args、alias、raw、hideFromSelect）を抽出し、適切な値を設定して AnnotationSpec を生成する
     * @param func 対象の FunctionProjection オブジェクト
     * @return 生成された @Function アノテーションの AnnotationSpec オブジェクト
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    private fun buildFunctionAnnotation(func: FunctionProjection): AnnotationSpec {
        logTraceEntered(func)
        // FunctionProjection から @Function を生成するためのビルダーを作成し、引数を設定して AnnotationSpec を生成する
        val result = AnnotationSpec.builder(Function::class).apply {
            val sqlFunc =
                if (func.raw.isNotBlank()) ColumnFunction.CUSTOM else func.function
            // ColumnFunction を引数として設定する
            addMember("$F_COLUMN_FUNCTION = %T.%L", ColumnFunction::class, sqlFunc.name)
            addMember("$F_ALIAS = %S", func.alias)
            // raw 引数が空でない場合は raw を設定し、そうでない場合は args をリテラルとして設定する
            if (func.raw.isNotBlank()) {
                addMember("$F_RAW = %S", func.raw)
            } else {
                val argsLiteral = func.args.joinToString(", ") { "\"$it\"" }
                addMember("$F_ARGS = [%L]", argsLiteral)
            }
            // hideFromSelect 引数を設定する
            addMember("hideFromSelect = %L", func.hideFromSelect)
        }.build()
        logTraceExiting(result)
        return result
    }

    /**
     * ## 戻り値の型解決メソッド
     * ### FunctionProjection とプロパティのマップから、関数の戻り値の型を解決するためのメソッド
     * ### 関数の種類（ColumnFunction）と引数の型に基づいて、適切な戻り値の型を決定するロジックを実装する
     * @param func 対象の FunctionProjection オブジェクト
     * @param propsByName プロパティ名をキー、KSPropertyDeclaration を値とするマップ
     * @return 解決された戻り値の型を表す TypeName オブジェクト
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    private fun resolveReturnType(
        func: FunctionProjection,
        propsByName: Map<String, KSPropertyDeclaration>
    ): TypeName {
        logTraceEntered(func, propsByName)
        // 明示されたreturnHintは、SQL関数の型推論結果より優先する。
        func.returnHint.type?.qualifiedName?.let { hintedType ->
            val hintedResult = ClassName.bestGuess(hintedType)
            logTraceExiting(hintedResult)
            return hintedResult
        }
        // 関数の種類（ColumnFunction）と引数の型に基づいて、適切な戻り値の型を決定するロジックを実装する
        val argTypes = func.args.mapNotNull { arg ->
            propsByName[arg]?.type?.resolve()?.declaration?.qualifiedName?.asString()
                ?: inferLiteralTypeName(arg)
        }
        // ColumnFunction に基づいて戻り値の型を決定するロジックを実装する
        val result = func.function.getReturnType(argTypes).let { ClassName.bestGuess(it) }
        logTraceExiting(result)
        return result
    }

    /**
     * ## リテラル型推論メソッド
     * ### 引数がプロパティ名に一致しない場合、リテラルとして解釈し、型を推論するためのメソッド
     * ### 引数が真偽値、文字列リテラル、整数リテラル、浮動小数点リテラルのいずれかに一致するかをチェックし、対応する型を返すロジックを実装する
     * @param literal 対象のリテラル文字列
     * @return 推論された型の完全修飾名。リテラルがどの型にも一致しない場合は null
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    private fun inferLiteralTypeName(literal: String): String? {
        logTraceEntered(literal)
        // 引数が真偽値、文字列リテラル、整数リテラル、浮動小数点リテラルのいずれかに一致するかをチェックし、対応する型を返すロジックを実装する
        val result = when {
            literal.equals("true", true) || literal.equals(
                "false",
                true
            ) -> Boolean::class.qualifiedName

            literal.startsWith("'") && literal.endsWith("'") -> String::class.qualifiedName
            literal.matches(Regex("^-?\\d+$")) -> Long::class.qualifiedName
            literal.matches(Regex("^-?\\d+\\.\\d+$")) -> Double::class.qualifiedName
            else -> null
        }
        logTraceExiting(result)
        return result
    }

    /**
     * ## 関数引数の検査メソッド
     * ### 関数プロジェクションの引数が、プロパティ名のセットに含まれているか、
     * ### またはリテラルとして有効な形式であるかを検査するためのメソッド
     * ### 引数がプロパティ名のセットに含まれていない場合、
     * ### リテラルとして有効な形式（真偽値、文字列リテラル、整数リテラル、浮動小数点リテラルのいずれか）であるかをチェックし、
     * ### そうでない場合はエラーをスローするロジックを実装する
     * @param functionProjection 対象の FunctionProjection オブジェクト
     * @param propertyNames プロパティ名のセット
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    private fun checkFunctionArgs(
        functionProjection: FunctionProjection,
        propertyNames: Collection<String>,
    ) {
        logTraceEntered(propertyNames, functionProjection)
        // 関数プロジェクションの引数を全走査
        functionProjection.args.forEach { arg ->
            if (!isSupportedFunctionArg(arg, propertyNames)) {
                logError(
                    buildString {
                        append("Function argument '")
                        append(arg)
                        append("' in projection function '")
                        append(functionProjection.alias)
                        append("' is invalid. ")
                        append("Allowed values are property names, string literals, numeric literals, and boolean literals.")
                    }
                )
            }
        }
        logTraceExiting()
    }

    /**
     * ## サポート関数引数判定メソッド
     * ### 関数でサポートされている引数であるかを判定するメソッド
     * @param arg 引数
     * @param propertyNames プロパティ名のリスト
     * @return 引数がサポートされている場合は true、そうでない場合は false
     * @author Masahiro Inoue
     * @since 2026-05-02
     */
    private fun isSupportedFunctionArg(
        arg: String,
        propertyNames: Collection<String>,
    ): Boolean {
        // 引数文字列を先頭と末尾の空白文字を除去
        val trimmed = arg.trim()
        return trimmed in propertyNames ||
                isStringLiteral(trimmed) ||
                isBooleanLiteral(trimmed) ||
                isNumericLiteral(trimmed)
    }

    /**
     * ## 文字列リテラル判定
     * ### 文字列が文字列値として扱えるか判定
     * @param value 対象の文字列
     * @author Masahiro Inoue
     * @since 2026-05-02
     */
    private fun isStringLiteral(value: String): Boolean =
        value.length >= 2 && value.startsWith("'") && value.endsWith("'")

    /**
     * ## ブール値リテラル判定
     * ### 文字列がブール値として扱えるか判定
     * @param value 対象の文字列
     * @author Masahiro Inoue
     * @since 2026-05-02
     */
    private fun isBooleanLiteral(value: String): Boolean =
        value.equals("true", ignoreCase = true) ||
                value.equals("false", ignoreCase = true)

    /**
     * ## 数字リテラル判定
     * ### 文字列が数字として扱えるか判定
     * @param value 対象の文字列
     * @author Masahiro Inoue
     * @since 2026-05-02
     */
    private fun isNumericLiteral(value: String): Boolean =
        value.matches(Regex("""[+-]?\d+""")) ||
                value.matches(Regex("""[+-]?\d+\.\d+"""))
}