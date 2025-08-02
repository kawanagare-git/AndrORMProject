package jp.pgw.lab78.androrm.common.annotation

import jp.pgw.lab78.androrm.common.dml.DMLInterfaceEnum

/**
 * ## AndrORM プロジェクションアノテーションクラス
 * ### このアノテーションが付与されたクラスと
 * ### このアノテーションに定義された情報を基に
 * ### 新たなエンティティクラスを生成する
 * @param entityNameExtend エンティティ派生名
 * @param properties クラスに定義するプロパティ名
 * @param commonInterface 共通インターフェース
 * @param customInterface 独自インターフェース
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class Projection(
    /** エンティティ派生名 */
    val entityNameExtend: String,
    /** テーブルエイリアス */
    val aliasExtend: String = "",
    /** クラスに定義するプロパティ名 */
    val properties: Array<String>,
    /** 共通インターフェス */
    val commonInterface: DMLInterfaceEnum = DMLInterfaceEnum.NOT_USE,
    /** 独自インターフェス */
    val customInterface: String = "",
)
