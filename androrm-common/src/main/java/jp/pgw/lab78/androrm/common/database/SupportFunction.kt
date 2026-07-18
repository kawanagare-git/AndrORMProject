package jp.pgw.lab78.androrm.common.database

import jp.pgw.lab78.androrm.common.Constants.EMPTY_STRING
import jp.pgw.lab78.androrm.common.MessageConstants.CE00008
import jp.pgw.lab78.androrm.common.MessageConstants.CE00009
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Function
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.shared.library.Utils.isNotNull
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaGetter

/**
 * SupportFunction オブジェクトクラス
 * database パッケージのクラスでサポートする関数群
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
object SupportFunction {
    /**
     * ## テーブルアノテーション取得メソッド
     * ### 指定されたエンティティクラスから
     * ### `@Table`アノテーションを取得します。
     * @receiver `@Table` アノテーションが付与されている [Entity] （上限境界）型の [KClass] インスタンス。
     * @param T [Entity] インターフェースを実装するクラスの型。
     * @return `KClass` に付与された `@Table` アノテーション
     * @throws IllegalStateException アノテーションが付与されていない場合
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <T : Entity> KClass<out T>.getTableAnnotation() =
        this.findAnnotation<Table>()
            ?: error(CE00008.format(this.qualifiedName))

    /**
     * ## テーブルエイリアス取得
     * ### 指定されたエンティティクラスから
     * ### `@Table`アノテーションに定義されているテーブルエイリアスを取得
     * @receiver `@Table` アノテーションが付与されている [Entity] （上限境界）型の [KClass] インスタンス。
     * @param T [Entity] インターフェースを実装するクラスの型。
     * @return `KClass` に付与された `@Table` アノテーションのテーブルエイリアス
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <T : Entity> KClass<out T>.getTableAlias() =
        this.getTableAnnotation().alias.ifEmpty { this.getTableName() }

    /**
     * ## テーブル名取得
     * ### エンティティクラスからテーブル名を取得する
     * @receiver `@Table` アノテーションが付与されている [Entity] （上限境界）型の [KClass] インスタンス。
     * @param T [Entity] インターフェースを実装するクラスの型。
     * @return 取得したテーブル名
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <T : Entity> KClass<out T>.getTableName() = this.getTableAnnotation().name
        .ifEmpty { this.simpleNameToSnakeCase() }

    /**
     * ## カラム名のエイリアスを取得
     * @receiver `@Column` アノテーションが付与されている [Entity] （上限境界）型の [KProperty1] インスタンス。
     * @param T [Entity] インターフェースを実装するクラスの型。
     * @return プロパティに付与された @Column アノテーションからカラムのエイリアスを取得
     *          / 取得できない場合、空文字列
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <T : Entity> KProperty1<out T, *>.getColumnAlias() =
        this.findColumnAnnotationSafely()?.alias?.takeIf { it.isNotBlank() } ?: EMPTY_STRING

    /**
     * ## カラムエイリアス取得
     * ### 指定されたエンティティクラスのプロパティから
     * ### `@Column` アノテーションに定義されているカラムエイリアスを取得
     * @receiver 対象プロパティを持つ [Entity] （上限境界）型の [KClass] インスタンス。
     * @param T [Entity] インターフェースを実装するクラスの型。
     * @param propertyName プロパティ名
     * @return プロパティに付与された `@Column` アノテーションのカラムエイリアス
     *         / 取得できない場合、空文字列
     * @author Masahiro Inoue
     * @since 2026-06-27
     */
    fun <T : Entity> KClass<out T>.getColumnAlias(propertyName: String): String =
        this.findColumnAnnotationByPropertyName(propertyName)
            ?.alias?.takeIf { columnAlias -> columnAlias.isNotBlank() } ?: EMPTY_STRING

    /**
     * ## カラム名の取得
     * @receiver 対象プロパティを持つ [Entity] （上限境界）型の [KClass] インスタンス。
     * @param T [Entity] インターフェースを実装するクラスの型。
     * @param propertyName プロパティ名
     * @return プロパティに付与された @Column アノテーションからカラム名を取得
     *          / 取得できない場合、プロパティ名をスネークケースに変換
     * @author Masahiro Inoue
     * @since 2026-06-27
     */
    fun <T : Entity> KClass<out T>.getColumnName(propertyName: String): String =
        this.findColumnAnnotationByPropertyName(propertyName)
            ?.name?.takeIf { columnName -> columnName.isNotBlank() } ?: propertyName.toSnakeCase()

    /**
     * ## カラム名の取得
     * @receiver `@Column` アノテーションが付与されている [Entity] （上限境界）型の [KProperty1] インスタンス。
     * @param T [Entity] インターフェースを実装するクラスの型。
     * @return プロパティに付与された @Column アノテーションからカラム名を取得
     *          / 取得できない場合、プロパティ名をスネークケースに変換
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun <T : Entity> KProperty1<out T, *>.getColumnName(): String =
        this.findColumnAnnotationSafely()?.name?.takeIf { it.isNotBlank() }
            ?: this.simpleNameToSnakeCase()

    /**
     * ## クラスをスネークケースに変換
     * @receiver [KClass] インスタンス。
     * @return スネークケースに変換されたクラス名
     * @throws IllegalStateException クラス名を取得できない場合
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun KClass<*>.simpleNameToSnakeCase(): String =
        this.simpleName?.toSnakeCase()
            ?: error(CE00009.format(this.qualifiedName))

    /**
     * ## プロパティ名をスネークケースに変換
     * @return スネークケースに変換されたプロパティ名
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun KProperty1<*, *>.simpleNameToSnakeCase(): String =
        this.name.toSnakeCase()

    /**
     * ## エイリアス生成
     * ### テーブルのエイリアスと拡張エイリアスで、目的のエイリアスを生成
     * @param tableAlias テーブルのエイリアス
     * @param extendAlias 拡張エイリアス
     * @return 生成したエイリアス
     * @author Masahiro Inoue
     * @since 2025-08-08
     */
    fun buildAlias(tableAlias: String?, extendAlias: String?) =
        if (tableAlias.hasText()) {
            "${tableAlias}_${extendAlias.orEmpty()}".removeSuffix("_")
        } else {
            null
        }

    /**
     * ## スネークケース変換関数
     * ### キャメルケースの文字列をスネークケースに変換
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun CharSequence.toSnakeCase(): String {
        return this.replace(Regex("([a-z])([A-Z])")) { "${it.groupValues[1]}_${it.groupValues[2]}" }
            .uppercase(Locale.ROOT)
    }

    /**
     * ## キャメルケース変換関数
     * ### スネークケースの文字列をキャメルケースに変換
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun CharSequence.toCamelCase(): String {
        return this.toString().lowercase(Locale.ROOT)
            .replace(Regex("_(.)")) { it.groupValues[1].uppercase(Locale.ROOT) }
    }

    /**
     * ## 文字列存在判定
     * ### 文字列変数に何かしら設定（null でもなく 空白でもない）
     * @return true 文字列変数に何かしら設定（null でもなく 空白でもない） / false null または 空白
     * @author Masahiro Inoue
     * @since 2025-08-08
     */
    fun CharSequence?.hasText() = !this.isNullOrBlank()

    /**
     * ## カラム判定関数
     * ### プロパティに Column アノテーションが付与されているか判定
     * @return true Column アノテーションが付与されている / false 付与されていない
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    fun KProperty1<*, *>.isColumn() = this.findAnnotation<Column>().isNotNull()

    /**
     * ## 関数カラム判定関数
     * ### プロパティに Function アノテーションが付与されているか判定
     * @return true Function アノテーションが付与されている / false 付与されていない
     * @author Masahiro Inoue
     * @since 2025-10-19
     */
    fun KProperty1<*, *>.isFunctionColumn() = this.findAnnotation<Function>().isNotNull()

    /**
     * ## 所有クラス取得拡張関数
     * @receiver KProperty1 インスタンス
     * @return 所有クラスの KClass インスタンス
     * @author Masahiro Inoue
     * @since 2026-01-17
     */
    fun KProperty1<*, *>.ownerKClass(): KClass<*> =
        this.parameters.first().type.classifier as KClass<*>

    /**
     * ## SELECT 非表示判定
     * ### プロパティが SELECT 句から除外対象か判定
     * ### @Function と @Column の両方を確認し、hideFromSelect の設定値を戻す
     * @receiver KProperty1 インスタンス
     * @return true SELECT 句から除外する / false 抽出対象
     * @author Masahiro Inoue
     * @since 2026-04-28
     */
    fun KProperty1<*, *>.isHiddenFromSelect(): Boolean =
        this.findAnnotation<Function>()?.hideFromSelect
            ?: this.findAnnotation<Column>()?.hideFromSelect
            ?: false

    /**
     * ## 値取得
     * ### リフレクションを利用して、インスタンスから値を取得する
     * @param target 取得対象のインスタンス
     * @param propertyName 値を取得するプロパティ名
     * @return 取得した値
     * @throws IllegalArgumentException 指定されたプロパティが存在しない場合
     * @author Masahiro Inoue
     * @since 2026-05-23
     */
    fun getPropertyValue(
        target: Any,
        propertyName: String,
    ): Any? {
        val property = target::class.memberProperties
            .firstOrNull { it.name == propertyName }
            ?: throw IllegalArgumentException("Property '$propertyName' is not found.")
        return property.getter.call(target)
    }

    /**
     * ## Column アノテーション安全取得
     * ### Kotlin reflection で取得できない場合、Java reflection で $annotations メソッドから取得する
     * @receiver KProperty1
     * @return Column アノテーション
     * @author Masahiro Inoue
     * @since 2026-06-27
     */
    private fun KProperty1<*, *>.findColumnAnnotationSafely(): Column? =
        runCatching {
            this.findAnnotation<Column>()
        }.getOrNull()
            ?: this.findAnnotationFromAnnotationMethod(Column::class.java)

    /**
     * ## プロパティアノテーション取得
     * ### Kotlin が生成する getXxx$annotations メソッドからアノテーションを取得する
     * @receiver KProperty1
     * @param annotationClass アノテーションクラス
     * @return アノテーション
     * @author Masahiro Inoue
     * @since 2026-06-27
     */
    private fun <A : Annotation> KProperty1<*, *>.findAnnotationFromAnnotationMethod(
        annotationClass: Class<A>,
    ): A? {
        val ownerClass = findOwnerJavaClassSafely()
            ?: return null

        val annotationMethodNames = createAnnotationMethodNames()

        return annotationMethodNames.firstNotNullOfOrNull { methodName ->
            ownerClass.declaredMethods
                .firstOrNull { method ->
                    method.name == methodName && method.parameterCount == 0
                }
                ?.getAnnotation(annotationClass)
        }
    }

    /**
     * ## アノテーションメソッド名生成
     * ### Kotlin が生成する getXxx$annotations / isXxx$annotations 候補を生成する
     * @receiver KProperty1
     * @return アノテーションメソッド名候補
     * @author Masahiro Inoue
     * @since 2026-06-27
     */
    private fun KProperty1<*, *>.createAnnotationMethodNames(): List<String> {
        val capitalizedName =
            name.replaceFirstChar { character ->
                character.titlecase(Locale.ROOT)
            }

        return listOf(
            "get${capitalizedName}\$annotations",
            "${name}\$annotations",
        )
    }

    /**
     * ## 所有 Java クラス安全取得
     * ### Kotlin reflection 解決に失敗する場合を考慮して Java getter から取得する
     * @receiver KProperty1
     * @return 所有 Java クラス
     * @author Masahiro Inoue
     * @since 2026-06-27
     */
    private fun KProperty1<*, *>.findOwnerJavaClassSafely(): Class<*>? =
        runCatching {
            this.javaGetter?.declaringClass
        }.getOrNull()
            ?: runCatching {
                this.ownerKClass().java
            }.getOrNull()

    /**
     * ## カラムアノテーション検索
     * ### プロパティ名を基に JAVA 形式の取得メソッドを抽出する
     * @return 生成した取得メソッド名のリスト
     * @author Masahiro Inoue
     * @since 2026-06-27
     */
    private fun <T : Entity> KClass<out T>.findColumnAnnotationByPropertyName(
        propertyName: String,
    ): Column? {
        val annotationMethodNames = createAnnotationMethodNames(propertyName)

        return annotationMethodNames.firstNotNullOfOrNull { methodName ->
            this.java.declaredMethods
                .firstOrNull { method ->
                    method.name == methodName && method.parameterCount == 0
                }
                ?.getAnnotation(Column::class.java)
        }
    }

    /**
     * ## プロパティ名→メソッド名生成
     * ### プロパティ名から JAVA 形式の取得メソッド名を生成する
     * @param propertyName プロパティ名
     * @return 生成した取得メソッド名のリスト
     * @author Masahiro Inoue
     * @since 2026-06-27
     */
    private fun createAnnotationMethodNames(
        propertyName: String,
    ): List<String> {
        val capitalizedName =
            propertyName.replaceFirstChar { character ->
                character.titlecase(Locale.ROOT)
            }

        return listOf(
            "get${capitalizedName}\$annotations",
            "is${capitalizedName}\$annotations",
            "${propertyName}\$annotations",
        )
    }
}
