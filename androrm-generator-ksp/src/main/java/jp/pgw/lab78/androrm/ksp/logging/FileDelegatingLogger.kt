package jp.pgw.lab78.androrm.ksp.logging

import jp.pgw.lab78.androrm.common.Constants.COMMA_SPACE
import jp.pgw.lab78.androrm.common.Constants.Log.*
import jp.pgw.lab78.androrm.ksp.logging.DefaultLogMessageGenerator.generateDebugMessage
import jp.pgw.lab78.androrm.ksp.logging.DefaultLogMessageGenerator.generateErrorMessage
import jp.pgw.lab78.androrm.ksp.logging.DefaultLogMessageGenerator.generateInfoMessage
import jp.pgw.lab78.androrm.ksp.logging.DefaultLogMessageGenerator.generateTraceMessage
import jp.pgw.lab78.androrm.ksp.logging.DefaultLogMessageGenerator.generateWarningMessage
import jp.pgw.lab78.androrm.ksp.logging.LogUtils.concat
import jp.pgw.lab78.androrm.ksp.logging.LogUtils.getMethodName
import jp.pgw.lab78.androrm.ksp.logging.LogUtils.toSingleLineLogString
import java.io.BufferedWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class FileDelegatingLogger(
    private val logFilePath: Path
) : LoggerLike, AutoCloseable {
    /** 排他制御用 */
    private val lock = Any()

    /** ログファイルへの書き込み用バッファードライター */
    private val writerDelegate = lazy {
        Files.createDirectories(logFilePath.parent)
        Files.newBufferedWriter(
            logFilePath,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )
    }
    private val writer: BufferedWriter by writerDelegate

    /**
     * ## ログファイル追記メソッド
     * @param message 出力メッセージ
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    private fun append(message: String) {
        synchronized(lock) {
            writer.appendLine(message)
            writer.flush()
        }
    }

    /**
     * ## インフォメーションログ出力メソッド
     * ### エラー発生時のログ出力用の簡易メソッド
     * @param infoMessage 出力メッセージ
     * @param details 表示情報
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    override fun logInfo(infoMessage: String, vararg details: Any) {
        append(infoMessage.ifEmpty {
            "${generateInfoMessage(infoMessage, getMethodName())}: " +
                    concat(details.toList(), COMMA_SPACE).toSingleLineLogString()
        })
    }

    /**
     * ## エントリーログ出力メソッド
     * ### メソッド実行時のログ出力用の簡易メソッド
     * @param infoMessage メソッド実行前の情報メッセージ
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    override fun logInfoEntered(infoMessage: String) {
        append(infoMessage.ifEmpty {
            "${generateInfoMessage(infoMessage, getMethodName())}: ${ENTERED.tag}"
        })
    }

    /**
     * ## イグジットログ出力メソッド
     * ### メソッド完了時のログ出力用の簡易メソッド
     * @param infoMessage メソッド実行後の情報メッセージ
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    override fun logInfoExiting(infoMessage: String) {
        append(infoMessage.ifEmpty {
            "${generateInfoMessage(infoMessage, getMethodName())}: ${EXITING.tag}"
        })
    }

    /**
     * ## ワーニングログ出力メソッド
     * ### エラー発生時のログ出力用の簡易メソッド
     * @param warnMessage 出力メッセージ
     * @param details 表示情報
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    override fun logWarning(warnMessage: String, vararg details: Any) {
        append(warnMessage.ifEmpty {
            "${generateWarningMessage(warnMessage, getMethodName())}: " +
                    concat(details.toList(), COMMA_SPACE).toSingleLineLogString()
        })
    }

    /**
     * ## エラーログ出力メソッド
     * ### エラー発生時のログ出力用の簡易メソッド
     * @param errMessage 出力メッセージ
     * @param details 表示情報
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    override fun logError(errMessage: String, vararg details: Any) {
        append(errMessage.ifEmpty {
            "${generateErrorMessage(errMessage, getMethodName())}: " +
                    concat(details.toList(), COMMA_SPACE).toSingleLineLogString()
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
        append(debugMessage.ifEmpty {
            "${generateDebugMessage(debugMessage, getMethodName())}: " +
                    concat(details.toList(), COMMA_SPACE).toSingleLineLogString()
        })
    }

    /**
     * ## トレースエントリーログ出力メソッド
     * ### メソッド実行時のトレースログ出力用の簡易メソッド
     * @param details メソッド引数群
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    override fun logTraceEntered(vararg details: Any) {
        append(
            if (details.size == 1 && details[0] is String) {
                details[0].toString()
            } else {
                "${generateTraceMessage(getMethodName())}: ${ENTERED.tag}" +
                        if (details.isEmpty()) {
                            ""
                        } else {
                            val detailsSub = details.toList().subList(1, details.size - 1)
                            ": ${concat(detailsSub, COMMA_SPACE).toSingleLineLogString()}"
                        }
            }
        )
    }

    /**
     * ## トレースイグジットログ出力メソッド
     * ### メソッド完了時のトレースログ出力用の簡易メソッド
     * @param result メソッド実行結果
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    override fun logTraceExiting(result: Any?) {
        append(
            if (result is String) {
                result
            } else {
                "${generateTraceMessage(getMethodName())}: ${EXITING.tag}" +
                        (result?.let { res -> ": ${res.toSingleLineLogString()}" } ?: "")
            }
        )
    }

    /**
     * ## クローズメソッド
     * ### ロガーのクローズ処理を行うメソッド
     * @author Masahiro Inoue
     * @since 2026-03-13
     */
    override fun close() {
        synchronized(lock) {
            if (writerDelegate.isInitialized()) {
                writer.flush()
                writer.close()
            }
        }
    }
}