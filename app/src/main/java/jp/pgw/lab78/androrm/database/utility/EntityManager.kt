package jp.pgw.lab78.androrm.database.utility

import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumn
import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumnAlias
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableAlias
import jp.pgw.lab78.androrm.common.database.SupportFunction.simpleNameToSnakeCase
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * ## ユーティリティ関数オブジェクトクラス
 * ### データベースヘルパーで使用する関数群
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
object Functions {
    /**
     * ## エンティティ定義管理クラス
     * ### エンティティの定義（構造）を管理
     * @param tableName テーブル名
     * @param columnInfo テーブルに定義してあるカラムの情報 キー:entity クラスのフィールド value:カラム名
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private data class EntityDefinitionManager(
        var tableName: String,
        var columnInfo: MutableMap<KProperty<*>, String>,
        var tableAlias: String,
        var columnAliasMap: MutableMap<KProperty<*>, String>
    )

    /** プレースホルダー名正規表現 */
    private val PLACE_HOLDER_REGEX = Regex(""":(\w+)""")

    /** エンティティクラス定義管理マップ */
    private val entityDefinitionMap = mutableMapOf<KClass<*>, EntityDefinitionManager>()

    /**
     * ## CREATE 文文字列生成関数
     * ### テーブルを作成するクエリを生成する
     * @param entityClass TableDefinitionEntity クラスのインスタンスを指定
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <T : TableDefinitionEntity> generateTableCreationQuery(entityClass: KClass<out T>): String {
        /** テーブル名の生成 */
        val tableName = getTableName(entityClass)
        /** カラム定義の生成 */
        val columnList = getColumnDefinitions(entityClass)
        return "CREATE TABLE $tableName (${columnList.joinToString(", ") {
                                                (columnName, sqlType) -> "$columnName $sqlType"
                                            }})"
    }

    /**
     * ## クラス取得
     * ### KProperty1<T, *> からクラスを取得する
     * @param property KProperty1<T, *> プロパティ
     * @return 取得したクラスの型
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Entity> extractClassFromProperty(property: KProperty1<T, *>): KClass<T>? =
        // property.parameters[0] はレシーバー（=宣言元）に対応する
        property.parameters.firstOrNull()?.type?.classifier as? KClass<T>

    /**
     * ## テーブル名取得
     * ### エンティティクラスからテーブル名を取得する
     * @param entityClass エンティティクラスを指定
     * @return 取得したテーブル名
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <T : Entity> getTableName(entityClass: KClass<T>): String =
        entityDefinitionMap.getOrPut(entityClass) {
            val tableAnnotation = entityClass.findAnnotation<Table>()
            // テーブル名の生成
            val baseName = tableAnnotation?.name
                ?.ifBlank { entityClass.simpleNameToSnakeCase() }
                ?: entityClass.simpleNameToSnakeCase()
            // エイリアスの生成
            val alias = tableAnnotation?.alias
                ?.takeIf { it.isNotBlank() }
                ?: baseName

            EntityDefinitionManager(
                tableName = "$baseName $alias",
                columnInfo = mutableMapOf(),
                tableAlias = alias,
                columnAliasMap = mutableMapOf()
            )
        }.tableName

    /**
     * ## 定義順カラム情報取得
     * ### カラム定義をコンストラクタ順に取得
     * @param [T] Entity インターフェイスの実装型
     * @param entityClass 対象のエンティティクラス
     * @return 定義順に並んだカラム名のリスト
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <T : Entity> getColumnDefinitions(entityClass: KClass<T>): List<Pair<String, String>> {
        val alias = getAlias(entityClass)
        // プライマリコンストラクタがないときはエラー
        val constructor = entityClass.primaryConstructor
            ?: error("No primary constructor for ${entityClass.simpleName}")
        // コンストラクタパラメータ順でプロパティをマッピング
        return constructor.parameters.map { param ->
            // プロパティ名と対応づけ
            val prop = entityClass.memberProperties
                .first { it.name == param.name }
            // @Column の name/alias を取得
            val colAnno = prop.findAnnotation<Column>()
            val baseName = colAnno?.name
                .takeIf { !it.isNullOrBlank() }
                ?: prop.getColumn()
            val columnAlias = colAnno?.alias
                .takeIf { !it.isNullOrBlank() }
                ?.let { prop.getColumnAlias() }
                ?: baseName
            val columnName = "${alias}.$baseName as ${alias}_$columnAlias"
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
     * @author Masahiro Inoue
     * @since 2025-08-01
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
                ?: entityClass.getTableAlias())

    /**
     * ## テーブル名とエイリアスを分離
     * @param tableName "users u" のような形式
     * @return Pair(テーブル名, エイリアス). エイリアスがない場合は ""。
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun splitTableNameAndAlias(tableName: String): Pair<String, String> {
        val parts = tableName.split(" ", limit = 2)
        return parts[0] to (parts.getOrNull(1) ?: "")
    }

    /**
     * ## エンティティクラス値マップ生成
     * ### 複数のエンティティインスタンスを受け取り
     * ### エンティティプロパティ名と値のマップをリストとして生成
     * @param entities 複数のエンティティインスタンス
     * @return エンティティプロパティ名と値のマップのリスト
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    inline fun <reified T: Entity> getValueFromEntity(vararg entities: T) : List<Map<String, Any>> {
        val entityList = listOf(*entities)
        val result : MutableList<Map<String, Any>> = mutableListOf()
        entityList.forEach { entity ->
            val map: MutableMap<String, Any> = mutableMapOf()
            T::class.memberProperties.forEach { property ->
                val value = property.get(entity)
                map[property.getColumn()] = value as Any
            }
            result.add(map)
        }
        return result
    }

    /**
     * ## プレースホルダーバインド
     * ### クエリに設定されたプレースホルダー名のバインド値を取得
     * ### 更にクエリのプレースホルダー名を「?」に変更する
     * @param query クエリ文字列
     * @param valuesMap バインド値のマップ
     * @return クエリのプレースホルダー名を「?」に変更した文字列 と バインド値のリスト（Pair）
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun bindPlaceholders(query: String, valuesMap: List<Map<String, Any>>): Pair<String, List<Array<String>>> {
        val argNames = mutableListOf<String>()
        val queryWithPlaceholders = PLACE_HOLDER_REGEX.replace(query) {
            argNames += it.groupValues[1]
            "?"
        }
        val args = argNames.map { key ->
            valuesMap.map {
                it[key]?.toString() ?: error("Missing bind value for :$it")
            }
            .toTypedArray()
        }
        return queryWithPlaceholders to args
    }

    fun convertToEntity(columnNames: Array<String>, kClass: KClass<*>) {

    }

    /** 型変換用マップ */
    private val fieldToColumnMap = mapOf(
        Int::class to "INTEGER"
        ,Long::class to "INTEGER"
        ,Float::class to "REAL"
        ,Double::class to "REAL"
        ,Boolean::class to "INTEGER"
        ,String::class to "TEXT"
        ,LocalDate::class to "DATETIME"
        ,LocalTime::class to "DATETIME"
        ,LocalDateTime::class to "DATETIME"
    )

    /**
     * ## Kotlin 型 SQL 型変換関数
     * ### クラスのフィールド型をデータベースのカラム型に変換
     * @param field 変換対象のフィールドを指定
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun mapKotlinTypeToSqlType(field : Any): String {
        val valueForJudgment = when (field) {
            // arg が既に KType の場合、KClass<*> にキャスト
            is KType -> field.classifier as? KClass<*>
            // それ以外は、KClass<*> を取得
            else -> field::class
        }
        if (valueForJudgment == null) {
            throw IllegalArgumentException("Unsupported type: $field")
        }else{
            return fieldToColumnMap[valueForJudgment] ?: throw IllegalArgumentException("Unsupported type: $valueForJudgment")
        }
    }

    /**
     * ## 値の文字列化
     * ### 指定された値を文字列化する
     * @param value 変換元の値
     * @return 文字列化された値
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun formatValue(value: Any): String = when (value) {
        is String -> "'$value'"
        is LocalDate -> "'$value'"
        is LocalDateTime -> "'$value'"
        is LocalTime -> "'$value'"
        else -> value.toString()
    }
}