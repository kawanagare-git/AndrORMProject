package jp.pgw.lab78.generated.annotation

/**
 * ## Table アノテーション
 * @param name テーブル名
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Table(val name: String = "",val alias: String = "")
