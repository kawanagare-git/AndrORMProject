package jp.pgw.lab78.generated.ksp.annotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@Repeatable
annotation class Projection(
    val entityNameExtend: String,
//    val fields: Array<AllClassProperties>,
    val implementsInterface: String
)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class Projection1(
    val entityNameExtend: String,
    val fields: Array<String>,
    val implementsInterface: String
)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@Repeatable
annotation class Projection2(
    val entityNameExtend: String,
    val fields: IntArray,
    val implementsInterface: String
)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
@Repeatable
annotation class Projection4(
    val entityNameExtend: String,
//    val fields: Array<jp.pgw.lab78.androrm.ksp.generated.AllClassProperties>,
    val names: Array<String>,
    val implementsInterface: String
)
