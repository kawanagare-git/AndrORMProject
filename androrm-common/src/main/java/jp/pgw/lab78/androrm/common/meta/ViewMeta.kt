package jp.pgw.lab78.androrm.common.meta

/**
 * ## VIEW メタ情報
 * ### VIEW 定義 Entity から解決した名称、エイリアス、出力カラムを保持する
 * @param defineEntityQualifiedName 定義 Entity の完全修飾名
 * @param entityName Kotlin 上の Entity 名
 * @param viewName SQLite 上の VIEW 名
 * @param viewAlias SELECT 時のエイリアス
 * @param properties VIEW 出力プロパティ
 * @author Masahiro Inoue
 * @since 2026-08-31
 */
data class ViewMeta(
    val defineEntityQualifiedName: String,
    val entityName: String,
    val viewName: String,
    val viewAlias: String,
    val properties: List<PropertyMeta>,
)
