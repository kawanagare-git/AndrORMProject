package jp.pgw.lab78.androrm.common.meta

/**
 * ## エンティティメタ情報
 * ### Entity 全体を表す正規化済みメタ情報
 * ### Select / Delete / Update / Upsert などから共通利用する
 *
 * @property entityName Kotlin 上の Entity 名
 * @property tableName DB 上のテーブル名
 * @property tableAlias SQL 上で使用するテーブルエイリアス
 * @property properties プロパティメタ情報一覧
 * @author Masahiro Inoue
 * @since 2026-04-27
 */
data class EntityMeta(
    /** 定義元のクラス又は実装クラスの FQN */
    val defineEntityQualifiedName: String,
    /** Kotlin 上の Entity 名 */
    val entityName: String,
    /** DB 上のテーブル名 */
    val tableName: String,
    /** SQL 上で使用するテーブルエイリアス */
    val tableAlias: String,
    /** プロパティメタ情報一覧 */
    val properties: List<PropertyMeta>,
)