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
            val generateCursorMapper =
                interfaces.any { it.toString().substringAfterLast('.') == "SelectEntity" } &&
                        canGenerateCursorMapper(constructorProps)
            val generateStringMapMapper =
                interfaces.isNotEmpty() && canGenerateStringMapMapper(constructorProps)
            if (generateCursorMapper || generateStringMapMapper) {
                writer.appendLine(" {")
                appendMapperCompanion(
                    writer = writer,
                    classNameFQN = classNameFQN,
                    properties = constructorProps,
                    generateCursorMapper = generateCursorMapper,
                    generateStringMapMapper = generateStringMapMapper,
                )
                writer.appendLine("}")
            } else {
                writer.appendLine()
            }
        }
        logTraceExiting(classNameFQN)
    }

    /**
     * ## Mapper companion object生成
     * ### Cursor Mapperと文字列Map Mapperを一つのcompanion objectへまとめて生成する
     * @param writer 生成ソース出力先
     * @param classNameFQN 生成Entityの完全修飾名
     * @param properties コンストラクタプロパティ
     * @param generateCursorMapper Cursor Mapperを生成する場合true
     * @param generateStringMapMapper 文字列Map Mapperを生成する場合true
     */
    private fun appendMapperCompanion(
        writer: java.io.BufferedWriter,
        classNameFQN: ClassName,
        properties: List<GeneratedProperty>,
        generateCursorMapper: Boolean,
        generateStringMapMapper: Boolean,
    ) {
        val mapperInterfaces = buildList {
            if (generateCursorMapper) {
                add(
                    "jp.pgw.lab78.androrm.database.CursorEntityMapper<${classNameFQN.simpleName}>"
                )
            }
            if (generateStringMapMapper) {
                add(
                    "jp.pgw.lab78.androrm.database.StringMapEntityMapper<${classNameFQN.simpleName}>"
                )
            }
        }
        writer.appendLine()
        writer.appendLine("  /** KSP生成Entityの直接転送Mapper。 */")
        writer.appendLine(
            "  public companion object : ${mapperInterfaces.joinToString(", ")} {",
        )
        if (generateCursorMapper) {
            appendCursorMapper(writer, classNameFQN, properties)
        }
        if (generateStringMapMapper) {
            appendStringMapMapper(writer, classNameFQN, properties)
        }
        writer.appendLine("  }")
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
                typeName = simpleTypeName(property),
                index = index,
                nullable = property.propertySpec.type.isNullable,
            )
        }
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
    }

    /**
     * ## 文字列Map Mapper生成
     * ### DBカラム名をキーとするMap<String, String?>と生成Entityの相互変換処理を生成する
     * @param writer 生成ソース出力先
     * @param classNameFQN 生成Entityの完全修飾名
     * @param properties コンストラクタプロパティ
     */
    private fun appendStringMapMapper(
        writer: java.io.BufferedWriter,
        classNameFQN: ClassName,
        properties: List<GeneratedProperty>,
    ) {
        val columnLiterals = properties.map { property ->
            kotlinStringLiteral(property.columnName)
        }
        writer.appendLine()
        writer.appendLine("    /** Map変換対象となるDBカラム名。 */")
        writer.appendLine(
            "    private val stringMapColumnNames: Set<String> = setOf(${columnLiterals.joinToString(", ")})"
        )
        writer.appendLine()
        writer.appendLine("    /** DBカラム名Mapを生成Entityへ直接転送する。 */")
        writer.appendLine(
            "    override fun fromMap(row: Map<String, String?>, ignoreUnknownColumns: Boolean): ${classNameFQN.simpleName} {"
        )
        writer.appendLine("      if (!ignoreUnknownColumns) {")
        writer.appendLine(
            "        row.keys.firstOrNull { columnName -> columnName !in stringMapColumnNames }?.let { columnName ->"
        )
        writer.appendLine(
            "          throw IllegalArgumentException(\"Column [\$columnName] does not exist in ${classNameFQN.simpleName}.\")"
        )
        writer.appendLine("        }")
        writer.appendLine("      }")
        writer.appendLine("      return ${classNameFQN.simpleName}(")
        properties.forEach { property ->
            val reader = requireNotNull(stringMapValueReader(property, classNameFQN))
            writer.appendLine(
                "        ${property.propertySpec.name} = $reader,"
            )
        }
        writer.appendLine("      )")
        writer.appendLine("    }")
        writer.appendLine()
        writer.appendLine("    /** 生成EntityをDBカラム名Mapへ直接転送する。 */")
        writer.appendLine(
            "    override fun toMap(entity: ${classNameFQN.simpleName}): Map<String, String?> = linkedMapOf("
        )
        properties.forEach { property ->
            val columnLiteral = kotlinStringLiteral(property.columnName)
            val writerExpression = requireNotNull(stringMapValueWriter(property))
            writer.appendLine("      $columnLiteral to $writerExpression,")
        }
        writer.appendLine("    )")

        if (properties.any { simpleTypeName(it) == "Boolean" }) {
            writer.appendLine()
            writer.appendLine("    /** Boolean文字列を厳密に変換する。 */")
            writer.appendLine(
                "    private fun parseBoolean(columnName: String, value: String): Boolean = when (value.lowercase()) {"
            )
            writer.appendLine("      \"1\", \"true\" -> true")
            writer.appendLine("      \"0\", \"false\" -> false")
            writer.appendLine(
                "      else -> throw IllegalArgumentException(\"Column [\$columnName] has invalid Boolean value [\$value].\")"
            )
            writer.appendLine("    }")
        }

        if (properties.any { simpleTypeName(it) == "ByteArray" }) {
            writer.appendLine()
            writer.appendLine("    /** 16進数文字列をByteArrayへ変換する。 */")
            writer.appendLine(
                "    private fun decodeHex(columnName: String, value: String): ByteArray {"
            )
            writer.appendLine(
                "      require(value.length % 2 == 0) { \"Column [\$columnName] has invalid hexadecimal value [\$value].\" }"
            )
            writer.appendLine("      return ByteArray(value.length / 2) { index ->")
            writer.appendLine("        val high = value[index * 2].digitToIntOrNull(16)")
            writer.appendLine("        val low = value[index * 2 + 1].digitToIntOrNull(16)")
            writer.appendLine(
                "        require(high != null && low != null) { \"Column [\$columnName] has invalid hexadecimal value [\$value].\" }"
            )
            writer.appendLine("        ((high shl 4) or low).toByte()")
            writer.appendLine("      }")
            writer.appendLine("    }")
            writer.appendLine()
            writer.appendLine("    /** ByteArrayを大文字16進数文字列へ変換する。 */")
            writer.appendLine("    private fun ByteArray.toHexString(): String {")
            writer.appendLine("      val hex = \"0123456789ABCDEF\"")
            writer.appendLine("      return buildString(size * 2) {")
            writer.appendLine("        this@toHexString.forEach { byte ->")
            writer.appendLine("          val value = byte.toInt() and 0xFF")
            writer.appendLine("          append(hex[value ushr 4])")
            writer.appendLine("          append(hex[value and 0x0F])")
            writer.appendLine("        }")
            writer.appendLine("      }")
            writer.appendLine("    }")
        }
    }

    /** 全プロパティを既存のValueFromCursorで読み取れるか確認する。 */
    private fun canGenerateCursorMapper(properties: List<GeneratedProperty>): Boolean =
        properties.filterNot { it.hideFromSelect }.all { property ->
            cursorValueReader(
                typeName = simpleTypeName(property),
                index = 0,
                nullable = property.propertySpec.type.isNullable,
            ) != null
        }

    /** 全プロパティを文字列Mapとの相互変換対象として扱えるか確認する。 */
    private fun canGenerateStringMapMapper(properties: List<GeneratedProperty>): Boolean =
        properties.all { property ->
            simpleTypeName(property) in STRING_MAP_SUPPORTED_TYPES
        }

    /** GeneratedPropertyの型単純名を取得する。 */
    private fun simpleTypeName(property: GeneratedProperty): String =
        property.propertySpec.type.toString()
            .removeSuffix("?")
            .substringAfterLast('.')

    /** MapからEntityへ設定する1プロパティ分の式を生成する。 */
    private fun stringMapValueReader(
        property: GeneratedProperty,
        classNameFQN: ClassName,
    ): String? {
        val typeName = simpleTypeName(property)
        if (typeName !in STRING_MAP_SUPPORTED_TYPES) return null
        val columnLiteral = kotlinStringLiteral(property.columnName)
        val conversion = when (typeName) {
            "Int" -> "value.toInt()"
            "Long" -> "value.toLong()"
            "Float" -> "value.toFloat()"
            "Double" -> "value.toDouble()"
            "Boolean" -> "parseBoolean($columnLiteral, value)"
            "String" -> "value"
            "LocalDate" -> "java.time.LocalDate.parse(value)"
            "LocalTime" -> "java.time.LocalTime.parse(value)"
            "LocalDateTime" -> "java.time.LocalDateTime.parse(value)"
            "ByteArray" -> "decodeHex($columnLiteral, value)"
            else -> return null
        }
        return if (property.propertySpec.type.isNullable) {
            "row[$columnLiteral]?.let { value -> $conversion }"
        } else {
            "requireNotNull(row[$columnLiteral]) { " +
                    kotlinStringLiteral(
                        "Column [${property.columnName}] is missing or null for ${classNameFQN.simpleName}."
                    ) +
                    " }.let { value -> $conversion }"
        }
    }

    /** EntityからMapへ設定する1プロパティ分の式を生成する。 */
    private fun stringMapValueWriter(property: GeneratedProperty): String? {
        val typeName = simpleTypeName(property)
        if (typeName !in STRING_MAP_SUPPORTED_TYPES) return null
        val access = "entity.${property.propertySpec.name}"
        return when (typeName) {
            "String" -> access
            "Boolean" -> if (property.propertySpec.type.isNullable) {
                "$access?.let { value -> if (value) \"true\" else \"false\" }"
            } else {
                "if ($access) \"true\" else \"false\""
            }
            "ByteArray" -> if (property.propertySpec.type.isNullable) {
                "$access?.toHexString()"
            } else {
                "$access.toHexString()"
            }
            else -> if (property.propertySpec.type.isNullable) {
                "$access?.toString()"
            } else {
                "$access.toString()"
            }
        }
    }

    /** Kotlinソースへ埋め込む文字列リテラルを生成する。 */
    private fun kotlinStringLiteral(value: String): String =
        buildString {
            append('"')
            value.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
            append('"')
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

    companion object {
        /** 文字列Mapとの相互変換に対応するKotlin型。 */
        private val STRING_MAP_SUPPORTED_TYPES = setOf(
            "Int",
            "Long",
            "Float",
            "Double",
            "Boolean",
            "String",
            "LocalDate",
            "LocalTime",
            "LocalDateTime",
            "ByteArray",
        )
    }
}