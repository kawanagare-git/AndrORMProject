package jp.pgw.lab78.androrm.ksp.validator

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** ViewDefinitionEntityの物理カラム重複規則を仕様から検証する。 */
class ViewDefinitionValidationSupportTest {
    /** 元Viewの異なるプロパティが同じ物理名なら拒否する。 */
    @Test
    fun duplicateSourcePhysicalNamesAreRejected() {
        assertTrue(ViewDefinitionValidationSupport.hasDuplicatePhysicalNames(listOf("EMPLOYEE_ID", "employee_id")))
    }

    /** implicit snake_caseと明示@Column.nameの同一名を拒否する。 */
    @Test
    fun implicitAndExplicitPhysicalNamesAreRejected() {
        val names = listOf(
            ViewDefinitionValidationSupport.effectivePhysicalName("employeeId", null),
            ViewDefinitionValidationSupport.effectivePhysicalName("other", "employee_id"),
        )
        assertTrue(ViewDefinitionValidationSupport.hasDuplicatePhysicalNames(names))
    }

    /** 複数Projectionで同じView列を参照しても重複扱いにしない。 */
    @Test
    fun repeatedProjectionReferencesAreNotPhysicalDuplicates() {
        assertFalse(
            ViewDefinitionValidationSupport.hasDuplicatePhysicalNames(
                listOf(
                    ViewDefinitionValidationSupport.effectivePhysicalName("employeeId", null),
                ),
            ),
        )
    }

    /** 物理名の大小文字だけが異なる場合も拒否する。 */
    @Test
    fun caseOnlyPhysicalNameDifferenceIsRejected() {
        assertTrue(ViewDefinitionValidationSupport.hasDuplicatePhysicalNames(listOf("payload", "PAYLOAD")))
    }
}
