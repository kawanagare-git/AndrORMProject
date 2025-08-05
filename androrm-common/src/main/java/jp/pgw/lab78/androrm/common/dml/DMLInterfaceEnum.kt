package jp.pgw.lab78.androrm.common.dml

import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.dml.interfaces.ConditionEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.InsertEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.UpdateEntity
import jp.pgw.lab78.androrm.common.dml.interfaces.UpsertEntity

/**
 * ## 共通インターフェス定義列挙型
 * ### AndrORM のインターフェース列挙型
 * ### @Projection.DMLInterfaceEnum に指定する
 * @param interfaceFQN 共通インターフェスの FQN
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
public enum class DMLInterfaceEnum(
    /** 共通インターフェスの FQN */
    public  val interfaceFQN: String) {
    NOT_USE(EMPTY_STRING),
    SELECT(SelectEntity::class.qualifiedName.toString()),
    INSERT(InsertEntity::class.qualifiedName.toString()),
    UPDATE(UpdateEntity::class.qualifiedName.toString()),
    UPSERT(UpsertEntity::class.qualifiedName.toString()),
    CONDITION(ConditionEntity::class.qualifiedName.toString()),
}
