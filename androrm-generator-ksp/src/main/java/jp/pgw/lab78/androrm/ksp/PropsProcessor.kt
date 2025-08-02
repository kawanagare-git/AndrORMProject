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
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.GenerateProps
import jp.pgw.lab78.androrm.common.annotation.EntityPackageInfo
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.dml.DMLInterfaceEnum

/**
 * ## AndrORM プロパティプロセッサクラス
 * ### AndrORM の標準アノテーションを基に
 * ### プロパティ一覧、データクラス、インターフェース一覧を作成
 * ### PropsProcessorProvider#create(SymbolProcessorEnvironment) から呼び出される
 * @param codeGenerator コード生成時に出力機能を提供
 * @param logger ビルド中の経過、警告、エラー等の情報を出力する環境を提供
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
class PropsProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    companion object  {
        /** @Projection の変数名定義（entityNameExtend） */
        const val EXTEND_NAME = "entityNameExtend"
        /** @Projection の変数名定義（aliasExtend） */
        const val EXTEND_ALIAS = "aliasExtend"
        /** @Projection の変数名定義（implementsInterface） */
        const val IMPLEMENTS_INTERFACE = "implementsInterface"
        /** @Projection の変数名定義（properties） */
        const val PROPERTIES = "properties"
        /** @Projection の変数名定義（commonInterface） */
        const val COMMON_INTERFACE = "commonInterface"
        /** @Projection の変数名定義（customInterface） */
        const val CUSTOM_INTERFACE = "customInterface"

        /** プロパティ名一覧出力先パッケージ */
        const val GENERATED_PACKAGE = "jp.pgw.lab78.androrm.ksp.generated"
        /** プロパティ名一覧 Enum 名 */
        const val GENERATED_PROPERTIES = "AllClassProperties"

        /** @Table の変数名定義（alias） */
        const val TABLE_ALIAS = "alias"

        /** @Column の変数名定義（name） */
        const val COLUMN_NAME = "name"
        /** @Column の変数名定義（alias） */
        const val COLUMN_ALIAS = "alias"

        /** @EntityPackageInfo の変数名定義（basePackage） */
        const val BASE_PACKAGE = "basePackage"

        /** 不明 */
        const val UNKNOWN = "Unknown"

        /** @GenerateProps */
        val GENERATE_PROPS = GenerateProps::class.qualifiedName!!
        /** @EntityPackageInfo */
        val ENTITY_PACKAGE_INFO = EntityPackageInfo::class.simpleName!!
        /** @EntityPackageInfo */
        val ENTITY_PACKAGE_INFO_FQN = EntityPackageInfo::class.qualifiedName!!
        /** @Projection */
        val PROJECTION = Projection::class.simpleName!!
        /** @Projection(FQN) */
        val PROJECTION_FQN = Projection::class.qualifiedName!!
        /** @Projections */
        val PROJECTIONS = Projections::class.simpleName!!
        /** @Projections(FQN) */
        val PROJECTIONS_FQN = Projections::class.qualifiedName!!
        /** @Table */
        val TABLE = Table::class.simpleName!!
        /** @Table(FQN) */
        val TABLE_FQN = Table::class.qualifiedName!!
        /** @Column */
        val COLUMN = Column::class.simpleName!!
        /** @Column(FQN) */
        val COLUMN_FQN = Column::class.qualifiedName!!
    }

    /** 自動生成するために必要な全プロパティ名 */
    val allClassProperties = mutableMapOf<String,List<String>>()

    /** 生成 @Table */
    lateinit var tableAnnotationSpec: AnnotationSpec

    /**
     * ## AndrORM アノテーションプロセスメソッド
     * ### AndrORM で定義されているアノテーションを解析し
     * ### プロパティ一覧、データクラス、インターフェース一覧を作成し
     * ### Kotlin ファイルを生成する
     * @param resolver アノテーション解析機能を提供
     * @return 解析に失敗したシンボル
     * @author Masahiro Inoue
     * @since 2025-08-01
     * @see SymbolProcessor.process
     */
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // Props 生成
        generateProps(resolver)
        // @Projection と @Projections から data class を生成
        generateDataClassFromProjections(resolver)
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
     * @author Masahiro Inoue
     * @since 2025-08-01
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
     * ## data class 生成
     * ### @Projection と @Projections のの検証と
     * ### data class を生成
     * @param resolver アノテーション解析機能を提供
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private fun generateDataClassFromProjections (resolver: Resolver) {
        // @Projection の抽出
        val projectionSymbols = resolver.getSymbolsWithAnnotation(PROJECTION_FQN, false)
        // @Projections の抽出
        val projectionsSymbols = resolver.getSymbolsWithAnnotation(PROJECTIONS_FQN, false)
        // Projection の処理
        processProjectionAnnotations(projectionSymbols.filterIsInstance<KSClassDeclaration>(), resolver)
        // Projections の処理
        projectionsSymbols.filterIsInstance<KSClassDeclaration>().forEach { classDecl ->
            classDecl.annotations
                // @Projections のアノテーションを抽出
                .filter {
                    it.shortName.asString() == "Projections" &&
                    it.annotationType.resolve().declaration.qualifiedName?.asString() == PROJECTIONS_FQN
                }
                // @Projections でループさせて @Projection を取り出す
                .forEach { annotation ->
                    val projectionList = extractProjectionList(annotation)  // ←ここで Array<Projection> を解析
                    projectionList.forEach { projection ->
                        checkProjectionFields(classDecl, annotation)
                        processSingleProjection(classDecl, projection, resolver)
                    }
                }
        }
    }

    /**
     * ##
     * @param classDecls アノテーション解析機能を提供
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private fun processProjectionAnnotations(
        classDecls: Sequence<KSClassDeclaration>,
        resolver: Resolver
    ) {
        classDecls.forEach { classDecl ->
            val annotation = classDecl.annotations.firstOrNull {
                it.shortName.asString() == PROJECTION &&
                it.annotationType.resolve().declaration.qualifiedName?.asString() == PROJECTION_FQN
            } ?: return@forEach
            checkProjectionFields(classDecl, annotation)
            processSingleProjection(classDecl, annotation, resolver)
        }
    }


    /**
     * ## プロジェクションフィールド配列検査メソッド
     * ### @Projection と @Projections を抽出し定義されている fields 内の
     * ### 文字列が @Projection 適用クラスに存在するプロパティ名か判定する。
     * ### １つでも存在していないプロパティ名が見つかったら、ビルドエラーとして処理する。
     * @param classDecl アノテーション解析機能を提供
     * @param annotation @Projection を抽出した解析対象
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private fun checkProjectionFields(
        classDecl: KSClassDeclaration,
        annotation: KSAnnotation,
    ) {
        // リスト化された fields の値
        val fieldsValues = collectFields(annotation)[PROPERTIES] as? List<*> ?: emptyList<String>()
        logger.info(">>>> Fields is '$fieldsValues'.")
        // @Projection が適用されたクラスのプロパティ名一覧を取得
        // ただし、allClassProperties に登録済みならば、allClassProperties からプロパティ名一覧を取得
        val fqn = classDecl.qualifiedName?.asString() ?: run {
            logger.error(">> Annotation target class is null.")
            return
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
                logger.error(">> Field '$field' is not declared in class" +
                        " '${classDecl.simpleName.asString()}'.", classDecl)
            }
        }
    }


    /**
     * ## @Projection によるデータクラス生成メソッド
     * ### @Projection が付与されているクラスの情報を基に
     * ### 新たに data class を生成し、
     * ### Kotlin ファイルとして出力する
     * @param classDecl アノテーション解析機能を提供
     * @param annotation @Projection を抽出した解析対象
     * @param resolver アノテーション解析機能を提供
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private fun processSingleProjection(
        classDecl: KSClassDeclaration,
        annotation: KSAnnotation,
        resolver: Resolver
    ) {
        val dataClassMaterialMap = collectFields(annotation).toMutableMap()
        // パッケージ名の抽出
        val packageName = generatedPackageNameFromAnnotation(
            classDecl,
            resolver.getSymbolsWithAnnotation(ENTITY_PACKAGE_INFO_FQN, false),
            dataClassMaterialMap[COMMON_INTERFACE].toString()
        )
        // クラス名の抽出
        val createClassName = classDecl.simpleName.asString() +
                dataClassMaterialMap[EXTEND_NAME].toString()
        // エイリアスの抽出
        val tableAnnotation = classDecl.annotations.firstOrNull { it.shortName.asString() == TABLE }
        val tableAlias =  tableAnnotation?.arguments?.firstOrNull { it.name?.asString() == TABLE_ALIAS }
                                                        ?.value as? String
        val extendAlias = dataClassMaterialMap[EXTEND_ALIAS].toString()
        tableAnnotationSpec = AnnotationSpec.builder(Table::class).apply {
                                    if (!extendAlias.isNullOrBlank()) {
                                        addMember("$TABLE_ALIAS = %S"
                                                    ,"${tableAlias}_$extendAlias")
                                    }
                                }
                                .build()
        // 必須プロパティの抽出
        val selectedProps = classDecl.getAllProperties()
            .filter { (dataClassMaterialMap[PROPERTIES] as List<*>).contains(it.simpleName.asString()) }
            .toList()
        // 実装マーカーインターフェースの抽出（共用）
        val interfaces = buildList<TypeName> {
            (dataClassMaterialMap[COMMON_INTERFACE]).toString()
                .let {
                    add(ClassName.bestGuess(DMLInterfaceEnum.valueOf(ClassName
                                                                        .bestGuess(it)
                                                                        .simpleName)
                                                            .interfaceFQN))
                }
            (dataClassMaterialMap[CUSTOM_INTERFACE] as? String)
                ?.takeIf { it.isNotBlank() }
                ?.let { add(ClassName.bestGuess(it)) }
        }
        // 実装マーカーインターフェースの抽出（特化）
        val fileSpec = createDataClassFile(
            ClassName(packageName, createClassName),
            selectedProps,
            interfaces
        )
        // データクラスを kt ファイルとして出力
        val fileDependency = classDecl.containingFile?.let { Dependencies(true, it) } ?: Dependencies(false)
        fileSpec.writeTo(codeGenerator, fileDependency)
    }

    /**
     * ## @Projections から @Projection のリストを抽出
     * ### @Projections に定義されている @Projection を List として抽出
     * @param annotation @Projection を抽出した解析対象
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private fun extractProjectionList(annotation: KSAnnotation): List<KSAnnotation> =
        // @Projections から value を抽出
        (annotation.arguments.firstOrNull { it.name?.asString() == "value" }
            // value がリストにキャスト可能か？
            ?.value as? List<*>)
            // 可能であれば value.filterIsInstance<KSAnnotation>() を戻す
            ?.filterIsInstance<KSAnnotation>()
            // 不可能であれば 空リストを戻す
            ?: emptyList()

    /**
     * ## パッケージ名生成メソッド
     * ### @EntityPackageInfo と @Projection の commonInterface を
     * ### 基にパッケージ名を生成
     * ### ただし、@EntityPackageInfo や @Projection の commonInterface が
     * ### 定義されていない場合 @Projection が付与されたクラスのパッケージ名を返す
     * @param classDeclaration クラス定義情報
     * @param symbols @EntityPackageInfo で絞り込んだアノテーションのシンボル
     * @param commonInterface @Projection に定義された commonInterface の値
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private fun generatedPackageNameFromAnnotation(
        classDeclaration: KSClassDeclaration,
        symbols: Sequence<KSAnnotated>,
        commonInterface: String
    ): String {
        logger.warn(">>> Generated Projection generatedPackageNameFromAnnotation in" +
                                " : ${classDeclaration} ,${symbols.count()} ,$commonInterface")
        if (commonInterface.isBlank() || symbols.count() == 0) {
            return classDeclaration.packageName.asString()
        }
        val common = generateCommonInterFace(commonInterface)
        logger.warn(">>> Generated Projection generatedPackageNameFromAnnotation : $common")
        // パッケージアノテーションの抽出
        symbols.filterIsInstance<KSFile>().forEach {
            // @EntityPackageInfo の単純名で抽出
            symbol ->
            val annotation = symbol.annotations.firstOrNull{
                it.shortName.asString() == ENTITY_PACKAGE_INFO
            }
            // @@EntityPackageInfo を FQN で検証
            if (annotation?.let{
                    it.annotationType.resolve().declaration.qualifiedName?.asString() == ENTITY_PACKAGE_INFO_FQN
                }?: false) {
                val base = annotation?.arguments?.firstOrNull {
                    it.name?.asString() == BASE_PACKAGE
                }?.value?.let {
                    logger.warn(">>> Generated Projection generatedPackageNameFromAnnotation base: $it")
                    it.toString()
                }
                // サブパッケージの取得
                // PackageInterfaceRelation から common.canonicalName に該当するものを抽出
                val sub = PackageInterfaceRelation.values().firstOrNull(){
                    DMLInterfaceEnum.valueOf(it.name).interfaceFQN == common.canonicalName
                }
                // DMLInterfaceEnum から取得した列挙子の relation の値を取得
                .let {it?.relation}
                // EntityPackageInfo の列挙子からサブパッケージを取得
                .let {
                    argument -> annotation?.arguments?.firstOrNull() {
                        it.name?.asString() == argument
                    }
                }
                .let {
                    logger.warn(">>> Generated Projection generatedPackageNameFromAnnotation sub last : ${it?.value}")
                    it?.value
                }
                return "$base.$sub"
            }
        }
        return common.packageName
    }

    /**
     * ## データクラス生成メソッド
     * ### 指定されたクラスから、指定されたプロパティのみを含む
     * ### データクラスを KotlinPoet で生成する
     * @param classNameFQN 生成するクラス名（FQN）
     * @param selectedProps プロパティ一覧（KSP の KSPropertyDeclaration）
     * @param interfaces 実装するインターフェス
     * @return FileSpec（Kotlin ファイル）
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private fun createDataClassFile(
        classNameFQN: ClassName,
        selectedProps: List<KSPropertyDeclaration>,
        interfaces: List<TypeName>
    ): FileSpec {
        logger.warn(">>> Generated Projection createDataClassFile: $interfaces")
        // クラスビルダー
        val classBuilder = TypeSpec.classBuilder(classNameFQN)
                                    .addModifiers(KModifier.DATA)
                                    .addSuperinterfaces(interfaces)
                                    .addAnnotation(tableAnnotationSpec)
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
                    .apply {
                        // copyColumnAnnotation の結果が null 以外の時に addAnnotation が実行
                        copyColumnAnnotation(prop)?.let {
                            addAnnotation(it)
                        }
                    }
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
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private fun collectFields(annotation: KSAnnotation): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        val annotationFQN = annotation.annotationType.resolve().declaration.qualifiedName?.asString()
        logger.warn(">>> Processing collectFields in with $annotationFQN")
        // アノテーションの引数を取得し加工
        annotation.arguments.forEach {
            val argName = it.name?.asString()
            val value = it.value
            when (value) {
                // アノテーション変数が配列
                is List<*> -> {
                    val castedValue = it.value as List<*>
                    logger.warn(">>> Processing collectFields Projection list" +
                                        " ${argName to castedValue}")
                    val firstElement = castedValue.firstOrNull()
                    logger.warn(">>> Processing collectFields Projection list" +
                                        " firstElement $firstElement")
                    val fieldList: List<Any> = when (firstElement) {
                        // KSType から変数型を取得
                        is KSType -> {
                            logger.warn(">>> Processing collectFields Projection list" +
                                                " type for ${argName to firstElement}")
                            castedValue.mapNotNull { geneType ->
                                (geneType as KSType).declaration.simpleName.asString()
                            }
                        }
                        // 変数型を文字列に変換
                        is String -> {
                            castedValue.filterIsInstance<String>()
                        }
                        // 変数型を Int に変換
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
                }
                // アノテーション変数が上記に当てはまらない
                else -> {
                    logger.warn(">>> Processing Projection collectFields else ${argName to it.value}}")
                    argName?.let { key -> result[key] = it.value ?: EMPTY_STRING }
                }
            }
        }
        return result
    }

    /**
     * ## カラムアノテーションコピー
     * ### 参照元のプロパティを基に @Column を複製
     * @param prop 参照元プロパティ / @Column の複製に使用
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private fun copyColumnAnnotation(prop: KSPropertyDeclaration): AnnotationSpec? {
        // @Column アノテーションを探す
        val columnAnnotation = prop.annotations
            .firstOrNull { it.shortName.asString() == COLUMN }
        if (columnAnnotation?.let{
                it.annotationType.resolve().declaration.qualifiedName?.asString() == COLUMN_FQN
            }?: false) {
            // アノテーション引数からカラム名を取得
            val columnName = columnAnnotation?.arguments
                ?.firstOrNull { it.name?.asString() == COLUMN_NAME }
                ?.value as? String
            // アノテーション引数からカラムエイリアスを取得
            val columnAlias = columnAnnotation?.arguments
                ?.firstOrNull { it.name?.asString() == COLUMN_ALIAS }
                ?.value as? String
            // AnnotationSpec に変換
            return if (!columnName.isNullOrBlank() || !columnAlias.isNullOrBlank()) {
                // @Column のコピー
                AnnotationSpec.builder(ClassName.bestGuess(COLUMN_FQN))
                    .apply {
                        if (!columnName.isNullOrBlank()) addMember("$COLUMN_NAME = %S",columnName)
                        if (!columnAlias.isNullOrBlank()) addMember("$COLUMN_ALIAS = %S",columnAlias)
                }.build()
            } else null
        }
        return null
    }

    /**
     * ## 共通インターフェースの生成
     * ### @Projection の commonInterface の値から
     * ### 共通インターフェースの FQN を取得する
     * @param commonInterface 共通インターフェースを文字列化した値
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private fun generateCommonInterFace(commonInterface: String) : ClassName {
        return ClassName.bestGuess((DMLInterfaceEnum.valueOf(ClassName.bestGuess(commonInterface)
                                                                        .simpleName))
                                                    .interfaceFQN)
    }
}

/**
 * ## プロパティプロセッサ提供元クラス
 * ### ビルドツールから呼び出され、プロパティプロセッサのインスタンスを
 * ### ビルドツールに戻す
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
class PropsProcessorProvider : SymbolProcessorProvider {

    /**
     * ## プロパティプロセッサ生成メソッド
     * ### プロパティプロセッサのインスタンスを生成する
     * @param environment シンボルプロセッサ環境（ビルドツールから渡される）
     * @return 生成されたプロパティプロセッサのインスタンス
     * @author Masahiro Inoue
     * @since 2025-08-01
     * @see SymbolProcessorProvider.create
     */
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return PropsProcessor(environment.codeGenerator, environment.logger)
    }
}

/**
 * ## パッケージとインターフェースのリレーションクラス
 * ### 標準インターフェースと出力先パッケージを紐づける
 * @param relation インターフェースと @EntityPackageInfo の変数の組み合わせ
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
enum class PackageInterfaceRelation(val relation: String){
    SELECT("selectPackage"),
    INSERT("insertPackage"),
    UPDATE("updatePackage"),
    UPSERT("upsertPackage"),
}
