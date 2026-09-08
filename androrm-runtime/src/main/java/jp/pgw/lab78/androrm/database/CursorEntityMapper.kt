package jp.pgw.lab78.androrm.database

import android.database.Cursor
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity

/**
 * ## CursorからSELECT Entityを生成するMapper契約
 * ### KSPが生成したSELECT Entity専用のCursor転送処理を公開する
 * @param T 変換先のSELECT Entity型
 * @author Masahiro Inoue
 * @since 2026-09-06
 */
interface CursorEntityMapper<out T : SelectEntity> {
    /**
     * ## Cursor行をEntityへ変換
     * ### columnIndexesはSELECT対象プロパティ順で、行ループ前に解決済みである
     * @param cursor 現在行を指すCursor
     * @param columnIndexes SELECT対象プロパティ順のCursorカラムIndex。hideFromSelectのプロパティは含まない
     * @return 生成されたEntity
     */
    fun map(cursor: Cursor, columnIndexes: IntArray): T
}
