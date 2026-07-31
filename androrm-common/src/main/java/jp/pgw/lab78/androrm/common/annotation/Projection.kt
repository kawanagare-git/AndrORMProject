package jp.pgw.lab78.androrm.common.annotation

import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum

/**
 * ## AndrORM プロジェクションアノテーションクラス
 * ### このアノテーションが付与されたクラスと
 * ### このアノテーションに定義された情報を基に
 * ### 新たなエンティティクラスを生成する
 * @param entityNameExtend エンティティ派生名
 * @param aliasExtend エイリアス派生
 * @param properties クラスに定義する ColumnProjection ※複数指定可
 * @param functions クラスに定義する FunctionProjection ※複数指定可
 * @param commonInterface 共通インターフェース ※複数指定可
 * @param customInterface commonInterface が NOT_USE の場合に使用する生成先サブパッケージ
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class Projection(
    /** エンティティ拡張名 */
    val entityNameExtend: String,
    /** テーブルエイリアス拡張名 */
    val aliasExtend: String = "",
    /** クラスに定義するプロパティ情報 */
    val properties: Array<ColumnProjection>,
    /** エンティティに関数を使用する場合に指定 */
    val functions: Array<FunctionProjection> = [],
    /** 共通インターフェス */
    val commonInterface: Array<DMLInterfaceEnum> = [DMLInterfaceEnum.NOT_USE],
    /** カスタム生成先サブパッケージ */
    val customInterface: Array<String> = [""],
)
