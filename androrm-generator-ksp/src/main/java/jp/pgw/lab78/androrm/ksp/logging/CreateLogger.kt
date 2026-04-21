package jp.pgw.lab78.androrm.ksp.logging

import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING

object CreateLogger {
    /** ログルートディレクトリ */
    private const val LOG_ROOT = "build"

    /** ログサブディレクトリ */
    private const val LOG_DIRECTORY = "logs"

    /** ロガーインスタンス */
    private lateinit var _logger: LoggerLike
    val logger: LoggerLike
        get() = _logger

    /**
     * ## ロガー初期化メソッド
     * ### KSP の SymbolProcessorEnvironment を使用して、ロガーインスタンスを初期化する
     * ### ロガーは CompositeLogger クラスのインスタンスで、KSP ロガーとファイルロガーの両方にログ出力する
     * @param environment KSP の SymbolProcessorEnvironment インスタンス
     * @author Masahiro Inoue
     * @since 2026-04-17
     */
    fun initialize(environment: SymbolProcessorEnvironment) {
        _logger = CompositeLogger(
            environment.logger,
            environment.options["androrm.moduleDir"] ?: EMPTY_STRING,
            LOG_ROOT,
            LOG_DIRECTORY
        )
    }
}