package jp.pgw.lab78.androrm.detekt.log

import jp.pgw.lab78.androrm.detekt.AndrOrmDetektMessages.Companion.logWarning
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.logging.*
import java.util.logging.Level.FINEST

/**
 * ## AndrOrm Detekt 用 Java Util Logging ロガー
 * ### Detekt 用 AndrOrm ログ出力ユーティリティクラス
 * @author Masahiro Inoue
 * @since 2025-12-25
 */
object AndrOrmLogger {
    private var initialized = false
    private val logLevel = FINEST

    /** ロガーインスタンス */
    val log: Logger = Logger.getLogger("AndrORM-detekt")

    /**
     * ## 英語固定ログフォーマッタ
     * ### ログレコードを英語固定フォーマットで整形するフォーマッタクラス
     * @author Masahiro Inoue
     * @since 2025-12-30
     */
    class EnglishLevelFormatter : Formatter() {

        /** 日付フォーマッタ */
        private val timeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())

        /**
         * ## ログレベル列挙型
         * ### ログレベルを英語固定で扱うための列挙型
         * @author Masahiro Inoue
         * @since 2025-12-30
         */
        enum class LogLevel(val level: Level?, val displayName: String) {
            SEVERE(Level.SEVERE, "SEVERE"),
            WARNING(Level.WARNING, "WARNING"),
            INFO(Level.INFO, "INFO"),
            CONFIG(Level.CONFIG, "CONFIG"),
            FINE(Level.FINE, "FINE"),
            FINER(Level.FINER, "DEBUG"),
            FINEST(Level.FINEST, "TRACE"),
            ALL(Level.ALL, "ALL");

            companion object {
                fun fromLevel(level: Level): String {
                    return entries.find { it.level == level }?.displayName ?: ALL.displayName
                }
            }
        }

        /**
         * ## ログレコード整形メソッド
         * ### ログレコードを英語固定フォーマットで整形するメソッド
         * @param record ログレコード
         * @return 整形されたログ文字列
         * @author Masahiro Inoue
         * @since 2025-12-30
         */
        override fun format(record: LogRecord): String {
            val time = timeFormatter.format(Instant.ofEpochMilli(record.millis))
            val level = LogLevel.fromLevel(record.level)        // ← 英語固定（INFO / FINE）
            val logger = record.loggerName ?: "-"
            val msg = formatMessage(record)

            return "$time [$level] [$logger] $msg${System.lineSeparator()}"
        }
    }

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
        val dir = File(System.getProperty("user.dir"), "androrm-detekt-rules/logs")
        dir.mkdirs()

        // ★ 日付フォーマットを ISO 風に固定
        System.setProperty(
            "java.util.logging.SimpleFormatter.format",
            "%1\$tF %1\$tT [%4\$s] %5\$s%6\$s%n"
        )

        val logger = log
        logger.level = logLevel          // ★ Logger 側
        logger.useParentHandlers = false   // ★ Gradle / Console 汚染防止

        val fileHandler = FileHandler(
            dir.resolve("androrm-detekt.log").toString(),
            true
        )

        fileHandler.level = logger.level     // ★ Handler 側（重要）
        fileHandler.formatter = EnglishLevelFormatter()

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
        log.warning(logWarning(msg))
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
