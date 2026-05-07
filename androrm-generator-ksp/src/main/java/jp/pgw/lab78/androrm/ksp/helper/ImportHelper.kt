package jp.pgw.lab78.androrm.ksp.helper

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import jp.pgw.lab78.androrm.common.logging.interfaces.LoggerLike
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger.logger

/**
 * ## インポート収集ヘルパー
 * ### アノテーションのプロパティとインターフェースから必要なインポートを収集するためのヘルパーオブジェクト
 * @param logger KSP のログ出力機能
 * @author Masahiro Inoue
 * @since 2026-02-26
 */
class ImportHelper() : LoggerLike by logger {
    /**
     * ## インポート収集
     * ### コンストラクタのプロパティとインターフェースから必要なインポートを収集する
     * @param constructorProps コンストラクタのプロパティリスト
     * @param interfaces クラスが実装するインターフェースのリスト
     * @return 必要なインポートのセット
     * @author Masahiro Inoue
     * @since 2026-02-26
     */
    fun collectImports(
        tableAnnotation: AnnotationSpec,
        constructorProps: List<PropertySpec>,
        interfaces: List<TypeName>
    ): Set<String> {
        logTraceEntered(constructorProps, interfaces)
        val imports = mutableSetOf<String>()
        // Table アノテーション
        imports += tableAnnotation.typeName.toString()
        constructorProps.forEach { prop ->
            // プロパティの型
            val type = prop.type.toString()
            if (!type.startsWith("kotlin.")) {
                imports += type
            }
            // アノテーション
            prop.annotations.forEach { ann ->
                imports += ann.typeName.toString()
                // Function の columnFunction の enum 型も拾う
                ann.members.forEach { member ->
                    val text = member.toString()
                    if ("ColumnFunction" in text) {
                        imports += "jp.pgw.lab78.androrm.common.database.function.ColumnFunction"
                    }
                }
            }
        }
        // インターフェース
        interfaces.forEach {
            imports += it.toString()
        }
        val result = imports
            .filter { "." in it }
            .filterNot { it.startsWith("kotlin.") }
            .toSet()
        logTraceExiting()
        return result
    }
}