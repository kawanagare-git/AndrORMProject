package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.MessageConstants.AE00038
import jp.pgw.lab78.androrm.database.entities.RuntimeTestAllEntityDelete
import org.junit.Test
import org.junit.jupiter.api.Assertions.*

/**
 * DELETE文の生成仕様を検証する。
 * @author Masahiro Inoue
 * @since 2026-05-24
 */
class DeleteTest {

    /**
     * 全件削除を明示した場合、WHERE句なしのDELETE文を生成し、
     * バインド値を保持しないことを検証する。
     * @author Masahiro Inoue
     * @since 2026-05-24
     */
    @Test
    fun testDeleteAll() {
        val delete = Delete(RuntimeTestAllEntityDelete::class)
            .deleteAll()
        assertEquals(
            "delete from TEST_ALL_ENTITY",
            delete.build(),
        )
        assertEquals(
            emptyList<Any?>(),
            delete.bindValues,
        )
    }

    /**
     * WHERE条件を指定したDELETE文とバインド値を検証する。
     * @author Masahiro Inoue
     * @since 2026-05-24
     */
    @Test
    fun testDeleteSelect() {
        val delete = Delete(RuntimeTestAllEntityDelete::class)
        delete.where { RuntimeTestAllEntityDelete::address like "長野県%" }
        val actual = delete.build()
        val values = delete.bindValues
        assertEquals(
            "delete from TEST_ALL_ENTITY where TEST_ALL_ENTITY.ADDRESS like ?",
            actual
        )
        assertEquals(listOf("長野県%"), values)
    }

    /**
     * WHERE条件もdeleteAllも指定されていない場合、
     * 全件削除SQLを生成しないことを検証する。
     * @author Masahiro Inoue
     * @since 2026-07-22
     */
    @Test
    fun testBuild_withoutWhereAndDeleteAll_throwsIllegalArgumentException() {
        val delete = Delete(RuntimeTestAllEntityDelete::class)
        val actual = assertThrows(IllegalArgumentException::class.java) {
            delete.build()
        }
        assertEquals(AE00038, actual.message)
    }

    /**
     * WHERE付きDELETE文を生成した後にdeleteAllを指定した場合、
     * 全件削除SQLへ再生成され、WHERE用バインド値が除外されることを検証する。
     * @author Masahiro Inoue
     * @since 2026-07-22
     */
    @Test
    fun testBuild_whereBuiltThenDeleteAll_rebuildsAllRecordsDelete() {
        val delete = Delete(RuntimeTestAllEntityDelete::class)
            .where {
                RuntimeTestAllEntityDelete::address like "長野県%"
            }
        assertEquals(
            "delete from TEST_ALL_ENTITY " +
                    "where TEST_ALL_ENTITY.ADDRESS like ?",
            delete.build(),
        )
        assertEquals(
            listOf("長野県%"),
            delete.bindValues,
        )
        delete.deleteAll()
        assertEquals(
            "delete from TEST_ALL_ENTITY",
            delete.build(),
        )
        assertEquals(
            emptyList<Any?>(),
            delete.bindValues,
        )
    }

    /**
     * deleteAllによる全件削除SQLを生成した後にWHEREを指定した場合、
     * 条件付きDELETE文へ再生成されることを検証する。
     * @author Masahiro Inoue
     * @since 2026-07-22
     */
    @Test
    fun testBuild_deleteAllBuiltThenWhere_rebuildsConditionalDelete() {
        val delete = Delete(RuntimeTestAllEntityDelete::class)
            .deleteAll()
        assertEquals(
            "delete from TEST_ALL_ENTITY",
            delete.build(),
        )
        delete.where {
            RuntimeTestAllEntityDelete::address like "長野県%"
        }
        assertEquals(
            "delete from TEST_ALL_ENTITY " +
                    "where TEST_ALL_ENTITY.ADDRESS like ?",
            delete.build(),
        )
        assertEquals(
            listOf("長野県%"),
            delete.bindValues,
        )
    }

    /**
     * 同じDeleteを複数回buildしても、
     * SQLおよびバインド値が変化しないことを検証する。
     * @author Masahiro Inoue
     * @since 2026-07-22
     */
    @Test
    fun testBuild_deleteAll_buildTwice_keepsQueryAndBindValues() {
        val delete = Delete(RuntimeTestAllEntityDelete::class)
            .deleteAll()
        val firstQuery = delete.build()
        val firstBindValues = delete.bindValues
        val secondQuery = delete.build()
        val secondBindValues = delete.bindValues
        assertEquals(firstQuery, secondQuery)
        assertEquals(
            "delete from TEST_ALL_ENTITY",
            secondQuery,
        )
        assertEquals(firstBindValues, secondBindValues)
        assertEquals(
            emptyList<Any?>(),
            secondBindValues,
        )
    }
}