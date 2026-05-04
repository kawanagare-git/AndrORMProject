package jp.pgw.lab78.androrm.ksp.logging

import jp.pgw.lab78.androrm.common.Constants.Element.METHOD
import jp.pgw.lab78.androrm.common.Constants.ModuleLabel.KSP
import jp.pgw.lab78.androrm.common.logging.LogLevel.*
import jp.pgw.lab78.androrm.ksp.logging.LogUtils.toSingleLineLogString

object DefaultLogMessageGenerator {
    /**
     * ## インフォメーションログメッセージ生成
     * ## ログレベル INFO のログメッセージを生成するためのユーティリティメソッド
     * @param infoMessage 出力メッセージ
     * @return ログメッセージ
     * @author Masahiro Inoue
     * @since 2026-05-01
     */
    fun generateInfoMessage(infoMessage: String, methodName: String) =
        "${KSP.tag} ${INFO.tag}: ${METHOD.tag}: $methodName: ${infoMessage.toSingleLineLogString()}"

    /**
     * ## ワーニングログメッセージ生成
     * ## ログレベル WARNING のログメッセージを生成するためのユーティリティメソッド
     * @param warnMessage 出力メッセージ
     * @return ログメッセージ
     * @author Masahiro Inoue
     * @since 2026-05-01
     */
    fun generateWarningMessage(warnMessage: String, methodName: String) =
        "${KSP.tag} ${WARN.tag}: ${METHOD.tag}: $methodName: ${warnMessage.toSingleLineLogString()}"

    /**
     * ## エラーログメッセージ生成
     * ## ログレベル ERROR のログメッセージを生成するためのユーティリティメソッド
     * @param errMessage 出力メッセージ
     * @return ログメッセージ
     * @author Masahiro Inoue
     * @since 2026-05-01
     */
    fun generateErrorMessage(errMessage: String, methodName: String) =
        "${KSP.tag} ${ERROR.tag}: ${METHOD.tag}: $methodName: ${errMessage.toSingleLineLogString()}"

    /**
     * ## デバッグログメッセージ生成
     * ## ログレベル DEBUG のログメッセージを生成するためのユーティリティメソッド
     * @param debugMessage 出力メッセージ
     * @return ログメッセージ
     * @author Masahiro Inoue
     * @since 2026-05-01
     */
    fun generateDebugMessage(debugMessage: String, methodName: String) =
        "${KSP.tag} ${DEBUG.tag}: ${METHOD.tag}: $methodName: ${debugMessage.toSingleLineLogString()}"

    /**
     * ## トレースログメッセージ生成
     * ## ログレベル TRACE のログメッセージを生成するためのユーティリティメソッド
     * @return ログメッセージ
     * @author Masahiro Inoue
     * @since 2026-05-01
     */
    fun generateTraceMessage(methodName: String) =
        "${KSP.tag} ${TRACE.tag}: ${METHOD.tag}: $methodName"
}