package jp.pgw.lab78.androrm.common.database

import jp.pgw.lab78.androrm.common.database.SupportFunction.buildAlias
import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumnAlias
import jp.pgw.lab78.androrm.common.database.SupportFunction.getColumnName
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableAlias
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableAnnotation
import jp.pgw.lab78.androrm.common.database.SupportFunction.getTableName
import jp.pgw.lab78.androrm.common.database.SupportFunction.hasText
import jp.pgw.lab78.androrm.common.database.SupportFunction.isColumn
import jp.pgw.lab78.androrm.common.database.SupportFunction.isFunctionColumn
import jp.pgw.lab78.androrm.common.database.SupportFunction.isHiddenFromSelect
import jp.pgw.lab78.androrm.common.database.SupportFunction.ownerKClass
import jp.pgw.lab78.androrm.common.database.SupportFunction.simpleNameToSnakeCase
import jp.pgw.lab78.androrm.common.database.SupportFunction.toCamelCase
import jp.pgw.lab78.androrm.common.database.SupportFunction.toSnakeCase
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Function
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * ## SupportFunction テスト
 * ### database パッケージ向けサポート関数を検証する
 * @author Masahiro Inoue
 * @since 2026-05-22
 */
class SupportFunctionTest {

    /**
     * ## getTableAnnotation は @Table アノテーションを取得できる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("getTableAnnotation は @Table アノテーションを取得できる")
    @Test
    fun testGetTableAnnotation() {
        val actual = AnnotatedEntity::class.getTableAnnotation()
        assertEquals("EMPLOYEE", actual.name)
        assertEquals("EMP", actual.alias)
    }

    /**
     * ## getTableAnnotation は @Table がない Entity の場合に例外を投げる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("getTableAnnotation は @Table がない Entity の場合に例外を投げる")
    @Test
    fun testGetTableAnnotationThrowsWhenTableAnnotationDoesNotExist() {
        val actual = assertThrows(IllegalStateException::class.java) {
            NoTableEntity::class.getTableAnnotation()
        }
        assertTrue(
            actual.message?.contains("does not have the `@Table` annotation") == true,
            "例外メッセージが期待と異なります。message=${actual.message}",
        )
    }

    /**
     * ## getTableName は @Table.name があればそれを返せる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("getTableName は @Table.name があればそれを返せる")
    @Test
    fun testGetTableNameFromAnnotation() {
        val actual = AnnotatedEntity::class.getTableName()
        assertEquals("EMPLOYEE", actual)
    }

    /**
     * ## getTableName は @Table.name が空ならクラス名をスネークケース化して返せる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("getTableName は @Table.name が空ならクラス名をスネークケース化して返せる")
    @Test
    fun testGetTableNameFromClassName() {
        val actual = EmptyTableNameEntity::class.getTableName()
        assertEquals("EMPTY_TABLE_NAME_ENTITY", actual)
    }

    /**
     * ## getTableAlias は @Table.alias があればそれを返せる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("getTableAlias は @Table.alias があればそれを返せる")
    @Test
    fun testGetTableAliasFromAnnotation() {
        val actual = AnnotatedEntity::class.getTableAlias()
        assertEquals("EMP", actual)
    }

    /**
     * ## getTableAlias は @Table.alias が空ならクラス名をスネークケース化して返せる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("getTableAlias は @Table.alias が空ならクラス名をスネークケース化して返せる")
    @Test
    fun testGetTableAliasFromClassName() {
        val actual = EmptyTableAliasEntity::class.getTableAlias()
        assertEquals("EMPTY_ALIAS_ENTITY", actual)
    }

    /**
     * ## getColumn は @Column.name があればそれを返せる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("getColumn は @Column.name があればそれを返せる")
    @Test
    fun testGetColumnNameFromAnnotation() {
        val actual = AnnotatedEntity::explicitColumn.getColumnName()
        assertEquals("EMPLOYEE_ID", actual)
    }

    /**
     * ## getColumn は @Column.name が空ならプロパティ名をスネークケース化して返せる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("getColumn は @Column.name が空ならプロパティ名をスネークケース化して返せる")
    @Test
    fun testGetColumnFromPropertyNameWhenColumnNameNameIsBlank() {
        val actual = AnnotatedEntity::blankColumnName.getColumnName()
        assertEquals("BLANK_COLUMN_NAME", actual)
    }

    /**
     * ## getColumn は @Column がない場合もプロパティ名をスネークケース化して返せる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("getColumn は @Column がない場合もプロパティ名をスネークケース化して返せる")
    @Test
    fun testGetColumnFromPropertyNameWhenColumnNameAnnotationDoesNotExist() {
        val actual = AnnotatedEntity::noAnnotationProperty.getColumnName()
        assertEquals("NO_ANNOTATION_PROPERTY", actual)
    }

    /**
     * ## getColumnAlias は @Column.alias があればそれを返せる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("getColumnAlias は @Column.alias があればそれを返せる")
    @Test
    fun testGetColumnNameAliasFromAnnotation() {
        val actual = AnnotatedEntity::explicitColumn.getColumnAlias()
        assertEquals("EMPLOYEE_ID_ALIAS", actual)
    }

    /**
     * ## getColumnAlias は @Column.alias が空なら空文字を返せる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("getColumnAlias は @Column.alias が空なら空文字を返せる")
    @Test
    fun testGetColumnNameAliasReturnsEmptyWhenAliasIsBlank() {
        val actual = AnnotatedEntity::blankColumnName.getColumnAlias()
        assertEquals("", actual)
    }

    /**
     * ## simpleNameToSnakeCase は KClass の simpleName をスネークケース化できる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("simpleNameToSnakeCase は KClass の simpleName をスネークケース化できる")
    @Test
    fun testKClassSimpleNameToSnakeCase() {
        val actual = AnnotatedEntity::class.simpleNameToSnakeCase()
        assertEquals("ANNOTATED_ENTITY", actual)
    }

    /**
     * ## simpleNameToSnakeCase は KProperty1 の name をスネークケース化できる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("simpleNameToSnakeCase は KProperty1 の name をスネークケース化できる")
    @Test
    fun testKPropertySimpleNameToSnakeCase() {
        val actual = AnnotatedEntity::explicitColumn.simpleNameToSnakeCase()
        assertEquals("EXPLICIT_COLUMN", actual)
    }

    /**
     * ## buildAlias は tableAlias と extendAlias から alias を生成できる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("buildAlias は tableAlias と extendAlias から alias を生成できる")
    @ParameterizedTest(name = "[{index}] tableAlias={0}, extendAlias={1}, expected={2}")
    @CsvSource(
        value = [
            "EMP, ID, EMP_ID",
            "EMP, <empty>, EMP",
            "EMP, <null>, EMP",
            "<empty>, ID, <null>",
            "<null>, ID, <null>",
            "'   ', ID, <null>",
        ],
        nullValues = ["<null>"],
        emptyValue = "<empty>",
    )
    fun testBuildAlias(
        tableAlias: String?,
        extendAlias: String?,
        expected: String?,
    ) {
        val actual = buildAlias(
            tableAlias = tableAlias.normalizeEmptyToken(),
            extendAlias = extendAlias.normalizeEmptyToken(),
        )

        assertEquals(expected.normalizeEmptyToken(), actual)
    }

    /**
     * ## toSnakeCase はキャメルケースをスネークケースへ変換できる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("toSnakeCase はキャメルケースをスネークケースへ変換できる")
    @ParameterizedTest(name = "[{index}] source={0}, expected={1}")
    @CsvSource(
        "employeeId, EMPLOYEE_ID",
        "employeeSubId, EMPLOYEE_SUB_ID",
        "EmployeeEntity, EMPLOYEE_ENTITY",
        "ABC, ABC",
        "abc, ABC",
    )
    fun testToSnakeCase(
        source: String,
        expected: String,
    ) {
        val actual = source.toSnakeCase()

        assertEquals(expected, actual)
    }

    /**
     * ## toCamelCase はスネークケースをキャメルケースへ変換できる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("toCamelCase はスネークケースをキャメルケースへ変換できる")
    @ParameterizedTest(name = "[{index}] source={0}, expected={1}")
    @CsvSource(
        "EMPLOYEE_ID, employeeId",
        "EMPLOYEE_SUB_ID, employeeSubId",
        "EMPLOYEE_ENTITY, employeeEntity",
        "ABC, abc",
        "abc, abc",
    )
    fun testToCamelCase(
        source: String,
        expected: String,
    ) {
        val actual = source.toCamelCase()

        assertEquals(expected, actual)
    }

    /**
     * ## hasText は null または空白だけの場合 false を返せる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("hasText は null または空白だけの場合 false を返せる")
    @ParameterizedTest(name = "[{index}] source={0}, expected={1}")
    @CsvSource(
        value = [
            "<null>, false",
            "<empty>, false",
            "'   ', false",
            "ABC, true",
            "  ABC  , true",
        ],
        nullValues = ["<null>"],
        emptyValue = "<empty>",
    )
    fun testHasText(
        source: String?,
        expected: Boolean,
    ) {
        val actual = source.normalizeEmptyToken().hasText()

        assertEquals(expected, actual)
    }

    /**
     * ## isColumn は @Column の有無を判定できる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("isColumn は @Column の有無を判定できる")
    @Test
    fun testIsColumn() {
        assertTrue(AnnotatedEntity::explicitColumn.isColumn())
        assertTrue(AnnotatedEntity::hiddenColumn.isColumn())
        assertFalse(AnnotatedEntity::functionColumn.isColumn())
        assertFalse(AnnotatedEntity::noAnnotationProperty.isColumn())
    }

    /**
     * ## isFunctionColumn は @Function の有無を判定できる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("isFunctionColumn は @Function の有無を判定できる")
    @Test
    fun testIsFunctionColumn() {
        assertTrue(AnnotatedEntity::functionColumn.isFunctionColumn())
        assertFalse(AnnotatedEntity::explicitColumn.isFunctionColumn())
        assertFalse(AnnotatedEntity::noAnnotationProperty.isFunctionColumn())
    }

    /**
     * ## ownerKClass はプロパティの所有クラスを取得できる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("ownerKClass はプロパティの所有クラスを取得できる")
    @Test
    fun testOwnerKClass() {
        val actual = AnnotatedEntity::explicitColumn.ownerKClass()
        assertEquals(AnnotatedEntity::class, actual)
    }

    /**
     * ## isHiddenFromSelect は @Function.hideFromSelect を優先して判定できる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("isHiddenFromSelect は @Function.hideFromSelect を優先して判定できる")
    @Test
    fun testIsHiddenFromSelectFunction() {
        val actual = AnnotatedEntity::functionColumn.isHiddenFromSelect()
        assertTrue(actual)
    }

    /**
     * ## isHiddenFromSelect は @Column.hideFromSelect を判定できる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("isHiddenFromSelect は @Column.hideFromSelect を判定できる")
    @Test
    fun testIsHiddenFromSelectColumn() {
        assertTrue(AnnotatedEntity::hiddenColumn.isHiddenFromSelect())
        assertFalse(AnnotatedEntity::explicitColumn.isHiddenFromSelect())
    }

    /**
     * ## isHiddenFromSelect は @Function / @Column がない場合 false を返せる
     * ### SupportFunction が仕様どおりに処理することを検証する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @DisplayName("isHiddenFromSelect は @Function / @Column がない場合 false を返せる")
    @Test
    fun testIsHiddenFromSelectNoAnnotation() {
        val actual = AnnotatedEntity::noAnnotationProperty.isHiddenFromSelect()

        assertFalse(actual)
    }

    /**
     * ## 空文字トークン正規化
     * ### @CsvSource の <empty> を空文字に変換する
     * @receiver CsvSource から受け取った文字列
     * @return <empty> の場合は空文字、それ以外は元の値
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    private fun String?.normalizeEmptyToken(): String? =
        when (this) {
            "<empty>" -> ""
            else -> this
        }

    /**
     * ## アノテーション付きテストEntity
     * ### Table、Column、Function の各アノテーションを使用する検証データを定義する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @Table(name = "EMPLOYEE", alias = "EMP")
    private data class AnnotatedEntity(
        @property:Column(name = "EMPLOYEE_ID", alias = "EMPLOYEE_ID_ALIAS")
        val explicitColumn: String = "E001",

        @property:Column(name = "", alias = "")
        val blankColumnName: String = "blank",

        @property:Column(name = "HIDDEN_COLUMN", hideFromSelect = true)
        val hiddenColumn: String = "hidden",

        @property:Function(
            columnFunction = ColumnFunction.COUNT,
            alias = "COUNT_ALL",
            hideFromSelect = true
        )
        val functionColumn: Long = 0L,

        val noAnnotationProperty: String = "none",
    ) : Entity

    /**
     * ## テーブル名未指定テストEntity
     * ### 空のTable名からクラス名をテーブル名へ変換する検証データを定義する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @Table(name = "", alias = "ETA")
    private data class EmptyTableNameEntity(
        val id: String = "1",
    ) : Entity

    /**
     * ## テーブルエイリアス未指定テストEntity
     * ### 空のTableエイリアスから既定エイリアスを解決する検証データを定義する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    @Table(name = "EMPTY_ALIAS_TABLE", alias = "")
    private data class EmptyTableAliasEntity(
        val id: String = "1",
    ) : Entity

    /**
     * ## TableアノテーションなしテストEntity
     * ### Tableアノテーションが存在しない場合の例外を検証するデータを定義する
     * @author Masahiro Inoue
     * @since 2026-05-22
     */
    private data class NoTableEntity(
        val id: String = "1",
    ) : Entity
}