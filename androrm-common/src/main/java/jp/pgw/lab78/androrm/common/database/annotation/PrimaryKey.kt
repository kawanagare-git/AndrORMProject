package jp.pgw.lab78.androrm.common.database.annotation

/**
 * ## PrimaryKey アノテーション
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PrimaryKey(
    val columns: Array<String>
)
