package jp.pgw.lab78.androrm.common.logging

import jp.pgw.lab78.androrm.common.MessageConstants.CE00010
import jp.pgw.lab78.androrm.common.logging.interfaces.AndrOrmLoggerLike
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.logging.FileHandler
import java.util.logging.Formatter
import java.util.logging.LogRecord
import java.util.logging.Logger

/**
 * ## ログスコープ列挙型
 * @author Masahiro Inoue
 * @since 2026-01-25
 */
enum class LogScope(private val moduleName: String) : AndrOrmLoggerLike {
    /** アプリケーションモジュール */
    APP("app"),

    /** 共通モジュール */
    COMMON("app"),

    /** DETEKTモジュール */
    DETEKT("androrm-detekt-rules"),

    /** GENERATORモジュール */
    GENERATOR("androrm-generator-ksp"),
    ;

    /** 最小ログレベル */
    private var minLogLevel: LogLevel = LogLevel.INFO

    /** ログ接尾辞 */
    private var logFileSuffix: String = ""

    /** ロガーインスタンス */
    private val logger by lazy {
        Logger.getLogger("$moduleName${if (logFileSuffix.isEmpty()) "" else "-$logFileSuffix"}")
    }

    /** ログファイル名 */
    private val logFilename by lazy {
        "androrm_${moduleName}${
            if (logFileSuffix.isNotEmpty()) {
                "_$logFileSuffix"
            } else {
                ""
            }
        }_${fileTimeFormatter.format(LocalDateTime.now())}"
    }

    /**
     * ## ロガー初期化メソッド
     * ### AndrOrmLoggerLike 実装メソッド
     * @param minLogLevel 最小ログレベル
     * @return ロガーインスタンス
     * @author Masahiro Inoue
     * @since 2026-01-25
     */
    override fun create(logFileSuffix: String, minLogLevel: LogLevel): Logger {
        this.minLogLevel = minLogLevel
        this.logFileSuffix = logFileSuffix
        // ★ 日付フォーマットを ISO 風に固定
        System.setProperty(
            "java.util.logging.SimpleFormatter.format",
            "%1\$tF %1\$tT [%4\$s] %5\$s%6\$s%n"
        )
        val dir = File(System.getProperty("user.dir"), "$LOG_ROOT/logs/$moduleName")
        dir.mkdirs()

        logger.level = minLogLevel.level
        logger.useParentHandlers = false

        val fileHandler = FileHandler(
            dir.resolve("$logFilename.log").toString(),
            true
        )
        // ★ Handler 側（重要）
        fileHandler.level = logger.level
        fileHandler.formatter = EnglishLevelFormatter()
        logger.addHandler(fileHandler)
        return logger
    }

    /**
     * ## ログ出力メソッド
     * ### AndrOrmLoggerLike 実装メソッド
     * @param message ログメッセージ
     * @author Masahiro Inoue
     * @since 2026-01-25
     */
    override fun log(logLevel: LogLevel, message: String) {
        if (minLogLevel.isLoggable(logLevel)) {
            logger.log(logLevel.level, ("[${this.moduleName}] $message"))
        }
    }

    companion object {
        /** 日付フォーマッタ（ファイル名用） */
        @Suppress("SpellCheckingInspection")
        private val fileTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS")
                .withZone(ZoneId.systemDefault())

        /** 日付フォーマッタ（ログテキスト用） */
        private val logTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())

        /**
         * ## 英語固定ログフォーマッタ
         * ### ログレコードを英語固定フォーマットで整形するフォーマッタクラス
         * @author Masahiro Inoue
         * @since 2026-01-25
         */
        class EnglishLevelFormatter : Formatter() {
            /**
             * ## ログレコード整形メソッド
             * ### ログレコードを英語固定フォーマットで整形するメソッド
             * @param record ログレコード
             * @return 整形されたログ文字列
             * @author Masahiro Inoue
             * @since 2026-01-25
             */
            override fun format(record: LogRecord): String {
                val time = logTimeFormatter.format(Instant.ofEpochMilli(record.millis))
                val level = LogLevel.fromLevel(record.level)        // ← 英語固定（INFO / FINE）
                val logger = record.loggerName ?: "-"
                val msg = formatMessage(record)

                return "$time [$level] [$logger] $msg${System.lineSeparator()}"
            }
        }

        /**
         * ## ログスコープ取得メソッド
         * ### 指定された名前に基づいて、対応するログスコープを取得する。
         * @param name ログスコープ名
         * @return 対応するログスコープ、存在しない場合は null
         * @author Masahiro Inoue
         * @since 2026-01-25
         */
        fun fromName(name: String): LogScope {
            return entries.find { it.name == name }
                ?: throw IllegalArgumentException(CE00010.format(name))
        }

        /** モジュール内のログ出力先 */
        private const val LOG_ROOT = "build"
    }
}