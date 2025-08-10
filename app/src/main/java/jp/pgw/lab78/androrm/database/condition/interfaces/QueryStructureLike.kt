package jp.pgw.lab78.androrm.database.condition.interfaces

/**
 * ## SQL クエリ構成インターフェース
 * ### クエリの構成要素を表すマーカインターフェス
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
interface QueryStructure {
    /**
     * ## 条件生成メソッド
     * ### 定義された条件から文字列を生成する
     * @return 生成された文字列
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun build(): String
}