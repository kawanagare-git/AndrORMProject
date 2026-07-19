package jp.pgw.lab78.androrm.common.database.annotation

/**
 * ## ユニークインデックス定義アノテーション
 * ### テーブルのユニークインデックスを定義するアノテーション
 * @property name ユニークインデックス名
 * @property properties ユニークインデックスを適用させるプロパティ
 * @author Masahiro Inoue
 * @since 2026-05-28
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class Unique(
    val name: String = "",
    val properties: Array<String>,
)