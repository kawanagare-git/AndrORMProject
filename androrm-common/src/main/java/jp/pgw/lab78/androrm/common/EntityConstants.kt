package jp.pgw.lab78.androrm.common

import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.dml.interfaces.*
import kotlin.reflect.KClass

/* Entity 系定数を追加・削除・更新を実施したら、
 * androrm-common/src/main/java/jp/pgw/lab78/androrm/common/annotation/EntityPackageInfo.kt
 * も適切に修正すること
 */
/**
 * ## 共通エンティティ定数
 * ### エンティティ定義で使用する定数を管理
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
object EntityConstants {
    /**
     * ## 共通インターフェス定義列挙型
     * ### AndrORM のインターフェース列挙型
     * ### @Projection.DMLInterfaceEnum に指定する
     * @param interfaceFQN 共通インターフェスの FQN
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    enum class DMLInterfaceEnum(
        val kClass: KClass<out Entity>,
        /** 共通インターフェスの FQN */
        val interfaceFQN: String = kClass.qualifiedName!!
    ) {
        NOT_USE(Entity::class, EMPTY_STRING),
        SELECT(SelectEntity::class),
        INSERT(InsertEntity::class),
        UPDATE(UpdateEntity::class),
        UPSERT(UpsertEntity::class),
        ABSERT(AbsertEntity::class),
        DELETE(DeleteEntity::class), ;
    }

    /**
     * ## パッケージとインターフェースのリレーションクラス
     * ### 標準インターフェースと出力先パッケージを紐づける
     * @param relation インターフェースと @EntityPackageInfo の変数の組み合わせ
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    enum class PackageInterfaceRelation(val relation: String) {
        SELECT("selectPackage"),
        INSERT("insertPackage"),
        UPDATE("updatePackage"),
        UPSERT("upsertPackage"),
        ABSERT("absertPackage"),
        DELETE("deletePackage"),
    }
}