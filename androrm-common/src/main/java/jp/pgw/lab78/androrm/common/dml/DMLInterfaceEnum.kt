package jp.pgw.lab78.androrm.common.dml

import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.dml.interfaces.ConditionEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.common.dml.interfaces.InsertEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.UpdateEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.UpsertEntity
import kotlin.reflect.KClass

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
    NOT_USE(Entity::class,EMPTY_STRING),
    SELECT(SelectEntity::class),
    INSERT(InsertEntity::class),
    UPDATE(UpdateEntity::class),
    UPSERT(UpsertEntity::class),
    CONDITION(ConditionEntity::class),;
}
