package jp.pgw.lab78.androrm.ksp.logging

import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING

interface LoggerLike : AutoCloseable {
    fun info(infoMessage: String, vararg details: Any)
    fun infoEntered(infoMessage: String = EMPTY_STRING)
    fun infoExiting(infoMessage: String = EMPTY_STRING)
    fun warning(warnMessage: String, vararg details: Any)
    fun error(errMessage: String, vararg details: Any)
    fun debug(debugMessage: String, vararg details: Any)
    fun traceEntered(vararg details: Any)
    fun traceExiting(result: Any? = null)
}