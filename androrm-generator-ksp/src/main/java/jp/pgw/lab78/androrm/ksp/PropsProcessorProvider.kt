package jp.pgw.lab78.androrm.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import jp.pgw.lab78.androrm.ksp.logging.CreateLogger

/**
 * ## プロパティプロセッサ提供元クラス
 * ### ビルドツールから呼び出され、プロパティプロセッサのインスタンスを
 * ### ビルドツールに戻す
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
class PropsProcessorProvider : SymbolProcessorProvider {
    /**
     * ## プロパティプロセッサ生成メソッド
     * ### プロパティプロセッサのインスタンスを生成する
     * @param environment シンボルプロセッサ環境（ビルドツールから渡される）
     * @return 生成されたプロパティプロセッサのインスタンス
     * @author Masahiro Inoue
     * @since 2025-08-01
     * @see SymbolProcessorProvider.create
     */
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        CreateLogger.initialize(environment)
        return PropsProcessor(
            environment.codeGenerator,
        )
    }
}

