package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.Constants.IndexType
import jp.pgw.lab78.androrm.common.MessageConstants
import jp.pgw.lab78.androrm.common.MessageConstants.AE00008
import jp.pgw.lab78.androrm.common.MessageConstants.AE00019
import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumn
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableName
import jp.pgw.lab78.androrm.common.database.annotation.Index
import jp.pgw.lab78.androrm.common.database.annotation.PrimaryKey
import jp.pgw.lab78.androrm.common.database.annotation.Unique
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.database.interfaces.QueryBuilderLike
import jp.pgw.lab78.androrm.database.utility.EntityManager.getConstructorOrderedProperties
import jp.pgw.lab78.androrm.database.utility.EntityManager.mapKotlinTypeToSqlType
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberProperties

/**
 * ## Create 文生成クラス
 * ### TableDefinitionEntity を基に CREATE TABLE 文を生成する
 *
 * @param entityClass テーブル定義 Entity クラス
 * @param tableNameOverride 生成対象テーブル名を上書きする場合に指定する
 * @author Masahiro Inoue
 * @since 2025-08-08
 */
class Create<T : TableDefinitionEntity>(
    private val entityClass: KClass<out T>,
    private val tableNameOverride: String? = null,
) : QueryBuilderLike<Any?> {

    /** テーブル名 */
    private val tableName: String =
        tableNameOverride ?: entityClass.getTableName()

    /** カラム定義対象プロパティ */
    private val columnProperties: List<KProperty1<out T, *>> =
        entityClass.getConstructorOrderedProperties()

    /** インデックス名格納領域 */
    private val usedIndexNames = mutableSetOf<String>()

    /**
     * ## CREATE 文文字列生成関数
     * ### テーブルを作成するクエリを生成する
     * @return CREATE TABLE 文
     * @author Masahiro Inoue
     * @since 2025-08-08
     */
    override fun build(): String {
        // カラム定義の取得
        val columnDefinitions = columnProperties.map { property ->
            buildColumnDefinition(property)
        }
        // プライマリィキー定義の取得
        val primaryKeyDefinition = buildPrimaryKeyDefinition()
        // カラム・プライマリキー定義のリストを生成
        val definitions = buildList {
            addAll(columnDefinitions)
            if (primaryKeyDefinition.isNotBlank()) {
                add(primaryKeyDefinition)
            }
        }
        return "create table $tableName (${definitions.joinToString(", ")})"
    }

    /**
     * ## INDEX 作成クエリ生成
     * ### @Index / @Unique から CREATE INDEX 文を生成する
     * @return CREATE INDEX / CREATE UNIQUE INDEX 文のリスト
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    fun buildIndexQueries(): List<String> {
        // @Index より create index 文を生成する
        val indexQueries = entityClass.findAnnotations<Index>()
            .mapIndexed { index, annotation ->
                // @Index の properties が空配列なら例外
                require(annotation.properties.isNotEmpty()) { AE00019 }
                // インデックス名の取得
                val indexName = annotation.name.takeIf { it.isNotBlank() }
                // インデックス名の生成
                    ?: buildDefaultIndexName(
                        prefix = "IDX",
                        serialNumber = index + 1,
                        properties = annotation.properties,
                    )
                // インデックス名の登録
                registerIndexName(indexName)
                // create index 文の生成
                buildIndexQuery(
                    indexName = indexName,
                    properties = annotation.properties,
                    indexType = IndexType.INDEX,
                )
            }
        // @Unique より create index 文を生成する
        val uniqueIndexQueries = entityClass.findAnnotations<Unique>()
            .mapIndexed { index, annotation ->
                // @Index の properties が空配列なら例外
                require(annotation.properties.isNotEmpty()) { AE00019 }
                // インデックス名の取得
                val indexName = annotation.name.takeIf { it.isNotBlank() }
                // インデックス名の生成
                    ?: buildDefaultIndexName(
                        prefix = "UQ",
                        serialNumber = index + 1,
                        properties = annotation.properties,
                    )
                // インデックス名の登録
                registerIndexName(indexName)
                // create index 文の生成
                buildIndexQuery(
                    indexName = indexName,
                    properties = annotation.properties,
                )
            }
        // 二つの create index 文リストを一つに統合
        return indexQueries + uniqueIndexQueries
    }

    /**
     * ## INDEX 作成クエリ生成
     * ### 指定された情報から CREATE INDEX 文を生成する
     *
     * @param indexName INDEX 名
     * @param properties INDEX 対象プロパティ名
     * @param indexType インデクスタイプを指定
     * @return CREATE INDEX 文
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    private fun buildIndexQuery(
        indexName: String,
        properties: Array<String>,
        indexType: IndexType = IndexType.UNIQUE,
    ): String {
        val columnNames = properties.map { propertyName -> resolveColumnName(propertyName) }
        return "create ${indexType.query}index if not exists $indexName " +
                "on $tableName (${columnNames.joinToString(", ")})"
    }

    /**
     * ## デフォルト INDEX 名生成
     * ### annotation の name が未指定相当の場合に INDEX 名を生成する
     *
     * @param prefix INDEX 名 prefix
     * @param serialNumber 連番
     * @param properties INDEX 対象プロパティ名
     * @return 生成した INDEX 名
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    private fun buildDefaultIndexName(
        prefix: String,
        serialNumber: Int,
        properties: Array<String>,
    ): String {
        val columnPart = properties.joinToString("_") { propertyName ->
            resolveColumnName(propertyName)
        }
        return listOf(prefix, tableName, serialNumber, columnPart).joinToString("_")
    }

    /**
     * ## プロパティ名から DB カラム名を解決
     * ### @Index / @Unique に指定された Kotlin プロパティ名から DB カラム名を取得する
     *
     * @param propertyName Kotlin プロパティ名
     * @return DB カラム名
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    private fun resolveColumnName(
        propertyName: String,
    ): String {
        val property = entityClass.memberProperties
            .firstOrNull { property ->
                property.name == propertyName
            } ?: error(AE00008.format(propertyName, entityClass.qualifiedName))
        return property.getColumn()
    }

    /**
     * ## カラム定義生成
     * ### 1カラム分の定義を生成する
     * @param property カラム定義対象プロパティ
     * @return カラム定義
     * @author Masahiro Inoue
     * @since 2025-08-08
     */
    private fun buildColumnDefinition(
        property: KProperty1<out T, *>,
    ): String {
        val columnName = property.getColumn()
        val sqlType = mapKotlinTypeToSqlType(property.returnType)
        return "$columnName $sqlType"
    }

    /**
     * ## PRIMARY KEY 定義生成
     * ### @PrimaryKey が付与されたカラムから PRIMARY KEY 句を生成する
     * @return PRIMARY KEY 句。対象がない場合は空文字
     * @author Masahiro Inoue
     * @since 2026-05-30
     */
    private fun buildPrimaryKeyDefinition(): String {
        val primaryKeyColumns = columnProperties
            .filter { property ->
                property.findAnnotation<PrimaryKey>() != null
            }
            .map { property ->
                property.getColumn()
            }
        return if (primaryKeyColumns.isEmpty()) {
            EMPTY_STRING
        } else {
            primaryKeyColumns.joinToString(
                separator = ", ",
                prefix = "primary key (",
                postfix = ")",
            )
        }
    }

    /**
     * ## インデクス名の登録
     * ### アノテーションから取得したインデクスを登録する
     *
     */
    private fun registerIndexName(
        indexName: String,
    ) {
        val normalizedIndexName = indexName.uppercase()
        require(usedIndexNames.add(normalizedIndexName)) {
            MessageConstants.AE00020.format(indexName)
        }
    }
}