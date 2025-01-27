package jp.pgw.lab78.androrm.annotation

/**
 * Table アノテーション
 * @param name テーブル名
 * @param toSnake テーブル名を指定しない代わりに data class のクラス名を
 *                  スネークケースに変換したものをテーブル名とする
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Table(val name: String = "", val toSnake: Boolean = true)
