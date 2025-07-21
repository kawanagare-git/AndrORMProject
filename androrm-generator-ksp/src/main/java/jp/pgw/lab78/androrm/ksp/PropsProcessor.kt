package jp.pgw.lab78.androrm.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import jp.pgw.lab78.androrm.common.GenerateProps
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import kotlin.reflect.KClass

/**
 * ## AndrORM プロパティプロセッサクラス
 * ### AndrORM の標準アノテーションを基に
 * ### プロパティ一覧、データクラス、インターフェース一覧を作成
 * ### PropsProcessorProvider#create(SymbolProcessorEnvironment) から呼び出される
 * @param codeGenerator コード生成時に出力機能を提供
 * @param logger ビルド中の経過、警告、エラー等の情報を出力する環境を提供
 */
class PropsProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    companion object  {
        /** アノテーション完全修飾名 */
        const val ANNOTATION_FQN = "annotationFQN"
        /** アノテーション完全修飾名 */
        const val EMPTY_STRING = ""

        /** @Projection の変数名定義（entityNameExtend） */
        const val EXTEND_NAME = "entityNameExtend"
        /** @Projection の変数名定義（implementsInterface） */
        const val IMPLEMENTS_INTERFACE = "implementsInterface"
        /** @Projection の変数名定義（fields） */
        const val FIELDS = "fields"


        /** 出力先パッケージ */
        const val GENERATED_PACKAGE = "jp.pgw.lab78.androrm.ksp.generated"
        /** プロパティ名一覧 Enum 名 */
        const val GENERATED_PROPERTIES = "AllClassProperties"

        /** 不明 */
        const val UNKNOWN = "Unknown"

        /** @GenerateProps */
        val GENERATE_PROPS = GenerateProps::class.qualifiedName.toString()
        /** @Projection */
        val PROJECTION_FQN = Projection::class.qualifiedName.toString()
        /** @Projection */
        val PROJECTION = Projection::class.simpleName.toString()
        /** @Projections */
        val PROJECTIONS = Projections::class.qualifiedName.toString()
    }
    /**
     * ## AndrORM アノテーションプロセスメソッド
     * ### AndrORM で定義されているアノテーションを解析し
     * ### プロパティ一覧、データクラス、インターフェース一覧を作成し
     * ### Kotlin ファイルを生成する
     * @param resolver アノテーション解析機能を提供
     * @return 解析に失敗したシンボル
     * @see SymbolProcessor.process
     */
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Props 生成
        generateProps(resolver)
        // Projection の検査
        checkProjectionFields(resolver)
        // Projection 生成
        generateProjectionDataClass(resolver)
        return emptyList()
    }

    /**
     * ## プロパティ一覧生成メソッド
     * ### @GenerateProps を抽出しプロパティ一覧を
     * ### Kotlin ファイルとして出力する
     * @param resolver アノテーション解析機能を提供
     */
    private fun generateProps(resolver: Resolver) {
        val symbols = resolver.getSymbolsWithAnnotation(GENERATE_PROPS)
        val classDecls = symbols.filterIsInstance<KSClassDeclaration>()
        val enumEntries = mutableListOf<String>()

        for (classDecl in classDecls) {
            val className = classDecl.simpleName.asString()
            val packageName = classDecl.packageName.asString()

            classDecl.getAllProperties().forEach { property ->
                val propName = property.simpleName.asString()
//                val entryName = "${propName}_${className}_${packageName}"
//                    .replace(".", "_") // パッケージ名に含まれる . を _ に変換
                val entryName = "${propName}_${className}"
                val entry = "$entryName(\"$propName\", \"$className\")"
                enumEntries += entry
            }
        }
        val uniqueEnumEntries = enumEntries.distinct()

        if (enumEntries.isEmpty()) return // 空なら出力しない

        val fileSpec = """
        |package $GENERATED_PACKAGE
        |
        |/**
        | * 自動生成された、全エンティティのプロパティ列挙型。
        | */
        |public enum class $GENERATED_PROPERTIES(
        |    public val propertyName: String,
        |    public val className: String,
        |) {
        |    ${uniqueEnumEntries.joinToString(",\n    ")};
        |}
    """.trimMargin()

        codeGenerator.createNewFile(
            dependencies = Dependencies(false),
            packageName = GENERATED_PACKAGE,
            fileName = GENERATED_PROPERTIES
        ).bufferedWriter().use { writer ->
            writer.write(fileSpec)
        }
    }

    /**
     * ## プロジェクションフィールド配列検査メソッド
     * ### @Projection と @Projections を抽出し定義されている fields 内の
     * ### 文字列が @Projection 適用クラスに存在するプロパティ名か判定する。
     * ### １つでも存在していないプロパティ名が見つかったら、ビルドエラーとして処理する。
     * @param resolver アノテーション解析機能を提供
     */
    private fun checkProjectionFields(resolver: Resolver) {
        // @Projection が付与されたクラスの抽出
        val symbols = resolver.getSymbolsWithAnnotation(PROJECTION_FQN)
        // @Projection
        symbols
            .filterIsInstance<KSClassDeclaration>()
            .forEach { classDecl ->
                val annotation = classDecl.annotations.firstOrNull {
                    val annotationType = it.annotationType.resolve().declaration
                    annotationType.simpleName.asString() == PROJECTION &&
                    annotationType.qualifiedName?.asString() == PROJECTION_FQN
                } ?: return@forEach

                val fieldsValues = (collectFields(annotation)[FIELDS] as? List<*>)
                                ?.filterIsInstance<String>() ?: emptyList()
                val declaredPropertyNames = classDecl.getAllProperties().map { it.simpleName.asString() }.toSet()

                fieldsValues.forEach { field ->
                    if (!declaredPropertyNames.contains(field)) {
                        logger.error("> Field '$field' is not declared in class '${classDecl.simpleName.asString()}'.", classDecl)
                    }
                }
            }
    }


    /**
     * ## データクラス生成メソッド
     * ### @Projection と @Projections を抽出しデータクラスを
     * ### Kotlin ファイルとして出力する
     * @param resolver アノテーション解析機能を提供
     */
    private fun generateProjectionDataClass(resolver: Resolver) {
        val dataClassMaterialMap = mutableMapOf<KClass<*>, Map<String,Any>>()
        val symbols = resolver.getSymbolsWithAnnotation(PROJECTION_FQN,false)
        symbols.filterIsInstance<KSClassDeclaration>()
            .map { classDecl ->
                val annotation = classDecl.annotations
                    .firstOrNull {
                        it.shortName.asString() == Projection::class.simpleName &&
                        it.annotationType.resolve().declaration.qualifiedName?.asString() == PROJECTION_FQN
                }
                logger.warn(">>> Processing classDecl -> annotation:$classDecl to $annotation / ${annotation?.arguments?.size}")
                classDecl to annotation
            }
            .forEach { (classDecl, annotation) ->
                logger.warn(">>> Processing $classDecl: $annotation /  ${annotation?.arguments?.size}")
                dataClassMaterialMap[Projection::class] = collectFields(annotation!!)
            }
//            if (annotationName == "jp.pgw.lab78.androrm.ksp.annotation.Projections") {
//                val value = ann.arguments.find { it.name?.asString() == "value" }?.value
//                val projectionAnnotations = (value as? List<*>)?.mapNotNull { it as? KSAnnotation } ?: continue
//
//                for (projectionAnn in projectionAnnotations) {
//                    val name = projectionAnn.arguments.find { arg -> arg.name?.asString() == "entityNameExtend" }?.value as? String ?: continue
//                    val fields = (projectionAnn.arguments.find { arg -> arg.name?.asString() == "fields" }?.value as? List<*>)
//                        ?.mapNotNull { it as? String } ?: continue
//
//                    val selectedProps = classDecl.getAllProperties()
//                        .filter { fields.contains(it.simpleName.asString()) }
//                        .toList()
//                    logger.info(">>> Processing class: $name in $selectedProps")
//                    projections.add(name to selectedProps)
//                }
//            }
        }

//        val pkg = classDecl.packageName.asString()
//        val originalName = classDecl.simpleName.asString()
//
//        for ((name, props) in projections) {
//            val projectionClassName = "${originalName}${name}"
//            logger.info(">>> Processing class: $projectionClassName")
//            val typeSpec = TypeSpec.classBuilder(projectionClassName)
//                .addModifiers(KModifier.DATA)
//                .primaryConstructor(
//                    FunSpec.constructorBuilder()
//                        .apply {
//                            logger.info(">>> Processing class: $projectionClassName in $props")
//                            for (prop in props) {
//                                val type = prop.type.resolve().toTypeName()
//                                addParameter(prop.simpleName.asString(), type)
//                            }
//                        }
//                        .build()
//                )
//                .apply {
//                    logger.info(">>> Processing class: $projectionClassName in $props")
//                    for (prop in props) {
//                        val type = prop.type.resolve().toTypeName()
//                        addProperty(
//                            PropertySpec.builder(prop.simpleName.asString(), type)
//                                .initializer(prop.simpleName.asString())
//                                .build()
//                        )
//                    }
//                }
//                .build()
//
//            val fileSpec = FileSpec.builder(pkg, projectionClassName)
//                .addType(typeSpec)
//                .build()
//            val fileDependency = classDecl.containingFile?.let { Dependencies(true, it) } ?: Dependencies(false)
//            fileSpec.writeTo(codeGenerator, fileDependency)
//            logger.info(">>> Generated Projection Class: $projectionClassName")
//        }
//    }

    /**
     * ## プロジェクトアノテーション解析処理
     * ### @Projection から引数の値を取得する
     * ### 取得した情報は Map に格納し戻す
     * @param annotation @Projection を抽出した解析対象
     * @return 引数情報（引数 to 値）を格納したマップ
     */
    private fun collectFields(annotation: KSAnnotation): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val annotationFQN = annotation.annotationType.resolve().declaration.qualifiedName?.asString()
        logger.warn(">>> Processing collectFields in with $annotationFQN")
        annotation.arguments.forEach {
            val argName = it.name?.asString()
            if (it.value is List<*> ) {
                val castedValue = it.value as List<*>
                logger.warn(">>> Processing Projection list ${argName to castedValue}")
                val firstElement = castedValue.firstOrNull()
                logger.warn(">>> Processing Projection list firstElement $firstElement")
                val fieldList: List<Any> = when (firstElement) {
                    is KSType -> {
                        logger.warn(">>> Processing Projection list type for ${argName to firstElement}")
                        castedValue.mapNotNull { geneType ->
                            (geneType as KSType).declaration.simpleName.asString()
                        }
                    }
                    is String -> {
                        castedValue.filterIsInstance<String>()
                    }
                    is Int -> {
                        castedValue.filterIsInstance<Int>()
                    }
                    else -> {
                        logger.warn(">>> Processing Unexpected type for " +
                                            "Projection ${firstElement?.javaClass?.name}")
                        emptyList()
                    }
                }
                logger.warn(">>> Processing Projection list type for ${argName to fieldList}")
                result[argName] to fieldList
            } else {
                logger.warn(">>> Processing Projection type for ${argName to it.value}")
                result[argName] to it.value
            }
        }
        return result
    }
}

/**
 * ## プロパティプロセッサ提供元クラス
 * ### ビルドツールから呼び出され、プロパティプロセッサのインスタンスを
 * ### ビルドツールに戻す
 */
class PropsProcessorProvider : SymbolProcessorProvider {

    /**
     * ## プロパティプロセッサ生成メソッド
     * ### プロパティプロセッサのインスタンスを生成する
     * @param environment シンボルプロセッサ環境（ビルドツールから渡される）
     * @return 生成されたプロパティプロセッサのインスタンス
     * @see SymbolProcessorProvider.create
     */
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return PropsProcessor(environment.codeGenerator, environment.logger)
    }
}
