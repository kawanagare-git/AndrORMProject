package jp.pgw.lab78.androrm.common.database.annotation

/**
 * ## DBマイグレーション旧カラム名指定
 * ### DBマイグレーション時のカラム名を変更した場合の旧カラム名を記述する
 * @property name 旧カラム名
 * @author Masahiro Inoue
 * @since 2026-07-18
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ColumnOldName(
    val name: String,
)