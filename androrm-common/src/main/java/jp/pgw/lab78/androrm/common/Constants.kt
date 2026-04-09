package jp.pgw.lab78.androrm.common

/**
 * ## 定数オブジェクト
 * @author Masahiro Inoue
 * @since 2025-08-01
 */
object Constants {
    /** 空文字列 */
    const val EMPTY_STRING = ""

    /** 不明 */
    const val UNKNOWN = "Unknown"

    /** 空白 */
    const val SPACE = " "

    /** カンマ */
    const val COMMA = ","

    /** カンマ + スペース */
    const val COMMA_SPACE = "$COMMA$SPACE"

    /** ログルートディレクトリ */
    const val LOG_ROOT = "build"

    /** ログルートディレクトリ */
    const val LOG_DIRECTORY = "logs"

    /**
     * ## ログタグ
     * ### ログ出力時のタグを定義する列挙クラス
     * - ENTERED: メソッドに入ったときのログタグ
     * - EXITING: メソッドから出るときのログタグ
     * @author Masahiro Inoue
     * @since 2026-03-10
     */
    enum class Log(keyword: String) {
        ENTERED("Entered"),
        EXITING("Exiting"),
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