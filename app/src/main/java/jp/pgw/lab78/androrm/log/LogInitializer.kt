package jp.pgw.lab78.androrm.log

import java.io.File

object LogInitializer {

    /**
     * LOG_DIR を環境に応じて初期化
     * - Android 実機/エミュレータ → context.filesDir/logs
     * - JVM ローカルテスト       → ./logs
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    fun initLogDir() {
        val logDirPath: String = if (isAndroidRuntime()) {
            try {
                val context = getAndroidContextSafely()
                File(context.filesDir, "logs").absolutePath
            } catch (e: Throwable) {
                File("logs").absolutePath
            }
        } else {
            File("logs").absolutePath
        }

        val logDir = File(logDirPath)
        println("Create log directory: ${logDir.absolutePath}")
        if (!logDir.exists()) logDir.mkdirs()

        System.setProperty("LOG_DIR", logDir.absolutePath)
    }

    /**
     * ## 簡易的な Android 実行環境判定
     * ### Android 環境かどうか判定
     * @return true: Android 実行環境 / false: JVM
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private fun isAndroidRuntime(): Boolean {
        return try {
            Class.forName("android.os.Build")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    /**
     * ## Android Context の取得
     * ### インスツルメントテスト・実機エミュレータで、
     * ### コンテキストを取得できるようにする
     * @return android.content.Context
     * @author Masahiro Inoue
     * @since 2025-08-01
     */
    private fun getAndroidContextSafely(): android.content.Context {
        val appClass = Class.forName("android.app.ActivityThread")
        val method = appClass.getMethod("currentApplication")
        return method.invoke(null) as android.content.Context
    }
}
