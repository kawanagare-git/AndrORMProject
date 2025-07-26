package jp.pgw.lab78.androrm.common.annotation

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class EntityPackageInfo(
    val basePackage: String = "jp.pgw.lab78.androrm.database.entities",
    val selectPackage: String = "select",
    val insertPackage: String = "insert",
    val updatePackage: String = "update",
    val upsertPackage: String = "upsert",
)
