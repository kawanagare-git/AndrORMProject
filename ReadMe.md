# AndrORM

> [!IMPORTANT]
> AndrORMは現在開発中です。
> 現在の公開バージョンは`0.1.1-alpha`です。
> アルファ版のため、今後APIや仕様が変更される可能性があります。

AndrORMは、Android／Kotlin向けに開発中のSQLite ORMです。

Kotlinの`data class`をテーブル定義として使用し、KSPによる用途別Entity生成、型安全なSQLビルダー、DML／SELECT実行、データベースアップグレード、トランザクションおよびSAVEPOINTを提供します。

## 目的

AndrORMは、SQLiteを直接扱う際の可読性・保守性を高めることを目的としています。

- テーブル定義をKotlinの`data class`へ集約
- SELECT／INSERT／UPDATE／DELETE／UPSERT／ABSERTのSQL生成
- SQL文字列とバインド値の一元管理
- KSPによる用途別Entityの自動生成
- EntityからSQLiteテーブルを作成
- データベースバージョン更新時のテーブル再構築とデータ移行
- Entity／Map／Cursor形式でのSELECT結果取得
- トランザクションおよびSAVEPOINTによる部分ロールバック
- Detekt独自ルールによるEntity定義の静的検証

## 現在の位置づけ

本プロジェクトは開発中です。

ランタイム、共通定義、KSP Processor、Detektルールおよび共有ライブラリを
独立した成果物として公開しています。

初回公開バージョンは`0.1.0-alpha`です。

## 対応環境

| 項目 | バージョン |
|---|---:|
| Kotlin | 1.9.24 |
| Android Gradle Plugin | 8.8.0 |
| Gradle Wrapper | 8.10.2 |
| JDK／JVM Target | 17 |
| compileSdk | 34 |
| targetSdk | 34 |
| minSdk | 24 |
| KSP | 1.9.24-1.0.20 |
| Detekt | 1.23.6 |
| JUnit Jupiter | 5.10.2 |

## モジュール構成

| モジュール | 役割 |
|---|---|
| `app` | AndrORMの動作確認、サンプル、Android Test |
| `androrm-runtime` | SQLビルダー、SQLite実行、DBヘルパー、ランタイムEntity変換 |
| `androrm-common` | アノテーション、共通メタ情報、DMLマーカーインターフェース、DEFAULT値検証 |
| `androrm-generator-ksp` | `@Projection`を基に用途別Entityを生成 |
| `androrm-detekt-rules` | AndrORM専用Detektルール |
| `shared-library` | モジュール間共通ユーティリティ |
| `test-support` | Unit Test共通コンバーターなどのテスト支援 |
| `ksp-fixtures` | KSP異常系検証用ソース |

主な依存関係は次のとおりです。

```text
app
 ├─ androrm-runtime
 └─ androrm-generator-ksp（KSP）

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

## 主な機能

### テーブル定義

- `@Table`
- `@Column`
- `@PrimaryKey`
- `@Index`
- `@Unique`
- `@ColumnOldName`
- `@MigrationDefault`

### KSP Entity生成

- `@Projection`
- `@Projections`
- `ColumnProjection`
- `FunctionProjection`
- `@EntityPackageInfo`
- DML用途別マーカーインターフェース
- 独自インターフェース追加

### SQL生成

- `Create`
- `Select`
- `Insert`
- `Update`
- `Delete`
- `Upsert`
- `Absert`

`Absert`はAndrORM独自の名称で、SQLiteの次のSQLを生成します。

```sql
INSERT ... ON CONFLICT (...) DO NOTHING
```

「Insert if absent」の意味で使用しています。

### SELECT

- DISTINCT
- INNER JOIN
- LEFT JOIN
- CROSS JOIN
- NATURAL JOIN
- ON句

現行の`JoinType`では、RIGHT JOINとFULL JOINは提供していません。

- WHERE
- GROUP BY
- HAVING
- ORDER BY
- NULLS FIRST／NULLS LAST
- LIMIT
- OFFSET
- サブクエリ
- EXISTS／NOT EXISTS
- 集約関数
- スカラー関数
- raw条件／raw式
- SQLバインド値管理

### 条件DSL

主な条件演算をDSLで指定できます。

- `eq`／`equal`
- `ne`／`notEqual`
- `gt`／`graterThan`
- `ge`／`graterEqual`
- `lt`／`lesserThan`
- `le`／`lessEqual`
- `like`／`notLike`
- `glob`／`notGlob`
- `inList`／`notInList`
- `inSelect`／`notInSelect`
- `between`
- `exists`／`notExists`
- `isNull`／`isNotNull`
- `and`／`or`
- `condition`によるraw条件

## セットアップ

### Maven Centralからの利用方法
AndrORMを利用する場合は、`androrm-runtime`、`androrm-generator-ksp`、`androrm-detekt-rules`の3成果物をすべて導入する必要があります。

KSPプラグインとDetektプラグインを有効にします。
- プロジェクトルート側：`build.gradle.kts（<プロジェクト名>）`
```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.6" apply false
}
```
- 対象モジュール側：`build.gradle.kts（:<モジュール名>）`
```kotlin
plugins {
    id("com.google.devtools.ksp")
    id("io.gitlab.arturbosch.detekt")
}
```
AndrORMのランタイム、KSP Processor、Detektルールを追加します。
- 対象モジュール側：`build.gradle.kts（:<モジュール名>）`
```
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
Core Library Desugaringを有効にします。
- 対象モジュール側：`build.gradle.kts（:<モジュール名>）`
```
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
### プロジェクトを開く

Android Studioでプロジェクトルートを開きます。

`local.properties`には、Android SDKのパスを設定してください。

```properties
sdk.dir=C\:\\Users\\USER\\AppData\\Local\\Android\\Sdk
```

### ビルド

Windows PowerShellの場合：

```powershell
.\gradlew :app:assembleDebug
```

Unit Test：

```powershell
.\gradlew :app:testDebugUnitTest
.\gradlew :androrm-runtime:testDebugUnitTest
.\gradlew :androrm-common:test
.\gradlew :androrm-generator-ksp:test
.\gradlew :androrm-detekt-rules:test
.\gradlew :shared-library:test
```

Android Test：

```powershell
.\gradlew :app:connectedDebugAndroidTest
```

Detekt：

```powershell
.\gradlew detektAll
```

`app`モジュールのUnit TestとAndroid Testだけを解析するタスクも用意されています。

```powershell
.\gradlew :app:detektUnitTestOnly
.\gradlew :app:detektAndroidTestOnly
```

## Entity定義

### 生成先パッケージ

生成先パッケージを指定するファイルを作成します。

```kotlin
@file:EntityPackageInfo(
    basePackage = "com.example.database.entities",
)

package com.example.database.entities

import jp.pgw.lab78.androrm.common.annotation.EntityPackageInfo
```

既定値では、用途ごとに次のサブパッケージへ生成されます。

| 用途 | 生成先 |
|---|---|
| SELECT | `select` |
| INSERT | `insert` |
| UPDATE | `update` |
| UPSERT | `upsert` |
| ABSERT | `absert` |
| DELETE | `delete` |

### テーブル定義例

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

`@Projection`は、元のテーブル定義Entityから用途別の`data class`を生成します。

| 引数 | 内容 |
|---|---|
| `entityNameExtend` | 生成クラス名へ追加する名称 |
| `aliasExtend` | テーブルaliasへ追加する名称 |
| `properties` | 生成対象プロパティ |
| `functions` | 生成対象SQL関数プロパティ |
| `commonInterface` | SELECT／INSERTなどの共通インターフェース |
| `customInterface` | 利用者独自インターフェース |

生成クラス名は、原則として次の形式です。

```text
元クラス名 + entityNameExtend
```

例：

```text
UserMaster + Select = UserMasterSelect
```

### `hideFromSelect`

`ColumnProjection.hideFromSelect = true`を指定したプロパティは、Entityメタ情報には保持されますが、SELECT句の抽出対象から除外されます。

条件式やDELETE用Entityなど、抽出結果として不要なプロパティを定義する際に使用します。

## AndrORM Entityの配置先

AndrORMでKSPによるEntity生成を行う場合、`@Projection`または`@Projections`を付与したKotlinのEntity定義は、Javaソースディレクトリではなく、Kotlinソースディレクトリへ配置してください。

```text
src/main/kotlin
```

配置例：

```text
app/src/main/kotlin/com/example/database/entities/
```

次のように、Kotlinファイルを`src/main/java`配下へ配置した場合、通常のKotlinコンパイルが成功していても、KSPが対象クラスを検出できず、用途別Entityが生成されないことがあります。

```text
src/main/java
```

KSPログに次のような出力があり、Entityが生成されない場合は、対象のKotlinファイルが`src/main/kotlin`配下に配置されていることを確認してください。

```text
findProjectionClasses: Exiting: []
```


## CREATE TABLE

```kotlin
val query = Create(UserMaster::class).build()
```

`@Index`と`@Unique`は、別のINDEX作成SQLとして取得します。

```kotlin
val create = Create(UserMaster::class)

val createTableQuery = create.build()
val indexQueries = create.buildIndexQueries(indexVersion = 1)
```

`Create`はプライマリコンストラクタのプロパティ順でカラムを生成します。

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

`limit()`は中間オブジェクトを返し、`offset()`、`build()`、`bindValues`を利用できます。

テーブルaliasを明示する場合は`TableRef`を使用します。

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

複数件を一つのVALUES句へ展開できます。

```kotlin
insert.addEntities(entityList)
```

## UPDATE

Entity全体からSET句を生成できます。

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

SET DSLも使用できます。

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

### 全件更新

WHEREのないUPDATEは、誤操作防止のため`updateAll()`を明示する必要があります。

```kotlin
val update = Update(UserMasterUpdate::class)
    .set {
        UserMasterUpdate::enabled assign false
    }
    .updateAll()
```

`where()`と`updateAll()`を同じインスタンスへ指定した場合は、後から呼び出した方が更新対象範囲として有効になります。

## DELETE

```kotlin
val delete = Delete(UserMasterDelete::class)
    .where {
        UserMasterDelete::id eq 1
    }
```

### 全件削除

WHEREのないDELETEは、誤操作防止のため`deleteAll()`を明示する必要があります。

```kotlin
val delete = Delete(UserMasterDelete::class)
    .deleteAll()
```

`where()`と`deleteAll()`を同じインスタンスへ指定した場合は、後から呼び出した方が削除対象範囲として有効になります。

## UPSERT

SQLiteの`INSERT ... ON CONFLICT ... DO UPDATE`を生成します。

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

UPSERTには、次の指定が必要です。

- 1件以上のEntity
- 1件以上の衝突判定カラム
- 1件以上の更新対象カラム

## ABSERT

SQLiteの`INSERT ... ON CONFLICT ... DO NOTHING`を生成します。

```kotlin
val absert = Absert(UserMasterUpsert::class)
    .addEntity(entity)
    .onConflict {
        column(UserMasterUpsert::id)
    }
```

ABSERTには、次の指定が必要です。

- 1件以上のEntity
- 1件以上の衝突判定カラム

## SQLの実行

### DBヘルパー

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

可変長引数コンストラクタを使用する場合：

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

### DML実行

```kotlin
val affectedRows = helper.executeDml(insert)
```

SQL文字列とバインド値を直接指定することもできます。

```kotlin
val affectedRows = helper.executeDml(
    query = sql,
    bindValues = bindValues,
)
```

`executeDml()`は、INSERT／UPDATE／DELETE／UPSERT／ABSERTなどの更新系SQLを実行し、処理件数を返します。

### SELECT結果

Entity形式：

```kotlin
val rows = helper.executeSelectAsEntityList(select)
```

Map形式：

```kotlin
val rows = helper.executeSelectAsMapList(select)
```

Cursor形式：

```kotlin
helper.executeSelectAsCursor(select).use { cursor ->
    // Cursorを利用
}
```

最後に実行したDMLまたはMap形式SELECTの実行時間は、`queryExecutionTime`へナノ秒単位で保持されます。

## トランザクション

```kotlin
helper.transaction {
    executeDml(insert)
    executeDml(update)
    executeDml(delete)
}
```

ブロックが正常終了した場合にコミットされます。

ブロック内で例外が外へ伝播した場合は、`setTransactionSuccessful()`が呼ばれないため、トランザクション全体がロールバックされます。

`executeSelectAsCursor()`で取得したCursorは、トランザクションブロック内で使用してください。

## SAVEPOINT

```kotlin
helper.transaction {
    executeDml(firstQuery)

    val secondResult = savepoint("second_process") {
        executeDml(secondQuery)
    }

    if (secondResult.isSuccess) {
        // SAVEPOINT内の処理成功
    } else {
        // secondQueryはSAVEPOINTまでロールバック済み
        val cause = secondResult.failure
    }

    executeDml(thirdQuery)
}
```

SAVEPOINT内の通常の処理例外は、次のように扱われます。

```text
blockで例外
→ ROLLBACK TO SAVEPOINT
→ RELEASE SAVEPOINT
→ SavepointResult(isSuccess = false)を返す
```

SAVEPOINT管理自体に失敗した場合は、次の`RuntimeException`が送出されます。

- `NotCreatedSavepointException`
- `FailureRollbackException`
- `FailureReleaseException`

これらの例外は、そのまま外側の`transaction()`へ伝播させ、トランザクション全体をロールバックする想定です。

SAVEPOINT名を省略した場合は、内部で一意な名称を生成します。

## データベースアップグレード

`AndrOrmDatabaseHelper#onUpgrade()`は、登録されたEntityを基に次の処理を行います。

1. 移行用の`<テーブル名>_new`を削除
2. 新旧カラムマッピングを作成
3. 新テーブルを作成
4. 旧テーブルから移行可能なデータを転送
5. INDEX／UNIQUE INDEXを作成
6. 旧テーブルを削除
7. 新テーブルを元の名称へ変更

> `<テーブル名>_new`はアップグレード処理専用の予約名です。利用者が同名テーブルを作成していた場合、アップグレード時に削除されるため使用しないでください。

### カラム名変更

カラム名だけを変更する場合は`@ColumnOldName`を指定します。

```kotlin
@Column(name = "DISPLAY_NAME")
@ColumnOldName("USER_NAME")
val displayName: String
```

### 追加した非nullableカラム

旧テーブルに存在しない非nullableカラムには、`@MigrationDefault`が必要です。

```kotlin
@MigrationDefault("1")
val enabled: Boolean
```

`@MigrationDefault`はDBアップグレード時の既存レコード補完専用です。

CREATE TABLE時のDEFAULT句には使用されません。

### CREATE時のDEFAULTとの違い

```kotlin
@Column(default = "1")
@MigrationDefault("1")
val enabled: Boolean
```

| アノテーション | 使用場面 |
|---|---|
| `@Column(default = "...")` | CREATE TABLEのDEFAULT句 |
| `@MigrationDefault("...")` | DBアップグレード時の既存レコード補完 |

data classのプロパティデフォルト値は、DBアップグレード時の補完値として使用されません。

### 独自カラムマッピング

より複雑なカラム移行が必要な場合は、`resolveColumnMappings()`をオーバーライドします。

```kotlin
override fun resolveColumnMappings() =
    mapOf(
        UserMaster::class to listOf(
            "OLD_USER_CODE" to "USER_CODE",
        )
    )
```

## Kotlin型とSQLite型

| Kotlin型 | SQLiteカラム型 | 保存／バインド |
|---|---|---|
| `Int` | `INTEGER` | `bindLong` |
| `Long` | `INTEGER` | `bindLong` |
| `Float` | `REAL` | `bindDouble` |
| `Double` | `REAL` | `bindDouble` |
| `Boolean` | `INTEGER` | `false = 0`、`true = 1` |
| `String` | `TEXT` | `bindString` |
| `LocalDate` | `DATETIME` | 文字列 |
| `LocalTime` | `DATETIME` | 文字列 |
| `LocalDateTime` | `DATETIME` | 文字列 |
| `ByteArray` | `BLOB` | `bindBlob` |

未対応型を指定した場合は例外になります。

## 日付・時刻データの注意事項

SQLiteでは、`DATE`や`DATETIME`などの日付・時刻を表す型名をカラム定義に使用できますが、実際の保存値は専用の日付・時刻型として保持されるのではなく、文字列や数値などの形式で管理されます。

AndrORMの標準型変換では、`LocalDate`、`LocalTime`、`LocalDateTime`を文字列へ変換して保存し、読出時に各型へ変換します。

同一カラムへ保存する日時値は、検索、比較、並べ替えおよび読出時の変換に不整合が発生しないよう、アプリケーション内で統一したフォーマットを使用してください。

任意の日時フォーマットを独自に使用する場合、その意味や相互変換の妥当性は利用者側で管理してください。

## DEFAULT／MigrationDefaultの値

共通バリデーターで、型ごとに次の値を検証します。

| Kotlin型 | 許可される主な値 |
|---|---|
| `Int`／`Long` | 整数 |
| `Float`／`Double` | 整数または小数 |
| `Boolean` | `0`または`1` |
| `String` | シングルクォートで囲んだSQL文字列 |
| `LocalDate` | `'yyyy-MM-dd'`、`CURRENT_DATE` |
| `LocalTime` | `'HH:mm:ss'`、`CURRENT_TIME` |
| `LocalDateTime` | `'yyyy-MM-ddTHH:mm:ss'`、`'yyyy-MM-dd HH:mm:ss'`、`CURRENT_TIMESTAMP`、`CURRENT_TIMESTAMP_ISO` |
| `ByteArray` | DEFAULT値非対応 |
| nullable型 | `NULL`を指定可能 |

`CURRENT_TIMESTAMP_ISO`は、SQLite実行時に次の式へ変換されます。

```sql
(strftime('%Y-%m-%dT%H:%M:%f', 'now', 'localtime'))
```

## KSP検証

KSP処理では、主に次の内容を検証します。

- `@Column`と`@Function`の不正な併用
- SELECT対象プロパティの有無
- alias重複
- `ColumnProjection`の対象プロパティ
- `FunctionProjection`の引数
- 関数戻り値型
- `@Column(default)`の型整合性
- `@MigrationDefault`の型整合性
- 生成インターフェースと用途の整合性

`FunctionProjection.returnHint`は、自動推論できない場合だけ指定します。

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

## Detekt独自ルール

`androrm-detekt-rules`には、次のルールが実装されています。

- `AndrOrmDuplicateTableNameRule`
  - Entity間の重複テーブル名を検出
- `AndrOrmEntityRefRule`
  - AndrORMのEntity参照方法を検証

## テスト

### テスト方式

| 区分 | フレームワーク |
|---|---|
| Unit Test | JUnit5（Jupiter）。JUnit4／Vintage互換も有効 |
| Android Test | JUnit4 |

## ディレクトリ構成

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

## ライセンス

MIT License

Copyright (c) 2025 kawanagare-git
