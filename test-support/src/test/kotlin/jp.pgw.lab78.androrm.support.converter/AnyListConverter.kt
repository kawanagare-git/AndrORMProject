package jp.pgw.lab78.androrm.support.converter

import org.junit.jupiter.params.converter.ArgumentConversionException
import org.junit.jupiter.params.converter.SimpleArgumentConverter

/**
 * ## Any List 変換コンバータ
 * ### "A:1:true:<null>" を listOf("A", 1, true, null) に変換する
 * @author Masahiro Inoue
 * @since 2026-05-20
 */
class AnyListConverter : SimpleArgumentConverter() {
    /**
     * ## convert メソッド
     * ### ＠CsvSource 又は @CsvFileSource に指定されている項目を配列やリストに変換する
     * @param source csv の元情報
     * @param targetType 変換先クラス
     * @return 変換後のインスタンス
     * @author Masahiro Inoue
     * @since 2026-05-20
     */
    override fun convert(
        source: Any?,
        targetType: Class<*>,
    ): Any {
        if (!List::class.java.isAssignableFrom(targetType)) {
            throw ArgumentConversionException("変換先は List<Any?> を想定しています。")
        }
        //
        val text = source?.toString() ?: return emptyList<Any?>()

        return CsvTokenParser.parseList(text)
            .map { token -> CsvTokenParser.toAnyValue(token) }
    }
}