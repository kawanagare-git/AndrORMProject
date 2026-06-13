package jp.pgw.lab78.androrm.ksp.logging

object LogUtils {
    /** スタックトレースでスキップするメソッド名のサフィックス */
    private val SKIP_METHOD_SUFFIXES = setOf("\$default", "invoke", "invokeSuspend")

    /** スタックトレースでスキップするメソッド名 */
    private val SKIP_METHOD_NAMES = setOf(
        "traceEntered",
        "traceExiting",
        "infoEntered",
        "infoExiting",
        "info",
        "warning",
        "error",
        "debug",
        "logInfo",
        "logInfoEntered",
        "logInfoExiting",
        "logWarning",
        "logError",
        "logDebug",
        "logTraceEntered",
        "logTraceExiting",
    )

    /** ログ再起上限 */
    private const val MAX_DEPTH = 4

    /** 最新階層表示要素数 */
    private const val MAX_ELEMENTS_AT_MAX_DEPTH = 4

    /** スタックトレースでスキップするための、このクラスのFQCN（完全修飾クラス名） */
    private val packageName = "${LogUtils::class.java.packageName}."

    /**
     * ## 引数文字列化メソッド
     * ### ログ出力する引数を文字列化
     * @param args ログ出力する引数のリスト
     * @param separator 引数を区切るセパレータ
     * @return 引数文字列
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    fun concat(args: List<Any?>, separator: String) =
        args.map(::normalizeDetail)
            .filter { it.isNotEmpty() }
            .joinToString(separator) { stringifyForLog(it) }

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
        val maxElements =
            ((MAX_ELEMENTS_AT_MAX_DEPTH * (MAX_DEPTH - depth + 1)) / MAX_DEPTH).coerceAtLeast(1)
        return when (value) {
            null -> "null"
            is Sequence<*> -> stringifyForLog(value.toList(), depth)
            is Array<*> -> stringifyForLog(value.toList(), depth)
            is Iterable<*> -> value.take(maxElements).map { stringifyForLog(it, depth + 1) }
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

    /**
     * ## 呼び出し元情報取得メソッド
     * ### ロガー・クラスに属さない、最初のスタックフレームを見つけます。
     * ### コンパイラが生成したメソッド名をチェックするよりも堅牢な方法です。
     * @return 呼び出し元のクラスとメソッド名をフォーマットした文字列。
     */
    fun getMethodName(): String {
        val stackTrace = Throwable().stackTrace
        val startIndex = stackTrace.indexOfLast { it.className.startsWith(packageName) } + 1
        val endIndex = stackTrace.lastIndex
        for (index in startIndex..endIndex) {
            val methodName = stackTrace[index].methodName
            if (methodName in SKIP_METHOD_NAMES
                || SKIP_METHOD_SUFFIXES.any { methodName.endsWith(it) }
            ) {
                continue
            }
            return methodName
        }
        return stackTrace[endIndex].methodName
    }

    /**
     * ## 詳細情報正規化メソッド
     * ### ログ出力する詳細情報を正規化
     * @param args ログ出力する詳細情報
     * @return 正規化された詳細情報文字列
     * @author Masahiro Inoue
     * @since 2026-05-01
     */
    fun normalizeDetail(args: Any?): String = args.toSingleLineLogString()

    /**
     * ## 1 行ログ文字列化メソッド
     * ### ログ出力する文字列を 1 行に整形する
     * @receiver ログ出力する文字列
     * @return 1 行に整形されたログ文字列
     * @author Masahiro Inoue
     * @since 2026-05-01
     */
    fun Any?.toSingleLineLogString(): String =
        this.toString()
            .replace("\r\n", " ")
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replace(Regex("\\s{2,}"), " ")
            .trim()
}