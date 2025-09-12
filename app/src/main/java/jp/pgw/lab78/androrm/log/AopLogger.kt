package jp.pgw.lab78.androrm.log

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory

/**
 * ## AOP ロガークラス
 * ### AndrORM 内部のメソッド実行をログ出力する
 * @author Masahiro Inoue
 * @since 2025-09-12
 */
@Aspect
class AopLogger {
    /** ログインスタンス*/
    private val selfLogger = LoggerFactory.getLogger(this::class.java)

    init {
        println("ログ出力準備：$selfLogger")
    }

    /**
     * ## トレースログ出力アドバイス
     * ### jp.pgw.lab78.androrm.database パッケージ以下の全てのメソッド実行前後にログ出力を行う
     * @param pjp ProceedingJoinPoint
     * @return メソッド実行結果
     * @throws Throwable メソッド実行例外
     * @author Masahiro Inoue
     * @since 2025-09-12
     */
    @Around("execution(* jp.pgw.lab78.androrm.database..*(..))")
    @Throws(Throwable::class)
    fun traceAdvice(pjp: ProceedingJoinPoint): Any? {
        val targetLogger = LoggerFactory.getLogger(pjp.target.javaClass)
        val beforeMessage = "メソッド開始: ${pjp.signature.name} / ${pjp.args.joinToString(", ")}"
        targetLogger.trace(beforeMessage)
        println(beforeMessage)
        val result = pjp.proceed(pjp.args)
        val afterMessage = "メソッド終了: ${pjp.signature.name} / $result"
        targetLogger.trace(afterMessage)
        println(afterMessage)
        return result
    }

    /**
     * ## インフォメーションログ出力アドバイス
     * ### @LogTarget アノテーションが付与されたメソッド実行前後にログ出力を行う
     * @param pjp ProceedingJoinPoint
     * @return メソッド実行結果
     * @throws Throwable メソッド実行例外
     * @author Masahiro Inoue
     * @since 2025-09-12
     */
    @Around("execution(* jp.pgw.lab78.androrm..*(..)) && @annotation(jp.pgw.lab78.androrm.log.annotations.LogTarget)")
    @Throws(Throwable::class)
    fun infoAdvice(pjp: ProceedingJoinPoint): Any? {
        val targetLogger = LoggerFactory.getLogger(pjp.target.javaClass)
        targetLogger?.info("メソッド開始: ${pjp.signature.name}")
        val result = pjp.proceed(pjp.args)
        targetLogger?.info("メソッド終了: ${pjp.signature.name}")
        return result
    }
}