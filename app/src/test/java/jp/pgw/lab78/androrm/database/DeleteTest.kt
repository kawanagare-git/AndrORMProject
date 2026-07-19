package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.database.entities.delete.TestAllEntityDelete
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * DELETE文の生成仕様を検証する。
 *
 * @author Masahiro Inoue
 * @since 2026-05-24
 */
class DeleteTest {

    /**
     * 全件削除を明示した場合のDELETE文を検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-24
     */
    @Test
    fun testDeleteAll() {
        val delete = Delete(TestAllEntityDelete::class)
        delete.deleteAll()
        val actual = delete.build()
        assertEquals("delete from TEST_ALL_ENTITY", actual)
    }

    /**
     * WHERE条件を指定したDELETE文とバインド値を検証する。
     *
     * @author Masahiro Inoue
     * @since 2026-05-24
     */
    @Test
    fun testDeleteSelect() {
        val delete = Delete(TestAllEntityDelete::class)
        delete.where { TestAllEntityDelete::address like "長野県%" }
        val actual = delete.build()
        val values = delete.bindValues
        assertEquals(
            "delete from TEST_ALL_ENTITY where TEST_ALL_ENTITY.ADDRESS like ?",
            actual
        )
        assertEquals(listOf("長野県%"), values)
    }
}