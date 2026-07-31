# AndrORM

> [!IMPORTANT]
> AndrORM is currently under development.
> This README targets version `0.1.2-alpha`.
> As this is an alpha release, the API and specifications may change in the future.

AndrORM is an SQLite ORM for Android and Kotlin that is currently under development.

It uses Kotlin `data class` declarations as table definitions and provides purpose-specific entity generation with KSP, a type-safe SQL builder, DML and SELECT execution, database upgrades, transactions, and SAVEPOINT support.

## Purpose

AndrORM is designed to improve readability and maintainability when working directly with SQLite.

- Centralize table definitions in Kotlin `data class` declarations
- Generate SQL for SELECT, INSERT, UPDATE, DELETE, UPSERT, and ABSERT
- Centrally manage SQL strings and bind values
- Automatically generate purpose-specific entities with KSP
- Create SQLite tables from entities
- Rebuild tables and migrate data when the database version is upgraded
- Retrieve SELECT results as entities, maps, or cursors
- Support transactions and partial rollback with SAVEPOINTs
- Statically validate entity definitions with custom Detekt rules

## Project Status

This project is currently under development.

The runtime, common definitions, KSP processor, Detekt rules, and shared library
are published as separate artifacts.

The first public release was `0.1.0-alpha`.

## Supported Environment

| Item | Version |
|---|---:|
| Kotlin | 1.9.24 |
| Android Gradle Plugin | 8.8.0 |
| Gradle Wrapper | 8.10.2 |
| JDK / JVM Target | 17 |
| compileSdk | 34 |
| targetSdk | 34 |
| minSdk | 24 |
| KSP | 1.9.24-1.0.20 |
| Detekt | 1.23.6 |
| JUnit Jupiter | 5.10.2 |

## Module Structure

| Module | Role |
|---|---|
| `app` | AndrORM behavior verification, samples, and Android tests |
| `androrm-runtime` | SQL builder, SQLite execution, database helper, and runtime entity conversion |
| `androrm-common` | Annotations, common metadata, DML marker interfaces, and DEFAULT value validation |
| `androrm-generator-ksp` | Generates purpose-specific entities based on `@Projection` |
| `androrm-detekt-rules` | Custom Detekt rules for AndrORM |
| `shared-library` | Utilities shared between modules |
| `test-support` | Test support such as shared unit-test converters |
| `ksp-fixtures` | Sources used to test KSP error cases |

The main dependencies are as follows.

```text
app
 ├─ androrm-runtime
 └─ androrm-generator-ksp (KSP)

androrm-runtime
 ├─ androrm-common
 └─ shared-library

androrm-generator-ksp
 ├─ androrm-common
 └─ shared-library

androrm-detekt-rules
 └─ androrm-common

androrm-common
 └─ shared-library
```

## Key Features

### Table Definitions

- `@Table`
- `@Column`
- `@PrimaryKey`
- `@Index`
- `@Unique`
- `@ColumnOldName`
- `@MigrationDefault`

### KSP Entity Generation

- `@Projection`
- `@Projections`
- `ColumnProjection`
- `FunctionProjection`
- `@EntityPackageInfo`
- Purpose-specific DML marker interfaces
- Per-DML generated-package configuration
- Custom generated subpackages using `commonInterface = [NOT_USE]` and `customInterface`

### Static Validation with Detekt

- Detect duplicate table names explicitly declared with `@Table` in the same source set
- Verify that entity properties referenced by `Select`, `join`, `where`, `having`, `on`, and `order` belong to the FROM entity or an entity that has already been joined
- Detect duplicate use of the same entity as the FROM entity and a JOIN target

Detekt is used to catch entity-definition and SELECT DSL reference errors before SQL is executed.

### SQL Generation

- `Create`
- `Select`
- `Insert`
- `Update`
- `Delete`
- `Upsert`
- `Absert`

`Absert` is an AndrORM-specific name and generates the following SQLite SQL.

```sql
INSERT ... ON CONFLICT (...) DO NOTHING
```

The name means "Insert if absent."

### SELECT

- DISTINCT
- INNER JOIN
- LEFT JOIN
- CROSS JOIN
- NATURAL JOIN
- ON clause

The current `JoinType` does not provide RIGHT JOIN or FULL JOIN.

- WHERE
- GROUP BY (automatically generated when `having` is specified)
- HAVING
- ORDER BY
- NULLS FIRST / NULLS LAST
- LIMIT
- OFFSET
- Subqueries
- EXISTS / NOT EXISTS
- Aggregate functions
- Scalar functions
- Raw conditions / raw expressions
- SQL bind-value management

### Condition DSL

The main conditional operations can be specified through the DSL.

- `eq` / `equal`
- `ne` / `notEqual`
- `gt` / `graterThan`
- `ge` / `graterEqual`
- `lt` / `lesserThan`
- `le` / `lessEqual`
- `like` / `notLike`
- `glob` / `notGlob`
- `inList` / `notInList`
- `inSelect` / `notInSelect`
- `between`
- `exists` / `notExists`
- `isNull` / `isNotNull`
- `and` / `or`
- Raw conditions using `condition`

## Setup

### Using AndrORM from Maven Central
To use AndrORM, all three artifacts—`androrm-runtime`, `androrm-generator-ksp`, and `androrm-detekt-rules`—must be added to the project.

Enable the KSP and Detekt plugins.
- Project root: `build.gradle.kts (<project-name>)`
```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.6" apply false
}
```
- Target module: `build.gradle.kts (:<module-name>)`
```kotlin
plugins {
    id("com.google.devtools.ksp")
    id("io.gitlab.arturbosch.detekt")
}
```
Add the AndrORM runtime, KSP processor, and Detekt rules.
- Target module: `build.gradle.kts (:<module-name>)`
```kotlin
dependencies {
    implementation("io.github.kawanagare-git:androrm-runtime:0.1.2-alpha")
    ksp("io.github.kawanagare-git:androrm-generator-ksp:0.1.2-alpha")
    detektPlugins("io.github.kawanagare-git:androrm-detekt-rules:0.1.2-alpha")
}
```
Enable Core Library Desugaring.
- Target module: `build.gradle.kts (:<module-name>)`
```kotlin
android {
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}
dependencies {
    coreLibraryDesugaring(
        "com.android.tools:desugar_jdk_libs:2.1.5"
    )
}
```
### Open the Project

Open the project root in Android Studio.

Set the Android SDK path in `local.properties`.

```properties
sdk.dir=C\:\\Users\\USER\\AppData\\Local\\Android\\Sdk
```

### Build

For Windows PowerShell:

```powershell
.\gradlew :app:assembleDebug
```

Unit tests:

```powershell
.\gradlew :app:testDebugUnitTest
.\gradlew :androrm-runtime:testDebugUnitTest
.\gradlew :androrm-common:test
.\gradlew :androrm-generator-ksp:test
.\gradlew :androrm-detekt-rules:test
.\gradlew :shared-library:test
```

Android tests:

```powershell
.\gradlew :app:connectedDebugAndroidTest
```

Detekt:

```powershell
.\gradlew detektAll
```

Tasks are also provided to analyze only the unit tests and Android tests in the `app` module.

```powershell
.\gradlew :app:detektUnitTestOnly
.\gradlew :app:detektAndroidTestOnly
```

## Entity Definitions

AndrORM entities may be written manually as Kotlin `data class` declarations or generated automatically with KSP.

KSP reduces the work required to create purpose-specific entities; it is not required to use AndrORM. Whether an entity is handwritten or generated, it can be used in the same way with the SQL builders and execution APIs.

### Generated Package

Create a file that specifies the package in which generated entities will be placed.

```kotlin
@file:EntityPackageInfo(
    basePackage = "com.example.database.entities",
)

package com.example.database.entities

import jp.pgw.lab78.androrm.common.annotation.EntityPackageInfo
```

By default, entities are generated in the following purpose-specific subpackages.

| Purpose | Generated Package |
|---|---|
| SELECT | `select` |
| INSERT | `insert` |
| UPDATE | `update` |
| UPSERT | `upsert` |
| ABSERT | `absert` |
| DELETE | `delete` |

### Example Table Definition

```kotlin
import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.ABSERT
import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.DELETE
import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.INSERT
import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.SELECT
import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.UPDATE
import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum.UPSERT
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.Projection
import jp.pgw.lab78.androrm.common.annotation.Projections
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.Index
import jp.pgw.lab78.androrm.common.database.annotation.MigrationDefault
import jp.pgw.lab78.androrm.common.database.annotation.PrimaryKey
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.database.annotation.Unique
import jp.pgw.lab78.androrm.common.dml.interfaces.ComprehensiveEntity
import java.time.LocalDateTime

@Projections(
    [
        Projection(
            entityNameExtend = "Select",
            properties = [
                ColumnProjection("id"),
                ColumnProjection("userCode"),
                ColumnProjection("userName"),
                ColumnProjection("enabled"),
            ],
            commonInterface = [SELECT],
        ),
        Projection(
            entityNameExtend = "Insert",
            properties = [
                ColumnProjection("id"),
                ColumnProjection("userCode"),
                ColumnProjection("userName"),
                ColumnProjection("enabled"),
                ColumnProjection("updatedAt"),
            ],
            commonInterface = [INSERT],
        ),
        Projection(
            entityNameExtend = "Update",
            properties = [
                ColumnProjection("userName"),
                ColumnProjection("enabled"),
                ColumnProjection("updatedAt"),
            ],
            commonInterface = [UPDATE],
        ),
        Projection(
            entityNameExtend = "Upsert",
            properties = [
                ColumnProjection("id"),
                ColumnProjection("userCode"),
                ColumnProjection("userName"),
                ColumnProjection("enabled"),
                ColumnProjection("updatedAt"),
            ],
            commonInterface = [UPSERT, ABSERT],
        ),
        Projection(
            entityNameExtend = "Delete",
            properties = [
                ColumnProjection("id", hideFromSelect = true),
            ],
            commonInterface = [DELETE],
        ),
    ],
)
@Table(name = "USER_MASTER", alias = "USER_MASTER")
@Index(properties = ["userName"])
@Unique(properties = ["userCode"])
data class UserMaster(
    @PrimaryKey
    val id: Int,

    @Column(name = "USER_CODE")
    val userCode: String,

    @Column(name = "USER_NAME")
    val userName: String,

    @Column(default = "1")
    @MigrationDefault("1")
    val enabled: Boolean,

    @Column(name = "UPDATED_AT", default = "CURRENT_TIMESTAMP_ISO")
    val updatedAt: LocalDateTime,
) : ComprehensiveEntity
```

### `@Projection`

`@Projection` generates purpose-specific `data class` declarations from the original table-definition entity.

| Argument | Description |
|---|---|
| `entityNameExtend` | Name appended to the generated class name |
| `aliasExtend` | Name appended to the table alias |
| `properties` | Properties to generate |
| `functions` | SQL function properties to generate |
| `commonInterface` | Common interfaces such as SELECT and INSERT |
| `customInterface` | Generated subpackage appended to `basePackage` when `commonInterface = [NOT_USE]` or when `commonInterface` is omitted |

Generated class names generally use the following format.

```text
OriginalClassName + entityNameExtend
```

Example:

```text
UserMaster + Select = UserMasterSelect
```

`customInterface` does not specify a Kotlin interface implemented by the generated class. Together with `commonInterface = [NOT_USE]`, it specifies a generated subpackage relative to `@EntityPackageInfo.basePackage`.

### `hideFromSelect`

A property with `ColumnProjection.hideFromSelect = true` remains in the entity metadata but is excluded from the SELECT list.

Use this for properties that are required for conditions or DELETE entities but are not needed in the query result.

## Entity Generation
Use the following command for normal KSP entity generation.
```powershell
.\gradlew.bat :app:kspDebugKotlin
```
When AndrORM entity definitions, `@Projection`, `@Projections`, or related declarations are added or changed, force the KSP task to run again.

```powershell
.\gradlew.bat :app:kspDebugKotlin --rerun-tasks
```
If entities are not generated by a normal KSP run, try this command first instead of immediately changing the source code or Gradle configuration.

## Location of AndrORM Entities

When generating entities with KSP in AndrORM, place Kotlin entity definitions annotated with `@Projection` or `@Projections` in the Kotlin source directory, not the Java source directory.

```text
src/main/kotlin
```

Example location:

```text
app/src/main/kotlin/com/example/database/entities/
```

If Kotlin files are placed under `src/main/java` as shown below, normal Kotlin compilation may succeed while KSP fails to detect the target classes, preventing purpose-specific entities from being generated.

```text
src/main/java
```

If the following message appears in the KSP log and no entities are generated, verify that the target Kotlin files are located under `src/main/kotlin`.

```text
findProjectionClasses: Exiting: []
```

### Customizing Generated Packages

The destination package can be changed with `@file:EntityPackageInfo` at the beginning of the Kotlin file. The file annotation must appear before the `package` declaration.

```kotlin
@file:EntityPackageInfo(
    basePackage = "com.example.database.entities",
    selectPackage = "select",
    insertPackage = "insert",
    updatePackage = "update",
    upsertPackage = "upsert",
    absertPackage = "absert",
    deletePackage = "delete",
)

package com.example.database.entities.define
```

Normally, the generated package combines `basePackage` with the subpackage corresponding to `commonInterface`. For example, the destination for `commonInterface = [SELECT]` is:

```text
com.example.database.entities.select
```

To generate an entity in an arbitrary subpackage without a standard DML interface, combine `commonInterface = [NOT_USE]` with `customInterface`.

```kotlin
Projection(
    entityNameExtend = "ManagementColumns",
    properties = [
        ColumnProjection("enabled"),
        ColumnProjection("createdAt"),
        ColumnProjection("updatedAt"),
    ],
    commonInterface = [NOT_USE],
    customInterface = ["interfaces.ManagementColumns"],
)
```

The fully qualified name of the generated class then has the following form:

```text
<basePackage>.interfaces.ManagementColumns.<source class name><entityNameExtend>
```

Example:

```text
com.example.database.entities.interfaces.ManagementColumns.UserMasterManagementColumns
```

If `customInterface` contains multiple values, the first non-blank value is used to determine the destination package.

## Defining Entities

AndrORM entities can be defined not only through KSP generation, but also manually as Kotlin `data class` declarations.

Properties shared by multiple tables can be separated into an interface or another common type and inherited by each entity. When `@Column` is applied to those shared properties, their column definitions can also be reused by each entity. However, key and index definitions are not inherited. Define `@PrimaryKey`, `@Index`, and `@Unique` directly on each applicable entity.

KSP-based entity generation is provided to reduce the effort required to define purpose-specific entities; it is not mandatory for using AndrORM. When defining an entity manually, add the required annotations such as `@Table`, `@Column`, and `@PrimaryKey`, and implement the marker interface corresponding to the intended purpose.

The following example shows a manually defined entity for SELECT operations.

```kotlin
import jp.pgw.lab78.androrm.common.database.annotation.Column
import jp.pgw.lab78.androrm.common.database.annotation.PrimaryKey
import jp.pgw.lab78.androrm.common.database.annotation.Table
import jp.pgw.lab78.androrm.common.dml.interfaces.SelectEntity

@Table(name = "USER_MASTER", alias = "U")
data class UserMasterManualSelect(
    @PrimaryKey
    @Column(name = "ID")
    val id: Int,

    @Column(name = "USER_NAME")
    val userName: String,
) : SelectEntity
```

A manually defined entity can be passed to the SQL builder in the same way as an entity generated by KSP.

```kotlin
val select = Select(UserMasterManualSelect::class)
    .where {
        UserMasterManualSelect::id eq 1
    }
```

> [!WARNING]
> A regular Kotlin class that is not a `data class` may work depending on its structure. However, AndrORM is tested on the assumption that entities are defined as `data class` declarations, so regular classes are outside the scope of guaranteed operation.

## CREATE TABLE

```kotlin
val query = Create(UserMaster::class).build()
```

`@Index` and `@Unique` are obtained as separate index-creation SQL statements.

```kotlin
val create = Create(UserMaster::class)

val createTableQuery = create.build()
val indexQueries = create.buildIndexQueries(indexVersion = 1)
```

`Create` generates columns in the order of the primary-constructor properties.

## SELECT

```kotlin
val select = Select(UserMasterSelect::class)
    .where {
        UserMasterSelect::enabled eq true
        and {
            UserMasterSelect::userName like "SAMPLE%"
        }
    }
    .order {
        UserMasterSelect::userName.asc
    }
    .limit(50)
    .offset(0)

val sql = select.build()
val bindValues = select.bindValues
```

`limit()` returns an intermediate object that provides `offset()`, `build()`, and `bindValues`.

Use `TableRef` to specify a table alias explicitly.

```kotlin
val userTable = TableRef(UserMasterSelect::class, "U")

val select = Select(userTable)
    .where {
        userTable[UserMasterSelect::id] gt 100
    }
```

### Automatic GROUP BY Generation

When `having` is specified, AndrORM automatically generates `GROUP BY` from the selected columns that are not aggregate functions. Users do not need to assemble the `GROUP BY` clause separately.

If function-column processing has already registered GROUP BY columns, those columns are retained and `having` does not generate duplicates.

## INSERT

```kotlin
val insert = Insert(UserMasterInsert::class)
    .addEntity(
        UserMasterInsert(
            id = 1,
            userCode = "SAMPLE001",
            userName = "SAMPLE USER",
            enabled = true,
            updatedAt = LocalDateTime.now(),
        )
    )

val sql = insert.build()
val bindValues = insert.bindValues
```

Multiple entities can be expanded into a single VALUES clause.

```kotlin
insert.addEntities(entityList)
```

## UPDATE

A SET clause can be generated from an entire entity.

```kotlin
val update = Update(UserMasterUpdate::class)
    .set(
        UserMasterUpdate(
            userName = "UPDATED USER",
            enabled = true,
            updatedAt = LocalDateTime.now(),
        )
    )
    .where {
        UserMasterUpdate::userName eq "OLD USER"
    }
```

The SET DSL can also be used.

```kotlin
val update = Update(UserMasterUpdate::class)
    .set {
        UserMasterUpdate::userName assign "UPDATED USER"
        UserMasterUpdate::enabled assign true
        UserMasterUpdate::updatedAt assign LocalDateTime.now()
    }
    .where {
        UserMasterUpdate::userName eq "OLD USER"
    }
```

### Updating All Rows

To prevent accidental updates, an UPDATE without a WHERE clause requires an explicit call to `updateAll()`.

```kotlin
val update = Update(UserMasterUpdate::class)
    .set {
        UserMasterUpdate::enabled assign false
    }
    .updateAll()
```

If both `where()` and `updateAll()` are specified on the same instance, the one called last determines the update scope.

## DELETE

```kotlin
val delete = Delete(UserMasterDelete::class)
    .where {
        UserMasterDelete::id eq 1
    }
```

### Deleting All Rows

To prevent accidental deletions, a DELETE without a WHERE clause requires an explicit call to `deleteAll()`.

```kotlin
val delete = Delete(UserMasterDelete::class)
    .deleteAll()
```

If both `where()` and `deleteAll()` are specified on the same instance, the one called last determines the deletion scope.

## UPSERT

Generates SQLite `INSERT ... ON CONFLICT ... DO UPDATE` SQL.

```kotlin
val upsert = Upsert(UserMasterUpsert::class)
    .addEntity(entity)
    .onConflict {
        column(UserMasterUpsert::id)
    }
    .set {
        UserMasterUpsert::userName assign excluded(UserMasterUpsert::userName)
        UserMasterUpsert::enabled assign excluded(UserMasterUpsert::enabled)
        UserMasterUpsert::updatedAt assign excluded(UserMasterUpsert::updatedAt)
    }
```

UPSERT requires the following:

- At least one entity
- At least one conflict-target column
- At least one column to update

## ABSERT

Generates SQLite `INSERT ... ON CONFLICT ... DO NOTHING` SQL.

```kotlin
val absert = Absert(UserMasterUpsert::class)
    .addEntity(entity)
    .onConflict {
        column(UserMasterUpsert::id)
    }
```

ABSERT requires the following:

- At least one entity
- At least one conflict-target column

## Executing SQL

### Database Helper

```kotlin
class AppDatabaseHelper(
    context: Context,
) : AndrOrmDatabaseHelper(
    context = context,
    databaseName = "app.db",
    version = 2,
    entities = listOf(
        UserMaster::class,
    ),
)
```

Using the vararg constructor:

```kotlin
class AppDatabaseHelper(
    context: Context,
) : AndrOrmDatabaseHelper(
    context,
    "app.db",
    2,
    UserMaster::class,
)
```

### DML Execution

```kotlin
val affectedRows = helper.executeDml(insert)
```

The SQL string and bind values can also be specified directly.

```kotlin
val affectedRows = helper.executeDml(
    query = sql,
    bindValues = bindValues,
)
```

`executeDml()` executes data-modification SQL such as INSERT, UPDATE, DELETE, UPSERT, and ABSERT, and returns the number of affected rows.

### SELECT Results

As entities:

```kotlin
val rows = helper.executeSelectAsEntityList(select)
```

As maps:

```kotlin
val rows = helper.executeSelectAsMapList(select)
```

As a cursor:

```kotlin
helper.executeSelectAsCursor(select).use { cursor ->
    // Use the cursor
}
```

The execution time of the most recently executed DML statement or map-based SELECT is stored in `queryExecutionTime` in nanoseconds.

## Transactions

```kotlin
helper.transaction {
    executeDml(insert)
    executeDml(update)
    executeDml(delete)
}
```

The transaction is committed when the block completes successfully.

If an exception propagates out of the block, `setTransactionSuccessful()` is not called and the entire transaction is rolled back.

A cursor obtained with `executeSelectAsCursor()` must be used within the transaction block.

## SAVEPOINT

```kotlin
val savepointResult = helper.transaction {
    executeDml(masterQuery)

    helper.savepoint("after_master") {
        executeDml(detailQuery1)
        executeDml(detailQuery2)
    }
}

if (savepointResult.isSuccess) {
    // Processing inside the SAVEPOINT succeeded
} else {
    // Changes made after the SAVEPOINT was created have been rolled back
    val cause = savepointResult.failure
}
```

As demonstrated by `AndrOrmDatabaseAndroidTest#step21_absertCombinedDataWithSavepoint`, operations before the SAVEPOINT and multiple operations inside it can be grouped in one `transaction`.

- `masterQuery` runs before the SAVEPOINT is created.
- If `detailQuery1` or `detailQuery2` throws an ordinary processing exception, only changes made after the SAVEPOINT was created are rolled back.
- `savepoint()` captures the exception in `SavepointResult.failure`, so the caller can inspect `isSuccess`.
- On success, the block's return value is stored in `SavepointResult.result`.
- If the outer `transaction` completes normally, operations before the SAVEPOINT and successful operations inside it are committed.

Normal processing exceptions inside a SAVEPOINT are handled as follows.

```text
Exception in block
→ ROLLBACK TO SAVEPOINT
→ RELEASE SAVEPOINT
→ Return SavepointResult(isSuccess = false)
```

If SAVEPOINT management itself fails, one of the following `RuntimeException` types is thrown.

- `NotCreatedSavepointException`
- `FailureRollbackException`
- `FailureReleaseException`

These exceptions are intended to propagate to the outer `transaction()` call, causing the entire transaction to be rolled back.

If the SAVEPOINT name is omitted, a unique name is generated internally.

## Database Upgrade

`AndrOrmDatabaseHelper#onUpgrade()` performs the following operations based on the registered entities.

1. Delete the migration table `<table-name>_new`
2. Create mappings between old and new columns
3. Create the new table
4. Transfer migratable data from the old table
5. Create indexes and unique indexes
6. Delete the old table
7. Rename the new table to the original table name

> `<table-name>_new` is reserved exclusively for upgrade processing. Do not use this name for user-created tables, because any table with that name will be deleted during an upgrade.

### Renaming Columns

Use `@ColumnOldName` when only the column name is changed.

```kotlin
@Column(name = "DISPLAY_NAME")
@ColumnOldName("USER_NAME")
val displayName: String
```

### Added Non-nullable Columns

A non-nullable column that does not exist in the old table requires `@MigrationDefault`.

```kotlin
@MigrationDefault("1")
val enabled: Boolean
```

`@MigrationDefault` is used only to populate existing rows during a database upgrade.

It is not used for the DEFAULT clause in CREATE TABLE.

### Difference from DEFAULT at Table Creation

```kotlin
@Column(default = "1")
@MigrationDefault("1")
val enabled: Boolean
```

| Annotation | Usage |
|---|---|
| `@Column(default = "...")` | DEFAULT clause in CREATE TABLE |
| `@MigrationDefault("...")` | Populating existing rows during a database upgrade |

A `data class` property default value is not used to populate rows during a database upgrade.

### Custom Column Mapping

Override `resolveColumnMappings()` when more complex column migration is required.

```kotlin
override fun resolveColumnMappings() =
    mapOf(
        UserMaster::class to listOf(
            "OLD_USER_CODE" to "USER_CODE",
        )
    )
```

## Kotlin Types and SQLite Types

| Kotlin Type | SQLite Column Type | Storage / Binding |
|---|---|---|
| `Int` | `INTEGER` | `bindLong` |
| `Long` | `INTEGER` | `bindLong` |
| `Float` | `REAL` | `bindDouble` |
| `Double` | `REAL` | `bindDouble` |
| `Boolean` | `INTEGER` | `false = 0`, `true = 1` |
| `String` | `TEXT` | `bindString` |
| `LocalDate` | `DATETIME` | String |
| `LocalTime` | `DATETIME` | String |
| `LocalDateTime` | `DATETIME` | String |
| `ByteArray` | `BLOB` | `bindBlob` |

Specifying an unsupported type results in an exception.

## Notes on Date and Time Data

SQLite allows type names such as `DATE` and `DATETIME` in column definitions, but stored values are not held in dedicated date/time types. They are managed as strings, numbers, or other storage formats.

AndrORM's standard type conversion stores `LocalDate`, `LocalTime`, and `LocalDateTime` values as strings and converts them back to their respective types when reading.

Use a consistent format for date and time values stored in the same column to avoid inconsistencies in searches, comparisons, sorting, and conversion when reading.

When using a custom date/time format, the user is responsible for managing its meaning and ensuring valid conversions.

## DEFAULT / MigrationDefault Values

The common validator validates the following values for each type.

| Kotlin Type | Main Allowed Values |
|---|---|
| `Int` / `Long` | Integer |
| `Float` / `Double` | Integer or decimal number |
| `Boolean` | `0` or `1` |
| `String` | SQL string enclosed in single quotes |
| `LocalDate` | `'yyyy-MM-dd'`, `CURRENT_DATE` |
| `LocalTime` | `'HH:mm:ss'`, `CURRENT_TIME` |
| `LocalDateTime` | `'yyyy-MM-ddTHH:mm:ss'`, `'yyyy-MM-dd HH:mm:ss'`, `CURRENT_TIMESTAMP`, `CURRENT_TIMESTAMP_ISO` |
| `ByteArray` | DEFAULT values are not supported |
| Nullable types | `NULL` is allowed |

`CURRENT_TIMESTAMP_ISO` is converted to the following expression when executed by SQLite.

```sql
(strftime('%Y-%m-%dT%H:%M:%f', 'now', 'localtime'))
```

## KSP Validation

KSP processing mainly validates the following.

- Invalid combinations of `@Column` and `@Function`
- Whether SELECT-target properties exist
- Duplicate aliases
- Target properties referenced by `ColumnProjection`
- Arguments of `FunctionProjection`
- Function return types
- Type compatibility of `@Column(default)`
- Type compatibility of `@MigrationDefault`
- Compatibility between generated interfaces and their purposes

Specify `FunctionProjection.returnHint` only when the return type cannot be inferred automatically.

```kotlin
ReturnHint.AUTO
ReturnHint.STRING
ReturnHint.INT
ReturnHint.LONG
ReturnHint.DOUBLE
ReturnHint.BOOLEAN
ReturnHint.DECIMAL
ReturnHint.DATE
ReturnHint.DATETIME
```

## Custom Detekt Rules

The following rules are implemented in `androrm-detekt-rules`.

- `AndrOrmDuplicateTableNameRule`
  - For entities implementing `TableDefinitionEntity`, detects duplicate table names explicitly declared with `@Table(name = ...)`
- `AndrOrmEntityRefRule`
  - Detects duplicate use of the same entity as the `Select` FROM entity and a JOIN target
  - Verifies that property references in `join`, `where`, `having`, `on`, and `order` belong to the FROM entity or an entity that has already been joined

## Tests

### Test Frameworks

| Category | Framework |
|---|---|
| Unit Test | JUnit 5 (Jupiter); JUnit 4 / Vintage compatibility is also enabled |
| Android Test | JUnit4 |

## Directory Structure

```text
AndrORM/
├─ app/
│  ├─ src/main/
│  ├─ src/test/
│  └─ src/androidTest/
├─ androrm-common/
├─ androrm-generator-ksp/
├─ androrm-detekt-rules/
├─ androrm-runtime/
├─ shared-library/
├─ test-support/
├─ ksp-fixtures/
├─ config/detekt/
├─ docs/
├─ UML/
├─ gradle/
├─ build.gradle.kts
├─ settings.gradle.kts
└─ LICENSE
```

## License

MIT License

Copyright (c) 2025 kawanagare-git
