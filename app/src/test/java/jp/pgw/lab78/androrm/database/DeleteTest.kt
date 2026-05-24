package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.database.entities.delete.TestAllEntityDelete
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class DeleteTest {

    @Test
    fun testDeleteAll() {
        val delete = Delete(TestAllEntityDelete::class)
        delete.deleteAll()
        val actual = delete.build()
        println(actual)
        assertEquals("delete from TEST_ALL_ENTITY", actual)
    }

    @Test
    fun testDeleteSelect() {
        val delete = Delete(TestAllEntityDelete::class)
        delete.where { TestAllEntityDelete::address like "長野県%" }
        val actual = delete.build()
        val values = delete.bindValues
        println("$actual / values=$values")
        assertEquals(
            "delete from TEST_ALL_ENTITY where TEST_ALL_ENTITY_DELETE.ADDRESS like ?",
            actual
        )
        assertEquals(listOf("長野県%"), values)
    }
}