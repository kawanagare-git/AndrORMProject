package jp.pgw.lab78.androrm.ksp.resolver

import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.EntityConstants.PackageInterfaceRelation
import jp.pgw.lab78.androrm.common.annotation.EntityPackageInfo
import jp.pgw.lab78.androrm.common.logging.interfaces.LoggerLike
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger.logger
import jp.pgw.lab78.androrm.ksp.projectoin.ProjectionDefinition

/**
 * ## インターフェース解決クラス
 * ### @EntityPackageInfo アノテーションと commonInterface を基に、出力先 package 名を解決する
 * ### commonInterface の列挙値から FQN を解決する
 * ### commonInterface / customInterface から、実装対象の interface 一覧を構築する
 * @author Masahiro Inoue
 * @since 2026-04-21
 */
class InterfaceResolver() : LoggerLike by logger {

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
     * ### @EntityPackageInfo と commonInterface を基に
     * ### 出力先 package 名を解決する
     * @param classDeclaration 対象クラスの宣言
     * @param symbols KSP のシンボルのシーケンスル
     * @param commonInterface @Projection アノテーションの commonInterface 引数。未指定の場合は null
     * @return 出力先 package 名
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    fun resolvePackageNameFromAnnotation(
        classDeclaration: KSClassDeclaration,
        symbols: Sequence<KSAnnotated>,
        commonInterface: DMLInterfaceEnum?
    ): String {
        logTraceEntered(classDeclaration, symbols, commonInterface ?: EMPTY_STRING)
        // commonInterface が空の場合は、クラスの package 名を返す
        if (commonInterface == null) {
            val result = classDeclaration.packageName.asString()
            logTraceExiting(result)
            return result
        }
        // commonInterface から FQN を解決する
        val common = generateCommonInterface(commonInterface)
        //
        symbols.filterIsInstance<KSFile>().forEach { symbol ->
            val annotation = symbol.annotations.firstOrNull {
                it.shortName.asString() == ENTITY_PACKAGE_INFO
            }
            // @EntityPackageInfo アノテーションが存在し、かつ commonInterface に対応する relation が定義されている場合は、basePackage と relation から package 名を構築して返す
            if (annotation?.let {
                    it.annotationType.resolve().declaration.qualifiedName?.asString() == ENTITY_PACKAGE_INFO_FQN
                } == true
            ) {
                // basePackage を抽出
                val base = annotation.arguments.firstOrNull {
                    it.name?.asString() == BASE_PACKAGE
                }?.value
                // commonInterface に対応する relation を抽出
                val sub = PackageInterfaceRelation.entries.firstOrNull {
                    DMLInterfaceEnum.valueOf(it.name).interfaceFQN == common.canonicalName
                }
                    ?.relation
                    ?.let { relationName ->
                        annotation.arguments.firstOrNull {
                            it.name?.asString() == relationName
                        }?.value
                    }

                val result = "$base.$sub"
                logTraceExiting(result)
                return result
            }
        }
        // @EntityPackageInfo アノテーションが存在しない、または commonInterface に対応する relation が定義されていない場合は、commonInterface の FQN を package 名として返す
        val result = common.packageName
        logTraceExiting(result)
        return result
    }

    /**
     * ## 共通インターフェース解決
     * ### commonInterface の列挙値から FQN を解決する
     * @param commonInterface @Projection アノテーションの commonInterface 引数
     * @return commonInterface に対応するインターフェースの ClassName オブジェクト
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    private fun generateCommonInterface(commonInterface: DMLInterfaceEnum): ClassName {
        logTraceEntered(commonInterface)
        // commonInterface が空の場合は、デフォルトのインターフェースを返す
        val result = ClassName.bestGuess(commonInterface.interfaceFQN)
        // commonInterface から FQN を解決して返す
        logTraceExiting(result)
        return result
    }

    /**
     * ## interface 一覧生成
     * ### commonInterface / customInterface から
     * ### 実装対象の interface 一覧を構築する
     * @param definition データクラスのマテリアルマップ。commonInterface と customInterface を含む
     * @return 実装対象の interface 一覧の TypeName のリスト
     * @author Masahiro Inoue
     * @since 2026-04-21
     */
    fun collectInterfaces(definition: ProjectionDefinition): List<TypeName> {
        logTraceEntered(definition)
        // commonInterface と customInterface から、実装対象の interface 一覧を構築する
        val result = buildList<TypeName> {
            definition.commonInterfaces.forEach { common ->
                add(ClassName.bestGuess(common.interfaceFQN))
            }
            definition.customInterfaces.filter { it.isNotBlank() }
                .forEach { custom -> add(ClassName.bestGuess(custom)) }
        }
        logTraceExiting(result)
        return result
    }
}