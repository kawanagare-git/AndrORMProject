package jp.pgw.lab78.androrm.common.database.annotation

/**
 * ## AndrORM テーブル列アノテーション
 * @param name フィールド名にカラム名を指定する
 * @param alias カラムのエイリアスを設定する
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Column(
    val name: String = "",
    val alias: String = "",
    val hideFromSelect: Boolean = false,
)
