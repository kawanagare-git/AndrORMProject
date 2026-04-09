package jp.pgw.lab78.androrm.ksp.logging

import com.google.devtools.ksp.processing.KSPLogger
import jp.pgw.lab78.androrm.common.Constants.COMMA_SPACE
import jp.pgw.lab78.androrm.common.Constants.Element.METHOD
import jp.pgw.lab78.androrm.common.Constants.Log.*
import jp.pgw.lab78.androrm.common.Constants.ModuleLabel.KSP
import jp.pgw.lab78.androrm.common.logging.LogLevel.*
import jp.pgw.lab78.androrm.ksp.logging.LogUtils.concat
import jp.pgw.lab78.androrm.ksp.logging.LogUtils.getMethodName
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
    constructor(logger: KSPLogger, moduleDir: String, vararg subDirectories: String) : this(
        logger,
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
    override fun info(infoMessage: String, vararg details: Any) {
        loggers.forEach {
            it.info(
                "${KSP.tag} ${INFO.tag}: ${METHOD.tag}: ${getMethodName()} $infoMessage",
                concat(details.toList(), COMMA_SPACE)
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
    override fun infoEntered(infoMessage: String) {
        loggers.forEach {
            it.infoEntered("${KSP.tag} ${INFO.tag}: ${METHOD.tag}: ${getMethodName()} ${ENTERED.tag}")
        }
    }

    /**
     * ## ログ出力メソッド出口
     * ### メソッド完了時のログ出力用の簡易メソッド
     * @param infoMessage メソッド実行結果の情報メッセージ
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    override fun infoExiting(infoMessage: String) {
        loggers.forEach {
            it.infoExiting("${KSP.tag} ${INFO.tag}: ${METHOD.tag}: ${getMethodName()} ${EXITING.tag}")
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
    override fun warning(warnMessage: String, vararg details: Any) {
        loggers.forEach {
            it.warning(
                "${KSP.tag} ${WARN.tag}: ${METHOD.tag}: ${getMethodName()} $warnMessage",
                concat(details.toList(), COMMA_SPACE)
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
    override fun error(errMessage: String, vararg details: Any) {
        loggers.forEach {
            it.error(
                "${KSP.tag} ${ERROR.tag}: ${METHOD.tag}: ${getMethodName()} $errMessage",
                concat(details.toList(), COMMA_SPACE)
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
    override fun debug(debugMessage: String, vararg details: Any) {
        loggers.forEach {
            it.debug(
                "${KSP.tag} ${DEBUG.tag}: ${METHOD.tag}: ${getMethodName()} $debugMessage",
                concat(details.toList(), COMMA_SPACE)
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
    override fun traceEntered(vararg details: Any) {
        loggers.forEach {
            it.traceEntered(
                "${KSP.tag} ${TRACE.tag}: ${METHOD.tag}: ${getMethodName()} ${ENTERED.tag}",
                concat(details.toList(), COMMA_SPACE)
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
    override fun traceExiting(result: Any?) {
        loggers.forEach {
            it.traceExiting("${KSP.tag} ${TRACE.tag}: ${METHOD.tag}: ${getMethodName()} ${EXITING.tag} ${result?.toString() ?: ""}")
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