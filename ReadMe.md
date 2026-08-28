# AndrORM

> [!IMPORTANT]
> AndrORMは現在開発中のアルファ版です。  
> 本資料の対象バージョンは`0.1.5-alpha`です。  
> 今後、APIや仕様が変更される可能性があります。

## AndrORMとは

**AndrORMは、SQLの自由度を残しながら、Kotlinの`data class`とKSPでSQLite開発を支援するAndroid向けORMです。**

SQLiteを直接扱う柔軟性を維持しつつ、SQLの組み立て、Entity定義、テーブル作成、データベース移行など、繰り返し発生する処理を支援します。

```kotlin
val select = Select(ProductSelect::class)
    .where { ProductSelect::enabled eq true }
    .order { ProductSelect::productCode.asc }
```

SQLを隠蔽することを目的としたORMではありません。

開発者がSQLの構造を意識しながら、文字列の連結やバインド値管理、CursorからEntityへの変換といった定型処理を減らすことを目的としています。

## Roomとの違い

RoomとAndrORMでは、SQLite開発を支援するという目的は共通していますが、SQLの扱い方と設計方針が異なります。

| 項目 | AndrORM | Room |
|---|---|---|
| クエリの定義 | KotlinによるSQLビルダー | `@Query`などにSQLを記述 |
| 動的な検索条件 | Kotlinコードとして組み立てる | DAOやクエリを用途ごとに構成 |
| SQLの自由度 | SQLiteを意識した構造を維持 | RoomのDAOを中心に構成 |
| Entity | 手作業またはKSPで生成 | アノテーションを付けて定義 |
| 用途別Entity | KSPでSELECT・DML用途別に生成可能 | EntityやDTOを個別に定義 |
| テーブル作成 | Entity定義から生成 | Schemaを基にRoomが管理 |
| データベース移行 | テーブル再構築とデータ移行を支援 | Migrationを定義 |
| SAVEPOINT | 専用APIを提供 | 原則としてトランザクション単位 |
| 静的検証 | AndrORM専用Detektルール | Annotation Processor／KSPによる検証 |

RoomがDAOとアノテーションを中心にデータアクセスを構成するのに対し、AndrORMはSQLビルダーを中心にクエリを組み立てます。

複雑な検索条件、JOIN、サブクエリ、集約関数、HAVINGなどをKotlinコード上で段階的に構築しながら、最終的に生成されるSQLを開発者が把握できる設計です。

AndrORMはRoomを置き換えること自体を目的としていません。Roomの設計が適しているプロジェクトではRoomを使用し、SQLの構造やSQLite固有の動作をより直接的に管理したい場合に、AndrORMを選択できます。

## どのような開発者に向いているか

AndrORMは、次のようなAndroid開発者を想定しています。

- SQLの知識を活かしてSQLiteを扱いたい
- 動的な検索条件をKotlinコードで構築したい
- SQL文字列の連結やバインド値管理を共通化したい
- JOIN、サブクエリ、集約関数、HAVINGを柔軟に利用したい
- テーブル定義をKotlinの`data class`へ集約したい
- SELECTやDMLの用途ごとにEntityを分けたい
- データベース移行やテーブル再構築を管理したい
- トランザクション内でSAVEPOINTによる部分ロールバックを利用したい
- SQLビルダーの参照ミスを静的解析で検出したい
- Repositoryの内部へDB処理を集約し、MVVMを疎結合に保ちたい

反対に、SQLをほとんど意識せず、DAOの定義を中心に開発したい場合は、Roomの方が適している可能性があります。

## 主な機能

- Kotlinの`data class`によるテーブル定義
- KSPによるSELECT・INSERT・UPDATEなどの用途別Entity生成
- Entityの手作業による定義にも対応
- SELECT／INSERT／UPDATE／DELETE／UPSERT／ABSERTのSQL生成
- INNER JOIN／LEFT JOIN／CROSS JOIN／NATURAL JOIN
- WHERE／GROUP BY／HAVING／ORDER BY
- `having`指定時のGROUP BY自動生成
- サブクエリ、EXISTS／NOT EXISTS
- 集約関数、スカラー関数、raw条件／raw式
- SQLとバインド値の一元管理
- Entity／Map／Cursor形式でのSELECT結果取得
- Entity定義からのSQLiteテーブル作成
- データベース更新時のテーブル再構築とデータ移行
- トランザクションおよびSAVEPOINT
- 独自型とSQLite型の相互変換
- AndrORM専用Detektルールによる静的検証

Detektでは、主に次の問題をSQL実行前に検出します。

- 明示したテーブル名の重複
- FROM元またはJOIN先に存在しないEntityプロパティの参照
- FROM元とJOIN先における同一Entityの重複指定

## 5～10分で動かす

### 1. プラグインを追加する

プロジェクトルートの`build.gradle.kts`へ、KSPとDetektを追加します。

```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.6" apply false
}
```

対象モジュールでプラグインを有効にします。

```kotlin
plugins {
    id("com.google.devtools.ksp")
    id("io.gitlab.arturbosch.detekt")
}
```

### 2. AndrORMを追加する

```kotlin
dependencies {
    implementation("io.github.kawanagare-git:androrm-runtime:0.1.2-alpha")
    ksp("io.github.kawanagare-git:androrm-generator-ksp:0.1.2-alpha")
    detektPlugins("io.github.kawanagare-git:androrm-detekt-rules:0.1.2-alpha")
}
```

### 3. Entityを定義する

```kotlin
@Projections(
    [
        Projection(
            entityNameExtend = "Select",
            properties = [
                ColumnProjection("productCode"),
                ColumnProjection("productName"),
                ColumnProjection("enabled"),
            ],
            commonInterface = [SELECT],
        ),
        Projection(
            entityNameExtend = "Insert",
            properties = [
                ColumnProjection("productCode"),
                ColumnProjection("productName"),
                ColumnProjection("enabled"),
            ],
            commonInterface = [INSERT],
        ),
    ],
)
@Table(name = "PRODUCT", alias = "P")
data class Product(
    @PrimaryKey
    @Column(name = "PRODUCT_CODE")
    val productCode: String,

    @Column(name = "PRODUCT_NAME")
    val productName: String,

    @Column(name = "ENABLED", default = "1")
    val enabled: Boolean,
)
```

EntityはKSPで用途別に生成するほか、必要なアノテーションとマーカーインターフェースを指定して手作業で作成することもできます。

### 4. KSPを実行する

Windowsでは、次のコマンドを実行します。

```powershell
.\gradlew.bat :app:kspDebugKotlin
```

### 5. SQLを組み立てる

```kotlin
val select = Select(ProductSelect::class)
    .where { ProductSelect::enabled eq true }
    .order { ProductSelect::productCode.asc }
```

ここまでで、Entity定義とAndrORMによるSQL構築を開始できます。

DBヘルパー、SQLの実行方法、Entity生成、データベース移行などの詳細は、READMEの各章を参照してください。

## 詳細資料

AndrORMのAPI、Entity生成、SQLビルダー、データベース移行などの詳細は、次の資料を参照してください。

- [AndrORM詳細仕様](docs/Reference.md)
- [AndrORM詳細仕様(英語)](docs/Reference_en.md)

## サンプルプロジェクト

実際のAndroidアプリへAndrORMを組み込んだ在庫管理サンプルを公開しています。

**[AndrORMSample](https://github.com/kawanagare-git/AndrORM-Sample)**

サンプルでは、次の構成と機能を確認できます。

- Jetpack Composeによる画面実装
- ViewModelと`StateFlow`による画面状態管理
- `SharedFlow`による一度限りのイベント通知
- Repositoryインターフェースによる疎結合
- AndrORM EntityからUIモデルへの変換
- 商品、商品分類、入出庫履歴の管理
- SELECT／INSERT／UPDATE／UPSERT／ABSERT
- トランザクションによる在庫数と入出庫履歴の整合性確保
- AndrORMをRepository内部へ閉じ込めたMVVM構成

AndrORMのAPIだけでなく、Compose、StateFlow、Repositoryと組み合わせた実践的な利用方法を確認できます。

## 連絡先
kawanagare6817androrm@gmail.com
