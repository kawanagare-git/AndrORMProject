package jp.pgw.lab78.androrm.common.database.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class Index(
    val name: String = "INDEX0",
    val properties: Array<String>,
)