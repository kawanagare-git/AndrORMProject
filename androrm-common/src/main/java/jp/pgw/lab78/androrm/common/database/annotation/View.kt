package jp.pgw.lab78.androrm.common.database.annotation

/**
 * ## View アノテーション
 * ### SQLite VIEW の名前と SELECT 時のエイリアスを定義する
 * @param name VIEW 名
 * @param alias VIEW のエイリアス
 * @author Masahiro Inoue
 * @since 2026-08-31
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class View(
    val name: String = "",
    val alias: String = "",
)
