package jp.pgw.lab78.androrm.detekt

class AndrOrmDetektMessages {
    companion object {
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
         * ## 警告ログメッセージ生成メソッド
         * ### 指定されたメッセージを基に、警告ログメッセージを生成する。
         * @param msg ログメッセージ
         * @return 生成された警告ログメッセージ
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
}