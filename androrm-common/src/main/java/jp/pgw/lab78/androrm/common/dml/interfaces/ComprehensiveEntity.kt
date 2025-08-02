package jp.pgw.lab78.androrm.common.dml.interfaces

/**
 * ## 包括クエリ生成 マーカーインターフェース
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
interface ComprehensiveEntity: SelectEntity, InsertEntity, UpdateEntity, UpsertEntity