package jp.pgw.lab78.androrm.common.dml.interfaces

/**
 * ## SQL 関数の基底インターフェース
 * ### すべての関数が共通で持つ要素を定義するインターフェース
 * @author Masahiro Inoue
 * @since 2024-10-30
 */
interface SqlFunction<E : Enum<E>> {
    /** 関数名 */
    val functionName: String

    /** 引数タイプ */
    val argumentArity: ArgumentArity

    /**
     * ## 戻り値の型取得
     * ### 関数の戻り値の型を取得する
     * @param argTypes 引数（必要に応じて型を決定するために使用されることがある）
     * @return 関数の戻り値の型、デフォルトでは Any クラス
     * @author Masahiro Inoue
     * @since 2026-04-30
     */
    fun getReturnType(argTypes: List<String> = emptyList()): Any

    /**
     * ## 関数生成
     * ### 関数クエリを生成する
     * @param args 引数
     * @return 関数名文字列
     * @author Masahiro Inoue
     * @since 2024-10-30
     */
    fun build(vararg args: String): String

    /**
     * ## 引数タイプ列挙型
     * ### 関数の引数タイプを定義する列挙型
     * @author Masahiro Inoue
     * @since 2024-10-30
     */
    enum class ArgumentArity {
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

