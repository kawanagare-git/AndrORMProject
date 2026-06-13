package jp.pgw.lab78.androrm.ksp

import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table

/**
 * ## 定数クラス
 * ### アノテーションのクラス名や引数名など、コード生成に必要な定数を定義するクラス
 * @author Masahiro Inoue
 * @since 2026-04-29
 */
object Constants {
    /** @Table のシンプルネーム */
    val TABLE = Table::class.simpleName!!

    /** @Table の変数名定義（name） */
    const val TABLE_NAME = "name"

    /** @Table の変数名定義（alias） */
    const val TABLE_ALIAS = "alias"

    /** @Column */
    val COLUMN = Column::class.simpleName!!

    /** @Column(FQN) */
    val COLUMN_FQN = Column::class.qualifiedName!!

    /** @Column の変数名定義（name） */
    const val COLUMN_NAME = "name"

    /** @Column の変数名定義（alias） */
    const val COLUMN_ALIAS = "alias"

    /** @Column の変数名定義（hideFromSelect） */
    const val COLUMN_HIDE_FROM_SELECT = "hideFromSelect"

    /** @Function */
    val FUNCTION = Function::class.simpleName!!

    /** @Function の変数名定義（columnFunction） */
    const val F_COLUMN_FUNCTION = "columnFunction"

    /** @Function の変数名定義（args） */
    const val F_ARGS = "args"

    /** @Function の変数名定義（alias） */
    const val F_ALIAS = "alias"

    /** @Function の変数名定義（raw） */
    const val F_RAW = "raw"

    /** @Function の変数名定義（hideFromSelect） */
    const val F_HIDE_FROM_SELECT = "hideFromSelect"

    /** @Column の変数名定義（defaultValue） */
    const val COLUMN_DEFAULT_VALUE = "default"

    /** @Function のエイリアス定義がない場合のフォールバック値 */
    const val FP_ALIAS_FALLBACK = "FUNCTION"
}