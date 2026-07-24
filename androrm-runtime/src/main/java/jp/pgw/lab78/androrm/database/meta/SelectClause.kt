package jp.pgw.lab78.androrm.database.meta

import jp.pgw.lab78.androrm.database.validation.QueryMethodCall

/**
 * ## select 文を構成要素列挙クラス
 * ### セレクト文を構成する要素を列挙子として構成する
 * @author Masahiro Inoue
 * @since 2026-07-10
 */
internal enum class SelectClause(
    val sql: String,
    override val methodName: String,
) : QueryMethodCall {
    SELECT("select", ""),
    JOIN("join", ""),
    WHERE("where", "where"),
    GROUP("group by", ""),
    HAVING("having", "having"),
    ORDER("order by", "order"),
    LIMIT("limit", "limit"),
    OFFSET("offset", "offset"),
}

