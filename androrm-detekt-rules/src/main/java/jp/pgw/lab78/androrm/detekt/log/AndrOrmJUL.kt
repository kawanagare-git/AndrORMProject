package jp.pgw.lab78.androrm.detekt.log

import java.io.File
import java.util.logging.FileHandler
import java.util.logging.Level.FINEST
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

/**
 * ## AndrOrm Detekt 用 Java Util Logging ロガー
 * ### Detekt 用 AndrOrm ログ出力ユーティリティクラス
 * @author Masahiro Inoue
 * @since 2025-12-25
 */
object AndrOrmJUL {
    private var initialized = false
    private val logLevel = FINEST
    val log: Logger = Logger.getLogger("androrm-detekt")

    /**
     * ## 初期化メソッド
     * ### ロガー初期化メソッド（ファイル出力設定）
     * @author Masahiro Inoue
     * @since 2025-12-25
     */
    fun initOnce() {
        if (initialized) return
        initialized = true
        // 例：プロジェクト直下/app/logs に吐く（DetektはGradleから実行されるので user.dir はルートになりやすい）
        val dir = File(System.getProperty("user.dir"), "app/logs")
        dir.mkdirs()

        val logger = log
        logger.level = logLevel          // ★ Logger 側
        logger.useParentHandlers = false   // ★ Gradle / Console 汚染防止

        val fileHandler = FileHandler(
            dir.resolve("androrm-detekt.log").toString(),
            true
        )

        fileHandler.level = logger.level     // ★ Handler 側（重要）
        fileHandler.formatter = SimpleFormatter()

        logger.addHandler(fileHandler)
    }

    /**
     * ## 警告ログ出力メソッド
     * ### 警告ログ出力メソッド
     * @param msg ログメッセージ
     * @author Masahiro Inoue
     * @since 2025-12-25
     */
    fun warning(msg: String) {
        log.warning(msg)
    }

    /**
     * ## 詳細ログ出力メソッド
     * ### 詳細ログ出力メソッド
     * @param msg ログメッセージ
     * @author Masahiro Inoue
     * @since 2025-12-25
     */
    fun debug(msg: String) {
        log.finer(msg)
    }
}
