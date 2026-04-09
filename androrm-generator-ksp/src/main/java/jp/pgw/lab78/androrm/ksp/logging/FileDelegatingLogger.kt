package jp.pgw.lab78.androrm.ksp.logging

import jp.pgw.lab78.androrm.common.Constants.COMMA_SPACE
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
    override fun info(infoMessage: String, vararg details: Any) {
        append("$infoMessage, ${details.joinToString(COMMA_SPACE)}")
    }

    /**
     * ## エントリーログ出力メソッド
     * ### メソッド実行時のログ出力用の簡易メソッド
     * @param infoMessage メソッド実行前の情報メッセージ
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    override fun infoEntered(infoMessage: String) {
        append(infoMessage)
    }

    /**
     * ## イグジットログ出力メソッド
     * ### メソッド完了時のログ出力用の簡易メソッド
     * @param infoMessage メソッド実行後の情報メッセージ
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    override fun infoExiting(infoMessage: String) {
        append(infoMessage)
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
        append("$warnMessage, ${details.joinToString(COMMA_SPACE)}")
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
        append("$errMessage, ${details.joinToString(COMMA_SPACE)}")
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
        append("$debugMessage, ${details.joinToString(COMMA_SPACE)}")
    }

    /**
     * ## トレースエントリーログ出力メソッド
     * ### メソッド実行時のトレースログ出力用の簡易メソッド
     * @param details メソッド引数群
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    override fun traceEntered(vararg details: Any) {
        append(details.joinToString(COMMA_SPACE))
    }

    /**
     * ## トレースイグジットログ出力メソッド
     * ### メソッド完了時のトレースログ出力用の簡易メソッド
     * @param result メソッド実行結果
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    override fun traceExiting(result: Any?) {
        append(result.toString())
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