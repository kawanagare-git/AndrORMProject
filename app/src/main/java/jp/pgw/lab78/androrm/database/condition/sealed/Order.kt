package jp.pgw.lab78.androrm.database.condition.sealed

import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumn
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.database.utility.Functions.extractClassFromProperty
import jp.pgw.lab78.androrm.database.utility.Functions.getAlias
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * ## SQL ソート基底クラス
 * ### SQL で使用する並び替え定義を生成するための基底クラス
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
sealed class Order {
    /**
     * ## 条件生成メソッド
     * ### 定義された条件から文字列を生成する
     * @return 生成された文字列
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    abstract fun build(): String
}

/**
 * ## 条件生成メソッド
 * ### 定義された条件から文字列を生成する
 * @return 生成された文字列
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
data class ColumnOrder<T1 : Entity>(
    val mainProperty: KProperty1<T1, *>,
    val descending: Boolean = false,
) : Order() {

    /**
     * ## 単一条件生成メソッド
     * ### 定義された条件から文字列を生成する
     * @return 生成された文字列
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    override fun build(): String {
        val mainAlias = getAlias(extractClassFromProperty(mainProperty) as KClass<out Entity>)
        val mainColumn = mainProperty.getColumn()
        return "$mainAlias$mainColumn ${if (descending) "desc" else ""}"
    }
}
