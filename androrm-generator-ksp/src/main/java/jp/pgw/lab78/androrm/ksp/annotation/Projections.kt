package jp.pgw.lab78.androrm.ksp.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Projections(
    val value: Array<Projection>
)
