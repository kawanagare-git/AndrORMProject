package jp.pgw.lab78.androrm.ksp.projectoin

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.Constants.UNKNOWN
import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.FunctionProjection
import jp.pgw.lab78.androrm.common.annotation.ReturnHint
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction.CUSTOM
import jp.pgw.lab78.androrm.common.logging.interfaces.LoggerLike
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger.logger
import jp.pgw.lab78.shared.library.Utils.isNull

/**
 * ## プロジェクション引数パーサークラス
 * ### @Projection アノテーションの引数を KSAnnotation オブジェクトから抽出し、ProjectionDefinition オブジェクトに変換するためのクラス
 * ### 引数の構造に応じて、プロパティ、関数、共通インターフェース、カスタムインターフェースを適切に処理する
 * @author Masahiro Inoue
 * @since 2026-04-17
 */
class ProjectionArgumentParser() : LoggerLike by logger {
    /**
     * ## Projection引数解析用定数
     * ### アノテーション引数名と既定値を保持する
     * @author Masahiro Inoue
     * @since 2026-04-17
     */
    companion object {
        /** @Projection の変数名定義（entityNameExtend） */
        private const val EXTEND_NAME = "entityNameExtend"

        /** @Projection の変数名定義（aliasExtend） */
        private const val EXTEND_ALIAS = "aliasExtend"

        /** @Projection の変数名定義（properties） */
        private const val PROPERTIES = "properties"

        /** @Projection の変数名定義（functions） */
        private const val FUNCTIONS = "functions"

        /** @Projection の変数名定義（commonInterface） */
        private const val COMMON_INTERFACE = "commonInterface"

        /** @Projection の変数名定義（customInterface） */
        private const val CUSTOM_INTERFACE = "customInterface"

        /** @ColumnProjection の変数名定義（property） */
        private const val CP_PROPERTY = "property"

        /** @ColumnProjection の変数名定義（hideFromSelect） */
        private const val CP_HIDE_FROM_SELECT = "hideFromSelect"

        /** @FunctionProjection の変数名定義（function） */
        private const val FP_FUNCTION = "function"

        /** @FunctionProjection の変数名定義（args） */
        private const val FP_ARGS = "args"

        /** @FunctionProjection の変数名定義（alias） */
        private const val FP_ALIAS = "alias"

        /** @FunctionProjection の変数名定義（hideFromSelect） */
        private const val FP_HIDE_FROM_SELECT = "hideFromSelect"

        /** @FunctionProjection の変数名定義（raw） */
        private const val FP_RAW = "raw"

        /** @Function の変数名定義（returnHint） */
        private const val FP_RETURN_HINT = "returnHint"

    }

    /**
     * @Projection アノテーション引数のパース
     * ## この関数は、KSAnnotation オブジェクトから @Projection アノテーションの引数を抽出し、ProjectionDefinition オブジェクトに変換します。
     * ### 引数の構造に応じて、プロパティ、関数、共通インターフェース、カスタムインターフェースを適切に処理します。
     * @param annotation パース対象の @Projection アノテーション
     * @return パース結果の ProjectionDefinition オブジェクト
     * @throws IllegalArgumentException 列挙型の引数値が定義済みの定数と一致しない場合
     * @author Masahiro Inoue
     * @since 2026-04-17
     */
    fun parse(annotation: KSAnnotation): ProjectionDefinition {
        logTraceEntered(annotation)

        var entityNameExtend = EMPTY_STRING
        var aliasExtend = EMPTY_STRING
        var properties: List<ColumnProjection> = emptyList()
        var functions: List<FunctionProjection> = emptyList()
        var commonInterfaces: List<DMLInterfaceEnum> = emptyList()
        var customInterfaces: List<String> = emptyList()

        annotation.arguments.forEach { arg ->
            val argName = arg.name?.asString()
            val value = arg.value
            when (argName) {
                // EXTEND_NAME:String 型で、null の場合は空文字列を使用
                EXTEND_NAME -> entityNameExtend = value as? String ?: EMPTY_STRING
                // EXTEND_ALIAS:String 型で、null の場合は空文字列を使用
                EXTEND_ALIAS -> aliasExtend = value as? String ?: EMPTY_STRING
                // PROPERTIES:List<ColumnProjection> 型で、null の場合は空リストを使用。
                // リストの各要素は KSAnnotation として処理し、ColumnProjection オブジェクトに変換
                PROPERTIES -> {
                    properties = (value as? List<*>)?.mapNotNull { element ->
                        (element as? KSAnnotation)?.let { ksAnn ->
                            ColumnProjection(
                                property = ksAnn.argumentOf<String>(CP_PROPERTY) ?: EMPTY_STRING,
                                hideFromSelect = ksAnn.argumentOf<Boolean>(CP_HIDE_FROM_SELECT)
                                    ?: false
                            )
                        }
                    } ?: emptyList()
                }
                // FUNCTIONS:List<FunctionProjection> 型で、null の場合は空リストを使用。
                // リストの各要素は KSAnnotation として処理し、FunctionProjection オブジェクトに変換
                FUNCTIONS -> {
                    functions = (value as? List<*>)?.mapNotNull { element ->
                        (element as? KSAnnotation)?.let { ksAnn ->
                            val func = ksAnn.argumentOf<ColumnFunction>(FP_FUNCTION)
                            val raw = ksAnn.argumentOf<String>(FP_RAW) ?: EMPTY_STRING
                            if (func.isNull() && raw.isEmpty()) {
                                logError(
                                    "@FunctionProjection requires either 'function' or 'raw' to be specified",
                                    ksAnn
                                )
                            }
                            FunctionProjection(
                                function = func ?: CUSTOM,
                                args = ksAnn.argumentOf<List<String>>(FP_ARGS)?.toTypedArray()
                                    ?: emptyArray(),
                                alias = ksAnn.argumentOf<String>(FP_ALIAS) ?: EMPTY_STRING,
                                returnHint = ksAnn.argumentOf<ReturnHint>(FP_RETURN_HINT)
                                    ?: ReturnHint.AUTO,
                                raw = raw,
                                hideFromSelect = ksAnn.argumentOf<Boolean>(FP_HIDE_FROM_SELECT)
                                    ?: false
                            )
                        }
                    } ?: emptyList()
                }
                // COMMON_INTERFACE:List<DMLInterfaceEnum> 型で、null の場合は空リストを使用。
                // リストの各要素は DMLInterfaceEnum として処理。KSType や String からも
                // DMLInterfaceEnum を抽出できるようにする
                COMMON_INTERFACE -> {
                    commonInterfaces = (value as? List<*>)?.mapNotNull { element ->
                        when (element) {
                            is DMLInterfaceEnum -> element
                            is KSClassDeclaration -> runCatching {
                                DMLInterfaceEnum.valueOf(
                                    element.simpleName.asString()
                                )
                            }.getOrNull()

                            is KSType -> runCatching {
                                DMLInterfaceEnum.valueOf(element.declaration.simpleName.asString())
                            }.getOrNull()

                            is String -> runCatching {
                                DMLInterfaceEnum.valueOf(element)
                            }.getOrNull()

                            else -> null
                        }
                    } ?: emptyList()
                }
                // CUSTOM_INTERFACE:List<String> 型で、null の場合は空リストを使用。
                CUSTOM_INTERFACE -> {
                    customInterfaces = (value as? List<*>)?.filterIsInstance<String>()
                        ?.filter { it.isNotBlank() }
                        ?: emptyList()
                }
            }
        }
        val result = ProjectionDefinition(
            entityNameExtend = entityNameExtend,
            aliasExtend = aliasExtend,
            properties = properties,
            functions = functions,
            commonInterfaces = commonInterfaces,
            customInterfaces = customInterfaces
        )
        logTraceExiting(result)
        return result
    }

    /**
     * ## KSAnnotation から指定された名前の引数を型 T として抽出する拡張関数
     * ### この関数は、KSAnnotation オブジェクトから指定された名前の引数を検索し、その値を型 T として返します。
     * ### 引数が見つからない場合や、型が一致しない場合は null を返します。
     * ### 特に、T が Enum 型の場合は、KSType から Enum 名を抽出して適切に変換します。
     * @param name 抽出する引数の名前
     * @return 引数の値を型 T として返す。引数が見つからない場合や型が一致しない場合は null を返す。
     * @throws IllegalArgumentException 列挙型の引数値が定義済みの定数と一致しない場合
     * @author Masahiro Inoue
     * @since 2026-04-17
     */
    private inline fun <reified T> KSAnnotation.argumentOf(name: String): T? {
        logTraceEntered(name)
        // 引数リストから指定された名前の引数を検索し、その値を取得。
        // 引数が見つからない場合は null を返す
        val argValue = arguments.firstOrNull {
            it.name?.asString() == name
        }?.value ?: run {
            logTraceExiting(UNKNOWN)
            return null
        }
        // T が Enum 型の場合、KSType から Enum 名を抽出して適切に変換
        if (T::class.java.isEnum) {
            val ksType = argValue as? KSType ?: run {
                logTraceExiting(UNKNOWN)
                return null
            }
            // KSType から Enum 名を抽出して、Java の Enum.valueOf を使用して T 型の Enum 値に変換
            val enumName = ksType.declaration.simpleName.asString()
            // KSType から Enum 名を抽出して、Java の Enum.valueOf を使用して T 型の Enum 値に変換
            @Suppress("UNCHECKED_CAST")
            val result =
                java.lang.Enum.valueOf(T::class.java as Class<out Enum<*>>, enumName) as T
            logTraceExiting(result)
            return result
        }
        logTraceExiting(argValue)
        return argValue as? T
    }
}