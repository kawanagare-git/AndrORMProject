package jp.pgw.lab78.androrm.database.interfaces.orm.entity.query

/**
 * ## 包括クエリ生成 マーカーインターフェース
 */
interface ComprehensiveEntity: SelectEntity, InsertEntity, UpdateEntity, UpsertEntity {}