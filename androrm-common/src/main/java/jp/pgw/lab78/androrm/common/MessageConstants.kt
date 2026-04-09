package jp.pgw.lab78.androrm.common

object MessageConstants {
    /** エラーメッセージ: Null 値が許可されていないフィールドに Null が設定された場合のエラー */
    const val ERROR_NULL_NOT_ALLOWED = "Null value is not allowed for this field."

    /** エラーメッセージ: データベース接続に失敗した場合のエラー */
    const val ERROR_DATABASE_CONNECTION_FAILED = "Failed to connect to the database."

    /** エラーメッセージ: クエリの実行に失敗した場合のエラー */
    const val ERROR_QUERY_EXECUTION_FAILED = "Failed to execute the query."

    /** エラーメッセージ: データの挿入に失敗した場合のエラー */
    const val ERROR_DATA_INSERTION_FAILED = "Failed to insert data into the database."

    /** エラーメッセージ: データの更新に失敗した場合のエラー */
    const val ERROR_DATA_UPDATE_FAILED = "Failed to update data in the database."

    /** エラーメッセージ: データの削除に失敗した場合のエラー */
    const val ERROR_DATA_DELETION_FAILED = "Failed to delete data from the database."

    /** エラーメッセージ: ColumnFunction の完全修飾名が取得出来ない場合のエラー */
    const val ERROR_COLUMN_FUNCTION_FULLY_QUALIFIED_NAME =
        "Failed to obtain the fully qualified name of ColumnFunction."

    /** エラーメッセージ: ColumnFunction のシンプルネームが取得出来ない場合のエラー */
    const val ERROR_COLUMN_FUNCTION_SIMPLE_NAME =
        "Failed to obtain the simple name of ColumnFunction."
}