package jp.pgw.lab78.androrm.detekt

/**
 * ## AndrORM 解析結果用メッセージ
 * @author Masahiro Inoue
 * @since 2025-12-27
 */
object AndrOrmDetektMessages {
    /**
     * ## 無効なプロパティ参照メッセージ生成メソッド
     * ### 指定されたエンティティとプロパティ参照に基づいて、無効なプロパティ参照のエラーメッセージを生成する。
     * @param entity エンティティ名
     * @param property プロパティ参照名
     * @return 生成されたエラーメッセージ
     * @author Masahiro Inoue
     * @since 2025-12-27
     */
    fun invalidPropertyReference(entity: String, property: String): String =
        "The entity `$entity` of the property reference `$property` " +
                "does not match any of the entities specified in Select()'s FROM or JOIN clauses."

    /**
         * ## テーブル名重複メッセージ生成メソッド
         * ### 同じテーブル名を明示指定した二つのテーブル定義Entityを示す。
         * @param tableName 重複したテーブル名
         * @param firstEntity 先に定義されたEntity名
         * @param duplicateEntity 重複側のEntity名
         * @return 生成されたエラーメッセージ
         * @author Masahiro Inoue
         * @since 2026-07-20
         */
        fun duplicateTableName(
            tableName: String,
            firstEntity: String,
            duplicateEntity: String,
        ): String =
            "The table name `$tableName` is duplicated by TableDefinitionEntity " +
                    "`$firstEntity` and `$duplicateEntity`."

        /**
     * ## デバッグログメッセージ生成メソッド
     * ### 処理対象の関数名と抽出テキストからデバッグログメッセージを生成する。
     * @param name 処理対象の関数名
     * @param textExtraction 抽出したソーステキスト
     * @return 生成されたデバッグログメッセージ
     * @author Masahiro Inoue
     * @since 2025-12-27
     */
    fun logDebug(name: String, textExtraction: String): String =
        "[AndrOrmEntityRefRule] processCall: name='$name' in $textExtraction"

    /**
     * ## 警告ログメッセージ生成メソッド
     * ### 指定されたメッセージを基に、警告ログメッセージを生成する。
     * @param message ログメッセージ
     * @return 生成された警告ログメッセージ
     * @author Masahiro Inoue
     * @since 2026-01-07
     */
    fun logWarning(message: String): String =
        "[AndrOrmEntityRefRule] $message See the following path for details.:build/reports/detekt/"
}