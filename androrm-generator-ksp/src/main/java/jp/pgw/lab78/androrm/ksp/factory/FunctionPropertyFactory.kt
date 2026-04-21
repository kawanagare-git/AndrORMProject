package jp.pgw.lab78.androrm.ksp.factory

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toTypeName
import jp.pgw.lab78.androrm.common.annotation.FunctionProjection
import jp.pgw.lab78.androrm.common.annotation.ReturnHint
import jp.pgw.lab78.androrm.common.database.SupportFunction.toCamelCase
import jp.pgw.lab78.androrm.common.database.annotation.Function
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger.logger
import jp.pgw.lab78.androrm.ksp.logging.LoggerLike

/**
 * ## Function プロパティファクトリークラス
 * ### KSPropertyDeclaration から @Function アノテーションを生成するためのクラス
 * ### @Function アノテーションの引数（function、args、alias、raw、hideFromSelect）を抽出し、適切な値を設定して AnnotationSpec を生成する
 * @author Masahiro Inoue
 * @since 2026-04-21
 */
class FunctionPropertyFactory : LoggerLike by logger {

    companion object {
        private const val F_COLUMN_FUNCTION = "columnFunction"
        private const val F_ARGS = "args"
        private const val F_ALIAS = "alias"
        private const val F_RAW = "raw"
    }

    /**
     * ## @Function リスト生成メソッド
     * ### KSPropertyDeclaration から @Function を生成するためのメソッド
     * ### @Function の引数（function、args、alias、raw、hideFromSelect）を抽出し、適切な値を設定して AnnotationSpec を生成する
     * @param functions 関数プロジェクションのリスト
     * @param propsByName プロパティ名をキー、KSPropertyDeclaration を値とするマップ
     * @return 生成されたプロパティの PropertySpec のリスト
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    fun createAll(
        functions: List<FunctionProjection>,
        propsByName: Map<String, KSPropertyDeclaration>
    ): List<PropertySpec> {
        traceEntered(functions, propsByName)
        // プロパティ名のセットを取得する
        val properties = propsByName.keys
        // 各 FunctionProjection について、引数の検査、戻り値の型の解決、@Function の生成を行い、PropertySpec を作成する
        val result = functions.map { func ->
            checkFunctionArgs(properties, func)
            val typeName = resolveReturnType(func, propsByName)
            val annotation = buildFunctionAnnotation(func)
            // PropertySpec を作成する
            PropertySpec.builder(func.alias.toCamelCase(), typeName)
                .addAnnotation(annotation)
                .build()
        }
        traceExiting(result)
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
        traceEntered(func)
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
        traceExiting(result)
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
        traceEntered(func, propsByName)
        // 関数の種類（ColumnFunction）と引数の型に基づいて、適切な戻り値の型を決定するロジックを実装する
        val argTypes = func.args.mapNotNull { arg ->
            propsByName[arg]?.type?.resolve()?.toTypeName() ?: inferLiteralTypeName(arg)
        }
        // ColumnFunction に基づいて戻り値の型を決定するロジックを実装する
        val result = when (func.function) {
            ColumnFunction.AVG -> DOUBLE
            ColumnFunction.SUM -> LONG.takeIf { argTypes.all { it == INT || it == LONG } }
                ?: DOUBLE.takeIf { argTypes.any { it == DOUBLE || it == FLOAT } }
                ?: ANY

            ColumnFunction.MAX,
            ColumnFunction.MIN -> argTypes.firstOrNull() ?: ANY

            ColumnFunction.COUNT,
            ColumnFunction.COUNT_ALL -> LONG

            ColumnFunction.GROUP_CONCAT -> STRING
            ColumnFunction.COALESCE,
            ColumnFunction.IFNULL,
            ColumnFunction.NULLIF -> argTypes.reduceOrNull(::widerType) ?: ANY

            ColumnFunction.LENGTH -> INT
            ColumnFunction.LOWER,
            ColumnFunction.UPPER,
            ColumnFunction.REPLACE,
            ColumnFunction.SUBSTR,
            ColumnFunction.CONCAT,
            ColumnFunction.TRIM,
            ColumnFunction.LTRIM,
            ColumnFunction.RTRIM -> STRING

            ColumnFunction.RANDOM -> LONG
            ColumnFunction.ROUND -> DOUBLE
            ColumnFunction.DATE,
            ColumnFunction.TIME,
            ColumnFunction.DATETIME,
            ColumnFunction.STRFTIME -> STRING

            ColumnFunction.JULIANDAY -> DOUBLE
            ColumnFunction.ABS -> {
                when (val t = argTypes.firstOrNull()) {
                    LONG, INT -> LONG
                    DOUBLE, FLOAT -> DOUBLE
                    else -> DOUBLE
                }
            }

            ColumnFunction.CUSTOM -> when (func.returnHint) {
                ReturnHint.STRING -> STRING
                ReturnHint.INT -> INT
                ReturnHint.LONG -> LONG
                ReturnHint.DOUBLE -> DOUBLE
                ReturnHint.BOOLEAN -> BOOLEAN
                else -> STRING
            }
        }
        traceExiting(result)
        return result
    }

    /**
     * ## リテラル型推論メソッド
     * ### 引数がプロパティ名に一致しない場合、リテラルとして解釈し、型を推論するためのメソッド
     * ### 引数が真偽値、文字列リテラル、整数リテラル、浮動小数点リテラルのいずれかに一致するかをチェックし、対応する型を返すロジックを実装する
     * @param literal 対象のリテラル文字列
     * @return 推論された型を表す TypeName オブジェクト。リテラルがどの型にも一致しない場合は null を返す。
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    private fun inferLiteralTypeName(literal: String): TypeName? {
        traceEntered(literal)
        // 引数が真偽値、文字列リテラル、整数リテラル、浮動小数点リテラルのいずれかに一致するかをチェックし、対応する型を返すロジックを実装する
        val result = when {
            literal.equals("true", true) || literal.equals("false", true) -> BOOLEAN
            literal.startsWith("'") && literal.endsWith("'") -> STRING
            literal.matches(Regex("^-?\\d+$")) -> LONG
            literal.matches(Regex("^-?\\d+\\.\\d+$")) -> DOUBLE
            else -> null
        }
        traceExiting(result)
        return result
    }

    /**
     * ## 型の広い方を決定するメソッド
     * ### 2 つの型を比較し、より広い方の型を返すためのメソッド
     * ### 型の広さの順序を定義し、引数の型に基づいて適切な型を返すロジックを実装する
     * @param type1 比較対象の最初の型を表す TypeName オブジェクト
     * @param type2 比較対象の2番目の型を表す TypeName オブジェクト
     * @return 2 つの型のうち、より広い方の型を表す TypeName オブジェクト
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    private fun widerType(type1: TypeName, type2: TypeName): TypeName {
        traceEntered(type1, type2)
        // 型の広さの順序を定義し、引数の型に基づいて適切な型を返すロジックを実装する
        val result = when {
            type1 == type2 -> type1
            type1 == STRING || type2 == STRING -> STRING
            type1 == DOUBLE || type2 == DOUBLE || type1 == FLOAT || type2 == FLOAT -> DOUBLE
            type1 == LONG || type2 == LONG -> LONG
            type1 == INT || type2 == INT -> LONG
            else -> ANY
        }
        traceExiting(result)
        return result
    }

    /**
     * ## 関数引数の検査メソッド
     * ### 関数プロジェクションの引数が、プロパティ名のセットに含まれているか、
     * ### またはリテラルとして有効な形式であるかを検査するためのメソッド
     * ### 引数がプロパティ名のセットに含まれていない場合、
     * ### リテラルとして有効な形式（真偽値、文字列リテラル、整数リテラル、浮動小数点リテラルのいずれか）であるかをチェックし、
     * ### そうでない場合はエラーをスローするロジックを実装する
     * @param properties プロパティ名のセット
     * @param functionProjection 対象の FunctionProjection オブジェクト
     * @throws IllegalArgumentException 関数プロジェクションの引数がプロパティ名のセットに含まれておらず、かつリテラルとして有効な形式でもない場合にスローされる例外
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    private fun checkFunctionArgs(
        properties: Set<String>,
        functionProjection: FunctionProjection
    ) {
        traceEntered(functionProjection, properties)
        // 関数プロジェクションの引数が、プロパティ名のセットに含まれているか、またはリテラルとして有効な形式であるかを検査するためのロジックを実装する
        val columnFunction = functionProjection.function
        if (columnFunction == ColumnFunction.CUSTOM) {
            traceExiting()
            return
        }
        // 引数がプロパティ名のセットに含まれていない場合、リテラルとして有効な形式（真偽値、文字列リテラル、整数リテラル、浮動小数点リテラルのいずれか）であるかをチェックし、そうでない場合はエラーをスローするロジックを実装する
        val args = functionProjection.args.toList()
        // 引数が1つだけの場合は、単一の引数として検査し、複数の場合はすべての引数を検査する
        val invalidArgs = if (columnFunction.isSingleArgument) {
            args.firstOrNull()?.takeIf {
                !it.matches(Regex("'[^']*'")) && it !in properties
            }?.let { listOf(it) } ?: emptyList()
        } else {
            args.filter { !it.matches(Regex("'[^']*'")) && it !in properties }
        }
        // 無効な引数が存在する場合はエラーをスローする
        if (invalidArgs.isNotEmpty()) {
            error("Invalid argument for '${functionProjection.alias}': $invalidArgs")
        }
        traceExiting()
    }
}