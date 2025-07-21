package jp.pgw.lab78.androrm.common.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class Projection(
    val entityNameExtend: String,
    val fields: Array<String>,
    val implementsInterface: String
)
