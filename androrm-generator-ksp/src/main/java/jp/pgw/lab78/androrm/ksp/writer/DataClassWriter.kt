package jp.pgw.lab78.androrm.ksp.writer

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import jp.pgw.lab78.androrm.common.Constants.COMMA
import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.logging.interfaces.LoggerLike
import jp.pgw.lab78.androrm.ksp.factory.GeneratedProperty
import jp.pgw.lab78.androrm.ksp.helper.AnnotationHelper
import jp.pgw.lab78.androrm.ksp.helper.ImportHelper
import jp.pgw.lab78.androrm.ksp.helper.TypeHelper
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger.logger

/**
 * ## データクラス出力クラス
 * ### クラス名、プロパティ、アノテーション、インターフェースを基に、データクラスのコードを生成する
 * @param codeGenerator KSP の CodeGenerator インスタンス
 * @param importHelper インポートヘルパーインスタンス
 * @param annotationHelper アノテーションヘルパーインスタンス
 * @param typeHelper 型ヘルパーインスタンス
 * @author Masahiro Inoue
 * @since 2026-04-21
 */
class DataClassWriter(
    private val codeGenerator: CodeGenerator,
    private val importHelper: ImportHelper,
    private val annotationHelper: AnnotationHelper,
    private val typeHelper: TypeHelper,
) : LoggerLike by logger {

    /**
     * ## データクラス出力メソッド
     * ### クラス名、プロパティ、アノテーション、インターフースを基に、データクラスのコードを生成する
     * @param classNameFQN 出力するクラスの完全修飾名
     * @param tableAnnotationSpec クラスに付与するアノテーションの AnnotationSpec
     * @param normalProps クラスのプロパティのリスト（通常のプロパティ）
     * @param functionProps クラスのプロパティのリスト（関数プロパティ）
     * @param interfaces クラスが実装するインターフェースのリスト
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    fun write(
        classNameFQN: ClassName,
        tableAnnotationSpec: AnnotationSpec,
        normalProps: List<GeneratedProperty>,
        functionProps: List<GeneratedProperty>,
        interfaces: List<TypeName>
    ) {
        logTraceEntered(classNameFQN, tableAnnotationSpec, normalProps, functionProps, interfaces)
        // コンストラクタのプロパティは、通常のプロパティと関数プロパティを結合したリストとする
        val constructorProps = normalProps + functionProps
        val propertySpecs = constructorProps.map { it.propertySpec }
        // データクラスのコードを生成する
        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(false),
            packageName = classNameFQN.packageName,
            fileName = classNameFQN.simpleName
        )
        // コード生成の際に、クラスに付与するアノテーション、プロパティの型、インターフェースの型を基に、必要なインポートを収集する
        file.bufferedWriter().use { writer ->
            val imports = importHelper.collectImports(
                tableAnnotationSpec,
                propertySpecs,
                interfaces
            )
            // クラスの package 名、インポート文、クラス宣言を出力する
            if (classNameFQN.packageName.isNotBlank()) {
                writer.appendLine("package ${classNameFQN.packageName}")
                writer.appendLine()
            }
            // インポート文を出力する
            imports.sorted().forEach { importFqn ->
                writer.appendLine("import $importFqn")
            }
            // クラス宣言を出力する
            writer.appendLine()
            writer.appendLine(annotationHelper.getSimpleName(tableAnnotationSpec))
            // インターフェースのリストをカンマ区切りの文字列に変換する
            val interfaceText =
                if (interfaces.isEmpty()) {
                    EMPTY_STRING
                } else {
                    interfaces.joinToString(", ") { typeHelper.getSimpleName(it) }
                }
            // クラス宣言を出力する
            writer.appendLine("public data class ${classNameFQN.simpleName}(")
            // コンストラクタのプロパティを出力する
            constructorProps.forEachIndexed { index, generatedProperty ->
                val prop = generatedProperty.propertySpec
                prop.annotations.forEach { ann ->
                    writer.appendLine("  ${annotationHelper.getSimpleName(ann)}")
                }
                // プロパティの型を基に、必要なインポートを収集する
                val comma = if (index == constructorProps.lastIndex) EMPTY_STRING else COMMA
                val defaultValue = if (generatedProperty.hideFromSelect) {
                    " = null"
                } else {
                    EMPTY_STRING
                }
                writer.appendLine(
                    "  public val ${prop.name}: ${typeHelper.getSimpleName(prop)}$defaultValue$comma"
                )
                writer.appendLine()
            }
            // クラス宣言の閉じ括弧を出力する
            writer.append(")")
            if (interfaceText.isNotBlank()) {
                writer.append(" : $interfaceText")
            }
            if (interfaces.any { it.toString().substringAfterLast('.') == "SelectEntity" }
                && canGenerateCursorMapper(constructorProps)) {
                writer.appendLine(" {")
                appendCursorMapper(writer, classNameFQN, constructorProps)
                writer.appendLine("}")
            } else {
            writer.appendLine()
        }
        }
        logTraceExiting(classNameFQN)
    }

    /**
     * ## Cursor Mapper生成
     * ### SELECT Entity専用のCursorからコンストラクタへの直接転送処理を生成する
     * @param writer 生成ソース出力先
     * @param classNameFQN 生成Entityの完全修飾名
     * @param properties コンストラクタプロパティ
     */
    private fun appendCursorMapper(
        writer: java.io.BufferedWriter,
        classNameFQN: ClassName,
        properties: List<GeneratedProperty>,
    ) {
        val visibleProperties = properties.filterNot { it.hideFromSelect }
        val readers = visibleProperties.mapIndexed { index, property ->
            property to cursorValueReader(
                typeName = property.propertySpec.type.toString()
                    .removeSuffix("?")
                    .substringAfterLast('.'),
                index = index,
                nullable = property.propertySpec.type.isNullable,
            )
        }
        writer.appendLine()
        writer.appendLine("  /** KSP生成SELECT EntityのCursor直接Mapper。 */")
        writer.appendLine(
            "  public companion object : jp.pgw.lab78.androrm.database.CursorEntityMapper<${classNameFQN.simpleName}> {",
        )
        writer.appendLine("    /** Cursorの現在行を生成Entityへ直接転送する。 */")
        writer.appendLine(
            "    override fun map(cursor: android.database.Cursor, columnIndexes: IntArray): ${classNameFQN.simpleName} {",
        )
        writer.appendLine("      return ${classNameFQN.simpleName}(")
        var visibleIndex = 0
        properties.forEach { property ->
            if (property.hideFromSelect) {
                return@forEach
            }
            val reader = readers[visibleIndex].second!!
            writer.appendLine(
                "        ${property.propertySpec.name} = $reader,"
            )
            visibleIndex++
        }
        writer.appendLine("      )")
        writer.appendLine("    }")
        writer.appendLine("  }")
    }

    /** 全プロパティを既存のValueFromCursorで読み取れるか確認する。 */
    private fun canGenerateCursorMapper(properties: List<GeneratedProperty>): Boolean =
        properties.filterNot { it.hideFromSelect }.all { property ->
            cursorValueReader(
                typeName = property.propertySpec.type.toString()
                    .removeSuffix("?")
                    .substringAfterLast('.'),
                index = 0,
                nullable = property.propertySpec.type.isNullable,
            ) != null
        }

    /** ValueFromCursorのenum名をKotlin型名から解決する。 */
    private fun cursorValueReader(typeName: String, index: Int, nullable: Boolean): String? {
        val valueFromCursor = when (typeName) {
            "Int" -> "INT"
            "Long" -> "LONG"
            "Float" -> "FLOAT"
            "Double" -> "DOUBLE"
            "Boolean" -> "BOOLEAN"
            "String" -> "STRING"
            "LocalDate" -> "LOCAL_DATE"
            "LocalTime" -> "LOCAL_TIME"
            "LocalDateTime" -> "LOCAL_DATE_TIME"
            "ByteArray" -> "BYTE_ARRAY"
            else -> return null
        }
        val cursorRead =
            "jp.pgw.lab78.androrm.database.validation.ValueFromCursor.$valueFromCursor." +
                    "getValueFromCursor(cursor, columnIndexes[$index]) as $typeName"
        return if (nullable) {
            "(if (cursor.isNull(columnIndexes[$index])) null else $cursorRead)"
        } else {
            cursorRead
        }
    }
}