package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.MessageConstants.AE00004
import jp.pgw.lab78.androrm.common.MessageConstants.AE00005
import jp.pgw.lab78.androrm.common.MessageConstants.AE00010
import jp.pgw.lab78.androrm.common.MessageConstants.AE00039
import jp.pgw.lab78.androrm.common.MessageConstants.AE00040
import jp.pgw.lab78.androrm.common.MessageConstants.AE00041
import jp.pgw.lab78.androrm.common.MessageConstants.AE00042
import jp.pgw.lab78.androrm.common.MessageConstants.AE00043
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * ## UnionAllテスト
 * ### 複数SelectのSQL、結果列、句、バインド値、入力検証を確認する
 * @author Masahiro Inoue
 * @since 2026-09-01
 */
class UnionAllTest {
    /**
     * ## 基本UNION ALL生成検証
     * ### 2件のSelectを指定順に結合して結果Entityのaliasと項目aliasへ正規化することを確認する
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    @Test
    fun build_withTwoSelects_buildsNormalizedUnionAll() {
        val selectA = Select(UnionSourceA::class).where { UnionSourceA::id eq 10 }
        val selectB = Select(UnionSourceB::class).where { UnionSourceB::name eq "B" }

        val actual = UnionAll.unionAll(UnionResult::class, selectA, selectB)

        assertAll(
            {
                assertEquals(
                    "select UNION_ALL_RESULT.A_ID as UR_RESULT_ID, " +
                            "UNION_ALL_RESULT.A_NAME as UR_RESULT_NAME from (" +
                            "select A.ID as A_ID, A.NAME as A_NAME " +
                            "from SOURCE_A A where A.ID = ? union all " +
                            "select B.ID as B_ID, B.NAME as B_NAME " +
                            "from SOURCE_B B where B.NAME = ?) UNION_ALL_RESULT",
                    actual.build(),
                )
            },
            { assertEquals(listOf(10, "B"), actual.bindValues) },
        )
    }

    /**
     * ## 複数Selectバインド順検証
     * ### 3件のSelectと同一Selectの再利用を指定順に展開することを確認する
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    @Test
    fun build_withThreeSelects_mergesBindValuesInSqlOrder() {
        val selectA = Select(UnionSourceA::class).where { UnionSourceA::id eq 1 }
        val selectB = Select(UnionSourceB::class).where { UnionSourceB::id eq 2 }

        val actual = UnionAll(UnionResult::class, selectA, selectB, selectA)

        assertAll(
            { assertEquals(2, actual.build().split(" union all ").size - 1) },
            { assertEquals(listOf(1, 2, 1), actual.bindValues) },
        )
    }

    /**
     * ## ORDER・LIMIT・OFFSET検証
     * ### UNION ALL全体の句とバインド値がSQL順に追加されることを確認する
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    @Test
    fun build_withOrderLimitOffset_appendsCompoundClauses() {
        val actual = UnionAll.unionAll(
            UnionResult::class,
            Select(UnionSourceA::class).where { UnionSourceA::id eq 10 },
            Select(UnionSourceB::class).where { UnionSourceB::id eq 20 },
        ).order {
            UnionResult::id.desc
            UnionResult::name.nullsLast
        }.limit(30).offset(5)

        assertAll(
            {
                assertEquals(
                    "select UNION_ALL_RESULT.A_ID as UR_RESULT_ID, " +
                            "UNION_ALL_RESULT.A_NAME as UR_RESULT_NAME from (" +
                            "select A.ID as A_ID, A.NAME as A_NAME " +
                            "from SOURCE_A A where A.ID = ? union all " +
                            "select B.ID as B_ID, B.NAME as B_NAME " +
                            "from SOURCE_B B where B.ID = ?) UNION_ALL_RESULT " +
                            "order by UR_RESULT_ID desc nulls first, UR_RESULT_NAME asc nulls last " +
                            "limit ? offset ?",
                    actual.build(),
                )
            },
            { assertEquals(listOf(10, 20, 30, 5), actual.bindValues) },
        )
    }

    /**
     * ## LIMIT中間オブジェクト検証
     * ### LIMITだけを指定してSQLとバインド値を参照できることを確認する
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    @Test
    fun limit_withoutOffset_buildsAndExposesBindValues() {
        val limitClause = UnionAll.unionAll(
            UnionResult::class,
            Select(UnionSourceA::class),
            Select(UnionSourceB::class),
        ).limit(15)

        assertAll(
            { assertEquals(true, limitClause.build().endsWith("limit ?")) },
            { assertEquals(listOf(15), limitClause.bindValues) },
        )
    }

    /**
     * ## 再build検証
     * ### 複数回buildしても値が重複せず、構成Selectの変更が反映されることを確認する
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    @Test
    fun build_repeatedAfterComponentChange_rebuildsWithoutDuplicateBindValues() {
        val selectA = Select(UnionSourceA::class)
        val actual = UnionAll.unionAll(
            UnionResult::class,
            selectA,
            Select(UnionSourceB::class).where { UnionSourceB::id eq 20 },
        )
        actual.build()
        selectA.where { UnionSourceA::name eq "changed" }

        val first = actual.build()
        val second = actual.build()

        assertAll(
            { assertEquals(first, second) },
            { assertEquals(listOf("changed", 20), actual.bindValues) },
        )
    }

    /**
     * ## Select件数検証
     * ### 2件未満のSelectを拒否することを確認する
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    @Test
    fun constructor_withOneSelect_throwsIllegalArgumentException() {
        val actual = assertThrows<IllegalArgumentException> {
            UnionAll(UnionResult::class, Select(UnionSourceA::class))
        }

        assertEquals(AE00039, actual.message)
    }

    /**
     * ## 結果列数検証
     * ### 結果Entityと構成Selectの列数不一致を指定位置付きで拒否することを確認する
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    @Test
    fun build_withMismatchedColumnCount_throwsIllegalArgumentException() {
        val actual = assertThrows<IllegalArgumentException> {
            UnionAll.unionAll(
                UnionResult::class,
                Select(UnionSourceA::class),
                Select(UnionSingleColumnSource::class),
            ).build()
        }

        assertEquals(AE00040.format(2, 2, 1), actual.message)
    }

    /**
     * ## ORDER対象Entity検証
     * ### 結果Entity以外のプロパティをORDERへ指定できないことを確認する
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    @Test
    fun build_withForeignOrderProperty_throwsIllegalArgumentException() {
        val actual = assertThrows<IllegalArgumentException> {
            createUnionAll().order { UnionSourceA::id.asc }.build()
        }

        assertEquals(AE00041.format("id", "UnionResult"), actual.message)
    }

    /**
     * ## ORDER対象表示列検証
     * ### 結果Entityの非表示プロパティをORDERへ指定できないことを確認する
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    @Test
    fun build_withHiddenOrderProperty_throwsIllegalArgumentException() {
        val actual = assertThrows<IllegalArgumentException> {
            UnionAll.unionAll(
                UnionResultWithHiddenColumn::class,
                Select(UnionSourceA::class),
                Select(UnionSourceB::class),
            ).order { UnionResultWithHiddenColumn::hidden.asc }.build()
        }

        assertEquals(
            AE00041.format("hidden", "UnionResultWithHiddenColumn"),
            actual.message,
        )
    }

    /**
     * ## 構成Select ORDER検証
     * ### 構成Select自身のORDER BYを拒否することを確認する
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    @Test
    fun build_withComponentOrder_throwsIllegalArgumentException() {
        val selectA = Select(UnionSourceA::class).order { UnionSourceA::id.asc }

        val actual = assertThrows<IllegalArgumentException> {
            UnionAll.unionAll(UnionResult::class, selectA, Select(UnionSourceB::class)).build()
        }

        assertEquals(AE00042.format("order by", 1), actual.message)
    }

    /**
     * ## 構成Select LIMIT・OFFSET検証
     * ### 構成Select自身のLIMITとOFFSETを拒否することを確認する
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    @Test
    fun build_withComponentLimitOffset_throwsIllegalArgumentException() {
        val selectA = Select(UnionSourceA::class)
        selectA.limit(10).offset(2)

        val actual = assertThrows<IllegalArgumentException> {
            UnionAll.unionAll(UnionResult::class, selectA, Select(UnionSourceB::class)).build()
        }

        assertEquals(AE00042.format("limit", 1), actual.message)
    }

    /**
     * ## 結果カラム名重複検証
     * ### 派生テーブルへ公開するカラム名の重複を拒否することを確認する
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    @Test
    fun constructor_withDuplicateResultColumnNames_throwsIllegalArgumentException() {
        val actual = assertThrows<IllegalArgumentException> {
            UnionAll.unionAll(
                UnionResultWithDuplicateColumns::class,
                Select(UnionSourceA::class),
                Select(UnionSourceB::class),
            )
        }

        assertEquals(
            AE00043.format("ID", "UnionResultWithDuplicateColumns"),
            actual.message,
        )
    }

    /**
     * ## 句重複・負数検証
     * ### ORDER、LIMIT、OFFSETの重複と負数が既存Selectと同じ規則で拒否されることを確認する
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    @Test
    fun clauseValidation_rejectsDuplicatesAndNegativeValues() {
        val duplicateOrder = createUnionAll().order { UnionResult::id.asc }
        val orderError = assertThrows<IllegalStateException> {
            duplicateOrder.order { UnionResult::name.asc }
        }
        val duplicateLimit = createUnionAll()
        duplicateLimit.limit(1)
        val limitError = assertThrows<IllegalStateException> { duplicateLimit.limit(2) }
        val limitClause = createUnionAll().limit(10)
        limitClause.offset(1)
        val offsetError = assertThrows<IllegalStateException> { limitClause.offset(2) }
        val negativeLimit = assertThrows<IllegalArgumentException> { createUnionAll().limit(-1) }
        val negativeOffset = assertThrows<IllegalArgumentException> {
            createUnionAll().limit(1).offset(-1)
        }

        assertAll(
            { assertEquals(AE00010.format("UnionAll", "order"), orderError.message) },
            { assertEquals(AE00010.format("UnionAll", "limit"), limitError.message) },
            { assertEquals(AE00010.format("UnionAll", "offset"), offsetError.message) },
            { assertEquals(AE00004, negativeLimit.message) },
            { assertEquals(AE00005, negativeOffset.message) },
        )
    }

    /**
     * ## 共通UnionAll生成
     * @return 2件の無条件Selectを持つUnionAll
     * @author Masahiro Inoue
     * @since 2026-09-01
     */
    private fun createUnionAll(): UnionAll<UnionResult> = UnionAll.unionAll(
        UnionResult::class,
        Select(UnionSourceA::class),
        Select(UnionSourceB::class),
    )
}

/** UNION ALL元Aを表すテストEntity。 */
@Table(name = "SOURCE_A", alias = "A")
private data class UnionSourceA(
    @Column(name = "ID") val id: Int,
    @Column(name = "NAME") val name: String,
) : SelectEntity

/** UNION ALL元Bを表すテストEntity。 */
@Table(name = "SOURCE_B", alias = "B")
private data class UnionSourceB(
    @Column(name = "ID") val id: Int,
    @Column(name = "NAME") val name: String,
) : SelectEntity

/** UNION ALL元の1列Selectを表すテストEntity。 */
@Table(name = "SINGLE_SOURCE", alias = "S")
private data class UnionSingleColumnSource(
    @Column(name = "ID") val id: Int,
) : SelectEntity

/** UNION ALLの結果列を表すテストEntity。 */
@Table(name = "UNION_RESULT", alias = "UR")
private data class UnionResult(
    @Column(name = "ID", alias = "RESULT_ID") val id: Int,
    @Column(name = "NAME", alias = "RESULT_NAME") val name: String,
) : SelectEntity

/** 非表示列を持つUNION ALL結果を表すテストEntity。 */
@Table(name = "UNION_RESULT_HIDDEN", alias = "URH")
private data class UnionResultWithHiddenColumn(
    @Column(name = "ID") val id: Int,
    @Column(name = "NAME") val name: String,
    @Column(name = "HIDDEN", hideFromSelect = true) val hidden: String,
) : SelectEntity

/** 重複カラム名を持つUNION ALL結果を表すテストEntity。 */
@Table(name = "UNION_RESULT_DUPLICATE", alias = "URD")
private data class UnionResultWithDuplicateColumns(
    @Column(name = "ID", alias = "FIRST_ID") val firstId: Int,
    @Column(name = "ID", alias = "SECOND_ID") val secondId: Int,
) : SelectEntity
