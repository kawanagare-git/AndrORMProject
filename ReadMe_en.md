# AndrORM

> [!IMPORTANT]
> AndrORM is currently under development.
> The current published version is `0.1.1-alpha`.
> Because this is an alpha release, APIs and specifications may change in future versions.

AndrORM is a SQLite ORM for Android and Kotlin that is currently under development.

It uses Kotlin `data class` declarations as table definitions and provides purpose-specific Entity generation with KSP, a type-safe SQL builder, DML and SELECT execution, database upgrades, transactions, and SAVEPOINT support.

## Purpose

AndrORM aims to improve readability and maintainability when working directly with SQLite.

- Centralize table definitions in Kotlin `data class` declarations
- Generate SQL for SELECT, INSERT, UPDATE, DELETE, UPSERT, and ABSERT
- Manage SQL strings and bind values in one place
- Automatically generate purpose-specific Entities with KSP
- Create SQLite tables from Entities
- Rebuild tables and migrate data during database version upgrades
- Retrieve SELECT results as Entity, Map, or Cursor data
- Support partial rollback with transactions and SAVEPOINTs
- Perform static validation of Entity definitions with custom Detekt rules

## Current Status

This project is under development.

The runtime, common definitions, KSP Processor, Detekt rules, and shared library are published as independent artifacts.

The first published version was `0.1.0-alpha`.

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
| `app` | AndrORM operation checks, samples, and Android Tests |
| `androrm-runtime` | SQL builder, SQLite execution, database helper, and runtime Entity conversion |
| `androrm-common` | Annotations, common metadata, DML marker interfaces, and DEFAULT value validation |
| `androrm-generator-ksp` | Generates purpose-specific Entities based on `@Projection` |
| `androrm-detekt-rules` | AndrORM-specific Detekt rules |
| `shared-library` | Common utilities shared among modules |
| `test-support` | Test support such as shared converters for Unit Tests |
| `ksp-fixtures` | Sources for KSP error-case validation |

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

## Main Features

### Table Definition

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
- Marker interfaces for each DML purpose
- Support for custom interfaces

### SQL Generation

- `Create`
- `Select`
- `Insert`
- `Update`
- `Delete`
- `Upsert`
- `Absert`

`Absert` is an AndrORM-specific term that generates the following SQLite SQL.

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
- GROUP BY
- HAVING
- ORDER BY
- NULLS FIRST / NULLS LAST
- LIMIT
- OFFSET
- Subqueries
- EXISTS / NOT EXISTS
- Aggregate functions
- Scalar functions
- Raw conditions and raw expressions
- SQL bind value management

### Condition DSL

The main condition operators can be specified through the DSL.

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
- Raw conditions with `condition`

## Setup

### Using AndrORM from Maven Central

To use AndrORM, all three artifacts—`androrm-runtime`, `androrm-generator-ksp`, and `androrm-detekt-rules`—must be included.

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

Add the AndrORM runtime, KSP Processor, and Detekt rules.

- Target module: `build.gradle.kts (:<module-name>)`

```kotlin
dependencies {
    implementation(
        "io.github.kawanagare-git:androrm-runtime:0.1.1-alpha"
    )
    ksp(
        "io.github.kawanagare-git:androrm-generator-ksp:0.1.1-alpha"
    )
    detektPlugins(
        "io.github.kawanagare-git:androrm-detekt-rules:0.1.1-alpha"
    )
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

### Opening the Project

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

Unit Tests:

```powershell
.\gradlew :app:testDebugUnitTest
.\gradlew :androrm-runtime:testDebugUnitTest
.\gradlew :androrm-common:test
.\gradlew :androrm-generator-ksp:test
.\gradlew :androrm-detekt-rules:test
.\gradlew :shared-library:test
```

Android Tests:

```powershell
.\gradlew :app:connectedDebugAndroidTest
```

Detekt:

```powershell
.\gradlew detektAll
```

Tasks are also provided for analyzing only the Unit Tests and Android Tests in the `app` module.

```powershell
.\gradlew :app:detektUnitTestOnly
.\gradlew :app:detektAndroidTestOnly
```

## Entity Definitions

### Generated Package

Create a file that specifies the package for generated Entities.

```kotlin
@file:EntityPackageInfo(
    basePackage = "com.example.database.entities",
)

package com.example.database.entities

import jp.pgw.lab78.androrm.common.annotation.EntityPackageInfo
```

By default, generated Entities are placed in the following subpackages according to purpose.

| Purpose | Generated Package |
|---|---|
| SELECT | `select` |
| INSERT | `insert` |
| UPDATE | `update` |
| UPSERT | `upsert` |
| ABSERT | `absert` |
| DELETE | `delete` |

### Table Definition Example

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

`@Projection` generates purpose-specific `data class` declarations from the original table-definition Entity.

| Argument | Description |
|---|---|
| `entityNameExtend` | Text appended to the generated class name |
| `aliasExtend` | Text appended to the table alias |
| `properties` | Properties to generate |
| `functions` | SQL function properties to generate |
| `commonInterface` | Common interfaces such as SELECT or INSERT |
| `customInterface` | User-defined interfaces |

Generated class names generally follow this format.

```text
Original class name + entityNameExtend
```

Example:

```text
UserMaster + Select = UserMasterSelect
```

### `hideFromSelect`

## Location of AndrORM Entity Definitions

When generating entities with AndrORM's KSP processor, Kotlin entity definitions annotated with `@Projection` or `@Projections` must be placed in the Kotlin source directory, not the Java source directory.

```text
src/main/kotlin
```

Example:

```text
app/src/main/kotlin/com/example/database/entities/
```

If Kotlin files are placed under `src/main/java`, the Kotlin compilation itself may succeed, but KSP may fail to detect the annotated classes, and the generated entities may not be created.

```text
src/main/java
```

If no entities are generated and the KSP log contains an entry similar to the following, confirm that the target Kotlin files are located under `src/main/kotlin`.

```text
findProjectionClasses: Exiting: []
```

A property with `ColumnProjection.hideFromSelect = true` remains in the Entity metadata but is excluded from the SELECT clause.

Use this for condition expressions, DELETE Entities, and other properties that are not needed in query results.

## CREATE TABLE

```kotlin
val query = Create(UserMaster::class).build()
```

SQL statements for `@Index` and `@Unique` are obtained separately.

```kotlin
val create = Create(UserMaster::class)

val createTableQuery = create.build()
val indexQueries = create.buildIndexQueries(indexVersion = 1)
```

`Create` generates columns in the property order of the primary constructor.

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

`limit()` returns an intermediate object through which `offset()`, `build()`, and `bindValues` are available.

Use `TableRef` to specify a table alias explicitly.

```kotlin
val userTable = TableRef(UserMasterSelect::class, "U")

val select = Select(userTable)
    .where {
        userTable[UserMasterSelect::id] gt 100
    }
```

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

Multiple records can be expanded into a single VALUES clause.

```kotlin
insert.addEntities(entityList)
```

## UPDATE

A SET clause can be generated from an entire Entity.

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

### Updating All Records

An UPDATE without a WHERE clause requires an explicit call to `updateAll()` as a safeguard against accidental operations.

```kotlin
val update = Update(UserMasterUpdate::class)
    .set {
        UserMasterUpdate::enabled assign false
    }
    .updateAll()
```

When `where()` and `updateAll()` are specified on the same instance, the method called later determines the effective update scope.

## DELETE

```kotlin
val delete = Delete(UserMasterDelete::class)
    .where {
        UserMasterDelete::id eq 1
    }
```

### Deleting All Records

A DELETE without a WHERE clause requires an explicit call to `deleteAll()` as a safeguard against accidental operations.

```kotlin
val delete = Delete(UserMasterDelete::class)
    .deleteAll()
```

When `where()` and `deleteAll()` are specified on the same instance, the method called later determines the effective delete scope.

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

UPSERT requires the following.

- At least one Entity
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

ABSERT requires the following.

- At least one Entity
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

When using the vararg constructor:

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

You can also specify the SQL string and bind values directly.

```kotlin
val affectedRows = helper.executeDml(
    query = sql,
    bindValues = bindValues,
)
```

`executeDml()` executes modifying statements such as INSERT, UPDATE, DELETE, UPSERT, and ABSERT, and returns the number of affected rows.

### SELECT Results

As Entities:

```kotlin
val rows = helper.executeSelectAsEntityList(select)
```

As Maps:

```kotlin
val rows = helper.executeSelectAsMapList(select)
```

As a Cursor:

```kotlin
helper.executeSelectAsCursor(select).use { cursor ->
    // Use the Cursor
}
```

The execution time of the most recently executed DML statement or Map-based SELECT is stored in `queryExecutionTime` in nanoseconds.

## Transactions

```kotlin
helper.transaction {
    executeDml(insert)
    executeDml(update)
    executeDml(delete)
}
```

The transaction is committed when the block completes successfully.

If an exception propagates out of the block, `setTransactionSuccessful()` is not called, so the entire transaction is rolled back.

A Cursor obtained with `executeSelectAsCursor()` should be used within the transaction block.

## SAVEPOINT

```kotlin
helper.transaction {
    executeDml(firstQuery)

    val secondResult = savepoint("second_process") {
        executeDml(secondQuery)
    }

    if (secondResult.isSuccess) {
        // The SAVEPOINT block completed successfully
    } else {
        // secondQuery has already been rolled back to the SAVEPOINT
        val cause = secondResult.failure
    }

    executeDml(thirdQuery)
}
```

Ordinary processing exceptions inside a SAVEPOINT block are handled as follows.

```text
Exception in block
→ ROLLBACK TO SAVEPOINT
→ RELEASE SAVEPOINT
→ Return SavepointResult(isSuccess = false)
```

If SAVEPOINT management itself fails, one of the following `RuntimeException` subclasses is thrown.

- `NotCreatedSavepointException`
- `FailureRollbackException`
- `FailureReleaseException`

These exceptions are expected to propagate to the outer `transaction()` call and cause the entire transaction to be rolled back.

If the SAVEPOINT name is omitted, an internally generated unique name is used.

## Database Upgrades

Based on the registered Entities, `AndrOrmDatabaseHelper#onUpgrade()` performs the following process.

1. Drop the migration table named `<table name>_new`
2. Create old-to-new column mappings
3. Create the new table
4. Transfer all data that can be migrated from the old table
5. Create INDEX and UNIQUE INDEX definitions
6. Drop the old table
7. Rename the new table to the original table name

> `<table name>_new` is a reserved name used exclusively by the upgrade process. Do not create a user table with this name, because it will be dropped during an upgrade.

### Renaming a Column

Use `@ColumnOldName` when only a column name is changed.

```kotlin
@Column(name = "DISPLAY_NAME")
@ColumnOldName("USER_NAME")
val displayName: String
```

### Adding a Non-nullable Column

A newly added non-nullable column that does not exist in the old table requires `@MigrationDefault`.

```kotlin
@MigrationDefault("1")
val enabled: Boolean
```

`@MigrationDefault` is used only to supply values for existing records during database upgrades.

It is not used for a DEFAULT clause in CREATE TABLE.

### Difference from CREATE-time DEFAULT Values

```kotlin
@Column(default = "1")
@MigrationDefault("1")
val enabled: Boolean
```

| Annotation | Usage |
|---|---|
| `@Column(default = "...")` | DEFAULT clause in CREATE TABLE |
| `@MigrationDefault("...")` | Value supplied to existing records during a database upgrade |

A property default value in a `data class` is not used as a replacement value during database upgrades.

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

An exception is thrown when an unsupported type is specified.

## Notes on Date and Time Data

SQLite allows type names such as `DATE` and `DATETIME` in column definitions, but the stored value is not maintained as a dedicated date-time type. It is stored as text, a number, or another supported SQLite storage type.

AndrORM's standard type conversion serializes `LocalDate`, `LocalTime`, and `LocalDateTime` as strings and converts them back to their respective types when reading data.

Use a consistent format throughout the application for date and time values stored in the same column so that searches, comparisons, ordering, and conversion during reads remain consistent.

When using a custom date-time format, the user is responsible for managing its meaning and ensuring valid conversions.

## DEFAULT and MigrationDefault Values

The common validator checks the following values according to the Kotlin type.

| Kotlin Type | Main Allowed Values |
|---|---|
| `Int` / `Long` | Integer |
| `Float` / `Double` | Integer or decimal |
| `Boolean` | `0` or `1` |
| `String` | SQL string enclosed in single quotation marks |
| `LocalDate` | `'yyyy-MM-dd'`, `CURRENT_DATE` |
| `LocalTime` | `'HH:mm:ss'`, `CURRENT_TIME` |
| `LocalDateTime` | `'yyyy-MM-ddTHH:mm:ss'`, `'yyyy-MM-dd HH:mm:ss'`, `CURRENT_TIMESTAMP`, `CURRENT_TIMESTAMP_ISO` |
| `ByteArray` | DEFAULT values are not supported |
| Nullable type | `NULL` is allowed |

At SQLite execution time, `CURRENT_TIMESTAMP_ISO` is converted to the following expression.

```sql
(strftime('%Y-%m-%dT%H:%M:%f', 'now', 'localtime'))
```

## KSP Validation

KSP processing primarily validates the following.

- Invalid combinations of `@Column` and `@Function`
- Presence of properties selected by SELECT
- Duplicate aliases
- Target properties referenced by `ColumnProjection`
- Arguments of `FunctionProjection`
- Function return types
- Type compatibility of `@Column(default)`
- Type compatibility of `@MigrationDefault`
- Compatibility between generated interfaces and their purposes

Specify `FunctionProjection.returnHint` only when the type cannot be inferred automatically.

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

`androrm-detekt-rules` provides the following rules.

- `AndrOrmDuplicateTableNameRule`
  - Detects duplicate table names across Entities
- `AndrOrmEntityRefRule`
  - Validates how AndrORM Entities are referenced

## Testing

### Test Frameworks

| Category | Framework |
|---|---|
| Unit Test | JUnit 5 (Jupiter), with JUnit 4 / Vintage compatibility also enabled |
| Android Test | JUnit 4 |

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
