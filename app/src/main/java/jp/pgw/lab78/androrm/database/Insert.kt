package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableName
import jp.pgw.lab78.androrm.common.dml.interfaces.InsertEntity
import jp.pgw.lab78.androrm.database.interfaces.QueryBuilderLike
import jp.pgw.lab78.androrm.database.utility.EntityManager.getColumns
import kotlin.reflect.KClass

/**
 * ## Insert 文生成クラス
 * ### insert 句を構成する要素を基に insert 文を生成します
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
class Insert<T: InsertEntity>(
    private val entityClass: KClass<out T>
): QueryBuilderLike<T> {
    /** テーブル名：クラス名をスネークケース（大文字）に変換 */
    private val tableName = entityClass.getTableName()

    /** 挿入カラムリスト */
    private val insertColumnList = mutableListOf<Pair<String, String>>()

    /** カラム名の定義文字列 */
    private val columnDefine: String

    /** プレースホルダー名の定義文字列 */
    private val placeholders: String
    /**
     * ## コンストラクタ
     * ### 一番単純な select 文を生成します
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    init {
        // カラム名：クラスのメンバー・プロパティ名をスネークケース（大文字）に変換
        val columns = entityClass.getColumns()
        columnDefine = columns.joinToString(", ", "(", ")")
        placeholders = columns.joinToString(", ", "(", ")") { ":$it" }
    }

    /**
     * ## insert 文生成
     * ### 基本的な insert 文を生成します
     * @return insert into テーブル名 (カラム定義) values (カラムの数だけ「?」を展開)
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    override fun build() =
        "insert into $tableName $columnDefine values $placeholders"

    /**
     * ## Insert 文を生成します
     * ### プレースホルダ名形式の insert 文を生成します
     * @param entities インサートするデータが格納されたエンティティクラスのリスト
     * @return insert into テーブル名 (カラム定義) values (「:プレースホルダー」を展開) to list<Any>
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun build(entities: List<T>) =
        "insert into $tableName $columnDefine values${
            placeholders // ここはプラテスが想定している「:prop1 ,:prop2」を展開する機能で実装
        }" to entities.map {
            // columnDefine の順で entities の値を取得する
        }
}
