import io.gitlab.arturbosch.detekt.test.compileAndLint
import jp.pgw.lab78.androrm.detekt.AndrOrmEntityRefRule
import kotlin.test.Test
import kotlin.test.assertEquals

class AndrOrmEntityRefRuleTest {

    @Test
    fun `property reference is detected where OK`() {
        val code = """
            class EmployeeEntity(val employeeId: Int)
            class DepartmentEntity(val employeeId: Int)
        
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

    @Test
    fun `property reference is detected NG 1`() {
        val code = """
            class EmployeeEntity(val employeeId: Int)
            class DepartmentEntity(val employeeId: Int)
            class DepartmentEntityX(val employeeIdDebug: Int)
        
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
        println(code)
        assertEquals(3, findings.size)
    }

    @Test
    fun `property reference is detected NG 2`() {
        val code = """
            class EmployeeEntity(val employeeId: Int)
            class DepartmentEntity(val employeeId: Int)
            class DepartmentEntityX(val employeeIdDebug: Int)
        
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

    @Test
    fun `property reference is detected NG 3`() {
        val code = """
            class EmployeeEntity(val employeeId: Int)
            class DepartmentEntity(val employeeId: Int)
            class DepartmentEntityX(val employeeIdDebug: Int)
        
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

    @Test
    fun `property reference is detected NG 4`() {
        val code = """
            class EmployeeEntity(val employeeId: Int)
            class DepartmentEntity(val employeeId: Int)
            class DepartmentEntityX(val employeeId: Int)
        
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

    @Test
    fun `property reference is detected NG 5`() {
        val code = """
            class EmployeeEntity(val employeeId: Int)
            class DepartmentEntity(val employeeId: Int)
            class DepartmentEntityX(val employeeIdDebug: Int)
        
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
