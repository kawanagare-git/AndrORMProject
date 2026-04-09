package jp.pgw.lab78.androrm.common.logging.interfaces

import jp.pgw.lab78.androrm.common.logging.LogLevel
import java.util.logging.Logger

/**
 * ## AndrORM ロガーライクインターフェース
 * @author Masahiro Inoue
 * @since 2026-01-25
 */
interface AndrOrmLoggerLike {
    /**
     * ## ロガー初期化メソッド
     * @param minLogLevel 最小ログレベル
     * @return ロガーインスタンス
     * @author Masahiro Inoue
     * @since 2026-01-25
     */
    fun create(
        logFileSuffix: String = "",
        minLogLevel: LogLevel = LogLevel.INFO
    ): Logger

    /**
     * ## ログ出力メソッド
     * @param logLevel ログレベル
     * @param message ログメッセージ
     * @author Masahiro Inoue
     * @since 2026-01-25
     */
    fun log(logLevel: LogLevel, message: String)
}