package jp.pgw.lab78.androrm.common.database

import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.database.annotation.Column
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation

/**
 * SupportFunction オブジェクトクラス
 * database パッケージのクラスでサポートする関数群
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
object SupportFunction {
    /**
     * ## クラス名をスネークケースに変換
     * @return スネークケースに変換されたクラス名（nullなら例外）
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun KClass<*>.simpleNameToSnakeCase(): String =
        this.simpleName?.toSnakeCase()
            ?: error("Could not determine class name for ${this.qualifiedName}")

    /**
     * ## カラム名の取得
     * @return プロパティに付与された @Column アノテーションからカラム名を取得
     *          、取得できない場合、プロパティ名をスネークケースに変換
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun KProperty1<*, *>.getColumn(): String =
        this.findAnnotation<Column>()?.name?.takeIf { it.isNotBlank() }
                                        ?: this.simpleNameToSnakeCase()

    /**
     * ## カラム名のエイリアスを取得
     * @return プロパティに付与された @Column アノテーションからカラムのエイリアスを取得
     *          、取得できない場合、空文字列
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun KProperty1<*, *>.getColumnAlias(): String =
        this.findAnnotation<Column>()?.alias?.takeIf { it.isNotBlank() } ?: EMPTY_STRING

    /**
     * ## プロパティ名をスネークケースに変換
     * @return スネークケースに変換されたクラス名（nullなら例外）
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun KProperty1<*, *>.simpleNameToSnakeCase(): String =
        this.name.toSnakeCase()

    /**
     * ## toSnakeCase 関数
     * ### キャメルケースの文字列をスネークケースに変換
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun String.toSnakeCase(): String {
        return this.replace(Regex("([a-z])([A-Z])")) { "${it.groupValues[1]}_${it.groupValues[2]}" }
            .uppercase(Locale.ROOT)
    }

}