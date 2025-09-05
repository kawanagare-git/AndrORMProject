package jp.pgw.lab78.androrm.ksp.logging

import com.google.devtools.ksp.processing.KSPLogger
import jp.pgw.lab78.androrm.common.logging.interfaces.LoggerLike

class KspLogger(private val logger: KSPLogger) : LoggerLike{

    override fun info(errMessage: String, vararg args: Any) {
        TODO("Not yet implemented")
    }

    override fun infoEntered(vararg args: Any) {
        TODO("Not yet implemented")
    }

    override fun infoExiting(result: Any?) {
        TODO("Not yet implemented")
    }

    override fun warning(errMessage: String, vararg args: Any) {
        TODO("Not yet implemented")
    }

    override fun error(errMessage: String, vararg args: Any) {
        TODO("Not yet implemented")
    }
}