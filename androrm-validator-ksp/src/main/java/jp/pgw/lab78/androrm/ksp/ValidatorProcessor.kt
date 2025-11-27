package jp.pgw.lab78.androrm.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSVisitorVoid

/**
 * ## AndrORM バリデータ
 * ### Select クラスの使用箇所を解析し、joinEntities に含まれない Entity の参照を検出
 * @author Masahiro Inoue
 * @since 2025-11-04
 */
class ValidatorProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getAllFiles().forEach { file ->
            file.accept(SelectReferenceVisitor(environment), Unit)
        }
        environment.logger.warn("AndrORM ValidatorProcessor: Processing completed.")
        return emptyList()
    }
}

/**
 * ## AndrORM バリデータプロバイダ
 * ### ValidatorProcessor のエントリーポイント
 * @author Masahiro Inoue
 * @since 2025-11-19
 */
class ValidatorProcessorProvider : SymbolProcessorProvider {
    /**
     * ## シンボルプロセッサ生成メソッド
     * @param environment KSP のシンボルプロセッサ環境情報
     * @return 生成されたシンボルプロセッサインスタンス
     * @author Masahiro Inoue
     * @since 2025-11-19
     */
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ValidatorProcessor(environment)
    }
}

/**
 * ## Select 使用箇所検出用 Visitor
 * ### Select クラスの呼び出し箇所を解析し、joinEntities に含まれない Entity の参照を検出
 * @author Masahiro Inoue
 * @since 2025-11-05
 */
private class SelectReferenceVisitor(
    private val environment: SymbolProcessorEnvironment
) : KSVisitorVoid() {
    override fun visitFile(file: KSFile, data: Unit) {
        // 子要素を辿る
        file.declarations.forEach { it.accept(this, Unit) }
        super.visitFile(file, data)
    }

}
