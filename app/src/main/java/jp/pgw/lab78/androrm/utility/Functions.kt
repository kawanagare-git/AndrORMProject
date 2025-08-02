package jp.pgw.lab78.androrm.utility

import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumn
import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumnAlias
import jp.pgw.lab78.androrm.common.database.SupportFunction.simpleNameToSnakeCase
import jp.pgw.lab78.androrm.common.database.SupportFunction.toSnakeCase
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
 * ## UtilityFunction オブジェクトクラス
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
object Functions {

    /**
     * ## エンティティ定義管理クラス
     * ### エンティティの定義（構造）を管理
     * @param tableName テーブル名
     * @param columnInfo テーブルに定義してあるカラムの情報 キー:entity クラスの フィールド value:カラム名
     * @author Masahiro Inoue
     * @since 2025-08-01
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
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <T : TableDefinitionEntity> generateTableCreationQuery(entityClass: KClass<T>): String {
        /** テーブル名の生成 */
        val tableName = getTableName(entityClass)

        /** カラム定義の生成 */
        val columnList = getColumnNames(entityClass)
        return "CREATE TABLE $tableName (${columnList.joinToString(", ") { (columnName, sqlType) -> "$columnName $sqlType" }})"
    }

    /**
     * ## クラス取得
     * ### KProperty1<T, *> からクラス名を取得する
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
     * @author Masahiro Inoue
     * @since 2025-08-01
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
     * @author Masahiro Inoue
     * @since 2025-08-01
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
                ?: prop.getColumn()
            val columnAlias = colAnno?.alias
                .takeIf { !it.isNullOrBlank() }
                ?.let { "as ${prop.getColumnAlias()}" }
                ?: ""
            val columnName = "$alias$baseName $columnAlias"
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
                ?: entityClass.simpleNameToSnakeCase()) + "."

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

    /** ## 型変換用マップ */
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
     * ## mapKotlinTypeToSqlType 関数
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