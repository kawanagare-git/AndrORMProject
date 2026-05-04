package jp.pgw.lab78.androrm.common.logging

import java.util.logging.Level

/**
 * ## ログレベル列挙型
 * @author Masahiro Inoue
 * @since 2026-01-25
 */
enum class LogLevel(private val levelOrder: Int, val level: Level) {
    /** TRACEレベル */
    TRACE(1, Level.FINEST),

    /** DEBUGレベル */
    DEBUG(2, Level.FINE),

    /** INFOレベル */
    INFO(3, Level.INFO),

    /** WARNレベル */
    WARN(4, Level.WARNING),

    /** ERRORレベル */
    ERROR(5, Level.SEVERE),
    ;

    /**
     * ## ログ出力判定メソッド
     * @param targetLevel 判定対象ログレベル
     * @return ログ出力可否
     * @author Masahiro Inoue
     * @since 2026-01-25
     */
    fun isLoggable(targetLevel: LogLevel): Boolean = this.levelOrder <= targetLevel.levelOrder

    /**
     * ## タグ取得プロパティー
     * ### ログ出力時のタグとして列挙型の名前を返す
     * @return タグ文字列
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    val tag: String = name

    companion object {
        /**
         * ## ログレベル取得メソッド
         * @param level レベルオーダー
         * @return ログレベル列挙型
         * @author Masahiro Inoue
         * @since 2026-01-25
         */
        fun fromLevel(level: Level): LogLevel {
            return entries.find { it.level == level } ?: INFO
        }
    }
}