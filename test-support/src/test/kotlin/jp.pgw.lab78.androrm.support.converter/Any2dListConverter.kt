package jp.pgw.lab78.androrm.support.converter

import org.junit.jupiter.params.converter.ArgumentConversionException
import org.junit.jupiter.params.converter.SimpleArgumentConverter

/**
 * ## Any 2次元 List 変換コンバータ
 * ### "A:1^B:2" を listOf(listOf("A", 1), listOf("B", 2)) に変換する
 * @author Masahiro Inoue
 * @since 2026-05-20
 */
class Any2dListConverter : SimpleArgumentConverter() {

    /**
     * ## convert メソッド
     * ### ＠CsvSource 又は @CsvFileSource に指定されている項目を2次元リストに変換する
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
        // targetType が2次元リストではない場合
        if (!List::class.java.isAssignableFrom(targetType)) {
            throw ArgumentConversionException("Expected target type: List<List<Any?>>.")
        }
        // csv の元情報を基に変換の準備をする、但し、null の場合2次元の空リストを戻す
        val text = source?.toString() ?: return emptyList<List<Any?>>()
        // csv の元情報を基に2次元リストに変換
        return CsvTokenParser.parse2dList(text)
            .map { row ->
                row.map { token ->
                    CsvTokenParser.toAnyValue(token)
                }
            }
    }
}