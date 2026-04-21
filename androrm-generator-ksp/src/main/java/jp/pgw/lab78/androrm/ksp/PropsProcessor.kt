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
import jp.pgw.lab78.androrm.ksp.writer.DataClassWriter

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

    /** Column プロパティ生成 */
    private val columnPropertyFactory = ColumnPropertyFactory()

    /** Function プロパティ生成 */
    private val functionPropertyFactory = FunctionPropertyFactory()

    /** data class 生成 */
    private val dataClassWriter = DataClassWriter(
        codeGenerator = codeGenerator,
        importHelper = importHelper,
        annotationHelper = annotationHelper,
        typeHelper = typeHelper,
    )

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
        val tableAnnotationSpec = tableAnnotationFactory.create(classDecl, definition.aliasExtend)
        // プロパティ名一覧と、プロパティ名 → hideFromSelect のマップを生成
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
        // 通常列の PropertySpec を生成（@Column / @PrimaryKey はコピー済み）
        val normalProps = selectedProps.map { prop ->
            columnPropertyFactory.create(
                prop,
                hideFromSelect = hideFromSelectByProperty[prop.simpleName.asString()] == true
            )
        }
        // functions 部分（関数列）の抽出と PropertySpec 生成
        val functionProps = functionPropertyFactory.createAll(
            definition.functions,
            propsByName
        )
        // マーカーインターフェースの抽出
        val interfaces = interfaceResolver.collectInterfaces(definition)
        // createClassName という名前で、packageName パッケージに、tableAnnotationSpec アノテーション、normalProps プロパティ、functionProps プロパティ、interfaces インターフェースを持つ data class を生成する
        dataClassWriter.write(
            classNameFQN = ClassName(packageName, createClassName),
            tableAnnotationSpec = tableAnnotationSpec,
            normalProps = normalProps,
            functionProps = functionProps,
            interfaces = interfaces
        )
        traceExiting(createClassName)
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
