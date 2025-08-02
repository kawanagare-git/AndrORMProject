package jp.pgw.lab78.androrm.common.annotation

/**
 * ## AndrORM プロジェクション複数定義アノテーションクラス
 * ### @Projection の定義を配列として複数定義する
 * @param value エンティティ派生名
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Projections(
    val value: Array<Projection>
)
