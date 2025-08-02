package jp.pgw.lab78.generated.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import jp.pgw.lab78.androrm.database.AndrOrmDatabaseHelper
import org.junit.*
import org.junit.Assert.assertNotNull
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class AndrOrmDatabaseHelperTest {

    private lateinit var dbHelper: AndrOrmDatabaseHelper
    private lateinit var context: Context
    private lateinit var mockDb: SQLiteDatabase

    @Before
    fun setUp() {
        // Context をモックする
        context = ApplicationProvider.getApplicationContext()

        // DatabaseHelper を作成
        dbHelper = spy(AndrOrmDatabaseHelper(context, version = 1, entities = arrayOf(TestEntity::class)))

        // SQLiteDatabase をモック
        mockDb = mock(SQLiteDatabase::class.java)

        // dbHelper.onCreate(db) などを呼び出して、初期化をテスト
        dbHelper.onCreate(mockDb)
    }

    /**
     * onCreate テストメソッド
     */
    @Test
    fun onCreateTest() {
        assertNotNull(dbHelper)
        // verify(dbHelper).onCreate(mockDb) は不可 → spy を利用する
        verify(dbHelper, times(1)).onCreate(mockDb)

    }

    /**
     * onUpgrade テストメソッド
     */
    @Test
    fun onUpgradeTest() {
        assertNotNull(dbHelper)
    }
}