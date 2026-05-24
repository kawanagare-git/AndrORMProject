package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.MessageConstants.AE00011
import jp.pgw.lab78.androrm.common.database.SupportFunction.getPropertyValue
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableName
import jp.pgw.lab78.androrm.common.dml.interfaces.InsertEntity
import jp.pgw.lab78.androrm.common.logging.aop.TraceLog
import jp.pgw.lab78.androrm.database.utility.EntityManager.getInsertTargets
import kotlin.reflect.KClass

/**
 * ## Insert 文生成クラス
 * ### insert 句を構成する要素を基に insert 文を生成します
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
class Insert<T : InsertEntity>(
    private val entityClass: KClass<out T>
) {
    /** テーブル名：クラス名をスネークケース（大文字）に変換 */
    private val tableName = entityClass.getTableName()

    /** 挿入カラムリスト */
    private val columnList = entityClass.getInsertTargets()

    /** カラム名の定義文字列 */
    // カラム名：クラスのメンバー・プロパティ名をスネークケース（大文字）に変換
    private val columnDefine: String =
        columnList.joinToString(", ", "(", ")") { it.second }

    /**
     * ## Insert 文を生成します
     * ### プレースホルダ名形式の insert 文を生成します
     * @param entities インサートするデータが格納されたエンティティクラスのリスト
     * @return insert into テーブル名 (カラム定義) values (「:プレースホルダー」を展開) to list<Any>
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    @TraceLog
    fun build(entities: List<T>): Pair<String, List<Any?>> {
        require(entities.isNotEmpty()) { AE00011 }
        val result =
            "insert into $tableName $columnDefine values${
                placeholders(entities.size, columnList.size)
            }" to entities.flatMap { entity ->
                columnList.map { (propertyName, _) ->
                    getPropertyValue(entity, propertyName)
                }
            }
        return result
    }

    /**
     * ## プレースホルダー定義文字列
     * ### 挿入行数に応じてプレースホルダー郡を生成
     * @param columnCount カラム数
     * @param rowCount 行数
     * @return 生成されたプレースホルダー郡
     * @author Masahiro Inoue
     * @since 2026-05-23
     */
    private fun placeholders(rowCount: Int, columnCount: Int): String {
        val oneRow = List(columnCount) { "?" }.joinToString(", ", "(", ")")
        return List(rowCount) { oneRow }.joinToString(", ")
    }
}
