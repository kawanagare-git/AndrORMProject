package jp.pgw.lab78.androrm.aop

import jp.pgw.lab78.androrm.common.Constants.ARGUMENT_DELIMITER
import jp.pgw.lab78.androrm.common.Constants.LogPhase
import jp.pgw.lab78.androrm.common.Constants.LogPhase.*
import jp.pgw.lab78.androrm.common.Constants.NO_ARGUMENTS
import jp.pgw.lab78.androrm.common.Constants.NULL_STRING
import jp.pgw.lab78.androrm.common.Constants.TERTIARY_DELIMITER
import jp.pgw.lab78.androrm.common.Constants.UNIT_RETURN
import jp.pgw.lab78.androrm.common.logging.LogLevel
import jp.pgw.lab78.androrm.common.logging.LogLevel.*
import jp.pgw.lab78.androrm.common.logging.LogScope.APP
import jp.pgw.lab78.androrm.common.logging.aop.InfoLog
import jp.pgw.lab78.androrm.common.logging.aop.TraceLog
import jp.pgw.lab78.shared.library.Utils.isNull
import jp.pgw.lab78.shared.library.Utils.isUnit
import jp.pgw.lab78.shared.library.Utils.isVoid
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import java.util.logging.Logger

/**
 * ## AOP ログ出力
 * ### AOP とアノテーションを使用してログ出力を実装
 * @author Masahiro Inoue
 * @since 2026-05-07
 */
@Aspect
class TraceAspect {

    /**
     * AOPログ出力で共有する定数とロガーを保持する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-07
     */
    companion object {
        /** スタックトレース表示行数 */
        private const val MAX_STACK_TRACE_LINES = 10

        /** ログインスタンス */
        private val logger: Logger by lazy {
            APP.create(
                minLogLevel = TRACE,
                logFileSuffix = "AOP"
            )
        }
    }

    /**
     * ## AOP ログ出力処理
     * ### @InfoLog または @TraceLog が付与されたメソッドに対しログを出力
     * @param joinPoint AOP が保持するインスタンス情報
     * @author Masahiro Inoue
     * @since 2026-05-07
     */
    @Around(
        "execution(* *(..)) && (" +
                "@annotation(jp.pgw.lab78.androrm.common.logging.aop.InfoLog) || " +
                "@annotation(jp.pgw.lab78.androrm.common.logging.aop.TraceLog)" +
                ") && " +
                "!within(jp.pgw.lab78.androrm.aop..*) && " +
                "!within(jp.pgw.lab78.androrm..*AjcClosure*)"
    )
    fun aroundLog(joinPoint: ProceedingJoinPoint): Any? {
        // メソッド情報の取得
        val methodSignature = joinPoint.signature as MethodSignature
        val method = methodSignature.method
        // メソッドに付与されているアノテーションの検査
        val hasTraceLog = method.isAnnotationPresent(TraceLog::class.java)
        val hasInfoLog = method.isAnnotationPresent(InfoLog::class.java)
        // ログ出力の分岐
        return when {
            hasTraceLog -> logAround(TRACE, joinPoint)
            hasInfoLog -> logAround(INFO, joinPoint)
            else -> joinPoint.proceed()
        }
    }

    /**
     * ## ログ出力処理
     * ### ログ出力内容をファイルへ出力
     * ### AOP で実施するためメソッド実行後の値を戻す
     * @param logLevel ログレベル
     * @param joinPoint AOP が保持するインスタンス情報
     * @author Masahiro Inoue
     * @since 2026-05-07
     */
    private fun logAround(
        logLevel: LogLevel,
        joinPoint: ProceedingJoinPoint,
    ): Any? {
        // クラス名等の情報取得
        val signature = joinPoint.signature
        val className = signature.declaringTypeName
        val methodName = signature.name
        val methodSignature = signature as MethodSignature
        val returnType = methodSignature.returnType
        // メソッドエンターログ
        logger.info(generateLogMessage(ENTERED, className, methodName))
        if (logLevel == TRACE) {
            logger.finest("$ENTERED args=${formatArgs(joinPoint.args)}")
        }
        // 実行対象メソッド呼び出し
        val result = try {
            joinPoint.proceed()
        } catch (t: Throwable) {
            // メソッド例外ログ
            logger.warning(throwingMessage(className, methodName, t))
            throw t
        }
        // メソッドイグジットログ
        if (logLevel == TRACE) {
            logger.finest(
                "$EXITING return=${
                    when {
                        returnType.isVoid() -> UNIT_RETURN
                        result.isUnit() -> UNIT_RETURN
                        result.isNull() -> NULL_STRING
                        else -> result
                    }
                }"
            )
        }
        logger.info(generateLogMessage(EXITING, className, methodName))
        return result
    }

    /**
     * ## 例外ログ生成
     * ### メソッドで例外が発生したときの情報をログ生成
     * ### スタックトレース出力は10行まで
     * @param className クラス名
     * @param methodName メソッド名
     * @param throwable 例外インスタンス
     * @return 生成されたログメッセージ
     * @author Masahiro Inoue
     * @since 2026-05-07
     */
    private fun throwingMessage(
        className: String, methodName: String, throwable: Throwable
    ): String {
        return buildString {
            append(generateLogMessage(THROWING, className, methodName))
            append(", exception=")
            append(throwable::class.qualifiedName ?: throwable::class.java.name)
            append(", message=")
            append(throwable.message ?: NULL_STRING)
            appendLine()
            append(formatStackTrace(throwable))
        }
    }

    /**
     * ## スタックトレース整形
     * ### スタックトレース出力を10行までに抑える
     * @param throwable 例外インスタンス
     * @return 整形されたログメッセージ
     * @author Masahiro Inoue
     * @since 2026-05-07
     */
    private fun formatStackTrace(
        throwable: Throwable,
    ): String {
        // スタックトレースの取得
        val stackTrace = throwable.stackTrace
        val shownLines = stackTrace.take(MAX_STACK_TRACE_LINES)
        // スタックトレースの生成
        return buildString {
            shownLines.forEach { element ->
                append("\tat ")
                appendLine(element.toString())
            }
            // スタックトレースの内容表示
            val remaining = stackTrace.size - shownLines.size
            if (remaining > 0) {
                append("\t... ")
                append(remaining)
                appendLine(" more")
            }
            throwable.cause?.let { cause ->
                append("Caused by: ")
                append(cause::class.qualifiedName ?: cause::class.java.name)
                append(": ")
                appendLine(cause.message ?: NULL_STRING)
            }
        }.trimEnd()
    }

    /**
     * ## 引数整形
     * ### トレースログ字に呼び出される
     * ### メソッド引数を羅列
     * @param args 引数郡
     * @return 整形された引数文字列
     * @author Masahiro Inoue
     * @since 2026-05-07
     */
    private fun formatArgs(args: Array<Any?>): String {
        return when {
            args.isEmpty() -> NO_ARGUMENTS
            else -> args.joinToString(
                prefix = "[", postfix = "]", separator = ARGUMENT_DELIMITER
            ) { it?.toString() ?: NULL_STRING }
        }
    }

    /**
     * ## ログ基本メッセージ生成
     * ### ログ出力時の基本メッセージを生成
     * @param logPhase ログ出力のフェーズ
     * @param className クラス名
     * @param methodName メソッド名
     * @return ログフェーズ、クラス名、メソッド名を連結したログメッセージ
     * @author Masahiro Inoue
     * @since 2026-05-07
     */
    private fun generateLogMessage(
        logPhase: LogPhase, className: String, methodName: String
    ) =
        "${logPhase.name}$TERTIARY_DELIMITER class=$className${ARGUMENT_DELIMITER}method=$methodName"
}
