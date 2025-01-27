package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.annotation.Column
import jp.pgw.lab78.androrm.annotation.PrimaryKey
import jp.pgw.lab78.androrm.annotation.Table
import jp.pgw.lab78.androrm.utility.Functions.mapKotlinTypeToSqlType
import jp.pgw.lab78.androrm.utility.Functions.toSnakeCase
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/**
 * SupportFunction オブジェクトクラス
 * database パッケージのクラスでサポートする関数群
 */
object SupportFunction {
    /**
     * generateTableCreationQuery 関数
     * テーブルを作成するクエリを生成する
     * @param entity entity クラスのインスタンスを指定
     */
    fun generateTableCreationQuery(entity: KClass<*>): String {
        val tableAnnotation = entity.findAnnotation<Table>() ?: error("Entity class must be annotated with @Table")
        val tableName = tableAnnotation.name.ifEmpty { entity.simpleName!!.toSnakeCase() }

        val columns = entity.memberProperties.joinToString(", ") { property ->
            val columnAnnotation = property.findAnnotation<Column>()
            val columnName = columnAnnotation?.name?.ifEmpty { property.name.toSnakeCase() } ?: property.name.toSnakeCase()
            val sqlType = mapKotlinTypeToSqlType(property.returnType)

            if (property.findAnnotation<PrimaryKey>() != null) "$columnName $sqlType PRIMARY KEY" else "$columnName $sqlType"
        }
        return "CREATE TABLE $tableName ($columns);"
    }
}