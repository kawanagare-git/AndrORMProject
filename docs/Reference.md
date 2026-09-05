# AndrORM

> [!IMPORTANT]
> AndrORMは現在開発中です。
> 本資料の対象バージョンは`0.1.7-alpha`です。
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

### VIEW定義

- `@View`
- `ViewDefinitionEntity`
- `ViewSelect`
- `CreateView`

### KSP Entity生成

- `@Projection`
- `@Projections`
- `ColumnProjection`
- `FunctionProjection`
- `@EntityPackageInfo`
- DML用途別マーカーインターフェース
- DML種別ごとの生成先パッケージ指定
- `commonInterface = [NOT_USE]`と`customInterface`によるカスタム生成先パッケージ指定

### Detektによる静的検証

- 同一ソース内の`@Table`で明示したテーブル名の重複を検出
- `Select`、`join`、`where`、`having`、`on`、`order`で参照するEntityプロパティが、FROM元またはJOIN済みEntityに属しているかを検証
- FROM元とJOIN先に、同じEntityを重複指定していないかを検出
- `@View`と`ViewDefinitionEntity`の不整合を検出
- `@Table`と`@View`の併用を検出
- 明示したVIEW名の重複、および明示したテーブル名との衝突を検出

Detektは、SQLを実行する前にEntity定義やSELECT DSLの参照ミスを検出するために使用します。

### SQL生成

- `Create`
- `CreateView`
- `Select`
- `ViewSelect`
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

### CREATE VIEW

AndrORMでは、SQLiteのVIEWを`@View`、`ViewDefinitionEntity`、`ViewSelect`、`CreateView`で定義できます。

### VIEW定義Entity

VIEWの列構造は、`@View`を付与し、`ViewDefinitionEntity`を実装した`data class`として定義します。

```kotlin
@View(name = "ACTIVE_EMPLOYEE", alias = "AV")
data class ActiveEmployeeViewDefinition(
    @Column(name = "ID")
    val id: Int,

    @Column(name = "NAME")
    val name: String,
) : ViewDefinitionEntity
```

`ViewDefinitionEntity`は`TableDefinitionEntity`および`SelectEntity`とは独立した、CREATE VIEW専用のマーカーインターフェースです。

`@View.name`を省略した場合はクラス名をスネークケースへ変換した名称を使用し、`alias`を省略した場合はVIEW名をaliasとして使用します。

VIEW定義では、次の制約があります。

- `@View`と`ViewDefinitionEntity`は組み合わせて使用する
- `@Table`と`@View`は同一クラスへ指定できない
- VIEW定義のプロパティへ`@Function`は指定できない
- VIEW定義のカラムへ`hideFromSelect = true`は指定できない
- 同一VIEW内で物理カラム名を重複できない

### VIEW用EntityのKSP生成

`ViewDefinitionEntity`へ`@Projection`または`@Projections`を指定すると、VIEWを通常の`Select`から参照するためのEntityをKSPで生成できます。

VIEW定義のProjectionで指定できる`commonInterface`は`SELECT`または`NOT_USE`のみです。Projectionを指定する場合は、少なくとも1つの`SELECT` Projectionが必要です。

INSERT、UPDATE、DELETE、UPSERT、ABSERT用EntityはVIEW定義から生成できません。

### `ViewSelect`

CREATE VIEWのSELECT本体には、通常の`Select`とは別に`ViewSelect`を使用します。

```kotlin
val viewSelect = ViewSelect(EmployeeSelect::class)
    .where {
        EmployeeSelect::enabled eq true
    }
```

通常の`Select`は条件値をバインド値として保持します。

```sql
WHERE E.ENABLED = ?
```

`ViewSelect`はSQLiteのVIEW定義でバインドパラメータを使用できないため、条件値をSQLリテラルへ展開します。

```sql
WHERE E.ENABLED = 1
```

`ViewSelect.bindValues`は空のまま保持されます。

主な値は次の形式へ変換されます。

| Kotlin値 | VIEW SQL |
|---|---|
| `null` | `NULL` |
| `Boolean` | `false = 0`、`true = 1` |
| `Byte`／`Short`／`Int`／`Long` | 数値リテラル |
| `Float`／`Double` | 数値リテラル |
| `String` | シングルクォート付き文字列 |
| `LocalDate`／`LocalTime`／`LocalDateTime` | シングルクォート付き文字列 |
| `ByteArray` | `X'...'`形式のBLOBリテラル |

文字列中のシングルクォートはSQLiteの規則に従ってエスケープします。`NaN`、無限大、NUL文字を含む文字列、未対応型はVIEW SQLリテラルとして使用できません。

生成SQLにSQLiteのバインドパラメータが残っている場合はエラーになります。検査対象は`?`、`?123`、`:name`、`@name`、`$name`です。文字列リテラル、引用された識別子、SQLコメント内の同じ文字列はバインドパラメータとして扱いません。

### `CreateView`

`CreateView`へVIEW定義Entityと`ViewSelect`を指定します。

```kotlin
val createView = CreateView(
    ActiveEmployeeViewDefinition::class,
    ViewSelect(EmployeeSelect::class)
        .where {
            EmployeeSelect::enabled eq true
        },
)

val createViewSql = createView.build()
```

`CreateView`はVIEW定義Entityの主コンストラクタ順で明示的なVIEWカラムリストを生成します。

```sql
CREATE VIEW "ACTIVE_EMPLOYEE" ("ID", "NAME") AS SELECT ...
```

VIEW定義Entityのカラム数と`ViewSelect`のSELECT出力列数が一致しない場合はエラーになります。

DROP VIEW文は次のように生成できます。

```kotlin
val dropViewSql = createView.buildDropQuery()
val dropViewSqlByName = CreateView.buildDropQuery("ACTIVE_EMPLOYEE")
```

### VIEWをSELECTする

KSPで生成したSELECT用Entityは、通常の`Select`のFROM元またはJOIN先として使用できます。

```kotlin
val select = Select(ActiveEmployeeViewSelect::class)
```

### DBヘルパーへのVIEW登録

VIEWをデータベース生成・更新時に管理する場合は、`AndrOrmDatabaseHelper`の`views`へ`CreateView`を登録します。

```kotlin
class AppDatabaseHelper(
    context: Context,
) : AndrOrmDatabaseHelper(
    context = context,
    databaseName = "app.db",
    version = 2,
    entities = listOf(
        Employee::class,
    ),
    views = listOf(
        CreateView(
            ActiveEmployeeViewDefinition::class,
            ViewSelect(EmployeeSelect::class)
                .where {
                    EmployeeSelect::enabled eq true
                },
        ),
    ),
)
```

新規作成時は、テーブルとINDEXを作成した後に、`views`の登録順でVIEWを作成します。

アップグレード時は、登録済みVIEWを逆順で`DROP VIEW IF EXISTS`し、テーブル移行後に登録順でVIEWを再作成します。

削除・改名され、現在の`views`へ登録されなくなった旧VIEWは`obsoleteViewNames()`で指定します。

```kotlin
override fun obsoleteViewNames(
    oldVersion: Int,
    newVersion: Int,
): List<String> =
    if (oldVersion < 2) {
        listOf("OLD_EMPLOYEE_VIEW")
    } else {
        emptyList()
    }
```

VIEWが別のVIEWを参照する場合は、参照されるVIEWを先に`views`へ登録してください。削除時は逆順で処理されます。

## SELECT

- DISTINCT
- INNER JOIN
- LEFT JOIN
- CROSS JOIN
- NATURAL JOIN
- ON句

現行の`JoinType`では、RIGHT JOINとFULL JOINは提供していません。

- WHERE
- GROUP BY（`having`指定時に自動生成）
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
```kotlin
dependencies {
    implementation("io.github.kawanagare-git:androrm-runtime:0.1.7-alpha")
    ksp("io.github.kawanagare-git:androrm-generator-ksp:0.1.7-alpha")
    detektPlugins("io.github.kawanagare-git:androrm-detekt-rules:0.1.7-alpha")
}
```
Core Library Desugaringを有効にします。
- 対象モジュール側：`build.gradle.kts（:<モジュール名>）`
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
> [!IMPORTANT]
> AndrORMは`minSdk 24`をサポートし、`LocalDate`、`LocalTime`、
> `LocalDateTime`などの`java.time` APIを使用するため、
> Core Library Desugaringを有効にして公開しています。
>
> そのため、AndrORMを使用するアプリ側の対象モジュールでも、
> Core Library Desugaringを有効にする必要があります。
> 利用アプリの`minSdk`が26以上の場合でも、この設定は必要です。
>
> 設定されていない場合は、AARメタデータの確認時に次のエラーが発生します。
>
> ```text
> Dependency 'io.github.kawanagare-git:androrm-runtime:<version>'
> requires core library desugaring to be enabled
> ```

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

AndrORMのEntityは、Kotlinの`data class`として手作業で定義しても、KSPで自動生成しても構いません。

KSPは用途別Entityの作成を省力化する機能であり、AndrORMの利用に必須ではありません。手作業で定義する場合も、自動生成する場合も、SQLビルダーと実行APIから同じように利用できます。

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
| `customInterface` | `commonInterface = [NOT_USE]`の場合に、`basePackage`へ追加する生成先サブパッケージ |

生成クラス名は、原則として次の形式です。

```text
元クラス名 + entityNameExtend
```

例：

```text
UserMaster + Select = UserMasterSelect
```

`customInterface`は、生成クラスが実装するKotlinインターフェースを指定する項目ではありません。`commonInterface = [NOT_USE]`と組み合わせ、`@EntityPackageInfo.basePackage`を基準とする生成先サブパッケージを指定します。

### `hideFromSelect`

`ColumnProjection.hideFromSelect = true`を指定したプロパティは、Entityメタ情報には保持されますが、SELECT句の抽出対象から除外されます。

条件式やDELETE用Entityなど、抽出結果として不要なプロパティを定義する際に使用します。

## Entity生成
通常KSPでのEntity生成は以下のコマンドを用いる
```powershell
.\gradlew.bat :app:kspDebugKotlin
```
AndrORMのEntity定義、`@Projection`、`@Projections`などを追加・変更した場合は、KSPタスクを強制的に再実行する。

```powershell
.\gradlew.bat :app:kspDebugKotlin --rerun-tasks
```
通常のKSP実行でEntityが生成されない場合でも、すぐにソースやGradle設定を変更せず、最初にこのコマンドを試行する。


### ルートパッケージへのEntity生成

KSPで解決された生成先がルートパッケージの場合、生成コードには`package`宣言を出力しません。

そのため、ルートパッケージのクラスへ`@Projection`または`@Projections`を指定した場合でも、生成コードをコンパイルできます。

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

### 生成先パッケージのカスタマイズ

生成先は、Kotlinファイルの先頭で`@file:EntityPackageInfo`を指定して変更できます。ファイルアノテーションは`package`宣言より前に記述します。

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

通常は、`basePackage`と`commonInterface`に対応するサブパッケージを結合した場所へ生成されます。たとえば`commonInterface = [SELECT]`の生成先は、次のとおりです。

```text
com.example.database.entities.select
```

標準DMLインターフェースを使用せず、任意のサブパッケージへ生成する場合は、`commonInterface = [NOT_USE]`と`customInterface`を組み合わせます。

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

この場合、生成されるクラスの完全修飾名は次の形式になります。

```text
<basePackage>.interfaces.ManagementColumns.<元クラス名><entityNameExtend>
```

例：

```text
com.example.database.entities.interfaces.ManagementColumns.UserMasterManagementColumns
```

`customInterface`に複数の値を指定した場合、生成先の決定に使用されるのは最初の空白でない値です。

## Entityの定義方法

AndrORMのEntityは、KSPで生成する方法だけでなく、Kotlinの`data class`として手書きする方法にも対応しています。

複数のテーブルで共通して使用する項目は、インターフェースなどへ分離し、各Entityへ継承できます。分離した共通項目に`@Column`を付与しておけば、カラム定義も各Entityで共通して利用できます。ただし、キーおよびインデックスの定義は継承対象ではありません。`@PrimaryKey`、`@Index`、`@Unique`は、対象となる各Entity自身に定義してください。

KSPによるEntity生成は、用途別Entityの定義を省力化するための機能であり、AndrORMを利用するための必須条件ではありません。手書きする場合は、`@Table`、`@Column`、`@PrimaryKey`などの必要なアノテーションを付与し、用途に対応するマーカーインターフェースを実装してください。

次は、SELECT用Entityを手書きする例です。

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

手書きしたEntityも、KSPで生成したEntityと同様にSQLビルダーへ指定できます。

```kotlin
val select = Select(UserMasterManualSelect::class)
    .where {
        UserMasterManualSelect::id eq 1
    }
```

> [!WARNING]
> `data class`ではない通常のKotlinクラスでも、クラスの構造によっては動作する可能性があります。ただし、AndrORMではEntityを`data class`として定義することを前提に検証しているため、通常クラスは動作保証の対象外です。

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

### GROUP BYの自動生成

`having`を指定すると、AndrORMはSELECT対象のうち集約関数ではない列から`GROUP BY`を自動生成します。利用者が`GROUP BY`を個別に組み立てる必要はありません。

SQL関数列の処理によって既に`GROUP BY`対象が登録されている場合は、その内容を維持し、`having`による重複生成は行いません。

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
val savepointResult = helper.transaction {
    executeDml(masterQuery)

    helper.savepoint("after_master") {
        executeDml(detailQuery1)
        executeDml(detailQuery2)
    }
}

if (savepointResult.isSuccess) {
    // SAVEPOINT内の処理成功
} else {
    // SAVEPOINT作成後の処理はロールバック済み
    val cause = savepointResult.failure
}
```

`AndrOrmDatabaseAndroidTest#step21_absertCombinedDataWithSavepoint`と同様に、SAVEPOINTより前の処理と、SAVEPOINT内の複数処理を1つの`transaction`にまとめて使用できます。

- `masterQuery`はSAVEPOINT作成前に実行されます。
- `detailQuery1`または`detailQuery2`で通常の処理例外が発生すると、SAVEPOINT作成後の変更だけがロールバックされます。
- `savepoint()`は例外を`SavepointResult.failure`へ格納して返すため、呼出側は`isSuccess`で成否を判定できます。
- SAVEPOINT内が成功した場合は、戻り値が`SavepointResult.result`へ格納されます。
- 外側の`transaction`が正常終了すれば、SAVEPOINTより前の処理と、成功したSAVEPOINT内の処理がコミットされます。

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
| `ByteArray` | `X'<偶数桁の16進数>'`形式のBLOBリテラル。`X''`は空BLOB |
| nullable型 | `NULL`を指定可能 |

BLOBの規則は`@Column(default = ...)`と`@MigrationDefault(...)`の双方に適用します。接頭辞と16進数の大文字・小文字を区別しません（例：`X'00FF'`、`x'00ff'`）。奇数桁、16進数以外の文字、通常の文字列リテラルは拒否します。`NULL`はnullable列だけに指定でき、空BLOBの`X''`とは異なります。

`@Column(default = "")`はDEFAULT句を生成しません。`@MigrationDefault`を省略したnullable追加列は転送対象から除外され、non-null追加列では指定が必須です。`@MigrationDefault("X'00FF'")`は既存行への転送値を指定し、CREATE TABLEのDEFAULT句にはなりません。

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
- `@View`と`ViewDefinitionEntity`の組み合わせ
- VIEW定義での`@Table`併用禁止
- VIEW Projectionでは`SELECT`または`NOT_USE`のみを許可
- VIEW定義から更新系Entityを生成しないこと

`FunctionProjection.returnHint`は、自動推論できない場合だけ指定します。

```kotlin
AndrOrmValueType.AUTO
AndrOrmValueType.INT
AndrOrmValueType.LONG
AndrOrmValueType.FLOAT
AndrOrmValueType.DOUBLE
AndrOrmValueType.BOOLEAN
AndrOrmValueType.STRING
AndrOrmValueType.LOCAL_DATE
AndrOrmValueType.LOCAL_TIME
AndrOrmValueType.LOCAL_DATE_TIME
AndrOrmValueType.BYTE_ARRAY
```

`AndrOrmValueType.AUTO`では、関数種別と引数型から戻り値型を自動推論します。`raw`式や`CUSTOM`相当の式など、自動推論できないBLOB結果を`ByteArray`として生成する場合は、`returnHint = AndrOrmValueType.BYTE_ARRAY`を指定します。明示した`returnHint`は自動推論結果より優先されます。

## Detekt独自ルール

`androrm-detekt-rules`には、次のルールが実装されています。

- `AndrOrmDuplicateTableNameRule`
  - `TableDefinitionEntity`を実装するEntityを対象に、`@Table(name = ...)`で明示したテーブル名の重複を検出
- `AndrOrmEntityRefRule`
  - `Select`および`ViewSelect`のFROM元とJOIN先に、同じEntityが重複していないかを検出
  - `join`、`where`、`having`、`on`、`order`内のプロパティ参照が、FROM元またはJOIN済みEntityに属しているかを検証
- `AndrOrmViewDefinitionRule`
  - `@View`と`ViewDefinitionEntity`の対応を検証
  - `@Table`と`@View`の併用を検出
  - `@View(name = ...)`で明示したVIEW名の重複を検出
  - 明示したVIEW名と明示したテーブル名の衝突を検出

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
