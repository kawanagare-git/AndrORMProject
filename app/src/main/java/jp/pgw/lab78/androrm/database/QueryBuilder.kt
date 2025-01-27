package jp.pgw.lab78.androrm.database

/**
 * QueryBuilder クラス
 *
 */
class QueryBuilder {
    private val selectColumns = mutableListOf<String>()
    private var fromTable: String? = null
    private var whereCondition: String? = null

    fun select(vararg columns: String): QueryBuilder {
        selectColumns.addAll(columns)
        return this
    }

    fun from(tableName: String): QueryBuilder {
        fromTable = tableName
        return this
    }

    fun where(condition: String): QueryBuilder {
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
