package jp.pgw.lab78.androrm.common.logging.interfaces

interface LoggerLike {
    fun info(infoMessage: String, vararg args: Any)
    fun infoEntered(vararg args: Any)
    fun infoExiting(result: Any? = null)
    fun warning(warnMessage: String, vararg args: Any)
    fun error(errMessage: String, vararg args: Any)
    fun traceEntered(vararg args: Any)
    fun traceExiting(result: Any? = null)
}