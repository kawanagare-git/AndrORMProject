package jp.pgw.lab78.shared.library

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 * @author Masahiro Inoue
 * @since 2026-07-17
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    /**
     * アプリケーションコンテキストのパッケージ名を検証する。
     * @author Masahiro Inoue
     * @since 2026-07-17
     */
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("jp.pgw.lab78.shared.library", appContext.packageName)
    }
}
