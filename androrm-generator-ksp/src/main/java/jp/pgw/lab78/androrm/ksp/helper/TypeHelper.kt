package jp.pgw.lab78.androrm.ksp.helper

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import jp.pgw.lab78.androrm.ksp.logging.LoggerLike

/**
 * ## 型ヘルパー
 * ### KSP の型関連の処理を補助するヘルパークラス
 * @param logger KSP のログ出力機能
 * @author Masahiro Inoue
 * @since 2026-02-26
 */
class TypeHelper(logger: LoggerLike) : LoggerLike by logger {
    /**
     * ## 型の単純名取得
     * ### プロパティの型から単純名を取得する
     * @param prop プロパティスペック
     * @return 型の単純名
     * @author Masahiro Inoue
     * @since 2026-02-27
     */
    fun getSimpleName(prop: PropertySpec): String {
        traceEntered(prop)
        val result = getSimpleName(prop.type)
        traceExiting(result)
        return result
    }

    /**
     * ## 型の単純名取得（TypeName版）
     * ### TypeName から単純名を取得する（ClassName と ParameterizedTypeName に対応）
     * @param type TypeName
     * @return 型の単純名
     * @author Masahiro Inoue
     * @since 2026-02-27
     */
    fun getSimpleName(type: TypeName): String {
        traceEntered(type)
        val base = when (type) {
            is ClassName -> type.simpleNames.joinToString(".") // ネスト型対応
            is ParameterizedTypeName -> {
                val raw = getSimpleName(type.rawType)
                val args = type.typeArguments.joinToString(", ") { getSimpleName(it) }
                "$raw<$args>"
            }

            else -> {
                // 最後の保険。未知のTypeNameは文字列ベースで軽く短縮。
                type.toString().substringAfterLast(".")
            }
        }
        val result = if (type.isNullable) "$base?" else base
        traceExiting(result)
        return result
    }
}