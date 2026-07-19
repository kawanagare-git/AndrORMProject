package jp.pgw.lab78.androrm.common.logging.interfaces

import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING

/**
 * ## ロガー共通インターフェース
 * ### ログの出力先に依存せず、情報、警告、エラー、デバッグおよびトレースを記録するための共通契約を定義する
 * ### 実装クラスは使用している出力先を解放できるよう、AutoCloseable の close 契約にも従う
 * @author Masahiro Inoue
 * @since 2025-09-01
 */
interface LoggerLike : AutoCloseable {
    /**
     * ## 情報ログ出力
     * ### 通常の処理状況を表すメッセージと詳細情報を出力する
     * @param infoMessage 情報ログの本文
     * @param details ログへ付加する詳細情報
     * @author Masahiro Inoue
     * @since 2025-09-01
     */
    fun logInfo(infoMessage: String, vararg details: Any)

    /**
     * ## 情報ログによる処理開始出力
     * ### 処理へ入ったことを情報レベルで出力する
     * @param infoMessage 処理開始時に付加するメッセージ
     * @author Masahiro Inoue
     * @since 2025-09-01
     */
    fun logInfoEntered(infoMessage: String = EMPTY_STRING)

    /**
     * ## 情報ログによる処理終了出力
     * ### 処理から出ることを情報レベルで出力する
     * @param infoMessage 処理終了時に付加するメッセージ
     * @author Masahiro Inoue
     * @since 2025-09-01
     */
    fun logInfoExiting(infoMessage: String = EMPTY_STRING)

    /**
     * ## 警告ログ出力
     * ### 処理を継続できる注意事項と詳細情報を出力する
     * @param warnMessage 警告ログの本文
     * @param details ログへ付加する詳細情報
     * @author Masahiro Inoue
     * @since 2025-09-01
     */
    fun logWarning(warnMessage: String, vararg details: Any)

    /**
     * ## エラーログ出力
     * ### 処理上の異常を表すメッセージと詳細情報を出力する
     * @param errMessage エラーログの本文
     * @param details ログへ付加する詳細情報
     * @author Masahiro Inoue
     * @since 2025-09-01
     */
    fun logError(errMessage: String, vararg details: Any)

    /**
     * ## デバッグログ出力
     * ### 動作確認に使用するメッセージと詳細情報を出力する
     * @param debugMessage デバッグログの本文
     * @param details ログへ付加する詳細情報
     * @author Masahiro Inoue
     * @since 2025-09-01
     */
    fun logDebug(debugMessage: String, vararg details: Any)

    /**
     * ## トレース開始ログ出力
     * ### 処理へ入ったことと呼び出し時の詳細情報をトレースレベルで出力する
     * @param details ログへ付加する引数などの詳細情報
     * @author Masahiro Inoue
     * @since 2025-09-01
     */
    fun logTraceEntered(vararg details: Any)

    /**
     * ## トレース終了ログ出力
     * ### 処理から出ることと処理結果をトレースレベルで出力する
     * @param result 処理結果。結果がない場合は null
     * @author Masahiro Inoue
     * @since 2025-09-01
     */
    fun logTraceExiting(result: Any? = null)
}