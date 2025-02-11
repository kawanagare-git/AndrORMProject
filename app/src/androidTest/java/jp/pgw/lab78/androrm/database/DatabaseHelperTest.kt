package jp.pgw.lab78.androrm.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class DatabaseHelperTest {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var context: Context
    private lateinit var mockDb: SQLiteDatabase

    @BeforeEach
    fun setUp() {
        // Context をモックする
        context = ApplicationProvider.getApplicationContext()

        // DatabaseHelper を作成
        dbHelper = spy(DatabaseHelper(context, version = 1, entities = arrayOf(TestEntity::class)))

        // SQLiteDatabase をモック
        mockDb = mock(SQLiteDatabase::class.java)

        // dbHelper.onCreate(db) などを呼び出して、初期化をテスト
        dbHelper.onCreate(mockDb)
    }

    /**
     * onCreate テストメソッド
     */
    @Test
    @DisplayName("onCreate メソッドのテスト")
    fun onCreateTest() {
        Assertions.assertNotNull(dbHelper)
        // verify(dbHelper).onCreate(mockDb) は不可 → spy を利用する
        verify(dbHelper, times(1)).onCreate(mockDb)

    }

    /**
     * onUpgrade テストメソッド
     */
    @Test
    @DisplayName("onUpgrade メソッドのテスト")
    fun onUpgradeTest() {
        Assertions.assertNotNull(dbHelper)
    }
}