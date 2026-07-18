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
 *
 * ### 仕様
 * #### 1件以上の Entity から複数行 INSERT 文を生成し、対象カラムは Entity の DML 対象定義順で固定する。
 * #### バインド値は Entity の追加順、各 Entity の対象カラム順で保持し、内容変更後は SQL を再構築する。
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

    /** ビルドフラグ */
    private var isBuild: Boolean = false

    /** クエリ格納 */
    private lateinit var query: String

    /** エンティティ一覧 */
    private var entities: MutableList<T> = mutableListOf()

    /** エンティティを1件追加 */
    fun addEntity(entity: T): Insert<T> {
        addEntities(listOf(entity))
        return this
    }

    /** エンティティを複数件追加 */
    fun addEntities(vararg entities: T): Insert<T> {
        addEntities(entities.asList())
        return this
    }

    /** エンティティを複数件追加 */
    fun addEntities(entities: List<T>): Insert<T> {
        isBuild = false
        this.entities.addAll(entities)
        return this
    }

    /**
     * ## Insert 文を生成します
     * ### 登録された Entity に対応する複数行 INSERT 文を、位置指定プレースホルダー「?」を使用して生成します
     * @return Entity の件数分の VALUES 句を持つ INSERT 文
     * @author Masahiro Inoue
     * @since 2026-05-23
     */
    @TraceLog
    override fun build(): String {
        require(entities.isNotEmpty()) { AE00011 }
        rebuildQueryIfRequired()
        clearBindValues()
        addBindValues(createBindValues())
        return query
    }

    /**
     * ## Insert 文を必要に応じて再構築します
     * ### Entity が追加されていない場合は、構築済みのクエリ文字列を再利用します
     * @author Masahiro Inoue
     * @since 2026-06-27
     */
    private fun rebuildQueryIfRequired() {
        if (isBuild) {
            return
        }
        query = "insert into $tableName $columnDefine values${
            placeholders(entities.size, columnList.size)
        }"
        isBuild = true
    }

    /**
     * ## バインド変数の生成
     * ### バインド変数をエンティティ一覧に登録された内容に従い生成する
     * @return 生成されたバインド変数
     * @author Masahiro Inoue
     * @since 2026-06-27
     */
    private fun createBindValues(): List<Any?> =
        entities.flatMap { entity ->
            columnList.map { (propertyName, _) ->
                getPropertyValue(entity, propertyName)
            }
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
