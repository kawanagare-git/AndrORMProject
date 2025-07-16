package jp.pgw.lab78.androrm.ksp.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class Projection(
    val name: String,
    val fields: Array<String>,
    val implementsInterface: String
)
