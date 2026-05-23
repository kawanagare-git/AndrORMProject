package jp.pgw.lab78.androrm.database.meta

import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.MessageConstants.AE00007
import jp.pgw.lab78.androrm.common.MessageConstants.AE00008
import jp.pgw.lab78.androrm.common.database.SupportFunction.simpleNameToSnakeCase
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Function
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.meta.EntityMeta
import jp.pgw.lab78.androrm.common.meta.PropertyMeta
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * ## 実行時 Entity メタ情報生成クラス
 * ### SelectEntity 実装クラスを実行時に解析し
 * ### EntityMeta / PropertyMeta を生成する
 *
 * @author Masahiro Inoue
 * @since 2026-04-27
 */
class RuntimeEntityMetaFactory {

    /**
     * ## EntityMeta 生成
     * ### Select 対象 Entity クラスから実行時メタ情報を生成する
     *
     * @param entityClass Select 対象 Entity クラス
     * @return 生成された EntityMeta
     */
    fun <T : SelectEntity> create(entityClass: KClass<out T>): EntityMeta {
        // テーブル情報の解決
        val tableAnnotation = entityClass.findAnnotation<Table>()
        // テーブル名の解決
        val tableName = tableAnnotation?.name
            ?.takeIf { it.isNotBlank() }
            ?: entityClass.simpleNameToSnakeCase()
        // テーブルエイリアスの解決
        val tableAlias = tableAnnotation?.alias
            ?.takeIf { it.isNotBlank() }
            ?: tableName
        // クラスの主コンストラクタを取得。存在しない場合はエラー
        val constructor = entityClass.primaryConstructor
            ?: error(AE00007.format(entityClass.qualifiedName))
        // クラスのプロパティを名前でマップ化
        val propertiesByName = entityClass.memberProperties.associateBy { it.name }
        // コンストラクタのパラメータに対応するプロパティを突き合わせて PropertyMeta を生成。存在しない場合はエラー
        val properties = constructor.parameters.map { param ->
            val property = propertiesByName[param.name]
                ?: error(AE00008.format(param.name, entityClass.qualifiedName))
            (property as KProperty1<out SelectEntity, *>).toPropertyMeta()
        }
        // EntityMeta を生成して返す
        return EntityMeta(
            defineEntityQualifiedName = entityClass.qualifiedName.orEmpty(),
            entityName = entityClass.simpleName.orEmpty(),
            tableName = tableName,
            tableAlias = tableAlias,
            properties = properties
        )
    }

    /**
     * ## PropertyMeta 生成
     * ### 1 プロパティ分の正規化済みメタ情報を生成する
     *
     * ### ルール:
     * - @Function がある → 関数列
     * - @Function がなく @Column がある → 通常カラム
     * - 両方ない → 暗黙 @Column
     */
    private fun KProperty1<out SelectEntity, *>.toPropertyMeta(): PropertyMeta {
        val columnAnnotation = findAnnotation<Column>()
        val functionAnnotation = findAnnotation<Function>()

        val hasColumnAnnotation = columnAnnotation != null
        val hasFunctionAnnotation = functionAnnotation != null
        val propertySnakeCase = simpleNameToSnakeCase()

        return if (hasFunctionAnnotation) {
            val aliasName =
                functionAnnotation?.alias?.ifBlank { propertySnakeCase } ?: propertySnakeCase

            PropertyMeta(
                propertyName = name,
                // 関数列では実体カラム名を持たないので alias を便宜上設定
                columnName = aliasName,
                aliasName = aliasName,
                isFunction = true,
                hideFromSelect = functionAnnotation?.hideFromSelect ?: false,
                hasColumnAnnotation = hasColumnAnnotation,
                hasFunctionAnnotation = true,
                functionType = functionAnnotation?.columnFunction,
                functionArgs = functionAnnotation?.args?.toList() ?: emptyList(),
                rawFunction = functionAnnotation?.raw ?: EMPTY_STRING
            )
        } else {
            val columnName = columnAnnotation?.name
                ?.takeIf { it.isNotBlank() }
                ?: propertySnakeCase

            val aliasName = columnAnnotation?.alias
                ?.takeIf { it.isNotBlank() }
                ?: columnName

            PropertyMeta(
                propertyName = name,
                columnName = columnName,
                aliasName = aliasName,
                isFunction = false,
                hideFromSelect = columnAnnotation?.hideFromSelect ?: false,
                hasColumnAnnotation = hasColumnAnnotation,
                hasFunctionAnnotation = false
            )
        }
    }
}