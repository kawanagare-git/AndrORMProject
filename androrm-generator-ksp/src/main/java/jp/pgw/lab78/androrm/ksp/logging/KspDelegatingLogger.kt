package jp.pgw.lab78.androrm.ksp.logging

import com.google.devtools.ksp.processing.KSPLogger
import jp.pgw.lab78.androrm.common.Constants.ARGUMENT_DELIMITER
import jp.pgw.lab78.androrm.common.Constants.LogPhase.*
import jp.pgw.lab78.androrm.common.logging.LogLevel
import jp.pgw.lab78.androrm.common.logging.interfaces.LoggerLike
import jp.pgw.lab78.androrm.ksp.logging.DefaultLogMessageGenerator.generateDebugMessage
import jp.pgw.lab78.androrm.ksp.logging.DefaultLogMessageGenerator.generateErrorMessage
import jp.pgw.lab78.androrm.ksp.logging.DefaultLogMessageGenerator.generateInfoMessage
import jp.pgw.lab78.androrm.ksp.logging.DefaultLogMessageGenerator.generateTraceMessage
import jp.pgw.lab78.androrm.ksp.logging.DefaultLogMessageGenerator.generateWarningMessage
import jp.pgw.lab78.androrm.ksp.logging.LogUtils.concat
import jp.pgw.lab78.androrm.ksp.logging.LogUtils.getMethodName
import jp.pgw.lab78.androrm.ksp.logging.LogUtils.toSingleLineLogString

/**
 * ## KSP ロガー委譲クラス
 * ### KSP のロガー機能を LoggerLike インターフェースに適合させるためのクラス
 * @param logger KSP のロガーインスタンス
 * @author Masahiro Inoue
 * @since 2026-02-27
 */
class KspDelegatingLogger(
    private val logger: KSPLogger,
    private val consoleLogLevel: LogLevel = LogLevel.WARN,
) : LoggerLike {
    /**
     * ## インフォメーションログ出力メソッド
     * ### エラー発生時のログ出力用の簡易メソッド
     * @param infoMessage 出力メッセージ
     * @param details 表示情報
     * @author Masahiro Inoue
     * @since 2026-02-27
     */
    override fun logInfo(infoMessage: String, vararg details: Any) {
        logger.info(infoMessage.ifEmpty {
            "${generateInfoMessage(infoMessage, getMethodName())}: " +
                    concat(details.toList(), ARGUMENT_DELIMITER).toSingleLineLogString()
        })
    }

    /**
     * ## エントリーログ出力メソッド
     * ### メソッド実行時のログ出力用の簡易メソッド
     * @param infoMessage メソッド引数群
     * @author Masahiro Inoue
     * @since 2026-02-27
     */
    override fun logInfoEntered(infoMessage: String) =
        logger.info(infoMessage.ifEmpty {
            "${generateInfoMessage(infoMessage, getMethodName())}: ${ENTERED.tag}"
        })

    /**
     * ## イグジットログ出力メソッド
     * ### メソッド完了時のログ出力用の簡易メソッド
     * @param infoMessage メソッド実行結果
     * @author Masahiro Inoue
     * @since 2026-02-27
     */
    override fun logInfoExiting(infoMessage: String) {
        logger.info(infoMessage.ifEmpty {
            "${generateInfoMessage(infoMessage, getMethodName())}: ${EXITING.tag}"
        })
    }

    /**
     * ## ワーニングログ出力メソッド
     * ### エラー発生時のログ出力用の簡易メソッド
     * @param warnMessage 出力メッセージ
     * @param details 表示情報
     * @author Masahiro Inoue
     * @since 2026-02-27
     */
    override fun logWarning(warnMessage: String, vararg details: Any) {
        logger.warn(warnMessage.ifEmpty {
            "${generateWarningMessage(warnMessage, getMethodName())}: " +
                    concat(details.toList(), ARGUMENT_DELIMITER).toSingleLineLogString()
        })
    }

    /**
     * ## エラーログ出力メソッド
     * ### エラー発生時のログ出力用の簡易メソッド
     * @param errMessage 出力メッセージ
     * @param details 表示情報
     * @author Masahiro Inoue
     * @since 2026-02-27
     */
    override fun logError(errMessage: String, vararg details: Any) {
        logger.error(errMessage.ifEmpty {
            "${generateErrorMessage(errMessage, getMethodName())}: " +
                    concat(details.toList(), ARGUMENT_DELIMITER).toSingleLineLogString()
        })
    }

    /**
     * ## デバッグログ出力メソッド
     * ### デバッグログ出力用の簡易メソッド
     * @param debugMessage 出力メッセージ
     * @param details メソッド引数群
     * @author Masahiro Inoue
     * @since 2026-03-31
     */
    override fun logDebug(debugMessage: String, vararg details: Any) {
        if (consoleLogLevel.isLoggable(LogLevel.DEBUG)) {
            logger.warn(debugMessage.ifEmpty {
                "${generateDebugMessage(debugMessage, getMethodName())}: " +
                        concat(details.toList(), ARGUMENT_DELIMITER).toSingleLineLogString()
            })
        }
    }

    /**
     * ## エントリーログ出力メソッド
     * ### メソッド実行時のログ出力用の簡易メソッド
     * @param details メソッド引数群
     * @author Masahiro Inoue
     * @since 2026-02-27
     */
    override fun logTraceEntered(vararg details: Any) {
        if (consoleLogLevel.isLoggable(LogLevel.DEBUG)) {
            logger.warn(
                when {
                    details.size == 1 && details[0] is String -> {
                        details[0].toString()
                    }

                    else -> {
                        "${generateTraceMessage(getMethodName())}: ${ENTERED.tag} " +
                                if (details.isEmpty()) {
                                    ""
                                } else {
                                    val detailsSub = details.toList().subList(1, details.size - 1)
                                    ": ${
                                        concat(
                                            detailsSub,
                                            ARGUMENT_DELIMITER
                                        ).toSingleLineLogString()
                                    }"
                                }
                    }
                }
            )
        }
    }

    /**
     * ## イグジットログ出力メソッド
     * ### メソッド完了時のログ出力用の簡易メソッド
     * @param result メソッド実行結果
     * @author Masahiro Inoue
     * @since 2026-02-27
     */
    override fun logTraceExiting(result: Any?) {
        if (consoleLogLevel.isLoggable(LogLevel.DEBUG)) {
            logger.warn(
                if (result is String) {
                    result
                } else {
                    "${generateTraceMessage(getMethodName())}: ${EXITING.tag}" +
                            (result?.let { res -> ": ${res.toSingleLineLogString()}" } ?: "")
                }
            )
        }
    }

    /**
     * ## クローズメソッド
     * ### ロガーのクローズ処理用のメソッド（KSP のロガーはクローズ不要のため、空実装）
     * @author Masahiro Inoue
     * @since 2026-03-13
     */
    override fun close() {
    }
}
