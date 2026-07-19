import io.gitlab.arturbosch.detekt.test.compileAndLint
import jp.pgw.lab78.androrm.detekt.AndrOrmEntityRefRule
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * ## Entity参照ルールテストクラス
 * ### クエリ内のプロパティ参照が対象Entityに属するかを検証する
 * @author Masahiro Inoue
 * @since 2026-07-17
 */
class AndrOrmEntityRefRuleTest {

    /**
     * ## 正しいプロパティ参照の検証
     * ### クエリ対象Entityのプロパティ参照が検出されないことを確認する
     * @author Masahiro Inoue
     * @since 2026-07-17
     */
    @Test
    fun `property reference is detected where OK`() {
        val code = """
            /**
             * Employeeを表すテスト用Entity
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            class EmployeeEntity(val employeeId: Int)
            /**
             * Departmentを表すテスト用Entity
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            class DepartmentEntity(val employeeId: Int)
        
            /**
             * 正しいEntity参照を含むクエリを構築する
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            fun query() {
                Select(EmployeeEntity::class)
                    .join(INNER, DepartmentEntity::class) on { 
                        EmployeeEntity::employeeId eq DepartmentEntity::employeeId 
                    }
                    .join(LEFT, DepartmentEntity::class) on { 
                        EmployeeEntity::employeeId eq DepartmentEntity::employeeId 
                    }
                    .where { EmployeeEntity::employeeId eq 10 }
                    .order( by = { EmployeeEntity::employeeId.asc })
            }
        """

        val findings = AndrOrmEntityRefRule().compileAndLint(code)

        assertEquals(0, findings.size)
    }

    /**
     * ## JOIN指定外Entity参照の検証
     * ### JOIN、WHEREおよびORDER BYの不正参照が検出されることを確認する
     * @author Masahiro Inoue
     * @since 2026-07-17
     */
    @Test
    fun `property reference is detected NG 1`() {
        val code = """
            /** Employeeを表すテスト用Entity
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            class EmployeeEntity(val employeeId: Int)
            /** Departmentを表すテスト用Entity
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            class DepartmentEntity(val employeeId: Int)
            /** クエリ対象外のDepartmentを表すテスト用Entity
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            class DepartmentEntityX(val employeeIdDebug: Int)
        
            /** 不正なEntity参照を含むクエリを構築する
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            fun query() {
                Select(EmployeeEntity::class)
                    .join(LEFT, DepartmentEntity::class ,on = { 
                        EmployeeEntity::employeeId eq DepartmentEntityX::employeeIdDebug 
                    })
                    .where { EmployeeEntity::employeeId eq DepartmentEntityX::employeeIdDebug }
                    .order(by = { DepartmentEntityX::employeeIdDebug.desc })
            }
        """

        val findings = AndrOrmEntityRefRule().compileAndLint(code)
        assertEquals(3, findings.size)
    }

    /**
     * ## JOIN条件内の対象外Entity参照の検証
     * ### JOIN条件に限って不正参照が検出されることを確認する
     * @author Masahiro Inoue
     * @since 2026-07-17
     */
    @Test
    fun `property reference is detected NG 2`() {
        val code = """
            /** Employeeを表すテスト用Entity
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            class EmployeeEntity(val employeeId: Int)
            /** Departmentを表すテスト用Entity
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            class DepartmentEntity(val employeeId: Int)
            /** クエリ対象外のDepartmentを表すテスト用Entity
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            class DepartmentEntityX(val employeeIdDebug: Int)
        
            /** 不正なJOIN参照を含むクエリを構築する
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            fun query() {
                Select(EmployeeEntity::class)
                    .join(LEFT, DepartmentEntity::class).on { 
                        EmployeeEntity::employeeId eq DepartmentEntityX::employeeIdDebug 
                    }
                    .where { EmployeeEntity::employeeId eq DepartmentEntity::employeeId }
                    .order { DepartmentEntity::employeeId.desc }
            }
        """

        val findings = AndrOrmEntityRefRule().compileAndLint(code)
        assertEquals(1, findings.size)
    }

    /**
     * ## 名前付き引数JOIN内の対象外Entity参照の検証
     * ### 名前付き引数で記述したJOIN条件でも不正参照が検出されることを確認する
     * @author Masahiro Inoue
     * @since 2026-07-17
     */
    @Test
    fun `property reference is detected NG 3`() {
        val code = """
            /** Employeeを表すテスト用Entity
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            class EmployeeEntity(val employeeId: Int)
            /** Departmentを表すテスト用Entity
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            class DepartmentEntity(val employeeId: Int)
            /** クエリ対象外のDepartmentを表すテスト用Entity
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            class DepartmentEntityX(val employeeIdDebug: Int)
        
            /** 名前付き引数による不正なJOIN参照を含むクエリを構築する
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            fun query() {
                Select(fromEntity = EmployeeEntity::class)
                    .join( on = { 
                        EmployeeEntity::employeeId eq DepartmentEntityX::employeeIdDebug 
                    }, joinType = LEFT, joinedEntity = DepartmentEntity::class,)
                    .where { EmployeeEntity::employeeId eq DepartmentEntity::employeeId }
                    .order { DepartmentEntity::employeeId.desc }
            }
        """

        val findings = AndrOrmEntityRefRule().compileAndLint(code)
        assertEquals(1, findings.size)
    }

    /**
     * ## ORDER BY内の対象外Entity参照の検証
     * ### 同名プロパティでも対象外Entityの参照が検出されることを確認する
     * @author Masahiro Inoue
     * @since 2026-07-17
     */
    @Test
    fun `property reference is detected NG 4`() {
        val code = """
            /** Employeeを表すテスト用Entity
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            class EmployeeEntity(val employeeId: Int)
            /** Departmentを表すテスト用Entity
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            class DepartmentEntity(val employeeId: Int)
            /** 同名プロパティを持つ対象外Departmentを表すテスト用Entity
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            class DepartmentEntityX(val employeeId: Int)
        
            /** ORDER BYに不正なEntity参照を含むクエリを構築する
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            fun query() {
                Select(EmployeeEntity::class)
                    .join(LEFT, DepartmentEntity::class).on { 
                        EmployeeEntity::employeeId eq DepartmentEntity::employeeId 
                    }
                    .where { EmployeeEntity::employeeId eq DepartmentEntity::employeeId }
                    .order (by ={ DepartmentEntityX::employeeId.desc })
            }
        """

        val findings = AndrOrmEntityRefRule().compileAndLint(code)
        assertEquals(1, findings.size)
    }

    /**
     * ## WHERE内の対象外Entity参照の検証
     * ### WHERE条件に限って不正参照が検出されることを確認する
     * @author Masahiro Inoue
     * @since 2026-07-17
     */
    @Test
    fun `property reference is detected NG 5`() {
        val code = """
            /** Employeeを表すテスト用Entity
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            class EmployeeEntity(val employeeId: Int)
            /** Departmentを表すテスト用Entity
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            class DepartmentEntity(val employeeId: Int)
            /** クエリ対象外のDepartmentを表すテスト用Entity
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            class DepartmentEntityX(val employeeIdDebug: Int)
        
            /** WHEREに不正なEntity参照を含むクエリを構築する
             * @author Masahiro Inoue
             * @since 2026-07-17
             */
            fun query() {
                Select(EmployeeEntity::class)
                    .join(LEFT, DepartmentEntity::class ,on = { 
                        EmployeeEntity::employeeId eq DepartmentEntity::employeeId 
                    })
                    .where { EmployeeEntity::employeeId eq DepartmentEntityX::employeeIdDebug }
                    .order (by ={ DepartmentEntity::employeeId.desc })
            }
        """

        val findings = AndrOrmEntityRefRule().compileAndLint(code)
        assertEquals(1, findings.size)
    }
}
