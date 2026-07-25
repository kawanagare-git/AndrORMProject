# AndrORM

> [!IMPORTANT]
> AndrORM is currently under development.
> The current published version is `0.1.1-alpha`.
> Because this is an alpha release, APIs and specifications may change in future versions.

AndrORM is a SQLite ORM for Android and Kotlin that is currently under development.

It uses Kotlin `data class` definitions as table definitions and provides KSP-based entity generation for different purposes, type-safe SQL builders, DML and SELECT execution, database upgrades, transactions, and SAVEPOINT support.

## Purpose

AndrORM is designed to improve readability and maintainability when working directly with SQLite.

- Centralize table definitions in Kotlin `data class` declarations
- Generate SQL for SELECT, INSERT, UPDATE, DELETE, UPSERT, and ABSERT
- Centrally manage SQL strings and bind values
- Automatically generate purpose-specific entities using KSP
- Create SQLite tables from entities
- Rebuild tables and migrate data during database version upgrades
- Retrieve SELECT results as Entity, Map, or Cursor values
- Support partial rollback through transactions and SAVEPOINTs
- Perform static validation of entity definitions using custom Detekt rules

## Current Status

This project is still under development.

The runtime, common definitions, KSP processor, and shared library are structured so that they can be published as independent artifacts.

The initial release version is `0.1.0-alpha`.

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
| `app` | AndrORM verification, samples, and Android Tests |
| `androrm-runtime` | SQL builders, SQLite execution, database helper, and runtime entity conversion |
| `androrm-common` | Annotations, shared metadata, DML marker interfaces, and DEFAULT value validation |
| `androrm-generator-ksp` | Generates purpose-specific entities based on `@Projection` |
| `androrm-detekt-rules` | AndrORM-specific Detekt rules |
| `shared-library` | Shared utilities used across modules |
| `test-support` | Test support utilities, including common Unit Test converters |
| `ksp-fixtures` | Source files used to validate KSP error cases |

The main dependencies are as follows:

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
- Additional custom interfaces

### SQL Generation

- `Create`
- `Select`
- `Insert`
- `Update`
- `Delete`
- `Upsert`
- `Absert`

`Absert` is an AndrORM-specific name that generates the following SQLite statement:

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
- Raw conditions / raw expressions
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
- Raw conditions using `condition`

## Setup

### Using AndrORM from Maven Central

AndrORM requires all three artifacts: `androrm-runtime`,
`androrm-generator-ksp`, and `androrm-detekt-rules`.

Enable the KSP and Detekt plugins.

```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.9.24-1.0.20"
    id("io.gitlab.arturbosch.detekt") version "1.23.6"
}
```

Add the AndrORM runtime, KSP processor, and Detekt rules.

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

Tasks are also available to analyze only the Unit Tests and Android Tests in the `app` module.

```powershell
.\gradlew :app:detektUnitTestOnly
.\gradlew :app:detektAndroidTestOnly
```

## Entity Definitions

### Generated Package

Create a file that specifies the package where generated entities will be placed.

```kotlin
@file:EntityPackageInfo(
    basePackage = "com.example.database.entities",
)

package com.example.database.entities

import jp.pgw.lab78.androrm.common.annotation.EntityPackageInfo
```

By default, entities are generated in the following subpackages according to their purpose.

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

`@Projection` generates purpose-specific `data class` declarations from the original table definition entity.

| Argument | Description |
|---|---|
| `entityNameExtend` | Text appended to the generated class name |
| `aliasExtend` | Text appended to the table alias |
| `properties` | Properties to include in the generated class |
| `functions` | SQL function properties to include in the generated class |
| `commonInterface` | Common interfaces such as SELECT and INSERT |
| `customInterface` | User-defined interfaces |

Generated class names generally use the following format:

```text
Original class name + entityNameExtend
```

Example:

```text
UserMaster + Select = UserMasterSelect
```

### `hideFromSelect`

A property with `ColumnProjection.hideFromSelect = true` remains in the entity metadata but is excluded from the SELECT projection list.

This is used for properties that are needed for conditions or DELETE entities but do not need to be included in query results.

## CREATE TABLE

```kotlin
val query = Create(UserMaster::class).build()
```

The SQL statements for `@Index` and `@Unique` are obtained separately.

```kotlin
val create = Create(UserMaster::class)

val createTableQuery = create.build()
val indexQueries = create.buildIndexQueries(indexVersion = 1)
```

`Create` generates columns in the order of the properties in the primary constructor.

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

### Update All Rows

An UPDATE without a WHERE clause requires an explicit `updateAll()` call to prevent unintended operations.

```kotlin
val update = Update(UserMasterUpdate::class)
    .set {
        UserMasterUpdate::enabled assign false
    }
    .updateAll()
```

When both `where()` and `updateAll()` are specified on the same instance, the one called later determines the update scope.

## DELETE

```kotlin
val delete = Delete(UserMasterDelete::class)
    .where {
        UserMasterDelete::id eq 1
    }
```

### Delete All Rows

A DELETE without a WHERE clause requires an explicit `deleteAll()` call to prevent unintended operations.

```kotlin
val delete = Delete(UserMasterDelete::class)
    .deleteAll()
```

When both `where()` and `deleteAll()` are specified on the same instance, the one called later determines the delete scope.

## UPSERT

Generates SQLite `INSERT ... ON CONFLICT ... DO UPDATE`.

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

- One or more entities
- One or more conflict target columns
- One or more columns to update

## ABSERT

Generates SQLite `INSERT ... ON CONFLICT ... DO NOTHING`.

```kotlin
val absert = Absert(UserMasterUpsert::class)
    .addEntity(entity)
    .onConflict {
        column(UserMasterUpsert::id)
    }
```

ABSERT requires the following:

- One or more entities
- One or more conflict target columns

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

A vararg constructor can also be used.

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

### Executing DML

```kotlin
val affectedRows = helper.executeDml(insert)
```

A SQL string and bind values can also be specified directly.

```kotlin
val affectedRows = helper.executeDml(
    query = sql,
    bindValues = bindValues,
)
```

`executeDml()` executes data modification statements such as INSERT, UPDATE, DELETE, UPSERT, and ABSERT, and returns the number of affected rows.

### SELECT Results

Entity format:

```kotlin
val rows = helper.executeSelectAsEntityList(select)
```

Map format:

```kotlin
val rows = helper.executeSelectAsMapList(select)
```

Cursor format:

```kotlin
helper.executeSelectAsCursor(select).use { cursor ->
    // Use the Cursor here
}
```

The execution time of the most recently executed DML statement or Map-format SELECT is stored in `queryExecutionTime` in nanoseconds.

## Transactions

```kotlin
helper.transaction {
    executeDml(insert)
    executeDml(update)
    executeDml(delete)
}
```

The transaction is committed when the block completes successfully.

If an exception propagates outside the block, `setTransactionSuccessful()` is not called, and the entire transaction is rolled back.

A Cursor returned by `executeSelectAsCursor()` must be used within the transaction block.

## SAVEPOINT

```kotlin
helper.transaction {
    executeDml(firstQuery)

    val secondResult = savepoint("second_process") {
        executeDml(secondQuery)
    }

    if (secondResult.isSuccess) {
        // Processing within the SAVEPOINT succeeded
    } else {
        // secondQuery has already been rolled back to the SAVEPOINT
        val cause = secondResult.failure
    }

    executeDml(thirdQuery)
}
```

A normal processing exception inside a SAVEPOINT is handled as follows:

```text
Exception in the block
→ ROLLBACK TO SAVEPOINT
→ RELEASE SAVEPOINT
→ Return SavepointResult(isSuccess = false)
```

If SAVEPOINT management itself fails, one of the following `RuntimeException` types is thrown:

- `NotCreatedSavepointException`
- `FailureRollbackException`
- `FailureReleaseException`

These exceptions are intended to propagate to the outer `transaction()` block so that the entire transaction is rolled back.

If the SAVEPOINT name is omitted, a unique internal name is generated.

## Database Upgrade

`AndrOrmDatabaseHelper#onUpgrade()` performs the following operations based on the registered entities:

1. Drop the migration table named `<table name>_new`
2. Create mappings between old and new columns
3. Create the new table
4. Transfer migratable data from the old table
5. Create INDEX and UNIQUE INDEX definitions
6. Drop the old table
7. Rename the new table to the original table name

> `<table name>_new` is reserved for the upgrade process. Do not create a table with the same name because it will be dropped during an upgrade.

### Renaming a Column

Use `@ColumnOldName` when only the column name changes.

```kotlin
@Column(name = "DISPLAY_NAME")
@ColumnOldName("USER_NAME")
val displayName: String
```

### Adding a Non-Nullable Column

A non-nullable column that does not exist in the old table requires `@MigrationDefault`.

```kotlin
@MigrationDefault("1")
val enabled: Boolean
```

`@MigrationDefault` is used only to supply values for existing rows during a database upgrade.

It is not used as a CREATE TABLE DEFAULT clause.

### Difference from CREATE-Time DEFAULT

```kotlin
@Column(default = "1")
@MigrationDefault("1")
val enabled: Boolean
```

| Annotation | Usage |
|---|---|
| `@Column(default = "...")` | DEFAULT clause used by CREATE TABLE |
| `@MigrationDefault("...")` | Value used to populate existing rows during a database upgrade |

A property default value declared in a `data class` is not used to populate existing rows during a database upgrade.

### Custom Column Mappings

For more complex column migrations, override `resolveColumnMappings()`.

```kotlin
override fun resolveColumnMappings() =
    mapOf(
        UserMaster::class to listOf(
            "OLD_USER_CODE" to "USER_CODE",
        )
    )
```

## Kotlin and SQLite Type Mapping

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

## Date and Time Data Notes

SQLite allows column type names such as `DATE` and `DATETIME`, but stored values are not held in a dedicated date-time data type. They are managed as strings, numbers, or other supported SQLite storage classes.

AndrORM's standard type conversion stores `LocalDate`, `LocalTime`, and `LocalDateTime` values as strings and converts them back to their corresponding types when reading them.

Use a consistent date-time format within the application for all values stored in the same column. This avoids inconsistencies during searches, comparisons, sorting, and conversion when reading values.

When using a custom date-time format, the application is responsible for managing its meaning and ensuring that conversions are valid.

## DEFAULT and MigrationDefault Values

The shared validator checks the following values according to their Kotlin type.

| Kotlin Type | Main Accepted Values |
|---|---|
| `Int` / `Long` | Integer |
| `Float` / `Double` | Integer or decimal |
| `Boolean` | `0` or `1` |
| `String` | SQL string literal enclosed in single quotes |
| `LocalDate` | `'yyyy-MM-dd'` or `CURRENT_DATE` |
| `LocalTime` | `'HH:mm:ss'` or `CURRENT_TIME` |
| `LocalDateTime` | `'yyyy-MM-ddTHH:mm:ss'`, `'yyyy-MM-dd HH:mm:ss'`, `CURRENT_TIMESTAMP`, or `CURRENT_TIMESTAMP_ISO` |
| `ByteArray` | DEFAULT values are not supported |
| Nullable types | `NULL` can be specified |

At SQLite execution time, `CURRENT_TIMESTAMP_ISO` is converted to the following expression:

```sql
(strftime('%Y-%m-%dT%H:%M:%f', 'now', 'localtime'))
```

## KSP Validation

The KSP process mainly validates the following:

- Invalid simultaneous use of `@Column` and `@Function`
- Presence of properties that can be selected
- Duplicate aliases
- Target properties specified by `ColumnProjection`
- Arguments specified by `FunctionProjection`
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

`androrm-detekt-rules` implements the following rules:

- `AndrOrmDuplicateTableNameRule`
  - Detects duplicate table names across entities
- `AndrOrmEntityRefRule`
  - Validates how AndrORM entities are referenced

## Tests

### Test Frameworks

| Category | Framework |
|---|---|
| Unit Test | JUnit5 (Jupiter), with JUnit4 / Vintage compatibility enabled |
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
