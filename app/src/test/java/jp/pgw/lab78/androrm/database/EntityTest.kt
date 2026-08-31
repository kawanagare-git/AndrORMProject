package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.Entity
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.database.entities.define.TestAllEntity
import jp.pgw.lab78.androrm.database.entities.insert.TestInsertEntity
import jp.pgw.lab78.androrm.database.entities.select.TestSelectEntity
import jp.pgw.lab78.androrm.database.entities.select.TestSelectEntityWithAlias
import jp.pgw.lab78.androrm.database.utility.EntityManager.extractClassFromProperty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Entityプロパティから宣言元クラスを解決する処理を検証する。
 *
 * @author Masahiro Inoue
 * @since 2025-07-17
 */
class EntityTest {

    // Dummy entity for alias test
    /**
     * 明示的なテーブル別名を持つ検証用Entity。
     *
     * @author Masahiro Inoue
     * @since 2025-07-17
     */
    @Table(name = "test_table", alias = "t")
    data class AliasTestEntity(val id: Int) : SelectEntity

    // Dummy entity without alias
    /**
     * テーブル別名を省略した検証用Entity。
     *
     * @author Masahiro Inoue
     * @since 2025-07-17
     */
    @Table
    data class FallbackEntity(val id: Int) : SelectEntity

    /**
     * パラメータ化テストへ検証データを提供する。
     *
     * @author Masahiro Inoue
     * @since 2025-07-17
     */
    companion object {
        /**
         * Entityプロパティと期待する宣言元クラスの組を返す。
         *
         * @return 検証対象プロパティと期待クラスの一覧
         * @author Masahiro Inoue
         * @since 2025-07-17
         */
        @JvmStatic
        fun propertyProvider(): List<Pair<KProperty1<out Entity, *>, KClass<out Entity>>> = listOf(
            TestAllEntity::id to TestAllEntity::class,
            TestSelectEntity::name to TestSelectEntity::class,
            TestAllEntity::insertDateTime to TestAllEntity::class,
            TestInsertEntity::birthday to TestInsertEntity::class,
            TestSelectEntityWithAlias::address to TestSelectEntityWithAlias::class
        )
    }

    /**
     * Entityプロパティから正しい宣言元クラスが取得されることを検証する。
     *
     * @param testData 検証対象プロパティと期待クラスの組
     * @author Masahiro Inoue
     * @since 2025-07-17
     */
    @ParameterizedTest
    @MethodSource("propertyProvider")
    fun `extractClassFromProperty should return correct KClass`(
        testData: Pair<KProperty1<out Entity, *>, KClass<out Entity>>
    ) {
        val (property, expectedClass) = testData

        // 安全キャストするために明示的な関数呼び出し（ジェネリクス警告防止）
        val actual = property.extractClassFromProperty()

        assertEquals(expectedClass, actual)
    }
}
