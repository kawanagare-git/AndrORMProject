package jp.pgw.lab78.androrm.ksp.resolver

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.Constants.NULL_STRING
import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.EntityConstants.PackageInterfaceRelation
import jp.pgw.lab78.androrm.common.annotation.EntityPackageInfo
import jp.pgw.lab78.androrm.common.logging.interfaces.LoggerLike
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger.logger
import jp.pgw.lab78.androrm.ksp.projectoin.ProjectionDefinition

/**
 * ## インターフェース解決クラス
 * ### @EntityPackageInfo アノテーションと andrOrmSubPackage を基に、出力先 package 名を解決する
 * ### andrOrmSubPackage の列挙値から FQN を解決する
 * ### andrOrmSubPackage / customSubPackage から、実装対象の interface 一覧を構築する
 * @author Masahiro Inoue
 * @since 2026-04-21
 */
class InterfaceResolver() : LoggerLike by logger {

    /**
     * ## インターフェース解決用定数
     * ### Entityパッケージ情報のアノテーション名を保持する
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    companion object {
        /** @EntityPackageInfo の変数名定義（basePackage） */
        private const val BASE_PACKAGE = "basePackage"

        /** @EntityPackageInfo */
        private val ENTITY_PACKAGE_INFO = EntityPackageInfo::class.simpleName!!

        /** @EntityPackageInfo(FQN) */
        private val ENTITY_PACKAGE_INFO_FQN = EntityPackageInfo::class.qualifiedName!!
    }

    /**
     * ## package 名解決
     * ### @EntityPackageInfo と andrOrmSubPackage を基に
     * ### 出力先 package 名を解決する
     * @param classDeclaration 対象クラスの宣言
     * @param symbols KSP のシンボルのシーケンスル
     * @param andrOrmSubPackage @Projection アノテーションの andrOrmSubPackage 引数。未指定の場合は null
     * @param customSubPackages @Projection アノテーションの customSubPackage 引数
     * @return 出力先 package 名
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    fun resolvePackageNameFromAnnotation(
        classDeclaration: KSClassDeclaration,
        symbols: Sequence<KSAnnotated>,
        andrOrmSubPackage: DMLInterfaceEnum?,
        customSubPackages: List<String>,
    ): String {
        logTraceEntered(
            classDeclaration,
            symbols,
            andrOrmSubPackage ?: EMPTY_STRING,
            customSubPackages,
        )
        // andrOrmSubPackage が NOT_USE の場合は、basePackage と customSubPackage から package 名を構築して返す
        if (andrOrmSubPackage == DMLInterfaceEnum.NOT_USE) {
            val basePackage = resolveBasePackage(symbols)
                ?: classDeclaration.packageName.asString()
            val customSubPackage = customSubPackages
                .firstOrNull { it.isNotBlank() }
                ?.trim('.')
                .orEmpty()
            return listOf(basePackage.trim('.'), customSubPackage)
                .filter { it.isNotBlank() }
                .joinToString(".")
                .also { logTraceExiting(it) }
        }
        // andrOrmSubPackage が空の場合は、クラスの package 名を返す
        if (andrOrmSubPackage == null) {
            val result = classDeclaration.packageName.asString()
            logTraceExiting(result)
            return result
        }
        // andrOrmSubPackage から FQN を解決する
        val common = generateCommonInterface(andrOrmSubPackage)
        //
        symbols.filterIsInstance<KSFile>().forEach { symbol ->
            val annotation = symbol.annotations.firstOrNull {
                it.shortName.asString() == ENTITY_PACKAGE_INFO
            }
            // @EntityPackageInfo アノテーションが存在し、かつ andrOrmSubPackage に対応する relation が定義されている場合は、basePackage と relation から package 名を構築して返す
            if (annotation?.let {
                    it.annotationType.resolve().declaration.qualifiedName?.asString() ==
                            ENTITY_PACKAGE_INFO_FQN
                } == true
            ) {
                // basePackage を抽出
                val base = annotation.arguments.firstOrNull {
                    it.name?.asString() == BASE_PACKAGE
                }?.value
                // andrOrmSubPackage に対応する relation を抽出
                val sub = PackageInterfaceRelation.entries.firstOrNull {
                    DMLInterfaceEnum.valueOf(it.name).interfaceFQN == common.canonicalName
                }
                    ?.relation
                    ?.let { relationName ->
                        annotation.arguments.firstOrNull {
                            it.name?.asString() == relationName
                        }?.value
                    }
                return "$base.$sub".also { logTraceExiting(it) }
            }
        }
        // @EntityPackageInfo アノテーションが存在しない、または andrOrmSubPackage に対応する relation が定義されていない場合は、andrOrmSubPackage の FQN を package 名として返す
        return common.packageName.also { logTraceExiting(it) }
    }

    /**
     * ## ベース package 名解決
     * ### @EntityPackageInfo から basePackage を取得する
     * @param symbols KSP のシンボルのシーケンスル
     * @return basePackage。@EntityPackageInfo が存在しない場合は null
     * @author Masahiro Inoue
     * @since 2026-07-31
     */
    private fun resolveBasePackage(
        symbols: Sequence<KSAnnotated>,
    ): String? {
        logTraceEntered(symbols)
        symbols.filterIsInstance<KSFile>().forEach { symbol ->
            val annotation = symbol.annotations.firstOrNull {
                it.shortName.asString() == ENTITY_PACKAGE_INFO
            }
            // @EntityPackageInfo アノテーションが存在する場合は、basePackage を返す
            if (annotation?.let {
                    it.annotationType.resolve().declaration.qualifiedName?.asString() ==
                            ENTITY_PACKAGE_INFO_FQN
                } == true
            ) {
                return (annotation.arguments.firstOrNull {
                    it.name?.asString() == BASE_PACKAGE
                }?.value as? String)
                    .also { logTraceExiting(it ?: EMPTY_STRING) }
            }
        }
        logTraceExiting(NULL_STRING)
        return null
    }

    /**
     * ## 共通インターフェース解決
     * ### andrOrmSubPackage の列挙値から FQN を解決する
     * @param andrOrmSubPackage @Projection アノテーションの andrOrmSubPackage 引数
     * @return andrOrmSubPackage に対応するインターフェースの ClassName オブジェクト
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    private fun generateCommonInterface(andrOrmSubPackage: DMLInterfaceEnum): ClassName {
        logTraceEntered(andrOrmSubPackage)
        // andrOrmSubPackage から FQN を解決して返す
        return ClassName.bestGuess(andrOrmSubPackage.interfaceFQN)
            .also { logTraceExiting(it) }
    }

    /**
     * ## interface 一覧生成
     * ### andrOrmSubPackage から
     * ### 実装対象の interface 一覧を構築する
     * @param definition データクラスのマテリアルマップ。andrOrmSubPackage を含む
     * @return 実装対象の interface 一覧の TypeName のリスト
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    fun collectInterfaces(
        definition: ProjectionDefinition,
    ): List<TypeName> {
        logTraceEntered(definition)
        // andrOrmSubPackages から、実装対象の interface 一覧を構築する
        return buildList<TypeName> {
            definition.andrOrmSubPackages
                .filterNot { it == DMLInterfaceEnum.NOT_USE }
                .forEach { common ->
                    add(ClassName.bestGuess(common.interfaceFQN))
                }
        }.also { logTraceExiting(it) }
    }
}