package jp.pgw.lab78.androrm.database.meta

import jp.pgw.lab78.androrm.common.MessageConstants.AE00007
import jp.pgw.lab78.androrm.common.MessageConstants.AE00008
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Function
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.function.ColumnFunction
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity
import jp.pgw.lab78.androrm.common.meta.PropertyMeta
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RuntimeEntityMetaFactoryTest {

    private val factory = RuntimeEntityMetaFactory()

    @Test
    fun testCreate_withExplicitTableAnnotation_returnsEntityMeta() {
        val actual = factory.create(ExplicitTableEntity::class)

        assertAll(
            {
                assertEquals(
                    ExplicitTableEntity::class.qualifiedName,
                    actual.defineEntityQualifiedName
                )
            },
            { assertEquals("ExplicitTableEntity", actual.entityName) },
            { assertEquals("EMPLOYEE", actual.tableName) },
            { assertEquals("EMP", actual.tableAlias) },
            {
                assertEquals(
                    listOf("id", "employeeName", "age"),
                    actual.properties.map { it.propertyName })
            },
        )
    }

    @Test
    fun testCreate_withoutTableAnnotation_usesClassNameSnakeCase() {
        val actual = factory.create(ImplicitTableEntity::class)

        assertAll(
            { assertEquals("IMPLICIT_TABLE_ENTITY", actual.tableName) },
            { assertEquals("IMPLICIT_TABLE_ENTITY", actual.tableAlias) },
        )
    }

    @Test
    fun testCreate_withBlankTableAnnotation_usesClassNameSnakeCase() {
        val actual = factory.create(BlankTableEntity::class)

        assertAll(
            { assertEquals("BLANK_TABLE_ENTITY", actual.tableName) },
            { assertEquals("BLANK_TABLE_ENTITY", actual.tableAlias) },
        )
    }

    @Test
    fun testCreate_withColumnAnnotation_returnsColumnPropertyMeta() {
        val actual = factory.create(ExplicitTableEntity::class)
        val property = actual.property("employeeName")

        assertAll(
            { assertEquals("employeeName", property.propertyName) },
            { assertEquals("EMPLOYEE_NAME", property.columnName) },
            { assertEquals("EMP_NAME", property.aliasName) },
            { assertFalse(property.isFunction) },
            { assertFalse(property.hideFromSelect) },
            { assertTrue(property.hasColumnAnnotation) },
            { assertFalse(property.hasFunctionAnnotation) },
            { assertNull(property.functionType) },
            { assertEquals(emptyList<String>(), property.functionArgs) },
            { assertEquals("", property.rawFunction) },
        )
    }

    @Test
    fun testCreate_withColumnHideFromSelect_returnsHiddenPropertyMeta() {
        val actual = factory.create(ExplicitTableEntity::class)
        val property = actual.property("age")

        assertAll(
            { assertEquals("AGE", property.columnName) },
            { assertEquals("AGE", property.aliasName) },
            { assertTrue(property.hideFromSelect) },
            { assertTrue(property.hasColumnAnnotation) },
            { assertFalse(property.hasFunctionAnnotation) },
        )
    }

    @Test
    fun testCreate_withoutColumnAnnotation_returnsImplicitColumnPropertyMeta() {
        val actual = factory.create(ImplicitTableEntity::class)
        val property = actual.property("employeeName")

        assertAll(
            { assertEquals("employeeName", property.propertyName) },
            { assertEquals("EMPLOYEE_NAME", property.columnName) },
            { assertEquals("EMPLOYEE_NAME", property.aliasName) },
            { assertFalse(property.isFunction) },
            { assertFalse(property.hideFromSelect) },
            { assertFalse(property.hasColumnAnnotation) },
            { assertFalse(property.hasFunctionAnnotation) },
        )
    }

    @Test
    fun testCreate_withBlankColumnAnnotation_usesPropertyNameSnakeCase() {
        val actual = factory.create(BlankColumnEntity::class)
        val property = actual.property("employeeName")

        assertAll(
            { assertEquals("EMPLOYEE_NAME", property.columnName) },
            { assertEquals("EMPLOYEE_NAME", property.aliasName) },
            { assertTrue(property.hasColumnAnnotation) },
        )
    }

    @Test
    fun testCreate_withFunctionAnnotation_returnsFunctionPropertyMeta() {
        val actual = factory.create(FunctionColumnEntity::class)
        val property = actual.property("totalGross")

        assertAll(
            { assertEquals("totalGross", property.propertyName) },
            { assertEquals("TOTAL_GROSS", property.columnName) },
            { assertEquals("TOTAL_GROSS", property.aliasName) },
            { assertTrue(property.isFunction) },
            { assertFalse(property.hideFromSelect) },
            { assertFalse(property.hasColumnAnnotation) },
            { assertTrue(property.hasFunctionAnnotation) },
            { assertEquals(ColumnFunction.SUM, property.functionType) },
            { assertEquals(listOf("GROSS"), property.functionArgs) },
            { assertEquals("", property.rawFunction) },
        )
    }

    @Test
    fun testCreate_withFunctionBlankAlias_usesPropertyNameSnakeCase() {
        val actual = factory.create(FunctionColumnEntity::class)
        val property = actual.property("countAll")

        assertAll(
            { assertEquals("COUNT_ALL", property.columnName) },
            { assertEquals("COUNT_ALL", property.aliasName) },
            { assertEquals(ColumnFunction.COUNT_ALL, property.functionType) },
            { assertEquals(emptyList<String>(), property.functionArgs) },
        )
    }

    @Test
    fun testCreate_withFunctionRaw_returnsRawFunctionPropertyMeta() {
        val actual = factory.create(FunctionColumnEntity::class)
        val property = actual.property("grossRank")

        assertAll(
            { assertEquals("GROSS_RANK", property.columnName) },
            { assertEquals("GROSS_RANK", property.aliasName) },
            { assertTrue(property.isFunction) },
            { assertTrue(property.hideFromSelect) },
            { assertEquals(ColumnFunction.CUSTOM, property.functionType) },
            { assertEquals(emptyList<String>(), property.functionArgs) },
            { assertEquals("row_number() over(order by GROSS desc)", property.rawFunction) },
        )
    }

    @Test
    fun testCreate_withColumnAndFunctionAnnotation_prioritizesFunction() {
        val actual = factory.create(ColumnAndFunctionEntity::class)
        val property = actual.property("totalGross")

        assertAll(
            { assertEquals("TOTAL_GROSS", property.columnName) },
            { assertEquals("TOTAL_GROSS", property.aliasName) },
            { assertTrue(property.isFunction) },
            { assertTrue(property.hasColumnAnnotation) },
            { assertTrue(property.hasFunctionAnnotation) },
            { assertEquals(ColumnFunction.SUM, property.functionType) },
            { assertEquals(listOf("GROSS"), property.functionArgs) },
        )
    }

    @Test
    fun testCreate_returnsPropertiesInConstructorParameterOrder() {
        val actual = factory.create(ConstructorOrderEntity::class)

        assertEquals(
            listOf("secondValue", "firstValue", "thirdValue"),
            actual.properties.map { it.propertyName },
        )
    }

    @Test
    fun testCreate_withoutPrimaryConstructor_throwsAE00007() {
        val actual = assertThrows<IllegalStateException> {
            factory.create(NoPrimaryConstructorEntity::class)
        }

        assertEquals(
            AE00007.format(NoPrimaryConstructorEntity::class.qualifiedName),
            actual.message,
        )
    }

    @Test
    fun testCreate_withoutConstructorProperty_throwsAE00008() {
        val actual = assertThrows<IllegalStateException> {
            factory.create(ConstructorParameterOnlyEntity::class)
        }

        assertEquals(
            AE00008.format("id", ConstructorParameterOnlyEntity::class.qualifiedName),
            actual.message,
        )
    }

    private fun jp.pgw.lab78.androrm.common.meta.EntityMeta.property(
        propertyName: String,
    ): PropertyMeta = properties.first { it.propertyName == propertyName }
}

@Table(name = "EMPLOYEE", alias = "EMP")
private data class ExplicitTableEntity(
    @Column(name = "ID")
    val id: Long,
    @Column(name = "EMPLOYEE_NAME", alias = "EMP_NAME")
    val employeeName: String,
    @Column(name = "AGE", hideFromSelect = true)
    val age: Int,
) : SelectEntity

private data class ImplicitTableEntity(
    val id: Long,
    val employeeName: String,
) : SelectEntity

@Table(name = "", alias = "")
private data class BlankTableEntity(
    val id: Long,
) : SelectEntity

private data class BlankColumnEntity(
    @Column(name = "", alias = "")
    val employeeName: String,
) : SelectEntity

@Table(name = "SALARY", alias = "SAL")
private data class FunctionColumnEntity(
    @Column(name = "GROSS")
    val gross: Long,
    @Function(
        columnFunction = ColumnFunction.SUM,
        alias = "TOTAL_GROSS",
        args = ["GROSS"],
    )
    val totalGross: Long,
    @Function(
        columnFunction = ColumnFunction.COUNT_ALL,
        alias = "",
    )
    val countAll: Long,
    @Function(
        columnFunction = ColumnFunction.CUSTOM,
        alias = "GROSS_RANK",
        raw = "row_number() over(order by GROSS desc)",
        hideFromSelect = true,
    )
    val grossRank: Long,
) : SelectEntity

private data class ColumnAndFunctionEntity(
    @Column(name = "TOTAL", alias = "TOTAL")
    @Function(
        columnFunction = ColumnFunction.SUM,
        alias = "TOTAL_GROSS",
        args = ["GROSS"],
    )
    val totalGross: Long,
) : SelectEntity

private data class ConstructorOrderEntity(
    @Column(name = "SECOND_VALUE")
    val secondValue: String,
    @Column(name = "FIRST_VALUE")
    val firstValue: Long,
    @Column(name = "THIRD_VALUE")
    val thirdValue: Int,
) : SelectEntity

private object NoPrimaryConstructorEntity : SelectEntity

@Suppress("UNUSED_PARAMETER")
private class ConstructorParameterOnlyEntity(id: Long) : SelectEntity
