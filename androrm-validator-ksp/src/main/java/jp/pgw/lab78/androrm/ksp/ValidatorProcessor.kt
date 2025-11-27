package jp.pgw.lab78.androrm.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSVisitorVoid
import kotlin.reflect.KClass

/**
 * ## AndrORM バリデータ
 * ### Select クラスの使用箇所を解析し、joinEntities に含まれない Entity の参照を検出
 * @author Kawana
 * @since 2025-11-04
 */
class AndrOrmValidatorProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getAllFiles().forEach { file ->
            file.accept(SelectReferenceVisitor(environment), Unit)
        }
        return emptyList()
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

    override fun visitCallExpression(expression: KSCallExpression, data: Unit) {
        super.visitCallExpression(expression, data)

        // Select 呼び出し箇所のみ抽出
        val type = expression.type?.resolve()?.declaration?.qualifiedName?.asString()
        if (type?.endsWith(".Select") == true) {
            validateSelectUsage(expression)
        }
    }

    /**
     * Select 呼び出しを解析して joinEntities を抽出
     */
    private fun validateSelectUsage(call: KSCallExpression) {
        val joinEntities = extractJoinEntities(call)
        environment.logger.info("Detected Select usage: joinEntities = $joinEntities")

        // 親スコープ内で where/having/order 呼び出しを解析
        val parent = call.parent ?: return
        val blocks = collectSiblingCalls(parent)

        blocks.forEach { block ->
            when (block.shortName?.asString()) {
                "where" -> checkClause(block, joinEntities, "WHERE")
                "having" -> checkClause(block, joinEntities, "HAVING")
                "order" -> checkClause(block, joinEntities, "ORDER")
            }
        }
    }

    /**
     * joinEntities 引数を抽出（named argument / positional どちらも対応）
     */
    private fun extractJoinEntities(call: KSCallExpression): List<String> {
        val joinArg = call.arguments.firstOrNull { it.name?.asString() == "joinEntities" }
            ?: call.arguments.getOrNull(2) // 第3引数 fallback

        val value = joinArg?.value ?: return emptyList()

        return when (value) {
            is List<*> -> value.mapNotNull { it.toString() }
            is KClass<*> -> listOf(value.simpleName ?: value.toString())
            else -> value.toString()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
    }

    /**
     * 同一スコープ内の関数呼び出しを収集
     */
    private fun collectSiblingCalls(parent: KSNode): List<KSCallExpression> {
        val calls = mutableListOf<KSCallExpression>()
        parent.accept(object : KSVisitorVoid() {
            override fun visitCallExpression(expression: KSCallExpression, data: Unit) {
                calls += expression
                super.visitCallExpression(expression, data)
            }
        }, Unit)
        return calls
    }

    /**
     * where / having / order の中で不正な Entity 参照をチェック
     */
    private fun checkClause(call: KSCallExpression, joinEntities: List<String>, clause: String) {
        val lambdaArg = call.arguments.firstOrNull()?.value as? KSNode ?: return

        lambdaArg.accept(object : KSVisitorVoid() {
            override fun visitMemberSelect(expression: KSMemberSelectExpression, data: Unit) {
                val qualifier = expression.receiver?.toString()?.substringBefore("::") ?: return
                if (qualifier !in joinEntities) {
                    environment.logger.error(
                        "$clause clause references entity `$qualifier` not joined in Select().",
                        expression
                    )
                }
                super.visitMemberSelect(expression, data)
            }
        }, Unit)
    }
}
