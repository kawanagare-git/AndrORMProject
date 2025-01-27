package jp.pgw.lab78.androrm.annotation

/**
 * TableColumns アノテーション
 * @param toSnake entity クラスのフィールド名にカラム名が指定しない場合 フィールド名を
 *           スネークケースに変換したものをカラム名とする
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class TableColumns(val toSnake: Boolean = true)
