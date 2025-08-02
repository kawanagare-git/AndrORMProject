package jp.pgw.lab78.androrm.common.database.annotation

/**
 * ## Table アノテーション
 * @param name テーブル名
 * @param alias テーブルのエイリアスを設定する
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Table(
    val name: String = "",
    val alias: String = ""
)
