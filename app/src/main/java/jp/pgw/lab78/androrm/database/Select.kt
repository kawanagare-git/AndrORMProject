package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.annotation.TableColumns

/**
 * QueryBuilder クラス
 *
 */
class Select(vararg val columns: TableColumns) {
    private val selectColumns = mutableListOf<String>()
    private var fromTable: String? = null
    private var whereCondition: String? = null

    fun from(tableName: String): Select {
        fromTable = tableName
        return this
    }

    fun where(condition: String): Select {
        whereCondition = condition
        return this
    }

    fun build(): String {
        if (fromTable == null) throw IllegalStateException("FROM clause is required")
        val columns = if (selectColumns.isEmpty()) "*" else selectColumns.joinToString(", ")
        val where = whereCondition?.let { " WHERE $it" } ?: ""
        return "SELECT $columns FROM $fromTable$where;"
    }
}
