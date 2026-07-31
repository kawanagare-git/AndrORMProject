package jp.pgw.lab78.androrm.common.database

import jp.pgw.lab78.androrm.common.MessageConstants.CE00016
import jp.pgw.lab78.androrm.common.database.annotation.Column
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaGetter

/**
 * ## Columnアノテーション解決クラス
 * ### 実装先プロパティと継承元インターフェースから
 * ### Columnアノテーションを解決する
 * @author Masahiro Inoue
 * @since 2026-07-31
 */
internal object ColumnAnnotationResolver {
    /**
     * ## プロパティからColumnアノテーションを解決
     * @param property 対象プロパティ
     * @return Columnアノテーション / 存在しない場合null
     * @author Masahiro Inoue
     * @since 2026-07-31
     */
    fun find(property: KProperty1<*, *>): Column? =
        findDeclared(property)
            ?: property.ownerKClassOrNull()
                ?.findFromInterfaces(property.name)

    /**
     * ## クラスとプロパティ名からColumnアノテーションを解決
     * @param ownerClass 対象クラス
     * @param propertyName プロパティ名
     * @return Columnアノテーション / 存在しない場合null
     * @author Masahiro Inoue
     * @since 2026-07-31
     */
    fun find(
        ownerClass: KClass<*>,
        propertyName: String,
    ): Column? =
        findDeclared(
            ownerClass = ownerClass,
            propertyName = propertyName,
        ) ?: ownerClass.findFromInterfaces(propertyName)

    /**
     * ## プロパティ自身のColumnアノテーション取得
     * @author Masahiro Inoue
     * @since 2026-07-31
     */
    private fun findDeclared(
        property: KProperty1<*, *>,
    ): Column? =
        runCatching {
            property.findAnnotation<Column>()
        }.getOrNull()
            ?: property.ownerKClassOrNull()
                ?.findFromAnnotationMethod(property.name)

    /**
     * ## クラス自身に宣言されたプロパティのColumnアノテーション取得
     * @author Masahiro Inoue
     * @since 2026-07-31
     */
    private fun findDeclared(
        ownerClass: KClass<*>,
        propertyName: String,
    ): Column? =
        runCatching {
            ownerClass.declaredMemberProperties
                .firstOrNull { property ->
                    property.name == propertyName
                }
                ?.let(::findDeclared)
        }.getOrNull()
            ?: ownerClass.findFromAnnotationMethod(propertyName)

    /**
     * ## 継承元インターフェースのColumnアノテーション取得
     * @author Masahiro Inoue
     * @since 2026-07-31
     */
    private fun KClass<*>.findFromInterfaces(
        propertyName: String,
    ): Column? {
        // @Column を保有している実装元（interface）のリストを作成
        val candidates =
            findNearestInterfaceAnnotations(propertyName = propertyName, visited = mutableSetOf())
                .distinctBy { candidate -> candidate.interfaceClass }
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
                propertyName,
                qualifiedName,
                candidates.joinToString { candidate ->
                    candidate.interfaceClass.qualifiedName ?: candidate.interfaceClass.toString()
                },
            )
        }
        // @Column を戻す
        return candidates.first().annotation
    }

    /**
     * ## 各継承経路で最も近いColumnアノテーション候補取得
     * @author Masahiro Inoue
     * @since 2026-07-31
     */
    private fun KClass<*>.findNearestInterfaceAnnotations(
        propertyName: String,
        visited: MutableSet<KClass<*>>,
    ): List<InterfaceColumnAnnotation> {
        if (!visited.add(this)) {
            return emptyList()
        }

        return supertypes
            .mapNotNull { superType ->
                superType.classifier as? KClass<*>
            }
            .flatMap { superClass ->
                val annotation =
                    if (superClass.java.isInterface) {
                        findDeclared(
                            ownerClass = superClass,
                            propertyName = propertyName,
                        )
                    } else {
                        null
                    }

                if (annotation != null) {
                    listOf(
                        InterfaceColumnAnnotation(
                            interfaceClass = superClass,
                            annotation = annotation,
                        ),
                    )
                } else {
                    superClass.findNearestInterfaceAnnotations(
                        propertyName = propertyName,
                        visited = visited,
                    )
                }
            }
    }

    /**
     * ## Kotlinが生成するアノテーションメソッドからColumnを取得
     * @author Masahiro Inoue
     * @since 2026-07-31
     */
    private fun KClass<*>.findFromAnnotationMethod(
        propertyName: String,
    ): Column? =
        createAnnotationMethodNames(propertyName)
            .firstNotNullOfOrNull { methodName ->
                java.declaredMethods
                    .firstOrNull { method ->
                        method.name == methodName &&
                                method.parameterCount == 0
                    }
                    ?.getAnnotation(Column::class.java)
            }

    /**
     * ## プロパティの所有クラス取得
     * @author Masahiro Inoue
     * @since 2026-07-31
     */
    private fun KProperty1<*, *>.ownerKClassOrNull(): KClass<*>? =
        runCatching {
            javaGetter?.declaringClass?.kotlin
        }.getOrNull()
            ?: runCatching {
                parameters.first().type.classifier as? KClass<*>
            }.getOrNull()

    /**
     * ## アノテーションメソッド名候補生成
     * @author Masahiro Inoue
     * @since 2026-07-31
     */
    private fun createAnnotationMethodNames(
        propertyName: String,
    ): List<String> {
        val capitalizedName =
            propertyName.replaceFirstChar { character ->
                character.titlecase(Locale.ROOT)
            }

        return listOf(
            "get${capitalizedName}\$annotations",
            "is${capitalizedName}\$annotations",
            "${propertyName}\$annotations",
        )
    }

    /**
     * ## Column定義変換
     * @author Masahiro Inoue
     * @since 2026-07-31
     */
    private fun Column.toDefinition() =
        ColumnDefinition(
            name = name,
            alias = alias,
            hideFromSelect = hideFromSelect,
            default = default,
        )

    /**
     * ## インターフェースColumn候補
     * @author Masahiro Inoue
     * @since 2026-07-31
     */
    private data class InterfaceColumnAnnotation(
        val interfaceClass: KClass<*>,
        val annotation: Column,
    )

    /**
     * ## Column定義
     * @author Masahiro Inoue
     * @since 2026-07-31
     */
    private data class ColumnDefinition(
        val name: String,
        val alias: String,
        val hideFromSelect: Boolean,
        val default: String,
    )
}