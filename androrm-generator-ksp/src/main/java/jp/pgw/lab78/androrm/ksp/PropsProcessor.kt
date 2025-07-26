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
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import jp.pgw.lab78.androrm.common.GenerateProps
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import jp.pgw.lab78.androrm.common.dml.DMLInterfaceEnum
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
        const val PROPERTIES = "properties"


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

    val allClassProperties = mutableMapOf<String,List<String>>()

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
     * ### 2025/07/31 KSP 上でアノテーション変数に配列を指定しても正しく取得できない
     * ### 加えて、モジュール間の参照の関係で、循環してしまうので、
     * ### 下記の enum クラスの生成は意味をなさない。
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
                val entryName = "${propName}_${className}_${packageName}"
                    .replace(".", "_") // パッケージ名に含まれる . を _ に変換
                val entry = "$entryName(\"$propName\", \"${packageName}.$className\")"
                enumEntries += entry
            }
        }
        val uniqueEnumEntries = enumEntries.distinct()
        // 空なら出力しない
        if (enumEntries.isEmpty()) return
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
        // enum クラスの生成
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
        symbols.filterIsInstance<KSClassDeclaration>()
            .forEach { classDecl ->
                val annotation = classDecl.annotations.firstOrNull {
                    // KSP のバグのため再度 クラス名と クラス FQN でフィルタをかける
                    val annotationType = it.annotationType.resolve().declaration
                    annotationType.simpleName.asString() == PROJECTION &&
                    annotationType.qualifiedName?.asString() == PROJECTION_FQN
                } ?: return@forEach
                // リスト化された fields の値
                val fieldsValues = collectFields(annotation)[PROPERTIES] as? List<*> ?: emptyList<String>()
                logger.info("> Fields is '$fieldsValues'.")
                // @Projection が適用されたクラスのプロパティ名一覧を取得
                // ただし、allClassProperties に登録済みならば、allClassProperties からプロパティ名一覧を取得
                val fqn = classDecl.qualifiedName?.asString()
                if (fqn == null){
                    logger.error("Annotation target class is null.")
                    return@forEach
                }
                val properties = allClassProperties[fqn]
                    ?: classDecl.getAllProperties().map {
                        // プロパティ名を取得
                        it.simpleName.asString()
                    }.toList()
                // allClassProperties[fqn] が、null 時の再代入
                allClassProperties[fqn] = properties
                // fields に指定されたプロパティ名の検査
                fieldsValues.forEach { field ->
                    if (!properties.contains(field)) {
                        logger.error(">>>> Field '$field' is not declared in class '${classDecl.simpleName.asString()}'.", classDecl)
                    }
                }
            }
    }


    /**
     * ## @Projection によるデータクラス生成メソッド
     * ### @Projection が付与されているクラスの情報を基に
     * ### 新たに data class を生成し、
     * ### Kotlin ファイルとして出力する
     * @param resolver アノテーション解析機能を提供
     */
    private fun generateProjectionDataClass(resolver: Resolver) {
        // @Projection が付与されたクラスを抽出
        val symbols = resolver.getSymbolsWithAnnotation(PROJECTION_FQN, false)
        // 抽出した Sequence<KSAnnotated> を基に data class 生成に必要な情報を取得
        symbols.filterIsInstance<KSClassDeclaration>()
                // @Projection がトップレベルに付与されたクラスを抽出（KSP のバグ対策）
                .mapNotNull { classDecl ->
                    val annotation = classDecl.annotations.firstOrNull {
                        it.shortName.asString() == PROJECTION &&
                        it.annotationType.resolve().declaration.qualifiedName?.asString() == PROJECTION_FQN
                    } ?: return@mapNotNull null
                    // クラス情報とアノテーション情報を戻す（次工程の forEach に譲渡）
                    classDecl to annotation
                }
                // クラス情報とアノテーション情報をを基にdata class 生成に必要な情報を取得
                .forEach { (classDecl, annotation) ->
                    // data class の素材情報を格納するマップ
                    val dataClassMaterialMap = collectFields(annotation).toMutableMap()
                    // パッケージ名を取得
                    val packageName = classDecl.packageName.asString()
                    // クラス名の生成（@Projection 付与クラス名 + @Projection の派生名）
                    val createClassName = classDecl.simpleName.asString() +
                            dataClassMaterialMap[EXTEND_NAME].toString()
                    // @Projection に定義してあるプロパティ名を付与クラスのプロパティ名から抽出
                    val selectedProps = classDecl.getAllProperties()
                        .filter { (dataClassMaterialMap[PROPERTIES] as List<*>)
                                    .contains(it.simpleName.asString())
                        }
                        .toList()
                    // date class 生成（ファイルを書き出せる状態にする）
                    val fileSpec = createDataClassFile(ClassName( packageName, createClassName), selectedProps)
                    val fileDependency = classDecl.containingFile
                                        ?.let { Dependencies(true, it) }
                                        ?: Dependencies(false)
                    // date class kt ファイル生成
                    fileSpec.writeTo(codeGenerator, fileDependency)
                    logger.info(">>> Generated Projection Class: $createClassName")
                }
    }

    /**
     * ## データクラス生成メソッド
     * ### 指定されたクラスから、指定されたプロパティのみを含む
     * ### データクラスを KotlinPoet で生成する
     * @param packageName 出力パッケージ
     * @param className 生成するクラス名
     * @param selectedProps プロパティ一覧（KSP の KSPropertyDeclaration）
     * @return FileSpec（Kotlin ファイル）
     */
    private fun createDataClassFile(
        classNameFQN: ClassName,
        selectedProps: List<KSPropertyDeclaration>
    ): FileSpec {
        // クラスビルダー
        val classBuilder = TypeSpec.classBuilder(classNameFQN)
            .addModifiers(KModifier.DATA)
        // コンストラクタビルダー
        val constructorBuilder = FunSpec.constructorBuilder()
        // プロパティ一覧からプロパティ名とプロパティ型を取得し data class の構成要素にする
        selectedProps.forEach { prop ->
            val name = prop.simpleName.asString()
            val type = prop.type.resolve().toTypeName()
            constructorBuilder.addParameter(name, type)
            classBuilder.addProperty(
                PropertySpec.builder(name, type)
                    .initializer(name)
                    .build()
            )
        }
        // クラスビルダーにプライマリィコンストラクタの構成を追加する
        classBuilder.primaryConstructor(constructorBuilder.build())
        // ファイルの構成要素としてクラスを追加し呼び出し元へ戻す
        return FileSpec.builder(classNameFQN)
            .addType(classBuilder.build())
            .build()
    }

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
        // アノテーションの引数を取得し加工
        annotation.arguments.forEach {
            val argName = it.name?.asString()
            val value = it.value
            if (value is List<*> ) {
                val castedValue = it.value as List<*>
                logger.warn(">>> Processing collectFields Projection list" +
                                    " ${argName to castedValue}")
                val firstElement = castedValue.firstOrNull()
                logger.warn(">>> Processing collectFields Projection list" +
                                    " firstElement $firstElement")
                val fieldList: List<Any> = when (firstElement) {
                    is KSType -> {
                        logger.warn(">>> Processing collectFields Projection list" +
                                            " type for ${argName to firstElement}")
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
                logger.warn(">>> Processing Projection collectFields list" +
                                    " type for ${argName to fieldList}")
                argName?.let { result[it] = fieldList }
            } else {
                logger.warn(">>> Processing Projection collectFields ${argName to it.value}")
                argName?.let {key -> result[key] = value?: EMPTY_STRING }
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

/**
 * ## パッケージとインターフェースのリレーションクラス
 * ###
 */
enum class PackageInterfaceRelation(private val relation: Pair<KClass<out DMLInterfaceEnum>, String>){
    SELECT(DMLInterfaceEnum.SELECT::class to "selectPackage"),
    INSERT(DMLInterfaceEnum.INSERT::class to "insertPackage"),
    UPDATE(DMLInterfaceEnum.UPDATE::class to "updatePackage"),
    UPSERT(DMLInterfaceEnum.UPSERT::class to "upsertPackage"),
}
