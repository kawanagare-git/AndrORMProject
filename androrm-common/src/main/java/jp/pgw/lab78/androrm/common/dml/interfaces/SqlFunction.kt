package jp.pgw.lab78.androrm.common.dml.interfaces

import jp.pgw.lab78.androrm.common.dml.interfaces.SqlFunction.ArgType.*

/**
 * ## SQL 関数の基底インターフェース
 * ### すべての関数が共通で持つ要素を定義するインターフェース
 * @author Masahiro Inoue
 * @since 2024-10-30
 */
interface SqlFunction {
    /** ## 引数タイプ */
    val argType: ArgType

    /**
     * ## 関数名取得
     * @return 関数名文字列
     * @author Masahiro Inoue
     * @since 2024-10-30
     */
    fun getFunctionName(): String

    /**
     * ## 関数生成
     * ### 関数クエリを生成する
     * @param arg 引数
     * @return 関数名文字列
     * @author Masahiro Inoue
     * @since 2024-10-30
     */
    fun build(vararg arg: String): String

    /**
     * ## 関数名取得
     * @return 関数名文字列
     * @author Masahiro Inoue
     * @since 2024-10-30
     */
    fun buildDefault(argType: ArgType, vararg arg: String): String = run {
        when (argType) {
            NONE -> "${getFunctionName()}()"
            SINGLE -> {
                require(arg.isNotEmpty()) {
                    "This function requires at least one argument."
                }
                "${getFunctionName()}(${arg.first()})"
            }

            MULTI -> {
                require(arg.size >= 2) {
                    "This function requires at least two arguments. Additional arguments are ignored."
                }
                "${this.getFunctionName()}(${arg.joinToString(",")})"
            }

            SPECIAL -> throw IllegalArgumentException("SPECIAL argType requires a custom build() implementation.")
        }
    }

    /**
     * ## 引数タイプ列挙型
     * ### 関数の引数タイプを定義する列挙型
     * @author Masahiro Inoue
     * @since 2024-10-30
     */
    enum class ArgType {
        /** ## 引数なし */
        NONE,

        /** ### 単一引数 */
        SINGLE,

        /** ### 複数引数 */
        MULTI,

        /** ### 特殊（カスタム実装：要 build() 実装） */
        SPECIAL;
    }
}

