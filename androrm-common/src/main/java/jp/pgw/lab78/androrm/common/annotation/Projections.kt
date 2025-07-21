package jp.pgw.lab78.androrm.common.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Projections(
    val value: Array<Projection>
)
