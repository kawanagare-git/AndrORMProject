package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.annotation.Column
import jp.pgw.lab78.androrm.annotation.Table
import jp.pgw.lab78.androrm.database.interfaces.Entity
import jp.pgw.lab78.androrm.database.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.utility.Functions.mapKotlinTypeToSqlType
import jp.pgw.lab78.androrm.utility.Functions.toSnakeCase
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

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
     * ### エンティティクラスからテーブル名を取得する
     * @param entityClass エンティティクラスを指定
     * @return 取得したテーブル名
     */
    fun <T : Entity> getTableName(entityClass: KClass<T>): String =
        entityDefinitionMap.getOrPut(entityClass) {
            val tableAnnotation = entityClass.findAnnotation<Table>()
            val computedTableName = tableAnnotation?.name
                                                    ?.ifBlank { entityClass.simpleNameToSnakeCase() }
                                                    ?.plus(" ${tableAnnotation.alias}")
                                                    ?: (entityClass.simpleNameToSnakeCase())
            EntityDefinitionManager(computedTableName, mutableMapOf())
        }.tableName//.let { splitTableNameAndAlias(it).first } // ← パースして「テーブル名」だけ返す

    /**
     * ## カラム名取得
     * ### エンティティクラスからカラム名のリストを取得する
     * @param entityClass エンティティクラスを指定
     * @return 取得したカラム名のリスト
     */
    fun <T : Entity> getColumnNames(entityClass: KClass<T>): List<Pair<String, String>> {
        val alias = getAlias(entityClass)

        return entityClass.memberProperties.map { prop ->
            // カラム名のベース（@Column.name or プロパティ名）
            val baseName = prop.findAnnotation<Column>()
                ?.name
                ?.takeIf { it.isNotBlank() }
                ?: prop.name.toSnakeCase()

            val columnName = "$alias.$baseName"

            // カラム情報を保存（entityDefinitionMap に登録されていることが前提）
            entityDefinitionMap[entityClass]?.columnInfo?.also {
                it[prop] = columnName
            } ?: error("Initialization required for ${entityClass.simpleName}.")

            val sqlType = mapKotlinTypeToSqlType(prop.returnType)
            columnName to sqlType
        }
    }

    /**
     * ## 定義順カラム取得
     * ### カラム定義をコンストラクタ順に取得
     * @param [T] Entity インターフェイスの実装型
     * @param entityClass 対象のエンティティクラス
     * @return 定義順に並んだカラム名のリスト
     */
    fun <T : Entity> getColumnDefinitions(entityClass: KClass<T>): List<Pair<String, String>> {
        val alias = getAlias(entityClass)
        // 1) プライマリコンストラクタがないときはエラー
        val constructor = entityClass.primaryConstructor
            ?: error("No primary constructor for ${entityClass.simpleName}")

        // 2) コンストラクタパラメータ順でプロパティをマッピング
        return constructor.parameters.map { param ->
            // プロパティ名と対応づけ
            val prop = entityClass.memberProperties
                .first { it.name == param.name }
            // @Column の name/alias を取得
            val colAnno = prop.findAnnotation<Column>()
            val baseName = colAnno?.name
                .takeIf { !it.isNullOrBlank() }
                ?: prop.name.toSnakeCase()
            val columnName = "$alias$baseName"
            // カラム情報を保存（entityDefinitionMap に登録されていることが前提）
            entityDefinitionMap[entityClass]?.columnInfo?.also {
                it[prop] = columnName
            } ?: error("Initialization required for ${entityClass.simpleName}.")
            // SQL 型マッピング（既存関数を呼び出し）
            val sqlType = mapKotlinTypeToSqlType(prop.returnType)
            // 結果をペアで返却
            columnName to sqlType
        }
    }

    /**
     * ## エイリアス取得
     * ### エンティティクラスからエイリアスを取得する
     * @param entityClass エンティティクラスを指定
     * @return 取得したエイリアス
     */
    fun <T : Entity> getAlias(entityClass: KClass<T>): String = (
        entityDefinitionMap[entityClass]
            ?.tableName
            // tableName を２分割する
            ?.split(" ", limit = 2)
            // 分割した２つ目を取得
            ?.getOrNull(1)
            // 取得した内容が空欄か？
            ?.takeIf { it.isNotBlank() }
            // 空欄の場合、クラス名をスネークケースに変換
            ?: entityClass.simpleNameToSnakeCase()) + "."

    /**
     * ## テーブル名とエイリアスを分離
     * @param tableName "users u" のような形式
     * @return Pair(テーブル名, エイリアス). エイリアスがない場合は ""。
     */
    fun splitTableNameAndAlias(tableName: String): Pair<String, String> {
        val parts = tableName.split(" ", limit = 2)
        return parts[0] to (parts.getOrNull(1) ?: "")
    }

    /**
     * ## クラス名をスネークケースに変換
     * @return スネークケースに変換されたクラス名（nullなら例外）
     */
    public fun KClass<*>.simpleNameToSnakeCase(): String =
        this.simpleName?.toSnakeCase()
            ?: error("Could not determine class name for ${this.qualifiedName}")

    /**
     * ## プロパティ名をスネークケースに変換
     * @return スネークケースに変換されたクラス名（nullなら例外）
     */
    fun KProperty1<*, *>.simpleNameToSnakeCase(): String =
        this.name.toSnakeCase()

    /**
     * ## 値の文字列化
     * ### 指定された値を文字列化する
     * @param value 変換元の値
     * @return 文字列化された値
     */
    fun formatValue(value: Any): String = when (value) {
        is String -> "'$value'"
        is LocalDate -> "'$value'"
        is LocalDateTime -> "'$value'"
        is LocalTime -> "'$value'"
        else -> value.toString()
    }

    /**
     * ## クラス取得
     * ### KProperty1<T, *> からクラス名を取得する
     * @param property KProperty1<T, *> プロパティ
     * @return 取得したクラスの型
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Entity> extractClassFromProperty(property: KProperty1<T, *>): KClass<T>? =
        // property.parameters[0] はレシーバー（=宣言元）に対応する
        property.parameters.firstOrNull()?.type?.classifier as? KClass<T>

}
