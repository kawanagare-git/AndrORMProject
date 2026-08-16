package jp.pgw.lab78.androrm.ksp.resolver

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.MessageConstants.CE00016
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_ALIAS
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_DEFAULT_VALUE
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_FQN
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_HIDE_FROM_SELECT
import jp.pgw.lab78.androrm.ksp.Constants.COLUMN_NAME
import jp.pgw.lab78.shared.library.Utils.isNotNull

/**
 * ## KSP用Columnアノテーション解決クラス
 * ### 実装先プロパティを優先し、
 * ### 存在しない場合は継承元インターフェースからColumnを取得する
 * @author Masahiro Inoue
 * @since 2026-07-31
 */
class KspColumnAnnotationResolver {

    /**
     * ## Columnアノテーション解決
     * ### 実装先プロパティを最優先する
     * @param ownerClass プロパティを実装するクラス
     * @param property 対象プロパティ
     * @return Columnアノテーション／存在しない場合null
     */
    fun find(
        ownerClass: KSClassDeclaration,
        property: KSPropertyDeclaration,
    ): KSAnnotation? =
        property.findColumnAnnotation()
            ?: ownerClass.findFromInterfaces(
                propertyName = property.simpleName.asString(),
            )

    /**
     * ## プロパティ自身のColumnアノテーション取得
     */
    private fun KSPropertyDeclaration.findColumnAnnotation(): KSAnnotation? =
        annotations.firstOrNull { annotation ->
            annotation.isColumnAnnotation()
        }

    /**
     * ## 継承元インターフェースからColumnを取得
     */
    private fun KSClassDeclaration.findFromInterfaces(
        propertyName: String,
    ): KSAnnotation? {
        // @Column を保有している実装元（interface）のリストを作成
        val candidates =
            findNearestInterfaceAnnotations(propertyName = propertyName, visited = mutableSetOf())
                .distinctBy { candidate ->
                    candidate.interfaceClass.qualifiedName
                        ?.asString()
                        ?: candidate.interfaceClass.simpleName.asString()
                }
        // 実装元のリストが空か
        if (candidates.isEmpty()) {
            return null
        }
        // @Column のメタ情報定義まで一致している重複を纏める
        val definitions =
            candidates.distinctBy { candidate -> candidate.annotation.toDefinition() }
        // メタ情報定義が一つだけなら通過
        check(definitions.size == 1) {
            CE00016.format(
                propertyName, qualifiedName?.asString() ?: simpleName.asString(),
                candidates.joinToString { candidate ->
                    candidate.interfaceClass.qualifiedName
                        ?.asString()
                        ?: candidate.interfaceClass.simpleName.asString()
                },
            )
        }
        return candidates.first().annotation
    }

    /**
     * ## 各継承経路で最も近いColumnを取得
     * ### 対象インターフェース自身にColumnがある場合、
     * ### その経路については上位インターフェースを検索しない
     */
    private fun KSClassDeclaration.findNearestInterfaceAnnotations(
        propertyName: String,
        visited: MutableSet<KSClassDeclaration>,
    ): List<InterfaceColumnAnnotation> {
        if (!visited.add(this)) {
            return emptyList()
        }
        return superTypes
            .mapNotNull { superType -> superType.resolve().declaration as? KSClassDeclaration }
            .flatMap { superClass ->
                val annotation =
                    if (superClass.classKind == ClassKind.INTERFACE) {
                        superClass.findDeclaredColumnAnnotation(
                            propertyName = propertyName,
                        )
                    } else {
                        null
                    }
                if (annotation.isNotNull()) {
                    sequenceOf(
                        InterfaceColumnAnnotation(
                            interfaceClass = superClass,
                            annotation = annotation,
                        ),
                    )
                } else {
                    superClass
                        .findNearestInterfaceAnnotations(
                            propertyName = propertyName,
                            visited = visited,
                        )
                        .asSequence()
                }
            }
            .toList()
    }

    /**
     * ## インターフェース自身に宣言されたプロパティからColumnを取得
     */
    private fun KSClassDeclaration.findDeclaredColumnAnnotation(
        propertyName: String,
    ): KSAnnotation? = declarations
        .filterIsInstance<KSPropertyDeclaration>()
        .firstOrNull { property -> property.simpleName.asString() == propertyName }
        ?.findColumnAnnotation()

    /**
     * ## Column判定
     */
    private fun KSAnnotation.isColumnAnnotation(): Boolean =
        annotationType.resolve()
            .declaration
            .qualifiedName
            ?.asString() == COLUMN_FQN

    /**
     * ## Column定義変換
     * ### 複数インターフェース間の設定内容比較に使用する
     */
    private fun KSAnnotation.toDefinition() =
        ColumnDefinition(
            name = stringArgument(COLUMN_NAME),
            alias = stringArgument(COLUMN_ALIAS),
            hideFromSelect = booleanArgument(COLUMN_HIDE_FROM_SELECT),
            defaultValue = stringArgument(COLUMN_DEFAULT_VALUE),
        )

    /**
     * ## 文字列引数取得
     */
    private fun KSAnnotation.stringArgument(argumentName: String): String =
        arguments.firstOrNull { argument -> argument.name?.asString() == argumentName }
            ?.value as? String
            ?: EMPTY_STRING

    /**
     * ## Boolean引数取得
     */
    private fun KSAnnotation.booleanArgument(argumentName: String): Boolean =
        arguments.firstOrNull { argument -> argument.name?.asString() == argumentName }
            ?.value as? Boolean
            ?: false

    /**
     * ## インターフェースColumn候補
     */
    private data class InterfaceColumnAnnotation(
        val interfaceClass: KSClassDeclaration,
        val annotation: KSAnnotation,
    )

    /**
     * ## Column設定内容
     */
    private data class ColumnDefinition(
        val name: String,
        val alias: String,
        val hideFromSelect: Boolean,
        val defaultValue: String,
    )
}