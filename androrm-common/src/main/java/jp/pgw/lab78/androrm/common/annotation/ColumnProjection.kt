package jp.pgw.lab78.androrm.common.annotation

/**
 * ## AndrORM カラムプロジェクションアノテーションクラス
 * ### このアノテーションに指定されたカラムアノテーションを生成するためのアノテーション
 * @param property フィールド名にカラム名を指定する
 * @param hideFromSelect SELECT 句の抽出カラムに含めない（条件にだけ使用する）
 * @author Masahiro Inoue
 * @since 2026-04-14
 */
annotation class ColumnProjection(
    val property: String,
    val hideFromSelect: Boolean = false,
)
