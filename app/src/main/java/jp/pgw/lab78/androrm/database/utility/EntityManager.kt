package jp.pgw.lab78.androrm.database.utility

import android.database.Cursor
import android.database.sqlite.SQLiteStatement
import jp.pgw.lab78.androrm.common.MessageConstants.AE00007
import jp.pgw.lab78.androrm.common.MessageConstants.AE00008
import jp.pgw.lab78.androrm.common.MessageConstants.AE00009
import jp.pgw.lab78.androrm.common.MessageConstants.AE00027
import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumn
import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumnAlias
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableAlias
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableAnnotation
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableName
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import jp.pgw.lab78.androrm.common.logging.aop.TraceLog
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.interfaces.SqlExpression
import jp.pgw.lab78.androrm.database.meta.RuntimeEntityMetaFactory
import jp.pgw.lab78.androrm.database.reference.ColumnRef
import jp.pgw.lab78.androrm.database.reference.TableRef
import jp.pgw.lab78.shared.library.Utils.isNotNull
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
        val aliases: MutableMap<String, EntityDefinition<T>>
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
    private val _tableMetadata = mutableMapOf<String, TableDefinition<Entity>>()

    /** テーブルメタデータ参照 */
    internal val tableMetadata: Map<String, TableDefinition<Entity>>
        get() = _tableMetadata

    /** SELECT 結果カラムとプロパティの紐づけ */
    data class SelectColumnTarget(
        val propertyName: String,
        val resultColumnName: String,
    )

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
        return _tableMetadata.getOrPut(tableAnnotation.name.ifEmpty { this.getTableName() }) {
            // テーブル名の生成
            val tableName = tableAnnotation.name
                .ifBlank { this.getTableName() }
            // エイリアスの生成
            val alias = tableAnnotation.alias
                .ifBlank { tableName }
            val entityDefinition = EntityDefinition<Entity>(alias, this, mutableMapOf())
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
            ?: error(AE00007.format(this.simpleName))
        // コンストラクタパラメータ順でプロパティをマッピング
        return constructor.parameters.map { param ->
            // メタデータから抽出準備
            val property = this.memberProperties.first { it.name == param.name }
            // @Column の name/alias を取得
            val columnAlias =
                "${alias}_${property.getColumnAlias().ifBlank { property.getColumn() }}"
            // tableMetadata から、カラム情報抽出
            val columnName = this.extractColumnMetadata(tableName, alias, columnAlias, property)
            // SQL 型マッピング（既存関数を呼び出し）
            val sqlType = mapKotlinTypeToSqlType(property.returnType)
            // 結果をペアで返却
            columnName to sqlType
        }
    }

    /**
     * ## DML 用名称取得
     * ### DML として使用するエンティティクラスからプロパティ名と DB カラム名をコンストラクタに定義されている順で取得
     * @receiver エンティティクラス
     * @return コンストラクタ順に定義された名称のペア
     * - first：プロパティ名
     * - second：DB カラム名
     * @author Masahiro Inoue
     * @since 2026-05-23
     */
    @TraceLog
    fun <T : Entity> KClass<out T>.getDmlTargets(): List<Pair<String, String>> {
        val tableName = this.createTableName()
        val alias = this.getTableAlias()
        // コンストラクタ情報の取得
        val constructor = this.primaryConstructor
            ?: error(AE00007.format(this.simpleName))
        return constructor.parameters.map { param ->
            // プロパティ名の取得
            val propertyName = param.name
                ?: error(AE00007.format(this.simpleName))
            // プロパティ情報の取得
            val property = this.memberProperties.first { it.name == propertyName }
            // プロパティ情報情報を基にカラムエイリアスを取得
            val columnAlias =
                "${alias}_${property.getColumnAlias().ifBlank { property.getColumn() }}"
            // カラム名を生成
            val columnName = this.extractColumnMetadata(
                tableName = tableName,
                alias = alias,
                columnAlias = columnAlias,
                property = property,
            )
            Pair(propertyName, columnName)
        }
    }

    /**
     * ## SELECT 結果カラム紐づけ取得
     * ### Entity のプロパティ名と SELECT 結果のカラム名を取得する
     * @receiver SELECT 用 Entity クラス
     * @return プロパティ名と SELECT 結果カラム名のリスト
     * @author Masahiro Inoue
     * @since 2026-06-05
     */
    @Synchronized
    fun <T : SelectEntity> KClass<out T>.getSelectColumnTargets(): List<SelectColumnTarget> =
        TableRef(this, this.getTableAlias()).getSelectColumnTargets()

    /**
     * ## SELECT 結果カラム紐づけ取得
     * ### TableRef の alias を基準に、Entity のプロパティ名と SELECT 結果カラム名を取得する
     * @receiver SELECT 用 TableRef
     * @return プロパティ名と SELECT 結果カラム名のリスト
     * @author Masahiro Inoue
     * @since 2026-06-10
     */
    @Synchronized
    fun <T : SelectEntity> TableRef<out T>.getSelectColumnTargets(): List<SelectColumnTarget> {
        // tableMetadata にカラム情報を登録する
        val entityMeta = RuntimeEntityMetaFactory().create(this.entityClass)
        // テーブル名の取得
        val tableName = entityMeta.tableName
        // テーブルエイリアス取得
        val alias = this.alias
        val constructor = this.entityClass.primaryConstructor
            ?: error(AE00007.format(this.entityClass.simpleName))
        // テーブル名のプロパティ一覧取得
        val propertyMetaMap = entityMeta.properties.associateBy { it.propertyName }
        val propertyMap = this.entityClass.memberProperties.associateBy { it.name }
        return constructor.parameters.map { param ->
            // プロパティ名の取得
            val propertyName = param.name
                ?: error(AE00007.format(this.entityClass.simpleName))
            val propertyMeta = propertyMetaMap[propertyName]
                ?: error(AE00027.format(propertyName, tableName, alias))
            val property = propertyMap[propertyName]
                ?: error(AE00027.format(propertyName, tableName, alias))
            // カラムエイリアスの生成
            val resultColumnName = "${alias}_${propertyMeta.aliasName}"
            _tableMetadata.getOrPut(tableName) {
                TableDefinition(tableName, mutableMapOf())
            }.aliases.getOrPut(alias) {
                EntityDefinition(alias, this.entityClass, mutableMapOf())
            }.columns[resultColumnName] = property
            SelectColumnTarget(
                propertyName = propertyName,
                resultColumnName = resultColumnName,
            )
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
        val definedProperty: KProperty1<out Entity, *>? = _tableMetadata.getOrPut(tableName) {
            // このブロックは、createTableName の保険。但し無かった場合、columnAlias to property も登録
            val entityDefinition = EntityDefinition<Entity>(
                alias,
                this,
                mutableMapOf(columnAlias to property)
            )
            TableDefinition(tableName, mutableMapOf(alias to entityDefinition))
        }.aliases.getOrPut(alias) {
            // このブロックは、createTableName の保険。但し無かった場合、columnAlias to property も登録
            EntityDefinition(alias, this, mutableMapOf(columnAlias to property))
        }.columns.put(columnAlias, property)
        return definedProperty?.getColumn() ?: property.getColumn()
    }

    /**
     * ## コンストラクタ定義順プロパティ取得
     * ### プライマリコンストラクタの定義順でプロパティを取得する
     * @receiver テーブル定義 Entity クラス
     * @return コンストラクタ定義順のプロパティ一覧
     * @author Masahiro Inoue
     * @since 2026-06-05
     */
    fun <T : TableDefinitionEntity> KClass<out T>.getConstructorOrderedProperties():
            List<KProperty1<out T, *>> {
        val constructor = this.primaryConstructor
            ?: error(AE00007.format(this.qualifiedName))
        return constructor.parameters.map { parameter ->
            val propertyName = parameter.name
                ?: error(AE00008.format("<unknown>", this.qualifiedName))
            this.memberProperties.firstOrNull { property ->
                property.name == propertyName
            } ?: error(AE00008.format(propertyName, this.qualifiedName))
        }
    }

    /**
     * ## エイリアス取得
     * ### エンティティクラスからエイリアスを取得する
     * @receiver `@Table` アノテーションが付与されている [Entity] （上限境界）型の [KClass] インスタンス。
     * @return 取得したエイリアス
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <T : Entity> KClass<T>.getAlias(): String = this.getTableAlias()

    /** 型変換用データクラス */
    data class DataConverter<T : Any>(
        val columnType: String,
        val toBindValue: (SQLiteStatement, Int, T) -> Unit,
        val toProperty: (Any?) -> T
    ) {
        @Suppress("UNCHECKED_CAST")
        fun toBind(statement: SQLiteStatement, index: Int, value: Any) {
            toBindValue(statement, index, value as T)
        }

        @Suppress("UNCHECKED_CAST")
        fun toProp(value: Any?): T = toProperty(value)
    }

    /** フィールド型 → カラム型変換用マップ */
    val DataConvertedMap = mapOf(
        Int::class to DataConverter(
            "INTEGER",
            { statement, index, value -> statement.bindLong(index, value.toLong()) },
            { value -> (value as Number).toInt() }
        ),
        Long::class to DataConverter(
            "INTEGER",
            { statement, index, value -> statement.bindLong(index, value) },
            { value -> (value as Number).toLong() }
        ),
        Float::class to DataConverter(
            "REAL",
            { statement, index, value -> statement.bindDouble(index, value.toDouble()) },
            { value -> (value as Number).toFloat() }
        ),
        Double::class to DataConverter(
            "REAL",
            { statement, index, value -> statement.bindDouble(index, value) },
            { value -> (value as Number).toDouble() }
        ),
        Boolean::class to DataConverter(
            "INTEGER",
            { statement, index, value -> statement.bindLong(index, if (value) 1L else 0L) },
            { value -> (value as Number).toLong() != 0L }
        ),
        String::class to DataConverter(
            "TEXT",
            { statement, index, value -> statement.bindString(index, value) },
            { value -> value.toString() }
        ),
        LocalDate::class to DataConverter<LocalDate>(
            "DATETIME",
            { statement, index, value -> statement.bindString(index, value.toString()) },
            { value -> LocalDate.parse(value as String) }
        ),
        LocalTime::class to DataConverter<LocalTime>(
            "DATETIME",
            { statement, index, value -> statement.bindString(index, value.toString()) },
            { value -> LocalTime.parse(value as String) }
        ),
        LocalDateTime::class to DataConverter<LocalDateTime>(
            "DATETIME",
            { statement, index, value -> statement.bindString(index, value.toString()) },
            { value -> LocalDateTime.parse(value as String) }
        ),
        ByteArray::class to DataConverter(
            "BLOB",
            { statement, index, value -> statement.bindBlob(index, value) },
            { value -> value as ByteArray }
        )
    )

    /** カラム型 → フィールド型変換用マップ */
    val columnToFieldMap = mapOf<Int, (Cursor, Int) -> Any?>(
        Cursor.FIELD_TYPE_NULL to { _, _ -> null },
        Cursor.FIELD_TYPE_INTEGER to { cursor, index -> cursor.getLong(index) },
        Cursor.FIELD_TYPE_FLOAT to { cursor, index -> cursor.getDouble(index) },
        Cursor.FIELD_TYPE_STRING to { cursor, index -> cursor.getString(index) },
        Cursor.FIELD_TYPE_BLOB to { cursor, index -> cursor.getBlob(index) },
    )

    /**
     * ## Kotlin 型 SQL 型変換関数
     * ### クラスのフィールド型をデータベースのカラム型に変換
     * @param field 変換対象のフィールドを指定
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun mapKotlinTypeToSqlType(field: Any): String {
        val valueForJudgment = when (field) {
            // arg が既に KType の場合、KClass<*> にキャスト
            is KType -> field.classifier as? KClass<*>
            // それ以外は、KClass<*> を取得
            else -> field::class
        }
        require(
            valueForJudgment.isNotNull()
                    || DataConvertedMap[valueForJudgment].isNotNull()
        ) {
            AE00009.format(valueForJudgment)
        }
        return DataConvertedMap[valueForJudgment]?.columnType!!
    }

    /**
     * ## 値の文字列化
     * ### 指定された値を文字列化する
     * @param valueHolder バインド値管理オブジェクト
     * @param value 変換元の値
     * @return 文字列化された値
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    @Suppress("UNCHECKED_CAST")
    fun formatValue(valueHolder: QueryWithBindValues, value: Any): String = when (value) {
        is KProperty1<*, *> -> {
            val property = value as KProperty1<out Entity, *>
            "${property.extractClassFromProperty().getTableAlias()}.${value.getColumn()}"
        }

        is ColumnRef<*, *> -> {
            val ref = value as ColumnRef<out Entity, *>
            ref.build()
        }

        is SqlExpression -> {
            value.build(valueHolder)
        }

        else -> {
            valueHolder.addBindValue(value)
            "?"
        }
    }
}
