package jp.pgw.lab78.androrm.database.queryparts

import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.logging.aop.InfoLog
import jp.pgw.lab78.androrm.common.logging.aop.TraceLog
import jp.pgw.lab78.androrm.database.DmlConstant.MULTI_SPACE_REGEX
import jp.pgw.lab78.androrm.database.condition.base.BaseSelect
import jp.pgw.lab78.androrm.database.meta.SelectClause
import jp.pgw.lab78.androrm.database.reference.TableRef

/**
 * ## exists 用 Select クラス
 * ### exists 専用のサブクエリを生成する
 *
 * ### 仕様
 * #### `select 1` を基礎として FROM、JOIN、WHERE、HAVING を構築し、EXISTS 条件向けのサブクエリを生成する。
 * @param fromTable exists の from 句に記述するテーブル参照
 * @author Masahiro Inoue
 * @since 2026-07-10
 */
class ExistsSelect<T : SelectEntity>(
    private val fromTable: TableRef<T>,
) : BaseSelect<T, ExistsSelect<T>>() {
    /** select 文の土台 */
    private val selectStatement = "select 1 "

    /** 主 Entity の正規化済みメタ情報 */
    override val mainEntityMeta = runtimeEntityMetaFactory.create(fromTable.entityClass)

    /**
     * ## イニシャライザ
     * ### 一番単純な select 文を生成します
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    init {
        /**
         * EXISTSサブクエリの主Entityを検証し、FROM句とテーブル別名を初期化する。
         *
         * @author Masahiro Inoue
         * @since 2026-07-10
         */
        @InfoLog
        @TraceLog
        fun initialize() {
            val mainTableName = mainEntityMeta.tableName
            val mainTableAlias = fromTable.alias
            // 主 Entity のメタ情報を検証
            validateEntityMeta(mainEntityMeta)
            // from 句とテーブル名の定義を設定
            queryStructureMap[SelectClause.SELECT] =
                mutableListOf("from $mainTableName $mainTableAlias")
            // select 文で使用するエンティティクラスを登録
            usedEntityClasses += fromTable
            registerTableAlias(mainTableName, mainTableAlias)
        }
        initialize()
    }

    /**
     * ## exists 専用 select 句生成
     * ### 定義された内容で select を生成する
     * @author Masahiro Inoue
     * @since 2026-07-10
     */
    override fun build(): String {
        if (isBuild) return query
        isBuild = true
        val baseStatement = selectStatement
        buildInClauseDefinitionOrder({})
        val otherClauses = SelectClause.entries
            .joinToString(" ") { queryStructure[it]?.joinToString(" ") ?: " " }
        // select 文を生成
        query = "$baseStatement $otherClauses".replace(MULTI_SPACE_REGEX, " ").trimEnd()
        return query
    }
}

