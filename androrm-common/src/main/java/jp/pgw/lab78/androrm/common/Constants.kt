package jp.pgw.lab78.androrm.common

/**
 * ## 定数オブジェクト
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
object Constants {
    /** ヌルもj亀裂 */
    const val NULL_STRING = "null"

    /** 空文字列 */
    const val EMPTY_STRING = ""

    /** 不明 */
    const val UNKNOWN = "Unknown"

    /** 空白 */
    const val SPACE = " "

    /** カンマ */
    private const val COMMA = ","

    /** スラッシュ */
    const val SLASH = "/"

    /** コロン */
    const val COLON = ":"

    /** 優先区切り文字 */
    const val PRIMARY_DELIMITER = COMMA
    const val SECONDARY_DELIMITER = SLASH
    const val TERTIARY_DELIMITER = COLON

    /** 引数区切り文字 */
    const val ARGUMENT_DELIMITER = "$PRIMARY_DELIMITER$SPACE"

    /** 引数無メッセージ */
    const val NO_ARGUMENTS = "No arguments"

    /** Unit クラス*/
    const val UNIT_RETURN = "Unit"

    /** ディレクトリィ区切り文字 */
    const val DIRECTORY_DELIMITER = SECONDARY_DELIMITER

    /**
     * ## ログタグ
     * ### ログ出力時のタグを定義する列挙クラス
     * - ENTERED: メソッドに入ったときのログタグ
     * - EXITING: メソッドから出るときのログタグ
     * - THROWING: 例外が発生したときのログタグ
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    enum class LogPhase(keyword: String) {
        ENTERED("Entered"),
        EXITING("Exiting"),
        THROWING("Throwing")
        ;

        val tag: String = keyword
    }

    /**
     * ## モジュールラベル
     * ### ログ出力時のモジュール識別子を定義する列挙クラス
     * - APP: メインアプリケーションモジュール
     * - KSP: KSP コード生成モジュール
     * - DETEKT: Detekt 静的解析モジュール
     * - COMMON: 共通ライブラリモジュール
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    enum class ModuleLabel(keyword: String, logFile: String) {
        APP("[AndrORM-MAIN]", "AndrORM.log"),
        KSP("[AndrORM-KSP]", "AndrORM_KSP.log"),
        DETEKT("[AndrORM-DETEKT]", "AndrORM_DETEKT.log"),
        COMMON("[AndrORM-COMMON]", "AndrORM_COMMON.log"),
        ;

        val tag: String = keyword
        val logFilename: String = logFile
    }

    /**
     * ## 要素種別
     * ### ログ出力時の要素種別を定義する列挙クラス
     * - CLASS: クラス要素
     * - METHOD: メソッド要素
     * - PROPERTY: プロパティ要素
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    enum class Element(keyword: String) {
        CLASS("class"),
        METHOD("method"),
        PROPERTY("property"),
        ;

        val tag: String = keyword
    }

    /**
     * ## 論理演算子
     * @param query クエリ文字列
     */
    enum class LogicalOperator(val query: String) {
        AND(" and "),
        OR(" or "),
    }
}