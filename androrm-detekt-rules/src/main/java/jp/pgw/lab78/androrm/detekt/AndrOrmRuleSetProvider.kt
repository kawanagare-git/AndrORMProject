package jp.pgw.lab78.androrm.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider
import jp.pgw.lab78.androrm.detekt.log.AndrOrmLogger

/**
 * ## AndrOrm Detekt ルールセットプロバイダ
 * ### Detekt にこのルールを認識させるための RuleSetProvider。
 * ### detekt.yml 側では:
 * ```
 *   androrm:
 *     AndrOrmEntityRefRule:
 *       active: true
 *     AndrOrmDuplicateTableNameRule:
 *       active: true
 * ```
 * ### のように書く想定。
 * @author Masahiro Inoue
 * @since 2025-11-30
 */
class AndrOrmRuleSetProvider : RuleSetProvider {
    /** ルールセット ID */
    override val ruleSetId: String = "androrm"

    /**
     * ## ルールセットインスタンス生成メソッド
     * ### Detekt にこのルールセットを認識させるためのインスタンス生成メソッド
     * @param config Detekt 設定情報
     * @return 生成されたルールセットインスタンス
     * @author Masahiro Inoue
     * @since 2025-11-30
     */
    override fun instance(config: Config): RuleSet {
        AndrOrmLogger.initOnce()
        AndrOrmLogger.log.info("[AndrOrmRuleSetProvider.instance] called")
        return RuleSet(
            ruleSetId,
            listOf(
                AndrOrmEntityRefRule(config),
                AndrOrmDuplicateTableNameRule(config),
            )
        )
    }
}
