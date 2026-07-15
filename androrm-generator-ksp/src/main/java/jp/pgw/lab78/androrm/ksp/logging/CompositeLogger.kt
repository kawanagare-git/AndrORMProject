package jp.pgw.lab78.androrm.ksp.logging

import com.google.devtools.ksp.processing.KSPLogger
import jp.pgw.lab78.androrm.common.Constants.ARGUMENT_DELIMITER
import jp.pgw.lab78.androrm.common.Constants.LogPhase.*
import jp.pgw.lab78.androrm.common.Constants.ModuleLabel.KSP
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
import java.nio.file.Path

/**
 * ## 複合ロガー
 * ### KSP ロガーとファイルロガーの両方にログ出力するためのクラス
 * @param logger KSP のロガーインスタンス
 * @param fullFilePath ログを出力するファイルのパス
 * @author Masahiro Inoue
 * @since 2026-03-10
 */
class CompositeLogger(
    private val logger: KSPLogger,
    private val consoleLogLevel: LogLevel = LogLevel.WARN,
    private val fullFilePath: Path
) : LoggerLike {
    /**
     * ## コンストラクタ
     * ### ログファイルのパスをサブディレクトリから構築するためのコンストラクタ
     * @param logger KSP のロガーインスタンス
     * @param subDirectories ログファイルのサブディレクトリ群
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    constructor(
        logger: KSPLogger,
        consoleLogLevel: LogLevel,
        moduleDir: String,
        vararg subDirectories: String
    ) : this(
        logger, consoleLogLevel,
        Path.of(moduleDir, *subDirectories, KSP.logFilename)
            .also { logger.warn("output path = ${it.toAbsolutePath()}") }
    )

    /** ロガーのリスト。KSP ロガーとファイルロガーの両方を保持する */
    private val loggers: List<LoggerLike> = listOf(
        KspDelegatingLogger(logger),
        FileDelegatingLogger(fullFilePath)
    )

    /**
     * ## インフォメーションログ出力メソッド
     * ### エラー発生時のログ出力用の簡易メソッド
     * @param infoMessage 出力メッセージ
     * @param details 表示情報
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    override fun logInfo(infoMessage: String, vararg details: Any) {
        val methodName = getMethodName()
        loggers.forEach {
            it.logInfo(
                generateInfoMessage(infoMessage, methodName),
                concat(details.toList(), ARGUMENT_DELIMITER).toSingleLineLogString()
            )
        }
    }

    /**
     * ## ログ出力メソッド入口
     * ### メソッド実行時のログ出力用の簡易メソッド
     * @param infoMessage メソッド実行前の情報メッセージ
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    override fun logInfoEntered(infoMessage: String) {
        val methodName = getMethodName()
        loggers.forEach {
            it.logInfoEntered("${generateInfoMessage(infoMessage, methodName)}: ${ENTERED.tag}")
        }
    }

    /**
     * ## ログ出力メソッド出口
     * ### メソッド完了時のログ出力用の簡易メソッド
     * @param infoMessage メソッド実行結果の情報メッセージ
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    override fun logInfoExiting(infoMessage: String) {
        val methodName = getMethodName()
        loggers.forEach {
            it.logInfoExiting("${generateInfoMessage(infoMessage, methodName)}: ${EXITING.tag}")
        }
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
        val methodName = getMethodName()
        loggers.forEach {
            it.logWarning(
                generateWarningMessage(warnMessage, methodName),
                concat(details.toList(), ARGUMENT_DELIMITER).toSingleLineLogString()
            )
        }
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
        val methodName = getMethodName()
        loggers.forEach {
            it.logError(
                generateErrorMessage(errMessage, methodName),
                concat(details.toList(), ARGUMENT_DELIMITER).toSingleLineLogString()
            )
        }
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
        val methodName = getMethodName()
        loggers.forEach {
            it.logDebug(
                generateDebugMessage(debugMessage, methodName),
                concat(details.toList(), ARGUMENT_DELIMITER).toSingleLineLogString()
            )
        }
    }

    /**
     * ## トレースエントリーログ出力メソッド
     * ### メソッド実行時のトレースログ出力用の簡易メソッド
     * @param details メソッド引数群
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    override fun logTraceEntered(vararg details: Any) {
        loggers.forEach {
            it.logTraceEntered(
                "${generateTraceMessage(getMethodName())}: ${ENTERED.tag}" +
                        if (details.isEmpty()) {
                            ""
                        } else {
                            ": ${
                                concat(details.toList(), ARGUMENT_DELIMITER)
                                    .toSingleLineLogString()
                            }"
                        }
            )
        }
    }

    /**
     * ## トレースイグジットログ出力メソッド
     * ### メソッド完了時のトレースログ出力用の簡易メソッド
     * @param result メソッド実行結果
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    override fun logTraceExiting(result: Any?) {
        val methodName = getMethodName()
        loggers.forEach { it ->
            it.logTraceExiting(
                "${generateTraceMessage(methodName)}: ${EXITING.tag}" +
                        (result?.let { res -> ": ${res.toSingleLineLogString()}" } ?: "")
            )
        }
    }

    /**
     * ## クローズメソッド
     * ### ロガーが AutoCloseable を実装している場合はクローズする
     * @author Masahiro Inoue
     * @since 2026-03-13
     */
    override fun close() {
        loggers.forEach { logger ->
            logger.close()
        }
    }
}