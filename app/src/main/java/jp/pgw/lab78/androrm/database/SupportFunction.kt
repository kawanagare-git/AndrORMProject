package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.annotation.Column
import jp.pgw.lab78.androrm.annotation.Table
import jp.pgw.lab78.androrm.database.interfaces.Entity
import jp.pgw.lab78.androrm.database.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.utility.Functions.mapKotlinTypeToSqlType
import jp.pgw.lab78.androrm.utility.Functions.toSnakeCase
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/**
 * SupportFunction オブジェクトクラス
 * database パッケージのクラスでサポートする関数群
 */
object SupportFunction {
    /**
     * ## エンティティ定義管理クラス
     * ### エンティティの定義（構造）を管理
     * @param tableName テーブル名
     * @param columnInfo テーブルに定義してあるカラムの情報　キー:entity クラスの フィールド　value:カラム名
     */
    data class EntityDefinitionManager(
        var tableName: String, var columnInfo: MutableMap<KProperty<*>, String>
    )

    @JvmStatic
    val entityDefinitionMap = mutableMapOf<KClass<*>, EntityDefinitionManager>()

    /**
     * ## generateTableCreationQuery 関数
     * ### テーブルを作成するクエリを生成する
     * @param entityClass TableDefinitionEntity クラスのインスタンスを指定
     */
    fun <T : TableDefinitionEntity> generateTableCreationQuery(entityClass: KClass<T>): String {
        /** テーブル名の生成 */
        val tableName = getTableName(entityClass)

        /** カラム定義の生成 */
        val columnList = getColumnNames(entityClass)
        return "CREATE TABLE $tableName (${columnList.joinToString(", ") { (columnName, sqlType) -> "$columnName $sqlType" }})"
    }

    /**
     * ## テーブル名取得
     * ### エンティティクラスからテーブル名取得する
     * @param entityClass エンティティクラスを指定
     * @return 取得したテーブル名
     */
    fun <T : Entity> getTableName(entityClass: KClass<T>): String
    // すでにマップに登録されているか確認し、なければ新たに登録する
            = entityDefinitionMap.getOrPut(entityClass) {
        // entityDefinitionMap から get できなかった場合、テーブル名を生成
        val tableAnnotation = entityClass.findAnnotation<Table>()
        val computedTableName =
            // @Table の有無
            tableAnnotation
                // @Table 有 属性 name の有
                ?.name
                // 属性 name の無
                ?.ifBlank {
                    // 属性 name 無、クラス名をスネークケースに変換
                    entityClass.simpleName?.toSnakeCase()
                    // entityClass.simpleName は通常 null はない
                        ?: error("Could not determine class name (${entityClass.simpleName}).")
                }
                // テーブル名のエイリアスを付与
                ?.plus(" ${tableAnnotation.alias}")
            // @Table 無、クラス名をスネークケースに変換
                ?: (entityClass.simpleName?.toSnakeCase()
                // entityClass.simpleName は通常 null はない
                    ?: error("Could not determine class name (${entityClass.simpleName})."))
        EntityDefinitionManager(computedTableName, mutableMapOf())
    }.tableName

    /**
     * ## カラム名取得
     * ### エンティティクラスからカラム名のリストを取得する
     * @param entityClass エンティティクラスを指定
     * @return 取得したカラム名のリスト
     */
    fun <T : Entity> getColumnNames(entityClass: KClass<T>): List<Pair<String, String>>
    // すでにマップに登録されているか確認し、なければ新たに登録する
            = entityClass.memberProperties.map { prop ->
        val columnAnnotation = prop.findAnnotation<Column>()
        val columnName =
            // @Column 有無
            columnAnnotation
            // @Column 有 name 属性の有
            ?.name
            // name 属性の無
            ?.ifBlank {
                // フィールド名をスネークケースに変換
                prop.name.toSnakeCase()
            }
            // エイリアスの付与
            ?.plus(" ${getAlias(entityClass)
                // 空でない alias のみ通す
                .takeIf { it.isNotBlank() }
                // 通った alias にドットを付ける
                ?.let { "$it." }
                ?: ""
            }")
        // @Column 無、フィールド名をスネークケースに変換
            ?: prop.name.toSnakeCase()
        // entityDefinitionMap[entityClass]?.columnInfo は、必ず存在すること
        entityDefinitionMap[entityClass]?.columnInfo?.also {
            it[prop] = columnName
        } ?: error("Initialization required for ${entityClass.simpleName}.")
        val sqlType = mapKotlinTypeToSqlType(prop.returnType)
        columnName to sqlType
    }
    // 既存の EntityDefinitionManager, entityDefinitionMap, Table アノテーション等はそのまま

    /**
     * ## エンティティのエイリアス取得
     * @param entityClass エンティティクラス
     * @return テーブルエイリアス（Table.annotation.alias が空なら、クラス名をスネークケースに変換したもの）
     */
    fun <T : Entity> getAlias(entityClass: KClass<T>): String =
        // Map に登録されているか？
        entityDefinitionMap[entityClass]
            // テーブル名からエイリアスを取得
            ?.tableName
            ?.split(" ", limit = 2)
            ?.getOrNull(1)
            // テーブル名が未登録（事実上あり得ない）かエイリアスが未登録
            ?: ""
}
