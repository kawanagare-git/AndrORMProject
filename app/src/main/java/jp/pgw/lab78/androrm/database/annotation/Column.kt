package jp.pgw.lab78.generated.annotation

/**
 * ## Column アノテーション
 * @param name フィールド名にカラム名を指定する
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Column(val name: String = "", val alias: String = "")
