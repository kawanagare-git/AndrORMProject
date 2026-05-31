package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.Constants.PRIMARY_DELIMITER
import jp.pgw.lab78.androrm.common.MessageConstants.AE00011
import jp.pgw.lab78.androrm.common.database.SupportFunction.getPropertyValue
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableName
import jp.pgw.lab78.androrm.common.dml.interfaces.InsertEntity
import jp.pgw.lab78.androrm.common.logging.LogLevel.TRACE
import jp.pgw.lab78.androrm.common.logging.LogScope.APP
import jp.pgw.lab78.androrm.common.logging.aop.TraceLog
import jp.pgw.lab78.androrm.database.condition.interfaces.QueryWithBindValues
import jp.pgw.lab78.androrm.database.interfaces.QueryBuilderLike
import jp.pgw.lab78.androrm.database.utility.EntityManager.getDmlTargets
import java.util.logging.Logger
import kotlin.reflect.KClass

/**
 * ## Insert 文生成クラス
 * ### insert 句を構成する要素を基に insert 文を生成します
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
class Insert<T : InsertEntity>(
    private val entityClass: KClass<out T>
) : QueryBuilderLike<T>, QueryWithBindValues() {
    companion object {
        /**
         * ## テーブル間データ転送用
         * ### 基本的な用途は AndrOrmDatabaseHelper での使用
         * @param tableName インサート先テーブル名
         * @param columnList インサート先カラム名
         * @param selectStatement インサート時のセレクト文
         * @author Masahiro Inoue
         * @since 2026-05-31
         */
        internal fun intoTableColumns(
            tableName: String,
            columnList: List<String>,
            selectStatement: String
        ): String =
            "insert into $tableName (${columnList.joinToString(PRIMARY_DELIMITER)}) $selectStatement"
    }

    /** ログ出力移譲 */
    private val logger: Logger by lazy { APP.create(minLogLevel = TRACE) }

    /** テーブル名：クラス名をスネークケース（大文字）に変換 */
    private val tableName = entityClass.getTableName()

    /** 挿入カラムリスト */
    private val columnList = entityClass.getDmlTargets()

    /** カラム名の定義文字列 */
    // カラム名：クラスのメンバー・プロパティ名をスネークケース（大文字）に変換
    private val columnDefine: String =
        columnList.joinToString(", ", "(", ")") { it.second }

    /** エンティティ一覧 */
    private var entities: MutableList<T> = mutableListOf()
        set(value) {
            field = value
        }

    /** エンティティを1件追加 */
    fun addEntity(entity: T) {
        entities.add(entity)
    }

    /** エンティティを複数件追加 */
    fun addEntities(entities: List<T>) {
        this.entities.addAll(entities)
    }

    /**
     * ## 条件生成メソッド
     * ### 定義された条件から文字列を生成する
     * @return 生成された文字列
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    override fun build(): String = build(entities).first

    /**
     * ## Insert 文を生成します
     * ### プレースホルダ名形式の insert 文を生成します
     * @param entities インサートするデータが格納されたエンティティクラスのリスト
     * @return insert into テーブル名 (カラム定義) values (「:プレースホルダー」を展開) to list<Any>
     * @author Masahiro Inoue
     * @since 2026-05-23
     */
    @TraceLog
    fun build(entities: List<T>): Pair<String, List<Any?>> {
        require(entities.isNotEmpty()) { AE00011 }
        this.entities = entities.toMutableList()
        clearBindValues()
        val result =
            "insert into $tableName $columnDefine values${
                placeholders(entities.size, columnList.size)
            }" to entities.flatMap { entity ->
                columnList.map { (propertyName, _) ->
                    getPropertyValue(entity, propertyName)
                }
            }
        addBindValues(result.second)
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
    @TraceLog
    private fun placeholders(rowCount: Int, columnCount: Int): String {
        val oneRow = List(columnCount) { "?" }.joinToString(", ", "(", ")")
        return List(rowCount) { oneRow }.joinToString(", ")
    }
}
