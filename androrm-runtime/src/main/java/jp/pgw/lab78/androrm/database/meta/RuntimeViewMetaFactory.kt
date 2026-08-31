package jp.pgw.lab78.androrm.database.meta

import jp.pgw.lab78.androrm.common.MessageConstants.AE00007
import jp.pgw.lab78.androrm.common.MessageConstants.AE00008
import jp.pgw.lab78.androrm.common.database.SupportFunction.findColumnAnnotation
import jp.pgw.lab78.androrm.common.database.SupportFunction.simpleNameToSnakeCase
import jp.pgw.lab78.androrm.common.database.annotation.Function
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.annotation.View
import jp.pgw.lab78.androrm.common.dml.interfaces.ViewDefinitionEntity
import jp.pgw.lab78.androrm.common.meta.PropertyMeta
import jp.pgw.lab78.androrm.common.meta.ViewMeta
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * ## 実行時 VIEW メタ情報生成
 * ### ViewDefinitionEntity を主コンストラクタ順に解析する
 * @author Masahiro Inoue
 * @since 2026-08-31
 */
class RuntimeViewMetaFactory {
    /**
     * ## VIEW メタ情報生成
     * @param entityClass VIEW 定義 Entity
     * @return 正規化済み VIEW メタ情報
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    fun <T : ViewDefinitionEntity> create(entityClass: KClass<out T>): ViewMeta {
        val view = requireNotNull(entityClass.findAnnotation<View>()) {
            "Class `${entityClass.qualifiedName}` does not have the `@View` annotation."
        }
        require(entityClass.findAnnotation<Table>() == null) {
            "Class `${entityClass.qualifiedName}` cannot have both `@Table` and `@View`."
        }
        val viewName = view.name.takeIf { it.isNotBlank() }
            ?: entityClass.simpleNameToSnakeCase()
        val constructor = entityClass.primaryConstructor
            ?: error(AE00007.format(entityClass.qualifiedName))
        val propertiesByName = entityClass.memberProperties.associateBy { it.name }
        val properties = constructor.parameters.map { parameter ->
            val property = propertiesByName[parameter.name]
                ?: error(AE00008.format(parameter.name, entityClass.qualifiedName))
            require(property.findAnnotation<Function>() == null) {
                "VIEW definition property `${property.name}` cannot use @Function."
            }
            val column = property.findColumnAnnotation()
            val columnName = column?.name?.takeIf { it.isNotBlank() }
                ?: property.simpleNameToSnakeCase()
            PropertyMeta(
                propertyName = property.name,
                columnName = columnName,
                aliasName = column?.alias?.takeIf { it.isNotBlank() } ?: columnName,
                isFunction = false,
                hideFromSelect = column?.hideFromSelect ?: false,
                hasColumnAnnotation = column != null,
                hasFunctionAnnotation = false,
            )
        }
        require(properties.none { it.hideFromSelect }) {
            "VIEW definition columns cannot set hideFromSelect=true."
        }
        val duplicateColumns = properties.groupBy { it.columnName.uppercase() }
            .filterValues { it.size > 1 }
            .keys
        require(duplicateColumns.isEmpty()) {
            "Duplicate VIEW column names: ${duplicateColumns.joinToString()}"
        }
        return ViewMeta(
            defineEntityQualifiedName = entityClass.qualifiedName.orEmpty(),
            entityName = entityClass.simpleName.orEmpty(),
            viewName = viewName,
            viewAlias = view.alias.takeIf { it.isNotBlank() } ?: viewName,
            properties = properties,
        )
    }
}
