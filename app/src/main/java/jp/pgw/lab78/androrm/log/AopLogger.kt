package jp.pgw.lab78.androrm.log

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory

@Aspect
class AopLogger {
    companion object {
        /** ログインスタンス*/
        val LOGGER = LoggerFactory.getLogger(AopLogger::class.java)
    }

    @Around("execution(* jp.pgw.lab78.androrm..*(..))")
    @Throws(Throwable::class)
    fun traceAdvice(pjp: ProceedingJoinPoint): Any? {
        LOGGER.trace("メソッド開始: ${pjp.signature.name} / ${pjp.args.joinToString( ", ")}")
        val result = pjp.proceed(pjp.args)
        LOGGER.trace("メソッド終了: ${pjp.signature.name} / $result")
        return result
    }

    @Around("execution(* jp.pgw.lab78.androrm..*(..)) && @annotation(jp.pgw.lab78.androrm.log.annotations.LogTarget)")
    @Throws(Throwable::class)
    fun infoAdvice(pjp: ProceedingJoinPoint): Any? {
        LOGGER?.info("メソッド開始: ${pjp.signature.name}")
        val result = pjp.proceed(pjp.args)
        LOGGER?.info("メソッド終了: ${pjp.signature.name}")
        return result
    }
}