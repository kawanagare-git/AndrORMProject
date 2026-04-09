package jp.pgw.lab78.androrm.ksp.logging

import com.google.devtools.ksp.processing.KSPLogger
import jp.pgw.lab78.androrm.common.Constants.COMMA_SPACE
import jp.pgw.lab78.androrm.common.Constants.Element.METHOD
import jp.pgw.lab78.androrm.common.Constants.Log.*
import jp.pgw.lab78.androrm.common.Constants.ModuleLabel.KSP
import jp.pgw.lab78.androrm.common.logging.LogLevel.*
import jp.pgw.lab78.androrm.ksp.logging.LogUtils.concat
import jp.pgw.lab78.androrm.ksp.logging.LogUtils.getMethodName

/**
 * ## KSP ロガー委譲クラス
 * ### KSP のロガー機能を LoggerLike インターフェースに適合させるためのクラス
 * @param logger KSP のロガーインスタンス
 * @author Masahiro Inoue
 * @since 2026-02-27
 */
class KspDelegatingLogger(private val logger: KSPLogger) : LoggerLike {
    /**
     * ## インフォメーションログ出力メソッド
     * ### エラー発生時のログ出力用の簡易メソッド
     * @param infoMessage 出力メッセージ
     * @param details 表示情報
     * @author Masahiro Inoue
     * @since 2026-02-27
     */
    override fun info(infoMessage: String, vararg details: Any) {
        val infoMessageDetails = concat(details.toList(), COMMA_SPACE)
        logger.info(infoMessage.ifEmpty {
            "${KSP.tag} ${INFO.tag}: ${METHOD.tag}: ${getMethodName()} $infoMessageDetails"
        })
    }

    /**
     * ## エントリーログ出力メソッド
     * ### メソッド実行時のログ出力用の簡易メソッド
     * @param args メソッド引数群
     * @author Masahiro Inoue
     * @since 2026-02-27
     */
    override fun infoEntered(infoMessage: String) =
        logger.info(infoMessage.ifEmpty { "${KSP.tag} ${INFO.tag}: ${METHOD.tag}: ${getMethodName()} ${ENTERED.tag}" })

    /**
     * ## イグジットログ出力メソッド
     * ### メソッド完了時のログ出力用の簡易メソッド
     * @param result メソッド実行結果
     * @author Masahiro Inoue
     * @since 2026-02-27
     */
    override fun infoExiting(infoMessage: String) {
        logger.info(infoMessage.ifEmpty { "${KSP.tag} ${INFO.tag}: ${METHOD.tag}: ${getMethodName()} ${EXITING.tag}" })
    }

    /**
     * ## ワーニングログ出力メソッド
     * ### エラー発生時のログ出力用の簡易メソッド
     * @param warnMessage 出力メッセージ
     * @param details 表示情報
     * @author Masahiro Inoue
     * @since 2026-02-27
     */
    override fun warning(warnMessage: String, vararg details: Any) {
        val warnMessageDetails = concat(details.toList(), COMMA_SPACE)
        logger.warn(warnMessage.ifEmpty {
            "${KSP.tag} ${WARN.tag}: ${METHOD.tag}: ${getMethodName()} $warnMessage $warnMessageDetails"
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
    override fun error(errMessage: String, vararg details: Any) {
        val errMessageDetails = concat(details.toList(), COMMA_SPACE)
        logger.error(errMessage.ifEmpty {
            "${KSP.tag} ${ERROR.tag}: ${METHOD.tag}: ${getMethodName()} $errMessageDetails"
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
    override fun debug(debugMessage: String, vararg details: Any) {
        val errMessageDetails = concat(details.toList(), COMMA_SPACE)
        logger.warn(debugMessage.ifEmpty {
            "${KSP.tag} ${ERROR.tag}: ${METHOD.tag}: ${getMethodName()} $errMessageDetails"
        })
    }

    /**
     * ## エントリーログ出力メソッド
     * ### メソッド実行時のログ出力用の簡易メソッド
     * @param details メソッド引数群
     * @author Masahiro Inoue
     * @since 2026-02-27
     */
    override fun traceEntered(vararg details: Any) {
        val traceDetails = concat(details.toList(), COMMA_SPACE)
        logger.warn(traceDetails.ifEmpty {
            "${KSP.tag} ${TRACE.tag}: ${ENTERED.tag} ${METHOD.tag}: ${getMethodName()}"
        })
    }

    /**
     * ## イグジットログ出力メソッド
     * ### メソッド完了時のログ出力用の簡易メソッド
     * @param result メソッド実行結果
     * @author Masahiro Inoue
     * @since 2026-02-27
     */
    override fun traceExiting(result: Any?) {
        logger.warn(
            result.toString()
                .ifEmpty { "${KSP.tag} ${TRACE.tag}: ${EXITING.tag} ${METHOD.tag}: ${getMethodName()}" })
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
