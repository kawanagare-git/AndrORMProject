package jp.pgw.lab78.androrm.common.annotation

/**
 * ## AndrORM 出力先パッケージ指定アノテーション
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class EntityPackageInfo(
    val basePackage: String = "jp.pgw.lab78.androrm.database.entities",
    val selectPackage: String = "select",
    val insertPackage: String = "insert",
    val updatePackage: String = "update",
    val upsertPackage: String = "upsert",
    val conditionPackage: String = "condition",
)
