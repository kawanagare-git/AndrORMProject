package jp.pgw.lab78.androrm.database.interfaces

/**
 * ## 包括クエリ生成 マーカーインターフェース
 */
interface ComprehensiveEntity:SelectEntity,InsertEntity,UpdateEntity,UpsertEntity {}