package jp.pgw.lab78.androrm.database.utility

import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumn
import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumnAlias
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableAlias
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableAnnotation
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableName
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * ## ユーティリティ関数オブジェクトクラス
 * ### データベースヘルパーで使用する関数群
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
object EntityManager {
    /**
     * ## テーブル定義
     * ### テーブル名と関連エンティティクラスを紐づける
     * @param tableName テーブル名
     * @param aliases 関連エンティティクラス
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    data class TableDefinition<T : Entity>(
        val tableName: String,
        val aliases: MutableMap<String,EntityDefinition<T>>
    )

    /**
     * ## テーブルエイリアス-エンティティクラス紐づけ定義
     * ### テーブルエイリアスとエンティティクラスを紐づける
     * @param alias テーブルエイリアス
     * @param entityClass エンティティクラス
     * @param columns カラム定義マップ
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    data class EntityDefinition<T : Entity>(
        val alias: String,
        val entityClass: KClass<out T>,
        val columns: MutableMap<String, KProperty1<out T, *>>
    )

    /** テーブルメタデータ管理 */
    private val tableMetadata = mutableMapOf<String, TableDefinition<Entity>>()

    /** プレースホルダー名正規表現 */
    private val PLACE_HOLDER_REGEX = Regex(""":(\w+)""")

    /**
     * ## クラス取得
     * ### KProperty1<T, *> からクラスを取得する
     * @receiver `@Column` アノテーションが付与されている [Entity] （上限境界）型の [KProperty1] インスタンス。
     * @param T [Entity] インターフェースを実装するクラスの型。
     * @return 取得したクラスの型
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Entity> KProperty1<T, *>.extractClassFromProperty(): KClass<T> =
        // this.parameters[0] はレシーバー（=宣言元）に対応する
        this.parameters.firstOrNull()?.type?.classifier as KClass<T>

    /**
     * ## テーブル名生成
     * ### エンティティクラスからテーブル名を取得する
     * @receiver `@Table` アノテーションが付与されている [Entity] （上限境界）型の [KClass] インスタンス。
     * @param T [Entity] インターフェースを実装するクラスの型
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <T : Entity> KClass<out T>.createTableName(): String {
        val tableAnnotation = this.getTableAnnotation()
        return tableMetadata.getOrPut(tableAnnotation.name.ifEmpty { this.getTableName() }) {
            // テーブル名の生成
            val tableName = tableAnnotation.name
                .ifBlank { this.getTableName() }
            // エイリアスの生成
            val alias = tableAnnotation.alias
                .ifBlank { tableName }
            val entityDefinition = EntityDefinition<Entity>(alias,this, mutableMapOf())
            TableDefinition(tableName, mutableMapOf(alias to entityDefinition))
        }.tableName
    }

    /**
     * ## 定義順カラム情報取得（カラム名とカラム型）
     * ### カラム定義をコンストラクタ順に取得
     * @receiver `@Table` アノテーションが付与されている [Entity] （上限境界）型の [KClass] インスタンス。
     * @param T [Entity] インターフェースを実装するクラスの型
     * @return 定義順に並んだカラム名とカラム型のリスト
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <T : TableDefinitionEntity> KClass<out T>.getColumnDefinitions(): List<Pair<String, String>> {
        val tableName = this.createTableName()
        val alias = this.getTableAlias()
        // プライマリコンストラクタがないときはエラー
        val constructor = this.primaryConstructor
            ?: error("No primary constructor for ${this.simpleName}")
        // コンストラクタパラメータ順でプロパティをマッピング
        return constructor.parameters.map { param ->
            // メタデータから抽出準備
            val property = this.memberProperties.first { it.name == param.name }
            // @Column の name/alias を取得
            val columnAlias = "${alias}_${property.getColumnAlias().ifBlank{property.getColumn()}}"
            // tableMetadata から、カラム情報抽出
            val columnName = this.extractColumnMetadata(tableName,alias,columnAlias,property)
            // SQL 型マッピング（既存関数を呼び出し）
            val sqlType = mapKotlinTypeToSqlType(property.returnType)
            // 結果をペアで返却
            columnName to sqlType
        }
    }

    /**
     * ## 定義順カラム情報取得
     * ### カラム定義をコンストラクタ順に取得
     * @receiver `@Table` アノテーションが付与されている [Entity] （上限境界）型の [KClass] インスタンス。
     * @param T [Entity] インターフェースを実装するクラスの型
     * @return 定義順に並んだカラム名のリスト
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <T : Entity> KClass<out T>.getColumns(): List<String> {
        val tableName = this.createTableName()
        val alias = this.getTableAlias()
        // プライマリコンストラクタがないときはエラー
        val constructor = this.primaryConstructor
            ?: error("No primary constructor for ${this.simpleName}")
        // コンストラクタパラメータ順でプロパティをマッピング
        return constructor.parameters.map { param ->
            // メタデータから抽出準備
            val property = this.memberProperties.first { it.name == param.name }
            // @Column の name/alias を取得
            val columnAlias = "${alias}_${property.getColumnAlias().ifBlank{property.getColumn()}}"
            // tableMetadata から、カラム名抽出
            this.extractColumnMetadata(tableName,alias,columnAlias,property)
        }
    }

    /**
     * ## テーブル内カラム定義情報抽出
     * ### テーブル内に定義されているカラム情報を抽出する
     * @receiver `@Table` アノテーションが付与されている [Entity] （上限境界）型の [KClass] インスタンス。
     * @param T [Entity] インターフェースを実装するクラスの型
     * @param tableName テーブル名
     * @param alias テーブルエイリアス
     * @param columnAlias カラムエイリアス
     * @param property プロパティ
     * @return プロパティ
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private fun <T : Entity> KClass<out T>.extractColumnMetadata(
        tableName: String,
        alias: String,
        columnAlias: String,
        property: KProperty1<out T, *>
    ): String {
        val definedProperty: KProperty1<out Entity, *>? = tableMetadata.getOrPut(tableName) {
            // このブロックは、createTableName の保険。但し無かった場合、columnAlias to property も登録
            val entityDefinition = EntityDefinition<Entity>(alias,
                                                            this,
                                                            mutableMapOf(columnAlias to property)
            )
            TableDefinition(tableName, mutableMapOf(alias to entityDefinition))
        }.aliases.getOrPut(alias) {
            // このブロックは、createTableName の保険。但し無かった場合、columnAlias to property も登録
            EntityDefinition(alias, this, mutableMapOf(columnAlias to property))
        }.columns.put(columnAlias, property)
        return definedProperty?.getColumn()?:property.getColumn()
    }


    /**
     * ## エイリアス取得
     * ### エンティティクラスからエイリアスを取得する
     * @receiver `@Table` アノテーションが付与されている [Entity] （上限境界）型の [KClass] インスタンス。
     * @return 取得したエイリアス
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <T : Entity> KClass<T>.getAlias(): String =  this.getTableAlias()

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
    fun bindPlaceholders(
        query: String,
        valuesMap: List<Map<String, Any>>
    ): Pair<String, List<Array<String>>> {
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
            return fieldToColumnMap[valueForJudgment]
                ?: throw IllegalArgumentException("Unsupported type: $valueForJudgment")
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
