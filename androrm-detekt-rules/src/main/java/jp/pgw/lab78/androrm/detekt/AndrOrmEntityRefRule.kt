package jp.pgw.lab78.androrm.detekt

import io.gitlab.arturbosch.detekt.api.*
import jp.pgw.lab78.androrm.detekt.AndrOrmDetektMessages.invalidPropertyReference
import jp.pgw.lab78.androrm.detekt.AndrOrmDetektMessages.logDebug
import jp.pgw.lab78.androrm.detekt.AndrOrmEntityRefRule.ExtractEntity.*
import jp.pgw.lab78.androrm.detekt.log.AndrOrmLogger
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
        JoinedEntity(1);

        /**
         * ## 文字列化
         *  @author Masahiro Inoue
         *  @since 2025-11-30
         */
        override fun toString() = this.name.replaceFirstChar { it.lowercaseChar() }

    }

    /** 報告済み位置情報の集合（重複報告防止用） */
    private val reportedPositions = mutableSetOf<String>()

    /** 変数化された相関サブクエリへ外側 SELECT から参照を許可する Entity */
    private val correlatedEntityNamesByProperty = mutableMapOf<KtProperty, MutableSet<String>>()

    /** ルール定義情報 */
    override val issue: Issue = Issue(
        id = "AndrOrmEntityRefRule",
        severity = Severity.Defect,
        description = "Checks whether a property referenced in the Select DSL belongs to an entity" +
                " specified in the FROM or JOIN clauses.",
        debt = Debt.TWENTY_MINS
    )

    /**
     * ## Kotlin ファイル訪問
     * ### 基底クラスのファイル訪問処理を実行し、解析対象のファイル名をログへ記録する。
     * @param file 解析対象の Kotlin ファイル
     * @author Masahiro Inoue
     * @since 2025-11-30
     */
    override fun visitKtFile(file: KtFile) {
        correlatedEntityNamesByProperty.clear()
        collectCorrelatedEntityNames(file)
        super.visitKtFile(file)
        AndrOrmLogger.log.info("[AndrOrmEntityRefRule] visitKtFile: ${file.name}")
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
            "Select", "ViewSelect" -> {
                AndrOrmLogger.debug(messageDebug)
                createContextFromSelect(call)
            }
            // ② join(...) -> joinedEntity を追加 + ラムダのプロパティを検査
            "join" -> {
                AndrOrmLogger.debug(messageDebug)
                if (context != null) {
                    updateContextWithJoin(call, context)
                    checkLambdaPropertyRefs(call, context)
                }
                context
            }
            // ③ where / having -> ラムダ内のプロパティを検査
            "where", "having", "on" -> {
                AndrOrmLogger.debug(messageDebug)
                if (context != null) {
                    checkLambdaPropertyRefs(call, context)
                }
                context
            }
            // ④ order -> ラムダ内のプロパティを検査
            "order" -> {
                AndrOrmLogger.debug(messageDebug)
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
        val allowedEntityNames = context.allEntityNames + resolveCorrelatedEntityNames(call)

        // ここで見るのは「XxxEntity::prop」のような callable reference だけ。
        // fromTable.prop("id") のような動的なものはそもそもここに現れないので対象外になる。
        lambda.bodyExpression?.accept(
            object : KtTreeVisitorVoid() {

                /** ネストした SELECT チェーンは、そのチェーン自身の検査へ委ねる。 */
                override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                    if (!expression.startsWithSelect()) {
                        super.visitDotQualifiedExpression(expression)
                    }
                }

                /**
                 * ## Callable 参照式の検査
                 * ### ラムダ内の Entity プロパティ参照を許可された Entity と照合し、違反を重複なく報告する。
                 * @param expression 検査対象の Callable 参照式
                 * @author Masahiro Inoue
                 * @since 2025-11-30
                 */
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
                        AndrOrmLogger.warning(message)
                    }
                }
            }
        )
    }

    /**
     * ## 変数化された相関サブクエリ解析
     * ### exists／notExists に渡される Select 変数へ外側 SELECT の Entity を関連付ける
     * @param file 解析対象 Kotlin ファイル
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    private fun collectCorrelatedEntityNames(file: KtFile) {
        val propertiesByName = mutableMapOf<String, MutableList<KtProperty>>()
        file.accept(
            object : KtTreeVisitorVoid() {
                override fun visitProperty(property: KtProperty) {
                    property.name?.let { propertyName ->
                        propertiesByName.getOrPut(propertyName) { mutableListOf() } += property
                    }
                    super.visitProperty(property)
                }
            }
        )
        file.accept(
            object : KtTreeVisitorVoid() {
                override fun visitCallExpression(expression: KtCallExpression) {
                    if (expression.calleeName() in setOf("exists", "notExists")) {
                        val outerEntityNames = findOuterSelectEntityNames(expression)
                        expression.valueArguments.forEach { argument ->
                            val propertyName =
                                (argument.getArgumentExpression() as? KtNameReferenceExpression)
                                    ?.getReferencedName()
                                    ?: return@forEach
                            propertiesByName[propertyName]
                                ?.filter { property ->
                                    property.textOffset < expression.textOffset &&
                                            containingFunction(property) == containingFunction(expression)
                                }
                                ?.maxByOrNull { property -> property.textOffset }
                                ?.let { property ->
                                    correlatedEntityNamesByProperty
                                        .getOrPut(property) { mutableSetOf() }
                                        .addAll(outerEntityNames)
                                }
                        }
                    }
                    super.visitCallExpression(expression)
                }
            }
        )
    }

    /**
     * ## 相関参照可能 Entity 解決
     * ### 変数化されたサブクエリとインラインサブクエリの外側 Entity を取得する
     * @param call 検査対象条件呼び出し
     * @return 相関参照を許可する Entity 名
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    private fun resolveCorrelatedEntityNames(call: KtCallExpression): Set<String> {
        val property = generateSequence(call.parent) { parent -> parent.parent }
            .filterIsInstance<KtProperty>()
            .firstOrNull()
        return correlatedEntityNamesByProperty[property].orEmpty() +
                findOuterSelectEntityNames(call)
    }

    /**
     * ## 外側 SELECT Entity 解決
     * ### 相関サブクエリを内包する where／having／on の受信側から Entity を抽出する
     * @param call サブクエリまたは exists 呼び出し
     * @return 外側 SELECT が FROM／JOIN で使用する Entity 名
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    private fun findOuterSelectEntityNames(call: KtCallExpression): Set<String> {
        val outerClauseCall = generateSequence(call.parent) { parent -> parent.parent }
            .filterIsInstance<KtCallExpression>()
            .firstOrNull { ancestorCall ->
                ancestorCall.calleeName() in setOf("where", "having", "on")
            }
            ?: return emptySet()
        val outerChain = outerClauseCall.parent as? KtDotQualifiedExpression
            ?: return emptySet()
        return collectSelectEntityNames(outerChain.receiverExpression)
    }

    /**
     * ## SELECT チェーン Entity 収集
     * ### FROM と JOIN の KClass リテラルから Entity 名を収集する
     * @param expression SELECT チェーン式
     * @return チェーンが使用する Entity 名
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    private fun collectSelectEntityNames(expression: KtExpression): Set<String> {
        val entityNames = mutableSetOf<String>()
        expression.accept(
            object : KtTreeVisitorVoid() {
                override fun visitCallExpression(call: KtCallExpression) {
                    val target = when (call.calleeName()) {
                        "Select", "ViewSelect" -> extractEntityExpression(call)
                        "join" -> extractEntityExpression(call, JoinedEntity)
                        else -> null
                    }
                    target?.let(::extractEntityNameFromKClassLiteral)?.let(entityNames::add)
                    super.visitCallExpression(call)
                }
            }
        )
        return entityNames
    }

    /**
     * ## 所属関数取得
     * @param element 所属関数を検索する Kotlin 要素
     * @return 最も近い名前付き関数
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    private fun containingFunction(element: KtElement): KtNamedFunction? =
        generateSequence(element.parent) { parent -> parent.parent }
            .filterIsInstance<KtNamedFunction>()
            .firstOrNull()

    /**
     * ## SELECT チェーン開始判定
     * @receiver 判定対象ドットチェーン
     * @return Select／ViewSelect から始まる場合 true
     * @author Masahiro Inoue
     * @since 2026-08-31
     */
    private fun KtDotQualifiedExpression.startsWithSelect(): Boolean {
        var root: KtExpression = this
        while (root is KtDotQualifiedExpression) {
            root = root.receiverExpression
        }
        return (root as? KtCallExpression)?.calleeName() in setOf("Select", "ViewSelect")
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
        // 1) trailing lambda  (where { ... })
        call.lambdaArguments.firstOrNull()
            ?.getLambdaExpression()
            ?.let { return it }

        // 2) named argument lambda (join(..., on = { ... }))
        for (arg in call.valueArguments) {
            val expr = arg.getArgumentExpression()

            // on = { ... }
            if (expr is KtLambdaExpression) {
                return expr
            }

            // on = foo({ ... }) みたいな形（将来の保険）
            if (expr is KtCallExpression) {
                expr.lambdaArguments.firstOrNull()
                    ?.getLambdaExpression()
                    ?.let { return it }
            }
        }

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
            .firstOrNull { it.getArgumentName()?.asName?.identifier == argumentName.toString() }
        // 位置指定
        val expr = (namedFromArg ?: selectCall.valueArguments.getOrNull(argumentName.index))
            ?.getArgumentExpression()
        // エラー処理
        if (expr == null) {
            AndrOrmLogger.warning(
                "argument '$argumentName' not found in:${selectCall.text.take(80)}"
            )
        }
        return expr
    }
}
