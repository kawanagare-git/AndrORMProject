package jp.pgw.lab78.androrm.ksp.logging

import com.google.devtools.ksp.processing.KSPLogger
import jp.pgw.lab78.androrm.common.logging.interfaces.LoggerLike
import jp.pgw.lab78.androrm.ksp.PropsProcessor.Companion.MAX_DEPTH
import jp.pgw.lab78.androrm.ksp.PropsProcessor.Companion.MAX_ELEMENTS_AT_MAX_DEPTH

class KspDelegatingLogger(private val logger: KSPLogger) : LoggerLike{

    /** スタックトレースでスキップするための、このクラスのFQCN（完全修飾クラス名） */
    private val fqcn = KspDelegatingLogger::class.qualifiedName

    /**
     * ## インフォメーションログ出力メソッド
     * ### エラー発生時のログ出力用の簡易メソッド
     * @param infoMessage 出力メッセージ
     * @param args 表示情報
     */
    override fun info(infoMessage: String, vararg args: Any) {
        val argsString = if (args.isNotEmpty()) {
            " args: '${args.joinToString(", ") { stringifyForLog(it) }}'"
        } else ""
        logger.info("[AndrORM-KSP] INFO: method: ${getMethodName()} $infoMessage$argsString")
    }

    /**
     * ## エントリーログ出力メソッド
     * ### メソッド実行時のログ出力用の簡易メソッド
     * @param args メソッド引数群
     */
    override fun infoEntered(vararg args: Any) {
        val argsString = if (args.isNotEmpty()) {
            " args: '${args.joinToString(", ") { stringifyForLog(it) }}'"
        } else ""
        logger.warn("[AndrORM-KSP] INFO: Entered method: ${getMethodName()}$argsString")
    }

    /**
     * ## イグジットログ出力メソッド
     * ### メソッド完了時のログ出力用の簡易メソッド
     * @param result メソッド実行結果
     */
    override fun infoExiting(result: Any?) {
        logger.warn("[AndrORM-KSP] INFO: Exiting method: ${getMethodName()} " +
                (result?.let { "'${it}'" } ?:""))
    }

    /**
     * ## ワーニングログ出力メソッド
     * ### エラー発生時のログ出力用の簡易メソッド
     * @param warnMessage 出力メッセージ
     * @param args 表示情報
     */
    override fun warning(warnMessage: String, vararg args: Any) {
        val argsString = if (args.isNotEmpty()) {
            " args: '${args.joinToString(", ") { stringifyForLog(it) }}'"
        } else ""
        logger.warn("[AndrORM-KSP] WARNING: method: ${getMethodName()} $warnMessage$argsString")
    }

    /**
     * ## エラーログ出力メソッド
     * ### エラー発生時のログ出力用の簡易メソッド
     * @param errMessage 出力メッセージ
     * @param args 表示情報
     */
    override fun error(errMessage: String, vararg args: Any) {
        val argsString = if (args.isNotEmpty()) {
            " args: '${args.joinToString(", ") { stringifyForLog(it) }}'"
        } else ""
        logger.error("[AndrORM-KSP] ERROR: method: ${getMethodName()} $errMessage$argsString")
    }

    /**
     * ## 呼び出し元情報取得メソッド
     * ### ロガー・クラスに属さない、最初のスタックフレームを見つけます。
     * ### コンパイラが生成したメソッド名をチェックするよりも堅牢な方法です。
     * @return 呼び出し元のクラスとメソッド名をフォーマットした文字列。
     */
    private fun getMethodName(): String {
        val stackTrace = Throwable().stackTrace
        var index = stackTrace.indexOfFirst { it.className != fqcn } + 1
        index = if (stackTrace[index].methodName.endsWith("\$default")) index + 1 else index
        return stackTrace[index].methodName
    }

    /**
     * ## オブジェクト文字列化メソッド
     * ### ログ出力するオブジェクトを文字列化
     * @param value ログ出力するオブジェクト
     * @param depth 再帰呼び出しの深さ
     * @return オブジェクト文字列
     */
    private fun stringifyForLog(value: Any?, depth: Int = 0): String {
        if (depth > MAX_DEPTH) return "..."
        // 深さに応じて表示要素数を制限
        val maxElements = ((MAX_ELEMENTS_AT_MAX_DEPTH * (MAX_DEPTH - depth + 1)) / MAX_DEPTH).coerceAtLeast(1)
        return when (value) {
            null -> "null"
            is Sequence<*> -> stringifyForLog(value.toList(), depth)
            is Array<*> -> stringifyForLog(value.toList(), depth)
            is Iterable<*> -> value.take(maxElements)
                .map { stringifyForLog(it, depth + 1) }
                .joinToString(", ", "[", if (value.count() > maxElements) ", ..." else "]")
            is Map<*, *> -> {
                val entries = value.entries.take(maxElements)
                entries.joinToString(", ", "{", if (value.size > maxElements) ", ...}" else "}") {
                    "${stringifyForLog(it.key, depth + 1)}:${stringifyForLog(it.value, depth + 1)}"
                }
            }
            else -> value.toString()
        }
    }

}