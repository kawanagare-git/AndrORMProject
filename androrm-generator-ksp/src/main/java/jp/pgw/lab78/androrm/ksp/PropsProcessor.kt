package jp.pgw.lab78.androrm.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import jp.pgw.lab78.androrm.common.GenerateProps
import jp.pgw.lab78.androrm.common.annotation.*
import jp.pgw.lab78.androrm.common.database.annotation.Function
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.DMLInterfaceEnum
import jp.pgw.lab78.androrm.ksp.factory.ColumnPropertyFactory
import jp.pgw.lab78.androrm.ksp.factory.FunctionPropertyFactory
import jp.pgw.lab78.androrm.ksp.factory.TableAnnotationFactory
import jp.pgw.lab78.androrm.ksp.helper.AnnotationHelper
import jp.pgw.lab78.androrm.ksp.helper.ImportHelper
import jp.pgw.lab78.androrm.ksp.helper.TypeHelper
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger.logger
import jp.pgw.lab78.androrm.ksp.logging.LoggerLike
import jp.pgw.lab78.androrm.ksp.projectoin.ProjectionArgumentParser
import jp.pgw.lab78.androrm.ksp.projectoin.ProjectionDefinition
import jp.pgw.lab78.androrm.ksp.projectoin.ProjectionExtractor
import jp.pgw.lab78.androrm.ksp.projectoin.ProjectionValidator
import jp.pgw.lab78.androrm.ksp.resolver.InterfaceResolver

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
) : SymbolProcessor, LoggerLike by logger {
    companion object {
        /** プロパティ名一覧出力先パッケージ */
        private const val GENERATED_PACKAGE = "jp.pgw.lab78.androrm.ksp.generated"

        /** プロパティ名一覧 Enum 名 */
        private const val GENERATED_PROPERTIES = "AllClassProperties"

        /** @Table の変数名定義（name） */
        private const val TABLE_NAME = "name"

        /** @Table の変数名定義（alias） */
        private const val TABLE_ALIAS = "alias"

        /** @Function の変数名定義（columnFunction） */
        private const val F_COLUMN_FUNCTION = "columnFunction"

        /** @Function の変数名定義（args） */
        private const val F_ARGS = "args"

        /** @Function の変数名定義（alias） */
        private const val F_ALIAS = "alias"

        /** @FunctionPrpjection の変数名定義（raw） */
        private const val F_RAW = "raw"

        /** @EntityPackageInfo の変数名定義（basePackage） */
        private const val BASE_PACKAGE = "basePackage"

        /** @GenerateProps */
        private val GENERATE_PROPS = GenerateProps::class.qualifiedName!!

        /** @EntityPackageInfo */
        private val ENTITY_PACKAGE_INFO = EntityPackageInfo::class.simpleName!!

        /** @EntityPackageInfo */
        private val ENTITY_PACKAGE_INFO_FQN = EntityPackageInfo::class.qualifiedName!!

        /** @Projection */
        private val PROJECTION = Projection::class.simpleName!!

        /** @Projection(FQN) */
        private val PROJECTION_FQN = Projection::class.qualifiedName!!

        /** @Projections */
        private val PROJECTIONS = Projections::class.simpleName!!

        /** @Projections(FQN) */
        private val PROJECTIONS_FQN = Projections::class.qualifiedName!!

        /** @Table */
        private val TABLE = Table::class.simpleName!!

        /** @Table(FQN) */
        private val TABLE_FQN = Table::class.qualifiedName!!

        /** @Column */
        private val FUNCTION = Function::class.simpleName!!

        /** @Column(FQN) */
        private val FUNCTION_FQN = Function::class.qualifiedName!!

        /** FunctionProjection の引数検査用正規表現 */
        private val STRING_LITERAL_REGEX = "^'.*'$".toRegex()

        /**
         * ## パッケージとインターフェースのリレーションクラス
         * ### 標準インターフェースと出力先パッケージを紐づける
         * @param relation インターフェースと @EntityPackageInfo の変数の組み合わせ
         * @author Masahiro Inoue
         * @since 2025-08-01
         */
        enum class PackageInterfaceRelation(val relation: String) {
            SELECT("selectPackage"),
            INSERT("insertPackage"),
            UPDATE("updatePackage"),
            UPSERT("upsertPackage"),
        }
    }

    /** インポート収集ヘルパー */
    val importHelper = ImportHelper()

    /** 型ヘルパー */
    val typeHelper = TypeHelper()

    /** アノテーションヘルパー */
    val annotationHelper = AnnotationHelper()

    /** interface / package 解決 */
    val interfaceResolver = InterfaceResolver()

    /** Projection 抽出 */
    private val projectionExtractor = ProjectionExtractor()

    /** Projection 引数解析 */
    private val projectionArgumentParser = ProjectionArgumentParser()

    /** Projection 妥当性検証 */
    private val projectionValidator = ProjectionValidator()

    /** Table アノテーション生成 */
    private val tableAnnotationFactory = TableAnnotationFactory()

    /** 自動生成するために必要な全プロパティ名 */
    private val allClassProperties = mutableMapOf<String, List<String>>()

    /** @Table 生成 */
    private lateinit var tableAnnotationSpec: AnnotationSpec

    /** Column プロパティ生成 */
    private val columnPropertyFactory = ColumnPropertyFactory()

    /** Function プロパティ生成 */
    private val functionPropertyFactory = FunctionPropertyFactory()

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
        traceEntered(resolver)
        // Props 生成
        generateProps(resolver)
        // @Projection と @Projections から data class を生成
        generateDataClassFromProjections(resolver)
        traceExiting()
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
                val entryName = "${propName}_${className}_${packageName}".replace(
                    ".", "_"
                ) // パッケージ名に含まれる . を _ に変換
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
     * ## data class 生成エントリポイント
     * ### @Projection / @Projections を探索し、
     * ### 対応する data class を生成する
     * - @Projection: 単一の射影指定
     * - @Projections: 複数の射影指定
     * @param resolver KSP のアノテーション解析リゾルバ
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    @OptIn(KspExperimental::class)
    private fun generateDataClassFromProjections(resolver: Resolver) {
        traceEntered(resolver)
        // @Projection と @Projections を両方まとめて拾う
        val allProjectionClasses = projectionExtractor.findProjectionClasses(resolver)
        // 各クラスごとに Projection 系アノテーションを展開
        allProjectionClasses.forEach { classDecl ->
            projectionExtractor.extractFromClass(classDecl).forEach { annotation ->
                val definition = projectionArgumentParser.parse(annotation)
                projectionValidator.validateAggregateConflicts(definition)
                projectionValidator.validateProperties(
                    classDecl, definition, allClassProperties
                )
                processSingleProjection(classDecl, definition, resolver)
            }
        }
        traceExiting()
    }

    /**
     * ## data class 生成処理
     * ### @Projection の内容から新しい data class を構築しファイル出力する
     * - properties → 通常の列
     * - functions → 関数列（SUM, AVG, …）
     *   - プロパティ化 + @Function アノテーション付与を別メソッドで処理
     * @param classDecl @Projection が適用されたクラス
     * @param definition 対象 @Projection
     * @param resolver KSP のアノテーション解析リゾルバ
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private fun processSingleProjection(
        classDecl: KSClassDeclaration, definition: ProjectionDefinition, resolver: Resolver
    ) {
        traceEntered(classDecl, definition, resolver)
        val packageName = interfaceResolver.resolvePackageNameFromAnnotation(
            classDecl,
            resolver.getSymbolsWithAnnotation(ENTITY_PACKAGE_INFO_FQN, false),
            definition.commonInterfaces.firstOrNull()
        )
        // パッケージ名、クラス名、テーブル名などメタ情報を構築
        val createClassName = classDecl.simpleName.asString() + definition.entityNameExtend
        // @Table の生成
        tableAnnotationSpec = tableAnnotationFactory.create(classDecl, definition.aliasExtend)
        val selectedPropertyNames = definition.properties.map { it.property }.toSet()

        val hideFromSelectByProperty = definition.properties.associate {
            it.property to it.hideFromSelect
        }
        // properties 部分（通常列）の抽出
        val selectedProps = classDecl.getAllProperties()
            .filter { it.simpleName.asString() in selectedPropertyNames }.toList()
        // プロパティ名 → KSPropertyDeclaration のマップを生成
        val propsByName: Map<String, KSPropertyDeclaration> =
            classDecl.getAllProperties().associateBy { it.simpleName.asString() }
        // マーカーインターフェースの抽出
        val interfaces = interfaceResolver.collectInterfaces(definition)
        // data class の構成を定義
        val fileSpec = createDataClassFile(
            ClassName(packageName, createClassName),
            selectedProps,
            hideFromSelectByProperty,
            functionPropertyFactory.createAll(definition.functions, propsByName),
            interfaces
        )
        traceExiting(fileSpec)
    }

    /**
     * ## マーカーインターフェース抽出
     * ### マーカーインターフェースのリストを生成
     * @param definition メタデータ
     * @return マーカーインターフェースのリスト
     * @author Masahiro Inoue
     * @since 2025-08-22
     */
    private fun collectInterfaces(definition: ProjectionDefinition): List<TypeName> {
        traceEntered(definition)
        // commonInterfaces と customInterfaces を結合して TypeName のリストを生成
        val result = buildList<TypeName> {
            definition.commonInterfaces.forEach { common ->
                add(ClassName.bestGuess(common.interfaceFQN))
            }
            definition.customInterfaces
                .filter { it.isNotBlank() }
                .forEach { custom ->
                    add(ClassName.bestGuess(custom))
                }
        }
        traceExiting(result)
        return result
    }

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
    private fun resolvePackageNameFromAnnotation(
        classDeclaration: KSClassDeclaration,
        symbols: Sequence<KSAnnotated>,
        commonInterface: String
    ): String {
        traceEntered(classDeclaration, symbols, commonInterface)
        if (commonInterface.isNullOrEmpty()) {
            return classDeclaration.packageName.asString()
        }
        val common = generateCommonInterface(commonInterface)
        // パッケージアノテーションの抽出
        symbols.filterIsInstance<KSFile>().forEach {
            // @EntityPackageInfo の単純名で抽出
                symbol ->
            val annotation = symbol.annotations.firstOrNull {
                it.shortName.asString() == ENTITY_PACKAGE_INFO
            }
            // @@EntityPackageInfo を FQN で検証
            if (annotation?.let {
                    it.annotationType.resolve().declaration.qualifiedName?.asString() == ENTITY_PACKAGE_INFO_FQN
                } == true) {
                val base = annotation.arguments.firstOrNull {
                    it.name?.asString() == BASE_PACKAGE
                }?.value
                // サブパッケージの取得
                // PackageInterfaceRelation から common.canonicalName に該当するものを抽出
                val sub = PackageInterfaceRelation.values().firstOrNull {
                    DMLInterfaceEnum.valueOf(it.name).interfaceFQN == common.canonicalName
                }
                    // DMLInterfaceEnum から取得した列挙子の relation の値を取得
                    .let { it?.relation }
                    // EntityPackageInfo の列挙子からサブパッケージを取得
                    .let { argument ->
                        annotation.arguments.firstOrNull {
                            it.name?.asString() == argument
                        }
                    }?.value
                val result = "$base.$sub"
                infoExiting(result)
                return result
            }
        }
        val result = common.packageName
        traceExiting(result)
        return result
    }

    /**
     * ## データクラス生成メソッド
     * ### 指定されたクラスから、指定されたプロパティのみを含む
     * ### データクラスを KotlinPoet で生成する
     * @param classNameFQN 生成するクラス名（FQN）
     * @param selectedProps プロパティ一覧（KSP の KSPropertyDeclaration）
     * @param functionProjections 関数列
     * @param interfaces 実装するインターフェス
     * @return FileSpec（Kotlin ファイル）
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private fun createDataClassFile(
        classNameFQN: ClassName,
        selectedProps: List<KSPropertyDeclaration>,
        hideFromSelectByProperty: Map<String, Boolean>,
        functionProps: List<PropertySpec>,
        interfaces: List<TypeName>
    ): FileSpec {
        traceEntered(
            classNameFQN, selectedProps, hideFromSelectByProperty, functionProps, interfaces
        )
        // ① 通常列 → PropertySpec（@Column / @PrimaryKey はコピー済み）
        val normalProps = selectedProps.map { prop ->
            columnPropertyFactory.create(
                prop,
                hideFromSelect = hideFromSelectByProperty[prop.simpleName.asString()] == true
            )
        }
        // ② コンストラクタに入れる列 = 通常列＋関数列
        val ctorProps = normalProps + functionProps
        // ③ 出力先は“従来どおり”
        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(false),
            packageName = classNameFQN.packageName,
            fileName = classNameFQN.simpleName
        )
        file.bufferedWriter().use { w ->
            val imports = importHelper.collectImports(tableAnnotationSpec, ctorProps, interfaces)
            // --- パッケージ ---
            w.appendLine("package ${classNameFQN.packageName}")
            w.appendLine()
            // --- インポート ---
            imports.sorted().forEach { w.appendLine("import $it") }
            w.appendLine()
            // --- @Table アノテーション（そのまま文字列化）---
            w.appendLine(annotationHelper.getSimpleName(tableAnnotationSpec))
            // --- data class 宣言ヘッダ ---
            val ifaceText = if (interfaces.isEmpty()) ""
            else interfaces.joinToString(", ") { typeHelper.getSimpleName(it) }
            w.appendLine("public data class ${classNameFQN.simpleName}(")
            // --- コンストラクタ引数（★ここが核心）---
            ctorProps.forEachIndexed { index, prop ->
                // 付与されているアノテーションをそのまま出力
                prop.annotations.forEach { ann ->
                    w.appendLine("  " + annotationHelper.getSimpleName(ann))
                }
                val comma = if (index == ctorProps.lastIndex) "" else ","
                // ★ val を確実に出す
                w.appendLine("  public val ${prop.name}: ${typeHelper.getSimpleName(prop)}$comma")
                w.appendLine()
            }
            w.append(")")
            if (ifaceText.isNotBlank()) {
                w.append(" : $ifaceText")
            }
            w.appendLine()
        }
        // ダミーの FileSpec を返す（実体は上で書き込み済み）
        val result = FileSpec.builder(classNameFQN).build()
        traceExiting(result)
        return result
    }

    /**
     * ## 共通インターフェースの生成
     * ### @Projection の commonInterface の値から
     * ### 共通インターフェースの FQN を取得する
     * @param commonInterface 共通インターフェースを文字列化した値
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private fun generateCommonInterface(commonInterface: String): ClassName {
        traceEntered(commonInterface)
        val result = ClassName.bestGuess(
            (DMLInterfaceEnum.valueOf(
                ClassName.bestGuess(commonInterface).simpleName
            )).interfaceFQN
        )
        traceExiting(result)
        return result
    }

    /**
     * ## アノテーション引数抽出
     * ### KSAnnotation から指定された名前の引数を抽出し、
     * ### 指定された型にキャストして戻す
     * @param T 抽出する引数の型
     * @param name 抽出する引数の名前
     * @return 抽出された引数の値 / 存在しない場合は null
     * @author Masahiro Inoue
     * @since 2025-09-05
     */
    inline fun <reified T> KSAnnotation.argumentOf(name: String): T? {
        traceEntered(name)
        val argValue = arguments.firstOrNull { it.name?.asString() == name }?.value ?: run {
            // 引数が存在しない場合は null を戻す
            traceExiting("null")
            return null
        }
        // Enum の場合は KSType から Enum を取得
        if (T::class.java.isEnum) {
            val ksType = argValue as? KSType ?: run {
                // 引数が存在しない場合は null を戻す
                traceExiting("null")
                return null
            }
            val enumName = ksType.declaration.simpleName.asString()

            @Suppress("UNCHECKED_CAST") val result =
                java.lang.Enum.valueOf(T::class.java as Class<out Enum<*>>, enumName) as T
            traceExiting(result)
            return result
        }
        traceExiting(argValue)
        return argValue as? T
    }

    /**
     * ## KSP の処理完了
     * ### KSP の処理が完了した際に呼び出されるメソッド
     * ### ここでは、生成したファイルをクローズしてリソースを解放する
     * @author Masahiro Inoue
     * @since 2026-03-13
     */
    override fun finish() {
        super.finish()
        close()
    }

    /**
     * ## KSP のエラー発生
     * ### KSP の処理中にエラーが発生した際に呼び出されるメソッド
     * ### ここでは、エラー発生をログに記録し、生成したファイルをクローズしてリソースを解放する
     * @author Masahiro Inoue
     * @since 2026-03-13
     */
    override fun onError() {
        super.onError()
        close()
    }
}
