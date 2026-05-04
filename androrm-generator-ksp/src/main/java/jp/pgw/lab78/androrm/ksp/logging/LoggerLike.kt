package jp.pgw.lab78.androrm.ksp.logging

import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING

interface LoggerLike : AutoCloseable {
    fun logInfo(infoMessage: String, vararg details: Any)
    fun logInfoEntered(infoMessage: String = EMPTY_STRING)
    fun logInfoExiting(infoMessage: String = EMPTY_STRING)
    fun logWarning(warnMessage: String, vararg details: Any)
    fun logError(errMessage: String, vararg details: Any)
    fun logDebug(debugMessage: String, vararg details: Any)
    fun logTraceEntered(vararg details: Any)
    fun logTraceExiting(result: Any? = null)
}