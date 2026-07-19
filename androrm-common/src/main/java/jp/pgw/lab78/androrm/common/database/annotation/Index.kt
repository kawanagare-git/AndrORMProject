package jp.pgw.lab78.androrm.common.database.annotation

/**
 * ## インデックス定義アノテーション
 * ### テーブルのインデックスを定義するアノテーション
 * @property name インデックス名
 * @property properties インデックスを適用させるプロパティ
 * @author Masahiro Inoue
 * @since 2026-05-28
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class Index(
    val name: String = "",
    val properties: Array<String>,
)