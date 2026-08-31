package jp.pgw.lab78.androrm.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ClassName
import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.annotation.EntityPackageInfo
import jp.pgw.lab78.androrm.common.dml.interfaces.ViewDefinitionEntity
import jp.pgw.lab78.androrm.common.logging.interfaces.LoggerLike
import jp.pgw.lab78.androrm.common.meta.EntityMetaValidator
import jp.pgw.lab78.androrm.ksp.factory.ColumnPropertyFactory
import jp.pgw.lab78.androrm.ksp.factory.FunctionPropertyFactory
import jp.pgw.lab78.androrm.ksp.factory.TableAnnotationFactory
import jp.pgw.lab78.androrm.ksp.factory.ViewAnnotationFactory
import jp.pgw.lab78.androrm.ksp.helper.AnnotationHelper
import jp.pgw.lab78.androrm.ksp.helper.ImportHelper
import jp.pgw.lab78.androrm.ksp.helper.TypeHelper
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger.logger
import jp.pgw.lab78.androrm.ksp.meta.KspEntityMetaFactory
import jp.pgw.lab78.androrm.ksp.projectoin.ProjectionArgumentParser
import jp.pgw.lab78.androrm.ksp.projectoin.ProjectionDefinition
import jp.pgw.lab78.androrm.ksp.projectoin.ProjectionExtractor
import jp.pgw.lab78.androrm.ksp.projectoin.ProjectionValidator
import jp.pgw.lab78.androrm.ksp.resolver.InterfaceResolver
import jp.pgw.lab78.androrm.ksp.resolver.KspColumnAnnotationResolver
import jp.pgw.lab78.androrm.ksp.validator.ColumnDefaultValueValidator
import jp.pgw.lab78.androrm.ksp.validator.MigrationDefaultValueValidator
import jp.pgw.lab78.androrm.ksp.writer.DataClassWriter
import jp.pgw.lab78.androrm.ksp.Constants.TABLE
import jp.pgw.lab78.androrm.ksp.Constants.VIEW

/**
 * ## AndrORM プロパティプロセッサクラス
 * ### AndrORM の標準アノテーションを基に
 * ### プロパティ一覧、データクラス、インターフェース一覧を作成
 * ### PropsProcessorProvider#create(SymbolProcessorEnvironment) から呼び出される
 * @param codeGenerator コード生成時に出力機能を提供
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
class PropsProcessor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor, LoggerLike by logger {
    /**
     * ## プロセッサ共通定義
     * ### Entityパッケージ情報の解決に使用する定数を保持する
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    companion object {
        /** @EntityPackageInfo */
        private val ENTITY_PACKAGE_INFO_FQN = EntityPackageInfo::class.qualifiedName!!
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

    /** View アノテーション生成 */
    private val viewAnnotationFactory = ViewAnnotationFactory()

    /** 自動生成するために必要な全プロパティ名 */
    private val allClassProperties = mutableMapOf<String, List<String>>()

    /** Columnアノテーション解決 */
    private val kspColumnAnnotationResolver = KspColumnAnnotationResolver()

    /** Columnプロパティ生成 */
    private val columnPropertyFactory =
        ColumnPropertyFactory(columnAnnotationResolver = kspColumnAnnotationResolver)

    /** KSP Entityメタ情報生成 */
    private val kspEntityMetaFactory =
        KspEntityMetaFactory(columnAnnotationResolver = kspColumnAnnotationResolver)

    /** @Column default妥当性検証 */
    private val columnDefaultValueValidator =
        ColumnDefaultValueValidator(columnAnnotationResolver = kspColumnAnnotationResolver)

    /** Function プロパティ生成 */
    private val functionPropertyFactory = FunctionPropertyFactory()

    /** @MigrationDefault value 妥当性検証 */
    private val migrationDefaultValueValidator = MigrationDefaultValueValidator()

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
        logTraceEntered(resolver)
        // @Projection と @Projections から data class を生成
        generateDataClassFromProjections(resolver)
        logTraceExiting()
        return emptyList()
    }

    /**
     * ## エンティティ(data class)生成エントリポイント
     * ### @Projection / @Projections を探索し、対応するエンティティを生成する
     * @param resolver KSP のアノテーション解析機能を提供
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    @OptIn(KspExperimental::class)
    private fun generateDataClassFromProjections(resolver: Resolver) {
        logTraceEntered(resolver)
        // 全プロジェクションクラスを取得
        val allProjectionClasses = projectionExtractor.findProjectionClasses(resolver)
        // 全プロジェクションクラスを捜査
        allProjectionClasses.forEach { classDecl ->
            if (!migrationDefaultValueValidator.validate(classDecl)) {
                return@forEach
            }
            val annotations = projectionExtractor.extractFromClass(classDecl)
            val definitions = annotations.map { annotation ->
                projectionArgumentParser.parse(annotation)
            }
            val isViewDefinition = classDecl.annotations.any {
                it.shortName.asString() == VIEW
            }
            val hasViewDefinitionMarker = classDecl.getAllSuperTypes().any {
                it.declaration.qualifiedName?.asString() == ViewDefinitionEntity::class.qualifiedName
            }
            if (isViewDefinition != hasViewDefinitionMarker) {
                logError("@View and ViewDefinitionEntity must be specified together.")
                return@forEach
            }
            if (isViewDefinition && !validateViewDefinition(classDecl, definitions)) {
                return@forEach
            }
            // 全アノテーションを捜査
            for (definition in definitions) {
                // ヴァリデータにかけて定期内容を検査
                projectionValidator.validateAggregateConflicts(classDecl, definition)
                projectionValidator.validateProperties(
                    classDecl,
                    definition,
                    allClassProperties,
                )
                if (columnDefaultValueValidator.validate(classDecl, definition)) {
                    val entityMeta = kspEntityMetaFactory.create(classDecl, definition)
                    val requireSelectableProperties =
                        definition.commonInterfaces.contains(DMLInterfaceEnum.SELECT)
                    val validationResult = EntityMetaValidator().validate(
                        entityMeta = entityMeta,
                        requireSelectableProperties = requireSelectableProperties,
                    )
                    validationResult.warnings.forEach { warning ->
                        logWarning(warning)
                    }
                    if (validationResult.hasErrors) {
                        validationResult.errors.forEach { error ->
                            logError(error)
                        }
                        continue
                    }
                    processSingleProjection(classDecl, definition, resolver)
                }
            }
        }
        logTraceExiting()
    }

    /**
     * ## エンティティ(data class) 生成処理
     * ### @Projection の内容から新しいエンティティを構築しファイル出力する
     * - properties → 通常の列
     * - functions → 関数列（SUM, AVG, …）
     * #### プロパティ化 + @Function アノテーション付与を別メソッドで処理
     * @param classDecl @Projection が適用されたクラス
     * @param definition 対象 @Projection
     * @param resolver KSP のアノテーション解析リゾルバ
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private fun processSingleProjection(
        classDecl: KSClassDeclaration, definition: ProjectionDefinition, resolver: Resolver
    ) {
        logTraceEntered(classDecl, definition, resolver)
        val packageName = interfaceResolver.resolvePackageNameFromAnnotation(
            classDecl,
            resolver.getSymbolsWithAnnotation(
                ENTITY_PACKAGE_INFO_FQN,
                false,
            ),
            definition.commonInterfaces.firstOrNull(),
            definition.customInterfaces,
        )
        // パッケージ名、クラス名、テーブル名などメタ情報を構築
        val createClassName = classDecl.simpleName.asString() + definition.entityNameExtend
        // @Table の生成
        val tableAnnotationSpec = if (classDecl.annotations.any { it.shortName.asString() == VIEW }) {
            viewAnnotationFactory.create(classDecl, definition.aliasExtend)
        } else {
            tableAnnotationFactory.create(classDecl, definition.aliasExtend)
        }
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
                ownerClass = classDecl,
                prop = prop,
                hideFromSelect = hideFromSelectByProperty[prop.simpleName.asString()] == true,
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
        logTraceExiting(createClassName)
    }

    /**
     * ## VIEW 定義 Projection 検証
     * ### VIEW では SELECT または NOT_USE だけを許可し、少なくとも一つの SELECT Entity 生成を要求する
     * @param classDecl VIEW 定義クラス
     * @param definitions Projection 定義一覧
     * @return 生成を継続できる場合 true
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    private fun validateViewDefinition(
        classDecl: KSClassDeclaration,
        definitions: List<ProjectionDefinition>,
    ): Boolean {
        if (classDecl.annotations.any { it.shortName.asString() == TABLE }) {
            logError("A VIEW definition cannot have both @Table and @View.")
            return false
        }
        val unsupported = definitions.flatMap { it.commonInterfaces }
            .filterNot { it == DMLInterfaceEnum.SELECT || it == DMLInterfaceEnum.NOT_USE }
            .distinct()
        if (unsupported.isNotEmpty()) {
            logError(
                "ViewDefinitionEntity supports only SELECT or NOT_USE projections. " +
                        "Unsupported: ${unsupported.joinToString()}"
            )
            return false
        }
        if (definitions.none { DMLInterfaceEnum.SELECT in it.commonInterfaces }) {
            logError("ViewDefinitionEntity requires at least one SELECT projection.")
            return false
        }
        return true
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
