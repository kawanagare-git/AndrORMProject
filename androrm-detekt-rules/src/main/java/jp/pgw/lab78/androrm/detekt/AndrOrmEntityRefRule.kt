package jp.pgw.lab78.androrm.detekt

import io.gitlab.arturbosch.detekt.api.*
import jp.pgw.lab78.androrm.detekt.AndrOrmDetektMessages.Companion.invalidPropertyReference
import jp.pgw.lab78.androrm.detekt.AndrOrmDetektMessages.Companion.logDebug
import jp.pgw.lab78.androrm.detekt.AndrOrmEntityRefRule.ExtractEntity.*
import jp.pgw.lab78.androrm.detekt.log.AndrOrmJUL
import org.jetbrains.kotlin.psi.*

/**
 * ## AndrORM の Select DSL 用の構文検査ルール。
 *
 * ### 目的：
 *  - Select(fromEntity = XxxEntity::class) で指定した Entity
 *  - join(joinType, YyyEntity::class) で指定した Entity
 * ### のいずれかに属するプロパティだけが
 * ### join / where / having / order のラムダ内で使われているかを検査する。
 * ### 対象とするコードのイメージ
 * ```
 *   val query = Select(MainEntity::class)
 *       .join(LEFT, SubEntity::class) {
 *           MainEntity::id eq SubEntity::mainId   // ✔ OK
 *       }
 *       .where {
 *           MainEntity::name like "%foo%"        // ✔ OK
 *       }
 *       .order {
 *           SubEntity::createdAt.asc             // ✔ OK
 *       }
 *
 *   // 以下は NG（Select で指定していない Entity のプロパティ）
 *   .where {
 *       OtherEntity::id eq MainEntity::id        // ❌ OtherEntity は not allowed
 *   }
 * ```
 * ### なお以下のような動的リフレクションパターンは対象外（検査しない）
 * ```
 *   val fromTable = MainEntity::class
 *   val joinTable = SubEntity::class
 *   Select(fromTable)
 *       .join(LEFT, joinTable) {
 *           fromTable.prop("id") eq joinTable.prop("name")  // ← ここはあえてスキップ
 *       }
 *  ```
 *  ### prop("id") は、プロパティを取得する拡張メソッドとする想定。
 *  @param config Detekt ルール設定情報
 *  @author Masahiro Inoue
 *  @since 2025-11-30
 */
class AndrOrmEntityRefRule(
    config: Config = Config.empty
) : Rule(config) {
    /** 引数抽出用の列挙型 */
    enum class ExtractEntity(val index: Int) {
        FromEntity(0),
        JoinedEntity(1)
    }

    /** 報告済み位置情報の集合（重複報告防止用） */
    private val reportedPositions = mutableSetOf<String>()

    /** ルール定義情報 */
    override val issue: Issue = Issue(
        id = "AndrOrmEntityRefRule",
        severity = Severity.Defect,
        description = "Checks whether a property referenced in the Select DSL belongs to an entity" +
                " specified in the FROM or JOIN clauses.",
        debt = Debt.TWENTY_MINS
    )

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)
        AndrOrmJUL.log.info("[AndrOrmEntityRefRule] visitKtFile: ${file.name}")
    }

    /**
     * ## Select から始まるドットチェーン
     * ### Select(...).join(...).where { ... }.order { ... }
     * ### をまとめて解析したいので、DotQualifiedExpression を入口にする。
     * @param expression DotQualifiedExpression ノード
     * @author Masahiro Inoue
     * @since 2025-11-30
     */
    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        // Select チェーンかどうかを解析して、必要なら検査を行う
        analyzeSelectChain(expression)
    }

    /**
     * ## Select チェーン解析用のコンテキスト情報
     * @param selectCall Select(...) 呼び出し式
     * @param fromEntityName Select の第1引数で指定された Entity 名
     * @param joinedEntityNames join(...) で追加された Entity 名の集合
     * @author Masahiro Inoue
     * @since 2025-11-30
     */
    private data class SelectChainContext(
        val selectCall: KtCallExpression,
        val fromEntityName: String,
        val joinedEntityNames: MutableSet<String> = mutableSetOf()
    ) {
        val allEntityNames: Set<String> get() = joinedEntityNames + fromEntityName
    }

    /**
     * ## 与えられた DotQualifiedExpression を再帰的に辿って、
     * - Select(...) 呼び出しを見つける
     * - join(...) を見つけたら joinedEntity を登録
     * - where / having / order / join のラムダ内のプロパティ参照を検査
     * @param root DotQualifiedExpression ノード
     * @return Select チェーンコンテキスト情報（Select チェーンでなければ null）
     * @author Masahiro Inoue
     * @since 2025-11-30
     */
    private fun analyzeSelectChain(root: KtDotQualifiedExpression): SelectChainContext? {
        return processExpression(root, context = null)
    }

    /**
     * ## 再帰的に式ノードを辿って Select チェーンを解析
     * @param expr 現在の式ノード
     * @param context 現在の Select チェーンコンテキスト情報
     * @return 更新された Select チェーンコンテキスト情報
     * @author Masahiro Inoue
     * @since 2025-11-30
     */
    private fun processExpression(
        expr: KtExpression,
        context: SelectChainContext?
    ): SelectChainContext? =
        when (expr) {
            is KtDotQualifiedExpression -> {
                // 左側（receiver）を先に解析してコンテキストを更新
                val leftCtx = processExpression(expr.receiverExpression, context)
                when (val selector = expr.selectorExpression) {
                    is KtCallExpression -> processCall(selector, leftCtx)
                    else -> leftCtx
                }
            }

            is KtCallExpression -> processCall(expr, context)
            else -> context
        }

    /**
     * ## 呼び出し式ノードを解析して Select チェーンコンテキストを更新
     * @param call 呼び出し式ノード
     * @param context 現在の Select チェーンコンテキスト情報
     * @return 更新された Select チェーンコンテキスト情報
     * @author Masahiro Inoue
     * @since 2025-11-30
     */
    private fun processCall(
        call: KtCallExpression,
        context: SelectChainContext?
    ): SelectChainContext? {
        val name = call.calleeName() ?: return context
        val messageDebug = logDebug(name, call.text.take(80))
        return when (name) {
            // ① Select(...) を見つけたら fromEntity を取得してコンテキスト作成
            "Select" -> {
                AndrOrmJUL.debug(messageDebug)
                createContextFromSelect(call)
            }
            // ② join(...) -> joinedEntity を追加 + ラムダのプロパティを検査
            "join" -> {
                AndrOrmJUL.debug(messageDebug)
                if (context != null) {
                    updateContextWithJoin(call, context)
                    checkLambdaPropertyRefs(call, context)
                }
                context
            }
            // ③ where / having -> ラムダ内のプロパティを検査
            "where", "having" -> {
                AndrOrmJUL.debug(messageDebug)
                if (context != null) {
                    checkLambdaPropertyRefs(call, context)
                }
                context
            }
            // ④ order -> ラムダ内のプロパティを検査
            "order" -> {
                AndrOrmJUL.debug(messageDebug)
                if (context != null) {
                    checkLambdaPropertyRefs(call, context)
                }
                context
            }

            else -> context
        }
    }

    /**
     * ## Select(...) から fromEntity を取り出してコンテキストを作成
     * @param call Select(...) 呼び出し式ノード
     * @return 生成された Select チェーンコンテキスト情報
     * @author Masahiro Inoue
     * @since 2025-11-30
     */
    private fun createContextFromSelect(call: KtCallExpression): SelectChainContext? {
        val fromArg = extractEntityExpression(call) ?: return null
        val fromName = extractEntityNameFromKClassLiteral(fromArg) ?: return null
        return SelectChainContext(
            selectCall = call,
            fromEntityName = fromName
        )
    }

    /**
     * ## join(...) から joinedEntity を取り出してコンテキストに追加
     * @param call join(...) 呼び出し式ノード
     * @param context 現在の Select チェーンコンテキスト情報
     * @author Masahiro Inoue
     * @since 2025-11-30
     */
    private fun updateContextWithJoin(
        call: KtCallExpression,
        context: SelectChainContext
    ) {
        // シグネチャ: join(joinType, joinedEntity, block)
        val joinedArg = extractEntityExpression(call, JoinedEntity) ?: return
        val joinedName = extractEntityNameFromKClassLiteral(joinedArg) ?: return
        context.joinedEntityNames += joinedName
    }

    /**
     * ## KClass リテラル式からエンティティ名を抽出
     * @param expr KClass リテラル式ノード
     * @return 抽出されたエンティティ名文字列、抽出できなければ null
     * @author Masahiro Inoue
     * @since 2025-11-30
     */
    private fun extractEntityNameFromKClassLiteral(expr: KtExpression): String? {
        val classLiteral = expr as? KtClassLiteralExpression ?: return null
        return classLiteral.receiverExpression?.text
    }

    /**
     * ## ラムダ内のプロパティ参照を検査
     * @param call 呼び出し式ノード
     * @param context 現在の Select チェーンコンテキスト情報
     * @author Masahiro Inoue
     * @since 2025-11-30
     */
    private fun checkLambdaPropertyRefs(
        call: KtCallExpression,
        context: SelectChainContext
    ) {
        val lambda = extractLambda(call) ?: return
        val allowedEntityNames = context.allEntityNames

        // ここで見るのは「XxxEntity::prop」のような callable reference だけ。
        // fromTable.prop("id") のような動的なものはそもそもここに現れないので対象外になる。
        lambda.bodyExpression?.accept(
            object : KtTreeVisitorVoid() {

                override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression) {
                    super.visitCallableReferenceExpression(expression)

                    // EmployeeEntity::id の EmployeeEntity 部分
                    val lhsText = expression.lhs?.text ?: return

                    // Select / join で指定された Entity に含まれない場合は NG
                    if (!allowedEntityNames.contains(lhsText)) {
                        val message = invalidPropertyReference(lhsText, expression.text)
                        val filePath = expression.containingKtFile.virtualFilePath
                        val offset = expression.textRange.startOffset
                        val key = "$filePath:$offset"
                        if (reportedPositions.add(key)) {
                            report(
                                CodeSmell(
                                    issue,
                                    Entity.from(expression),
                                    message = message
                                )
                            )
                        }
                        AndrOrmJUL.warning(message)
                    }
                }
            }
        )
    }

    /**
     * ## ラムダ式抽出ユーティリティ
     * - join(..., { ... })
     * - join(..., block = { ... })
     * - where { ... }
     * ### などから KtLambdaExpression を取り出すユーティリティ
     * @param call 呼び出し式ノード
     * @return 抽出されたラムダ式ノード、抽出できなければ null
     * @author Masahiro Inoue
     * @since 2025-11-30
     */
    private fun extractLambda(call: KtCallExpression): KtLambdaExpression? {
        // xxx( ..., { ... } )
        call.lambdaArguments.firstOrNull()?.getLambdaExpression()?.let { return it }

        // xxx( ..., block = { ... } ) または xxx({ ... })
        val last = call.valueArguments.lastOrNull()?.getArgumentExpression()
        if (last is KtLambdaExpression) return last
        return null
    }

    /**
     * ## 呼び出し式の関数名抽出ユーティリティ
     * @receiver 呼び出し式ノード
     * @return 抽出された関数名文字列、抽出できなければ null
     * @author Masahiro Inoue
     * @since 2025-11-30
     */
    private fun KtCallExpression.calleeName(): String? {
        val callee = calleeExpression ?: return null
        return when (callee) {
            is KtNameReferenceExpression -> callee.getReferencedName()
            is KtConstructorCalleeExpression ->
                callee.constructorReferenceExpression?.getReferencedName()

            else -> callee.text
        }
    }

    /**
     * ## Select コンストラクタ の fromEntity を取得する
     * @param selectCall Select(...) 呼び出し式ノード
     * @return 抽出された fromEntity 式ノード、抽出できなければ null
     * @author Masahiro Inoue
     * @since 2025-11-30
     */
    private fun extractEntityExpression(
        selectCall: KtCallExpression,
        argumentName: ExtractEntity = FromEntity
    ): KtExpression? {
        // 引数名指定
        val namedFromArg = selectCall.valueArguments
            .firstOrNull { it.getArgumentName()?.asName?.identifier == argumentName.name }
        // 位置指定
        val expr = (namedFromArg ?: selectCall.valueArguments.getOrNull(argumentName.index))
            ?.getArgumentExpression()
        // エラー処理
        if (expr == null) {
            AndrOrmJUL.log.warning(
                "[AndrOrmEntityRefRule] WARN: argument '$argumentName' not found in:" +
                        selectCall.text.take(80)
            )
        }
        return expr
    }
}
