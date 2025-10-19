package jp.pgw.lab78.androrm.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.GenerateProps
import jp.pgw.lab78.androrm.common.annotation.*
import jp.pgw.lab78.androrm.common.database.SupportFunction.buildAlias
import jp.pgw.lab78.androrm.common.database.SupportFunction.toCamelCase
import jp.pgw.lab78.androrm.common.database.SupportFunction.toSnakeCase
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Function
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction
import jp.pgw.lab78.androrm.common.dml.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.logging.interfaces.LoggerLike
import jp.pgw.lab78.androrm.ksp.logging.KspDelegatingLogger
import jp.pgw.lab78.shared.library.Utils.isNull

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
) : SymbolProcessor, LoggerLike by KspDelegatingLogger(logger) {

    companion object {
        /** @Projection の変数名定義（entityNameExtend） */
        private const val EXTEND_NAME = "entityNameExtend"

        /** @Projection の変数名定義（aliasExtend） */
        private const val EXTEND_ALIAS = "aliasExtend"

        /** @Projection の変数名定義（implementsInterface） */
        private const val IMPLEMENTS_INTERFACE = "implementsInterface"

        /** @Projection の変数名定義（properties） */
        private const val PROPERTIES = "properties"

        /** @Projection の変数名定義（functions） */
        private const val FUNCTIONS = "functions"

        /** @Projection の変数名定義（commonInterface） */
        private const val COMMON_INTERFACE = "commonInterface"

        /** @Projection の変数名定義（customInterface） */
        private const val CUSTOM_INTERFACE = "customInterface"

        /** プロパティ名一覧出力先パッケージ */
        private const val GENERATED_PACKAGE = "jp.pgw.lab78.androrm.ksp.generated"

        /** プロパティ名一覧 Enum 名 */
        private const val GENERATED_PROPERTIES = "AllClassProperties"

        /** @Table の変数名定義（name） */
        private const val TABLE_NAME = "name"

        /** @Table の変数名定義（alias） */
        private const val TABLE_ALIAS = "alias"

        /** @Column の変数名定義（name） */
        private const val COLUMN_NAME = "name"

        /** @Column の変数名定義（alias） */
        private const val COLUMN_ALIAS = "alias"

        /** @FunctionPrpjection の変数名定義（function） */
        private const val FP_FUNCTION = "function"

        /** @FunctionPrpjection の変数名定義（args） */
        private const val FP_ARGS = "args"

        /** @FunctionPrpjection の変数名定義（alias） */
        private const val FP_ALIAS = "alias"

        /** @Function の変数名定義（returnHint） */
        private const val FP_RETURN_HINT = "returnHint"

        /** @FunctionPrpjection の変数名定義（raw） */
        private const val FP_RAW = "raw"

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
        private val COLUMN = Column::class.simpleName!!

        /** @Column(FQN) */
        private val COLUMN_FQN = Column::class.qualifiedName!!

        /** @Column */
        private val FUNCTION = Function::class.simpleName!!

        /** @Column(FQN) */
        private val FUNCTION_FQN = Function::class.qualifiedName!!

        /** FunctionProjection の引数検査用正規表現 */
        private val STRING_LITERAL_REGEX = "^'.*'$".toRegex()
    }

    /** 自動生成するために必要な全プロパティ名 */
    private val allClassProperties = mutableMapOf<String, List<String>>()

    /** 生成 @Table */
    private lateinit var tableAnnotationSpec: AnnotationSpec

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
        infoEntered()
        // Props 生成
        generateProps(resolver)
        // @Projection と @Projections から data class を生成
        generateDataClassFromProjections(resolver)
        infoExiting()
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
        infoEntered()
        // @Projection と @Projections を両方まとめて拾う
        val allProjectionClasses = sequenceOf(
            resolver.getSymbolsWithAnnotation(PROJECTION_FQN, false),
            resolver.getSymbolsWithAnnotation(PROJECTIONS_FQN, false)
        ).flatten()
            .filterIsInstance<KSClassDeclaration>()
        // 各クラスごとに Projection 系アノテーションを展開
        allProjectionClasses.forEach { classDecl ->
            extractProjectionsFromClass(classDecl).forEach { projection ->
                checkAggregateConflicts(projection)
                checkProjectionProperties(classDecl, projection)
                processSingleProjection(classDecl, projection, resolver)
            }
        }
        infoExiting()
    }

    /**
     * ## @Projection の処理
     * ### 単一の @Projection を検証し、対応する data class を生成する
     * @param classDecls @Projection が付与されているクラス群
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private fun extractProjectionsFromClass(classDecl: KSClassDeclaration): Sequence<KSAnnotation> {
        infoEntered()
        val result = classDecl.annotations.flatMap { annotation ->
            when (annotation.annotationType.resolve().declaration.qualifiedName?.asString()) {
                PROJECTION_FQN -> listOf(annotation)
                PROJECTIONS_FQN -> extractProjectionList(annotation) // Array<Projection> の展開
                else -> emptyList()
            }
        }
        infoExiting(result)
        return result
    }

    /**
     * ## プロパティ存在検査
     * ### @Projection/@Projections で指定された `properties` の文字列が
     * ### 実際のクラスに存在するプロパティ名かどうかを検証する
     * - 存在しない場合はビルドエラー
     * @param classDecl @Projection が適用されたクラス
     * @param annotation 検査対象の @Projection アノテーション
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private fun checkProjectionProperties(
        classDecl: KSClassDeclaration,
        annotation: KSAnnotation,
    ) {
        infoEntered(classDecl, annotation)
        // リスト化された properties の値
        val propertiesValues =
            collectFields(annotation)[PROPERTIES] as? List<*> ?: emptyList<String>()
        // @Projection が適用されたクラスのプロパティ名一覧を取得
        val fqn = classDecl.qualifiedName?.asString() ?: run {
            this.error("Annotation target class is null.")
            return
        }
        // ただし、allClassProperties に登録済みならば、allClassProperties からプロパティ名一覧を取得
        val properties = allClassProperties[fqn]
            ?: classDecl.getAllProperties().map {
                // プロパティ名を取得
                it.simpleName.asString()
            }.toList()
        // allClassProperties[fqn] が、null 時の再代入
        allClassProperties[fqn] = properties
        // properties に指定されたプロパティ名の検査
        propertiesValues.forEach { property ->
            if (!properties.contains(property)) {
                error(
                    "property '$property' is not declared in class",
                    classDecl.simpleName.asString()
                )
            }
        }
        infoExiting()
    }

    /**
     * ## data class 生成処理
     * ### @Projection の内容から新しい data class を構築しファイル出力する
     * - properties → 通常の列
     * - functions → 関数列（SUM, AVG, …）
     *   - プロパティ化 + @Function アノテーション付与を別メソッドで処理
     * @param classDecl @Projection が適用されたクラス
     * @param annotation 対象 @Projection
     * @param resolver KSP のアノテーション解析リゾルバ
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private fun processSingleProjection(
        classDecl: KSClassDeclaration,
        annotation: KSAnnotation,
        resolver: Resolver
    ) {
        infoEntered(classDecl, annotation)
        val dataClassMaterialMap = collectFields(annotation).toMutableMap()
        // パッケージ名、クラス名、テーブル名などメタ情報を構築
        val packageName = generatedPackageNameFromAnnotation(
            classDecl,
            resolver.getSymbolsWithAnnotation(ENTITY_PACKAGE_INFO_FQN, false),
            (dataClassMaterialMap[COMMON_INTERFACE] as List<*>).firstOrNull().toString()
        )
        val createClassName = classDecl.simpleName.asString() +
                dataClassMaterialMap[EXTEND_NAME].toString()
        val tableAnnotation = classDecl.annotations
            .firstOrNull { it.shortName.asString() == TABLE }
        val tableName = tableAnnotation?.let { extractTableName(it) }
            ?: classDecl.simpleName.asString().toSnakeCase()
        val tableAlias = tableAnnotation?.let {
            geerateTableAlias(it, dataClassMaterialMap[EXTEND_ALIAS].toString())
        } ?: tableName
        // @Table の生成
        tableAnnotationSpec = AnnotationSpec.builder(Table::class).apply {
            addMember("$TABLE_NAME = %S", tableName)
            addMember("$TABLE_ALIAS = %S", tableAlias)
        }.build()
        // properties 部分（通常列）の抽出
        val selectedProps = classDecl.getAllProperties()
            .filter {
                (dataClassMaterialMap[PROPERTIES] as List<*>).contains(it.simpleName.asString())
            }.toList()
        // functions 部分（関数列）の抽出
        val functionProjections = (dataClassMaterialMap[FUNCTIONS] as? List<*>)
            ?.filterIsInstance<FunctionProjection>()
            ?: emptyList()
        // プロパティ名 → KSPropertyDeclaration のマップを生成
        val propsByName: Map<String, KSPropertyDeclaration> =
            classDecl.getAllProperties().associateBy { it.simpleName.asString() }
        // マーカーインターフェースの抽出
        val interfaces = collectInterfaces(dataClassMaterialMap)
        // data class の構成を定義
        val fileSpec = createDataClassFile(
            ClassName(packageName, createClassName),
            selectedProps,
            generateFunctionAnnotations(functionProjections, propsByName),
            interfaces
        )
        // データクラスを kt ファイルとして出力
        val fileDependency =
            classDecl.containingFile?.let { Dependencies(true, it) } ?: Dependencies(false)
        fileSpec.writeTo(codeGenerator, fileDependency)
        infoExiting(functionProjections)
    }

    /**
     * ## @Projections から @Projection のリストを抽出
     * ### @Projections に定義されている @Projection を List として抽出
     * @param annotation @Projection を抽出した解析対象
     * @return 抽出した @Projection のリスト
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private fun extractProjectionList(annotation: KSAnnotation): List<KSAnnotation> {
        infoEntered(annotation)
        // @Projections から value を抽出
        val result = (annotation.arguments.firstOrNull { it.name?.asString() == "value" }
            // value がリストにキャスト可能か？
            ?.value as? List<*>)
            // 可能であれば value.filterIsInstance<KSAnnotation>() を戻す
            ?.filterIsInstance<KSAnnotation>()
        // 不可能であれば 空リストを戻す
            ?: emptyList()
        infoExiting(result)
        return result
    }

    /**
     * ## テーブル名抽出
     * ### 指定されたテーブルアノテーションからテーブル名を抽出
     * @param tableAnnotation テーブルアノテーション
     * @param classDecl @Projection が適用されたクラス
     * @return 抽出したテーブル名
     * @author Masahiro Inoue
     * @since 2025-08-22
     */
    private fun extractTableName(
        tableAnnotation: KSAnnotation,
    ): String? {
        infoEntered(tableAnnotation)
        val result = tableAnnotation.arguments
            .firstOrNull { it.name?.asString() == TABLE_NAME }
            ?.value
            ?.takeIf { it is String && it.isNotBlank() }
            ?.let { it as String }
        infoExiting(result)
        return result
    }

    /**
     * ## テーブルエイリアス抽出
     * ### 指定されたテーブルアノテーションからテーブルエイリアスを抽出し
     * ### 引数で指定された拡張エイリアスと結合
     * @param tableAnnotation テーブルアノテーション
     * @param extendAlias 拡張エイリアス
     * @return 生成されたテーブルエイリアス
     * @author Masahiro Inoue
     * @since 2025-08-22
     */
    private fun geerateTableAlias(
        tableAnnotation: KSAnnotation,
        extendAlias: String
    ): String? {
        infoEntered(tableAnnotation, extendAlias)
        val aliasFromAnnotation = tableAnnotation.arguments
            .firstOrNull { it.name?.asString() == TABLE_ALIAS }
            ?.value as? String
        val result = buildAlias(aliasFromAnnotation, extendAlias)
        infoExiting(result)
        return result
    }

    /**
     * ## マーカーインターフェース抽出
     * ### マーカーインターフェースのリストを生成
     * @param dataClassMaterialMap メタデータ
     * @return マーカーインターフェースのリスト
     * @author Masahiro Inoue
     * @since 2025-08-22
     */
    private fun collectInterfaces(dataClassMaterialMap: MutableMap<String, Any>): List<TypeName> {
        infoEntered(dataClassMaterialMap)
        val result = buildList<TypeName> {
            (dataClassMaterialMap[COMMON_INTERFACE] as List<*>)
                .map {
                    add(
                        ClassName.bestGuess(
                            DMLInterfaceEnum.valueOf(
                                ClassName
                                    .bestGuess(it.toString())
                                    .simpleName
                            ).interfaceFQN
                        )
                    )
                }
            (dataClassMaterialMap[CUSTOM_INTERFACE] as? String)
                ?.takeIf { it.isNotBlank() }
                ?.let { add(ClassName.bestGuess(it)) }
        }
        infoExiting(result)
        return result
    }

    /**
     * ## FunctionProjection → PropertySpec 変換
     * ### FunctionProjection から PropertySpec のリストを生成する
     * @param classDecl @Projection が適用されたクラス
     * @param functions Projection に定義された関数列
     * @param propsByName プロパティ名とプロパティ定義のマップ
     * @return data class に埋め込む用の PropertySpec のリスト
     * @author Masahiro Inoue
     * @since 2025-08-22
     */
    private fun generateFunctionAnnotations(
        functions: List<FunctionProjection>,
        propsByName: Map<String, KSPropertyDeclaration>
    ): List<PropertySpec> {
        infoEntered(functions, propsByName)
        val properties = propsByName.keys
        val result = functions.map { func ->
            checkFunctionArgs(properties, func)
            // 戻り値型（KotlinPoet の TypeName）を解決
            val typeName = resolveReturnType(func, propsByName)
            // @Function アノテーションを生成（raw/CUSTOM・args を考慮）
            val annotation = buildFunctionAnnotation(func)
            // alias をキャメルケース に変換して PropertySpec を生成
            PropertySpec.builder(func.alias.toCamelCase(), typeName)
                // @Function とプロパティを結び付け
                .addAnnotation(annotation)
                .build()
        }
        infoExiting(result)
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
    private fun generatedPackageNameFromAnnotation(
        classDeclaration: KSClassDeclaration,
        symbols: Sequence<KSAnnotated>,
        commonInterface: String
    ): String {
        infoEntered(classDeclaration, symbols, commonInterface)
        if (commonInterface.isNullOrEmpty()) {
            return classDeclaration.packageName.asString()
        }
        val common = generateCommonInterFace(commonInterface)
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
                } ?: false) {
                val base = annotation?.arguments?.firstOrNull {
                    it.name?.asString() == BASE_PACKAGE
                }?.value
                // サブパッケージの取得
                // PackageInterfaceRelation から common.canonicalName に該当するものを抽出
                val sub = PackageInterfaceRelation.values().firstOrNull() {
                    DMLInterfaceEnum.valueOf(it.name).interfaceFQN == common.canonicalName
                }
                    // DMLInterfaceEnum から取得した列挙子の relation の値を取得
                    .let { it?.relation }
                    // EntityPackageInfo の列挙子からサブパッケージを取得
                    .let { argument ->
                        annotation?.arguments?.firstOrNull() {
                            it.name?.asString() == argument
                        }
                    }?.value
                val result = "$base.$sub"
                infoExiting(result)
                return result
            }
        }
        val result = common.packageName
        infoExiting(result)
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
        functionProps: List<PropertySpec>,
        interfaces: List<TypeName>
    ): FileSpec {
        infoEntered(classNameFQN, selectedProps, interfaces)
        // 通常列を PropertySpec へ
        val normalProps: List<PropertySpec> = selectedProps.map { it.toPropertySpec() }

        // ここで「通常列 + 関数列（@Function 付き）」を合体
        val allProps = buildList {
            addAll(normalProps)
            addAll(functionProps)
        }

        // クラスビルダー
        // ファイルの構成要素としてクラスを追加し呼び出し元へ戻す
        val result = FileSpec.builder(classNameFQN)
            .addType(
                TypeSpec.classBuilder(classNameFQN)
                    .addModifiers(KModifier.DATA)
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .apply {
                                allProps.forEach { prop ->
                                    addParameter(
                                        ParameterSpec.builder(prop.name, prop.type)
                                            .build()
                                    )
                                }
                            }
                            .build()
                    )
                    .addProperties(
                        allProps.map { prop ->
                            prop.toBuilder(prop.name, prop.type)
                                .initializer(prop.name)
                                .mutable(false)
                                .build()
                        }
                    )
                    .addSuperinterfaces(interfaces)
                    .addAnnotation(tableAnnotationSpec)
                    .build()
            )
            .build()
        infoExiting(result)
        return result
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
        infoEntered(annotation)
        val result = mutableMapOf<String, Any>()
        // アノテーションの引数を取得し加工
        annotation.arguments.forEach {
            val argName = it.name?.asString()
            val value = it.value
            when (argName) {
                FUNCTIONS -> {
                    // List<KSAnnotation> を List<FunctionProjection> に変換
                    val projections = (value as? List<*>)?.mapNotNull { v ->
                        (v as? KSAnnotation)?.let { ksAnn ->
                            val func = ksAnn.argumentOf<ColumnFunction>(FP_FUNCTION)
                            val raw = ksAnn.argumentOf<String>(FP_RAW) ?: EMPTY_STRING
                            if (func.isNull() && raw.isEmpty()) {
                                error(
                                    "@FunctionProjection requires either " +
                                            "'function' or 'raw' to be specified",
                                    ksAnn,
                                    ksAnn.argumentOf<ColumnFunction>(FP_FUNCTION) ?: "null"
                                )
                            }
                            FunctionProjection(
                                function = func
                                    ?: ColumnFunction.CUSTOM,
                                args = ksAnn.argumentOf<List<String>>(FP_ARGS)?.toTypedArray()
                                    ?: emptyArray(),
                                alias = ksAnn.argumentOf<String>(FP_ALIAS) ?: EMPTY_STRING,
                                returnHint = ksAnn.argumentOf<ReturnHint>(FP_RETURN_HINT)
                                    ?: ReturnHint.AUTO,
                                raw = raw
                            )
                        }
                    } ?: emptyList()
                    result[argName] = projections
                }

                COMMON_INTERFACE,
                CUSTOM_INTERFACE -> {
                    // 配列対応（String または DMLInterfaceEnum の配列）
                    val listValue: List<Any> = (value as? List<*>)?.mapNotNull { element ->
                        when (element) {
                            is KSType -> element.declaration.simpleName.asString()
                            is String -> element
                            is DMLInterfaceEnum -> element
                            else -> null
                        }
                    } ?: emptyList()
                    argName.let { result[it] = listValue }
                }

                else -> when (value) {
                    // アノテーション変数が配列
                    is List<*> -> {
                        val castedValue = it.value as List<*>
                        val firstElement = castedValue.firstOrNull()
                        val fieldList: List<Any> = when (firstElement) {
                            // KSType から変数型を取得
                            is KSType -> castedValue.mapNotNull { geneType ->
                                (geneType as KSType).declaration.simpleName.asString()
                            }
                            // 変数型を文字列に変換
                            is String -> castedValue.filterIsInstance<String>()
                            // 変数型を Int に変換
                            is Int -> castedValue.filterIsInstance<Int>()
                            // それ以外は空リスト
                            else -> emptyList()
                        }
                        argName?.let { result[it] = fieldList }
                    }
                    // アノテーション変数が上記に当てはまらない
                    else -> {
                        argName?.let { key -> result[key] = it.value ?: EMPTY_STRING }
                    }
                }
            }
        }
        infoExiting(result)
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
        infoEntered(prop)
        // @Column アノテーションを探す
        val columnAnnotation = prop.annotations
            .firstOrNull { it.shortName.asString() == COLUMN }
        if (columnAnnotation?.let {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == COLUMN_FQN
            } ?: false) {
            // アノテーション引数からカラム名を取得
            val columnName = columnAnnotation?.arguments
                ?.firstOrNull { it.name?.asString() == COLUMN_NAME }
                ?.value as? String
            // アノテーション引数からカラムエイリアスを取得
            val columnAlias = columnAnnotation?.arguments
                ?.firstOrNull { it.name?.asString() == COLUMN_ALIAS }
                ?.value as? String
            // AnnotationSpec に変換
            val result = if (!columnName.isNullOrBlank() || !columnAlias.isNullOrBlank()) {
                // @Column のコピー
                AnnotationSpec.builder(ClassName.bestGuess(COLUMN_FQN))
                    .apply {
                        if (!columnName.isNullOrBlank()) addMember(
                            "$COLUMN_NAME = %S",
                            columnName
                        )
                        if (!columnAlias.isNullOrBlank()) addMember(
                            "$COLUMN_ALIAS = %S",
                            columnAlias
                        )
                    }.build()
            } else {
                null
            }
            infoExiting(result)
            return result
        }
        infoExiting("null")
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
    private fun generateCommonInterFace(commonInterface: String): ClassName {
        infoEntered(commonInterface)
        val result = ClassName.bestGuess(
            (DMLInterfaceEnum.valueOf(
                ClassName.bestGuess(commonInterface)
                    .simpleName
            ))
                .interfaceFQN
        )
        infoExiting(result)
        return result
    }

    /**
     * ## PropertySpec 生成
     * ### KSPropertyDeclaration を基に PropertySpec を生成する
     * @receiver KSPropertyDeclaration 元データ
     * @return PropertySpec 生成結果
     * @author Masahiro Inoue
     * @since 2025-09-05
     */
    private fun KSPropertyDeclaration.toPropertySpec(): PropertySpec {
        return PropertySpec.builder(
            simpleName.asString(),
            this.type.toTypeName()
        ).initializer(simpleName.asString())
            .build()
    }

    /**
     * ## リテラル型推論
     * ### 引数がリテラルの場合、その型を推論する
     * - true/false → Boolean
     * - '...' → String
     * - 整数 → Long
     * - 小数 → Double
     * - それ以外 → null
     * @param literal リテラル文字列
     * @return 推論された型（TypeName） / 推論できない場合は null
     * @author Masahiro Inoue
     * @since 2025-09-05
     */
    private fun inferLiteralTypeName(literal: String): TypeName? = when {
        literal.equals("true", true) || literal.equals("false", true) -> BOOLEAN
        literal.startsWith("'") && literal.endsWith("'") -> STRING
        literal.matches(Regex("^-?\\d+$")) -> LONG          // 整数リテラルは Long 寄せ
        literal.matches(Regex("^-?\\d+\\.\\d+$")) -> DOUBLE // 小数は Double
        else -> null
    }

    /**
     * ## 戻り値型解決
     * ### 関数の戻り値型を解決する
     * - propsByName から引数の型を取得
     * - リテラルの場合は inferLiteralTypeName で型を推論
     * - 関数の種類に応じて戻り値型を決定
     * @param func 関数情報
     * @param propsByName プロパティ名とプロパティ定義のマップ
     * @return 解決された戻り値型（TypeName）
     * @author Masahiro Inoue
     * @since 2025-09-05
     */
    private fun resolveReturnType(
        func: FunctionProjection,
        propsByName: Map<String, KSPropertyDeclaration>
    ): TypeName {
        // 引数（カラム名 or リテラル）を TypeName に寄せる
        val argTypes: List<TypeName> = func.args.mapNotNull { arg ->
            propsByName[arg]?.type?.resolve()?.toTypeName()
                ?: inferLiteralTypeName(arg)
        }

        return when (func.function) {
            // AVG は常に Double
            ColumnFunction.AVG -> DOUBLE
            // SUM は引数が整数型なら Long、浮動小数点型なら Double、その他は ANY（事実上無）
            ColumnFunction.SUM -> LONG
                .takeIf { argTypes.all { it == INT || it == LONG } }
                ?: DOUBLE.takeIf { argTypes.any { it == DOUBLE || it == FLOAT } }
                ?: ANY
            // MAX / MIN は引数の型に依存
            ColumnFunction.MAX,
            ColumnFunction.MIN -> argTypes.firstOrNull() ?: ANY
            // COUNT は常に Long
            ColumnFunction.COUNT,
            ColumnFunction.COUNT_ALL -> LONG
            // GROUP_CONCAT は常に String
            ColumnFunction.GROUP_CONCAT -> STRING
            // null 判定関数は引数の型に依存（広い方に寄せる）
            ColumnFunction.COALESCE,
            ColumnFunction.IFNULL,
            ColumnFunction.NULLIF -> argTypes.reduceOrNull(::widerType) ?: ANY
            // LENGTH は常に Int
            ColumnFunction.LENGTH -> INT
            // 文字列関数は常に String
            ColumnFunction.LOWER,
            ColumnFunction.UPPER,
            ColumnFunction.REPLACE,
            ColumnFunction.SUBSTR,
            ColumnFunction.CONCAT,
            ColumnFunction.TRIM,
            ColumnFunction.LTRIM,
            ColumnFunction.RTRIM -> STRING
            // 数学関数
            ColumnFunction.RANDOM -> LONG
            ColumnFunction.ROUND -> DOUBLE
            // 日付/時刻関数は常に String
            ColumnFunction.DATE,
            ColumnFunction.TIME,
            ColumnFunction.DATETIME,
            ColumnFunction.STRFTIME -> STRING
            // 日付/時刻関数（数値型を返すもの）
            ColumnFunction.JULIANDAY -> DOUBLE
            // ABS は引数の型に依存
            ColumnFunction.ABS -> {
                val t = argTypes.firstOrNull()
                when (t) {
                    LONG, INT -> LONG
                    DOUBLE, FLOAT -> DOUBLE
                    else -> DOUBLE
                }
            }
            // CUSTOM または raw 指定 → returnHint に依存
            ColumnFunction.CUSTOM -> when (func.returnHint) {
                ReturnHint.STRING -> STRING
                ReturnHint.INT -> INT
                ReturnHint.LONG -> LONG
                ReturnHint.DOUBLE -> DOUBLE
                ReturnHint.BOOLEAN -> BOOLEAN
                else -> STRING
            }
        }
    }

    /**
     * ## 型の広い方を決定
     * ### 2つの TypeName を比較し、より広い型を決定する
     * - 同じ型ならその型を返す
     * - String が含まれる場合は String を返す
     * - Double または Float が含まれる場合は Double を返す
     * - Long が含まれる場合は Long を返す
     * - Int が含まれる場合は Long を返す（混在は Long に寄せる）
     * - それ以外は Any を返す
     * @param type1 比較対象の型1
     * @param type2 比較対象の型2
     * @return より広い型（TypeName）
     * @author Masahiro Inoue
     * @since 2025-09-05
     */
    private fun widerType(type1: TypeName, type2: TypeName): TypeName {
        if (type1 == type2) return type1
        if (type1 == STRING || type2 == STRING) return STRING
        if (type1 == DOUBLE || type2 == DOUBLE || type1 == FLOAT || type2 == FLOAT) return DOUBLE
        if (type1 == LONG || type2 == LONG) return LONG
        if (type1 == INT || type2 == INT) return LONG // 混在は Long に寄せる
        return ANY
    }

    /**
     * ## @Function アノテーション生成
     * ### FunctionProjection から @Function アノテーションを生成する
     * - raw が指定されている場合は raw を優先し、args は無視する
     * - raw が空で function が CUSTOM の場合はビルドエラー
     * - function が CUSTOM で returnHint が AUTO 以外の場合は returnHint を付与する
     * @param func 関数情報
     * @return 生成された @Function アノテーション
     * @author Masahiro Inoue
     * @since 2025-09-05
     */
    private fun buildFunctionAnnotation(func: FunctionProjection): AnnotationSpec {
        infoEntered(func)
        val result = AnnotationSpec.builder(Function::class).apply {
            // function
            val sqlFunc = if (func.raw.isNotBlank()) ColumnFunction.CUSTOM
            else func.function
            addMember("$F_COLUMN_FUNCTION = %T.%L", ColumnFunction::class, sqlFunc.name)
            // alias
            addMember("$F_ALIAS = %S", func.alias)
            // args or raw
            if (func.raw.isNotBlank()) {
                addMember("$F_RAW = %S", func.raw)
            } else {
                val argsLiteral = func.args.joinToString(", ") { "\"$it\"" }
                addMember("$F_ARGS = [%L]", argsLiteral)
            }
        }.build()
        infoExiting(result)
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
        infoEntered(name)
        val argValue = arguments.firstOrNull { it.name?.asString() == name }?.value
            ?: run {
                // 引数が存在しない場合は null を戻す
                infoExiting("null")
                return null
            }
        // Enum の場合は KSType から Enum を取得
        if (T::class.java.isEnum) {
            val ksType = argValue as? KSType ?: run {
                // 引数が存在しない場合は null を戻す
                infoExiting("null")
                return null
            }
            val enumName = ksType.declaration.simpleName.asString()

            @Suppress("UNCHECKED_CAST")
            val result = java.lang.Enum.valueOf(T::class.java as Class<out Enum<*>>, enumName) as T
            infoExiting(result)
            return result
        }
        infoExiting(argValue)
        return argValue as? T
    }

    /**
     * ## 関数引数検査
     * ### FunctionProjection の引数が元のクラスに存在するプロパティ名かどうかを検証する
     * - 存在しない場合、又は「'（シングルクオーテーション）」で囲われていない場合、ビルドエラー
     * - 文字列リテラルの場合は検査対象外
     * @param properties 元のクラスに存在するプロパティ名一覧
     * @param functionProjections 関数列
     * @author Masahiro Inoue
     * @since 2025-09-05
     */
    private fun checkFunctionArgs(
        properties: Set<String>,
        functionProjection: FunctionProjection
    ) {
        infoEntered(functionProjection, properties)
        // プロパティ名一覧を取得
        val columnFunction = functionProjection.function
        // CUSTOM は args 検査不要
        if (columnFunction == ColumnFunction.CUSTOM) {
            infoExiting()
            return
        }
        // 引数のリスト化
        val args = functionProjection.args.toList()
        //
        val invalidArgs = if (columnFunction.isSingleArgument) {
            // 単一引数の場合は first() だけ確認
            args.firstOrNull()?.takeIf {
                !it.matches(Regex("'[^']*'")) && it !in properties
            }?.let { listOf(it) } ?: emptyList()
        } else {
            // 複数引数の場合は一つでも存在すればOK
            args.filter { !it.matches(Regex("'[^']*'")) && it !in properties }
        }
        // 引数の定義誤りがある場合（List に登録）、ビルドエラー
        if (invalidArgs.isNotEmpty()) {
            error("Invalid argument for '${functionProjection.alias}': $invalidArgs")
        }
        infoExiting()
    }

    /**
     * ## 集計関数と通常カラムの重複検査
     * ### Projection に指定された通常カラムと集計関数の対象カラムが
     * ### 重複している場合、警告を出す
     * @param annotation @Projection を抽出した解析対象
     * @author Masahiro Inoue
     * @since 2025-09-05
     */
    private fun checkAggregateConflicts(annotation: KSAnnotation) {
        infoEntered(annotation)
        // properties を取得して正規化
        val propertiesValues: Set<String> =
            (annotation.arguments.firstOrNull { it.name?.asString() == PROPERTIES }?.value as? List<*>)
                ?.mapNotNull { it as? String }
                ?.map { it.trim().lowercase() }
                ?.toSet()
                ?: emptySet()
        // functions を取得
        val functionsValues: List<KSAnnotation> =
            (annotation.arguments.firstOrNull { it.name?.asString() == FUNCTIONS }?.value as? List<*>)
                ?.mapNotNull { it as? KSAnnotation }
                ?: emptyList()
        // functions の args を平坦化して正規化
        val functionTargetColsNormalized: Set<String> = functionsValues
            .filter { funcAnnotation ->
                val funcEnum = funcAnnotation.arguments.firstOrNull {
                    it.name?.asString() == FP_FUNCTION
                }?.value as? ColumnFunction
                funcEnum != ColumnFunction.CUSTOM
            }
            .flatMap { funcAnnotation ->
                (funcAnnotation.arguments.firstOrNull {
                    it.name?.asString() == FP_ARGS
                }?.value as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?.map { arg -> arg.trim().substringAfterLast('.').lowercase() }
                    ?: emptyList()
            }
            .toSet()
        // 重複検出
        val duplicates = propertiesValues.intersect(functionTargetColsNormalized)
        if (duplicates.isNotEmpty()) {
            warning(
                "Projection contains column(s) that are both in properties and used as aggregate targets: " +
                        duplicates.joinToString(", ")
            )
        }
        infoExiting()
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
enum class PackageInterfaceRelation(val relation: String) {
    SELECT("selectPackage"),
    INSERT("insertPackage"),
    UPDATE("updatePackage"),
    UPSERT("upsertPackage"),
    CONDITION("conditionPackage"),
}
