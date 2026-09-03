package jp.pgw.lab78.androrm.common.database.columns_controller.interfaces

/**
 * ## カラム制御モデル
 * ### カラム検証や値取得を実装させるためのインターフェイス
 * @author Masahiro Inoue
 * @since 2026-09-03
 */
interface ColumnControllerModel {
    /**
     * ## カラム転送
     * ### データを変換してカラム値へ転送する
     * @param value 変換元の値
     * @return 変換後の値
     * @author Masahiro Inoue
     * @since 2026-09-03
     */
    fun <T> toColumnValue(value: Any): T
}
