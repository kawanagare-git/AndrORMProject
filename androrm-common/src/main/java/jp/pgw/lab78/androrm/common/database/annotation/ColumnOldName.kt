package jp.pgw.lab78.androrm.common.database.annotation

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ColumnOldName(
    val name: String,
)