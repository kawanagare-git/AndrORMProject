package jp.pgw.lab78.androrm.common.database.annotation

/**
 * ## AndrORM テーブル列アノテーション
 * @param name フィールド名にカラム名を指定する
 * @param alias カラムのエイリアスを設定する
 * @param hideFromSelect SELECT 文の抽出項目から除外するか true:抽出項目から除外する / false:抽出項目
 * @param default CREATE TABLE 時の DEFAULT 句を指定する。DBアップグレード時の既存レコードに対する初期値では無い
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Column(
    val name: String = "",
    val alias: String = "",
    val hideFromSelect: Boolean = false,
    val default: String = "",
)
