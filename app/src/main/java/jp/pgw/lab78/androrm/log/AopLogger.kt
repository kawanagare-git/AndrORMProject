package jp.pgw.lab78.androrm.log

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory

@Aspect
class AopLogger {
    /** ログインスタンス*/
    private val selfLogger = LoggerFactory.getLogger(this::class.java)
    init {
        println("ログ出力準備：$selfLogger")
    }

    @Around("execution(* *(..))")
//    @Around("execution(* jp.pgw.lab78.androrm.database..*(..))")
    @Throws(Throwable::class)
    fun traceAdvice(pjp: ProceedingJoinPoint): Any? {
        val targetLogger = LoggerFactory.getLogger(pjp.target.javaClass)
        val beforeMessage = "メソッド開始: ${pjp.signature.name} / ${pjp.args.joinToString( ", ")}"
        targetLogger.trace(beforeMessage)
        println(beforeMessage)
        val result = pjp.proceed(pjp.args)
        val afterMessage = "メソッド終了: ${pjp.signature.name} / $result"
        targetLogger.trace(afterMessage)
        println(afterMessage)
        return result
    }

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