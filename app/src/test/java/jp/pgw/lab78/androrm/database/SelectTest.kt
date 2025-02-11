package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.database.entities.select.TestSelectEntity
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SelectTest {

    companion object {
        @JvmStatic
        lateinit var SELECT: Select<*>

        @BeforeAll
        @JvmStatic
        fun initialize() {
            SELECT = Select(TestSelectEntity::class)
        }

    }

    @Test
    fun build() {
    }
}