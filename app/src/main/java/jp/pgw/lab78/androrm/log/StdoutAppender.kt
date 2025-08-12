package jp.pgw.lab78.androrm.log

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.OutputStreamAppender
import ch.qos.logback.core.encoder.LayoutWrappingEncoder

class StdoutAppender : OutputStreamAppender<ILoggingEvent>() {
    init {
        // System.outに出力するようセット
        outputStream = System.out

        // エンコーダはLayoutWrappingEncoderを使う（パターンはXML側で指定）
        encoder = LayoutWrappingEncoder<ILoggingEvent>()
    }
}
