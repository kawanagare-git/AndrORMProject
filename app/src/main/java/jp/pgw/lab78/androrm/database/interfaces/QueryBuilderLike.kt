package jp.pgw.lab78.androrm.database.interfaces

/**
 * ## クエリ生成インターフェス
 * ### クエリ生成クラスに付与する生成インターフェス
 * @author Masahiro Inoue
 * @since 2025-08-08
 */
interface QueryBuilderLike<T> {
    /**
     * ## 生成メソッド
     * ### クエリを生成するときに呼び出す
     * @return 生成されたクエリ文字列
     * @author Masahiro Inoue
     * @since 2025-08-08
     */
    fun build(): String
}