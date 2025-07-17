package jp.pgw.lab78.androrm.common

import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * SupportFunction オブジェクトクラス
 * database パッケージのクラスでサポートする関数群
 */
object SupportFunction {
    /**
     * ## クラス名をスネークケースに変換
     * @return スネークケースに変換されたクラス名（nullなら例外）
     */
    fun KClass<*>.simpleNameToSnakeCase(): String =
        this.simpleName?.toSnakeCase()
            ?: error("Could not determine class name for ${this.qualifiedName}")

    /**
     * ## プロパティ名をスネークケースに変換
     * @return スネークケースに変換されたクラス名（nullなら例外）
     */
    fun KProperty1<*, *>.simpleNameToSnakeCase(): String =
        this.name.toSnakeCase()

    /**
     * ## toSnakeCase 関数
     * ### キャメルケースの文字列をスネークケースに変換
     */
    fun String.toSnakeCase(): String {
        return this.replace(Regex("([a-z])([A-Z])")) { "${it.groupValues[1]}_${it.groupValues[2]}" }
            .uppercase(Locale.ROOT)
    }

}
