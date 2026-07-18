package jp.pgw.lab78.androrm.common.database.annotation

/**
 * ## DBマイグレーション既定値アノテーション
 * ### 旧テーブルに存在しないカラムを追加するとき、既存レコードへ設定するSQL値を明示する
 * ### CREATE TABLE の DEFAULT 句には使用せず、DBアップグレード時だけ使用する
 * @property value 対象プロパティ型に対応したSQLリテラルまたは許可されたSQL予約値
 * @author Masahiro Inoue
 * @since 2026-07-18
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class MigrationDefault(
    val value: String,
)
