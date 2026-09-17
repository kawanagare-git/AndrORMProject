# AndrORM 要求仕様書

> 対象読者：AndrORM 実装担当「ソクラデックス（略称：ラデック）」  
> 仕様決定者：川流  
> 設計・調査・レビュー支援：プラテス  
> 基準ソース：`AndrORM202607181008.zip`  
> 作成日：2026-07-18
> 更新日：2026-09-05

---

## 1. 本書の目的

本書は、AndrORM の概念、設計上の要求、実装時の判断基準、テスト方針、ならびに実装担当が踏んではならない地雷をまとめたものである。

ラデックは、単にコンパイルが通るコードを作るのではなく、AndrORM の既存概念、公開 DSL、生成コード、実行系、テスト、および将来の引き継ぎまで含めた整合性を守ること。

本書は、基準ディレクトリィ内の最上位 `AGENTS.md`、Gradle 設定、`src/main`、`src/test`、`src/androidTest` を再確認して作成した。  
ZIP 内のキャッシュ、build 出力、ログ、生成済みファイルは、現行仕様の根拠から除外している。

本書では次の表現を使用する。

- **MUST**：必須。違反してはならない。
- **SHOULD**：原則として従う。外す場合は理由を示す。
- **MUST NOT**：禁止。
- **確認事項**：仕様決定者へ確認してから実装する。

---

## 2. 仕様・情報の優先順位

仕様または実装方針が競合した場合、次の順で判断すること。

1. 川流が会話内で直近に明示した決定
2. プロジェクト最上位の `AGENTS.md`
3. 川流が承認・修正した `REQUIREMENTS.md`
4. 最新のプロジェクトソース
5. 最新のテストコード
6. ソース内のコメント・KDoc
7. 過去資料、古い ZIP、古いログ

ただし、最上位 `AGENTS.md` にある謝罪手順、回答姿勢、コード生成ルール等の**開発基準は、順位にかかわらず常に適用する**。

### 2.1 競合時の原則

- ラデックは競合を黙って解釈してはならない。
- 最新ソースと文書が異なる場合は、どちらを変更すべきか川流へ提示すること。
- テストの期待値は、現在の実行結果から逆算してはならない。要求仕様から導くこと。
- ZIP 内の `.git`、`.gradle`、`.idea`、各 `build`、生成済み KSP 出力、ログ、CSV 出力結果は、実装確認の補助資料であり要求仕様の根拠にはしない。
- 同じ内容が「手書きソース」と「生成物」の双方にある場合は、生成元の手書きソースと KSP 生成規則を優先する。

---

## 3. AndrORM の基本概念

### 3.1 目的

AndrORM は、Android／Kotlin で SQLite を扱う際に、SQL の可読性、型安全性、保守性、再利用性を高めるための ORM／クエリビルダーである。

主な目的は次のとおり。

- SQL 文を Kotlin クラスと DSL で構築する。
- テーブル定義用 Entity から、用途別の DTO／DML Entity を KSP で生成する。
- SQL 文字列とバインド値を明示的に保持し、実行できるようにする。
- SQL の構造を隠しすぎず、生成結果を確認可能にする。
- SELECT、VIEW、UNION ALL、CREATE、INSERT、UPDATE、DELETE、UPSERT、ABSERT、DB 移行を一貫した概念で扱う。

### 3.2 AndrORM が重視するもの

- 型安全なプロパティ参照
- SQL と Kotlin コードの対応関係の分かりやすさ
- エンティティ定義の一元化
- KSP による定型コードの自動生成
- SQL 句の定義順とバインド値順の保証
- 自己結合、複数 JOIN、サブクエリ、集計関数への対応
- 実行結果を Cursor、Map、Entity の各形式で取得できること
- 仕様から導かれる回帰テスト

### 3.3 非目的

- SQL を完全に隠蔽することは目的ではない。
- Room の模倣や Room への依存は目的ではない。
- MyBatis、jOOQ 等の既存ライブラリの API をコピーすることは目的ではない。
- 現在の実装結果を正解として固定することは目的ではない。
- 「とりあえず動く」「最小構成」「暫定実装」で済ませることは認めない。

---

## 4. 現在のプロジェクト構成

| モジュール／領域 | 主な責務 |
|---|---|
| `app` | SQL ビルダー、条件 DSL、DB 実行、DB 移行、AndroidTest |
| `androrm-common` | アノテーション、DML マーカー、共通メタ情報、共通定数、ログ共通定義 |
| `androrm-generator-ksp` | `@Projection` 解析、検証、用途別 Entity の生成 |
| `androrm-detekt-rules` | AndrORM 固有の静的解析ルール |
| `shared-library` | 複数モジュールで使用する共通補助処理 |
| `test-support` | テスト補助資材（独立 Gradle モジュールではない） |
| `ksp-fixtures` | KSP 検証用素材 |

### 4.1 ビルド基準

現在の基準は次のとおり。

- Kotlin：1.9.24
- Android Gradle Plugin：8.8.0
- KSP：1.9.24-1.0.20
- Java／JVM：17
- compileSdk／targetSdk：34
- minSdk：24
- Unit Test：JUnit 5
- AndroidTest：JUnit 4
- detekt：1.23.6

### 4.2 モジュール設定に関する注意

`settings.gradle.kts` には `:androrm-logging-ksp` の参照があるが、基準 ZIP 内には同モジュールのソースディレクトリが確認できない。ラデックは設定を削除・新設する前に、川流へ現状を確認すること。

---

## 5. Entity モデル

### 5.1 基底インターフェース

Entity は用途ごとのマーカーインターフェースで区別する。

- `Entity`
- `TableDefinitionEntity`
- `SelectEntity`
- `InsertEntity`
- `UpdateEntity`
- `UpsertEntity`
- `AbsertEntity`
- `DeleteEntity`
- `ComprehensiveEntity`：`SelectEntity`、`InsertEntity`、`UpdateEntity`、`UpsertEntity` の複合用途
- `SqlFunction<E>`：SQL 関数 enum の共通契約

用途別クエリビルダーは、対応するマーカーを実装した Entity のみ受け付けること。`ComprehensiveEntity` に `DeleteEntity`／`AbsertEntity` を独断で追加しない。

### 5.2 テーブル定義 Entity

- テーブル定義は原則として Kotlin の `data class` で表現する。
- `@Table` でテーブル名と標準エイリアスを定義する。
- プロパティは DB カラムまたは関数列を表す。
- 同じテーブルから、SELECT 用、INSERT 用、UPDATE 用等の Entity を `@Projection` で生成する。

### 5.3 `@Table`

- `name` が指定されている場合、その値を DB テーブル名として使用する。
- `name` が空の場合、クラス名をスネークケースへ変換する。
- `alias` が指定されている場合、標準エイリアスとして使用する。
- `alias` が空の場合、テーブル名をエイリアスとして使用する。
- 実際のクエリで `TableRef` が渡された場合、`TableRef.alias` を最優先すること。

### 5.4 `@Column`

- `name` が空の場合、プロパティ名をスネークケースへ変換する。
- `alias` は SELECT 結果名の構築に使用する。
- `hideFromSelect = true` の列は SELECT 抽出列から除外する。
- `hideFromSelect = true` でも、条件式、SET、競合キー、メタ情報検証で利用可能でなければならない。
- `default` は CREATE TABLE の DEFAULT 句に使用する。

### 5.5 `@MigrationDefault`

- 旧テーブルに存在しないカラムを追加するとき、既存レコードへ設定する値を指定する。
- `@Column(default = ...)` とは独立し、CREATE TABLE の DEFAULT 句には使用しない。
- data class のプロパティデフォルト値はDBマイグレーション用の値として使用しない。
- 旧テーブルに存在しないnon-nullカラムには指定を必須とする。
- nullableカラムは指定を必須とせず、未指定時は転送対象から除外する。
- 値は文字列で保持し、対象プロパティ型に応じて `@Column(default = ...)` と同じ規則で検証する。
- 将来の適用バージョン等の情報を追加できるよう、`@Column` へ統合しない。

### 5.6 `@Function`

- 関数列は `columnFunction`、`args`、`alias` から生成する。
- `raw` が空でない場合、`columnFunction` と `args` より `raw` を優先する。
- `hideFromSelect` の扱いは `@Column` と同様とする。
- 関数列と通常列を同一プロパティに重複定義してはならない。

### 5.7 `@Projection`

- `@Projection` は repeatable とする。
- 生成クラス名は、定義元クラス名と `entityNameExtend` の連結とする。
- `properties` は `Array<ColumnProjection>` とする。
- `functions` は `Array<FunctionProjection>` とする。
- `commonInterface` により DML 用共通インターフェースを付与する。
- `customInterface` により独自インターフェースを追加できること。
- `aliasExtend` は既存エイリアスへ `_` 連結して派生エイリアスを生成する。

### 5.8 生成先パッケージ

生成先は `@EntityPackageInfo` と DML インターフェースの組み合わせから解決する。

- select
- insert
- update
- upsert
- absert
- delete

パッケージ解決ロジックを変更する場合は、全 DML 種別と既存生成先を確認すること。

---

## 6. Entity メタ情報と検証

### 6.1 共通メタモデル

KSP とランタイムは、次の共通モデルへ正規化する。

- `EntityMeta`
  - 定義元 FQN
  - Entity 名
  - テーブル名
  - テーブルエイリアス
  - `PropertyMeta` 一覧
- `PropertyMeta`
  - Kotlin プロパティ名
  - DB カラム名
  - 出力エイリアス
  - 関数列判定
  - `hideFromSelect`
  - `@Column`／`@Function` の有無
  - 関数種別、引数、raw SQL

KSP とランタイムで同じ定義に対する解釈が食い違ってはならない。

### 6.2 ERROR とする項目

少なくとも次を ERROR とする。

- 同一プロパティへの `@Column` と `@Function` の併用
- SELECT 用 Entity なのに抽出可能なプロパティが 0 件
- SELECT 対象列同士の出力エイリアス重複
- `@Projection.properties` に存在しないプロパティ名を指定
- 型に対して不正な DEFAULT 値を指定
- 必須のテーブル情報またはプロパティ情報を解決できない
- 異なる `TableDefinitionEntity` が大文字小文字を無視して同じ明示 `@Table.name` を持つ（detektで検出）

### 6.3 WARNING とする項目

SQL 構築自体は可能だが定義として不自然なものは WARNING とする。

例：

- hidden 列を含むエイリアス重複
- properties と集計関数対象の不自然な重複

WARNING を黙って消すためだけの変更をしてはならない。仕様上必要な警告か、不具合かを判断すること。

---

## 7. KSP コード生成要求

### 7.1 基本フロー

KSP は次の順で処理する。

1. `@Projection`／`@Projections` の付与クラスを探索
2. アノテーション引数を `ProjectionDefinition` へ解析
3. プロパティ、関数、重複、DEFAULT を検証
4. `EntityMeta` を生成し共通 Validator で検証
5. 出力パッケージ、クラス名、`@Table`、プロパティ、インターフェースを決定
6. Kotlin の `data class` を生成
7. エラー時は不正なクラスを生成しない

### 7.2 生成コードの要求

- 元 Entity の型、nullable、関連アノテーションを正しく引き継ぐこと。
- `@Column` がない場合でも、必要な補完情報を生成できること。
- `@PrimaryKey` 等、DML に必要なアノテーションを欠落させないこと。
- `hideFromSelect` は生成先 Entity に反映すること。
- 関数プロパティ名は alias を camelCase に変換して生成すること。
- 型推論ができない関数は `returnHint` を使用すること。
- 生成ファイルの重複、同名クラス衝突、生成漏れを防止すること。

### 7.3 KSP 修正時の説明

KSP モジュールの修正は、川流に対して次を説明すること。

- 変更対象ファイル
- シンボル解決の流れ
- 生成コードがどう変わるか
- KSP の実行タイミング
- 既存生成物への影響
- 検証した Gradle タスク

---

## 8. Kotlin 型と SQLite 型

| Kotlin 型 | SQLite 型 | DML バインド | SELECT 復元 |
|---|---|---|---|
| `Int` | `INTEGER` | `bindLong` | `Number.toInt()` |
| `Long` | `INTEGER` | `bindLong` | `Number.toLong()` |
| `Float` | `REAL` | `bindDouble` | `Number.toFloat()` |
| `Double` | `REAL` | `bindDouble` | `Number.toDouble()` |
| `Boolean` | `INTEGER` | `true=1`, `false=0` | 0 以外を `true` |
| `String` | `TEXT` | `bindString` | `toString()` |
| `LocalDate` | `DATETIME` | ISO 文字列 | `LocalDate.parse` |
| `LocalTime` | `DATETIME` | ISO 文字列 | `LocalTime.parse` |
| `LocalDateTime` | `DATETIME` | ISO 文字列（`T` 区切り） | `LocalDateTime.parse` |
| `ByteArray` | `BLOB` | `bindBlob` | `ByteArray` |

### 8.1 SQL値表現の文脈

- `ByteArray` は SQLite の BLOB 値として扱い、SQL リテラル表現 `X'<偶数桁の16進数>'` を使用できる。
- `NULL` は SQL リテラルとして記述できる。SQL リテラルとしての `NULL` と、`?` に対して `bindNull` する値は同じSQL値を表すが、生成経路を混同しない。
- 通常の実行SELECT／UNION ALLでは、値を `?` としてSQLへ出力し、`bindValues` に元のKotlin値をSQL出現順で保持する。`ByteArray` は `bindBlob`、`null` は `bindNull` とする。
- VIEW定義ではSQLiteのCREATE VIEWへバインド変数を保存できないため、値をSQLリテラルへ展開する。`ByteArray` は `X'...'`、`null` は `NULL` とする。
- ここで定める一般SQL値表現は、`@Column(default = ...)` および `@MigrationDefault` の適用可否を自動的に決めるものではない。DEFAULTの規則は9.2および21.2に従う。

未対応型を暗黙に `TEXT` 等へ逃がしてはならない。明示的に追加し、CREATE、DML バインド、SELECT 復元、DEFAULT 検証、テストを一式で更新すること。
未対応のKotlin型をSQLite型へ変換しようとした場合は、`AE00009` をメッセージとする `IllegalArgumentException` を送出し、`NullPointerException` にしてはならない。

---

## 9. CREATE TABLE 要求

### 9.1 CREATE

- Entity のコンストラクタ順を基準としてカラム定義を生成する。
- 複合主キーに対応する。
- `@Index` と `@Unique` からインデックス SQL を生成する。
- 名前未指定の `@Index` は `IDX_<テーブル名>_<propertiesに列挙したDBカラム名>_<バージョン>` 形式で命名する。
- 名前未指定の `@Unique` は `UNIQ_<テーブル名>_<propertiesに列挙したDBカラム名>_<バージョン>` 形式で命名する。
- 自動名のカラム部分はアノテーションの `properties` だけを記述順に列挙する。
- 利用者指定の `@Index.name`／`@Unique.name` は `<指定名>_<バージョン>` 形式で命名し、指定名とバージョンの境界をアンダースコアで区切る。
- 利用者指定名を含むインデックス名は、全テーブルを対象に大文字小文字を無視してDDL実行前に重複検査する。
- `CREATE INDEX`／`CREATE UNIQUE INDEX` に `IF NOT EXISTS` を付与せず、既存スキーマとの衝突を黙って無視しない。
- テーブル名上書きは DB 移行用に利用できること。
- CREATE TABLE／CREATE INDEX のテーブル名、カラム名、主キー列名、INDEX名、INDEX対象列名はダブルクォートで引用し、識別子内のダブルクォートを二重化する。
- CREATE が扱う SQL 識別子は空文字・空白だけの値および NUL 文字を拒否する。予約語、空白、ハイフン、セミコロン等は引用された一つの識別子として扱う。
- Kotlin の non-null プロパティにはカラム定義で `NOT NULL` 制約を付与し、nullable プロパティには付与しない。テーブルレベルの PRIMARY KEY 対象列も同じnull許容性規則に従う。

### 9.2 DEFAULT 検証

`@Column(default = ...)` は次の規則で検証する。

- 空文字：DEFAULT 句なし
- `NULL`：nullable のみ許可
- `Int`／`Long`：整数リテラル
- `Float`／`Double`：数値リテラル
- `Boolean`：`0` または `1`
- `String`：SQL のシングルクォート文字列
- `LocalDate`：有効な日付文字列または `CURRENT_DATE`
- `LocalTime`：有効な時刻文字列または `CURRENT_TIME`
- `LocalDateTime`：
  - `YYYY-MM-DDTHH:MM:SS`
  - `YYYY-MM-DD HH:MM:SS`
  - `CURRENT_TIMESTAMP`
  - `CURRENT_TIMESTAMP_ISO`
- `ByteArray`：`X'<偶数桁の16進数>'` 形式のBLOBリテラルを許可する。`X''` は空BLOBとして許可し、接頭辞と16進数の大文字・小文字を区別しない。奇数桁、16進数以外の文字、通常の文字列リテラルは拒否する。`NULL` はnullableのみ許可し、空BLOBと区別する。

`CURRENT_TIMESTAMP_ISO` は、SQLite 上で `T` 区切りのローカル日時を生成する式へ展開する。

このDEFAULT規則は、ViewSelectのSQLリテラル展開、通常SELECT／UNION ALLの値バインド、およびBLOB検索条件で使用する値表現を禁止するものではない。

---

## 10. クエリビルダー共通要求

### 10.1 基本インターフェース

- 各クエリビルダーは `build()` で SQL 文字列を返す。
- バインド値を持つクエリは `QueryWithBindValues` を使用する。
- SQL と `bindValues` の順序は常に一致しなければならない。
- 同じクエリを複数回 `build()` しても、バインド値が重複してはならない。
- クエリ構成を変更したメソッドは、キャッシュ済み SQL を無効化すること。
- `BaseSelect.queryString` は生成済み `query` の参照であり、`build()` 前は未初期化である。呼び出し側は実行または `build()` 後に参照する。
- コンストラクタ初期化時に `queryString` を評価してはならない。

### 10.2 エイリアス

- `TableRef` は「Entity クラス + 実クエリで使用する alias」を表す。
- `TableRef.alias` は SELECT 句、FROM 句、JOIN 句、WHERE、HAVING、SET 式で一貫して使用する。
- 自己結合では各 `TableRef` の別名を維持する。
- 同一クエリ内で同じ alias を再利用した場合はエラーとする。
- Entity の標準 alias を、明示された `TableRef.alias` で上書きできること。

### 10.3 SQL 句の順序

最終 SQL は原則として次の順で生成する。

1. SELECT／UPDATE／INSERT／DELETE
2. FROM または対象テーブル
3. JOIN
4. WHERE
5. GROUP BY
6. HAVING
7. ORDER BY
8. LIMIT
9. OFFSET

内部 Map への登録順ではなく、SQL 文法上の順序で出力すること。

---

## 11. SELECT 要求

### 11.1 通常 SELECT

通常の `Select<T>` は次をサポートする。

- `distinct`
- `join`／`on`
- `where`
- `having`
- 自動 `group by`
- `order`
- `limit`
- `offset`
- バインド値保持
- `TableRef` による別名指定
- 自己結合

現在の `JoinType` は次のとおり。

- `INNER`
- `LEFT`
- `CROSS`
- `NATURAL`

実装されていない JOIN 種別を、存在するものとして扱ってはならない。

### 11.2 SELECT 抽出列

- `hideFromSelect = true` のプロパティを除外する。
- 通常列は `tableAlias.column AS resultAlias` 形式で生成する。
- JOIN した各 Entity の列を追加する。
- 自己結合時は、それぞれの `TableRef.alias` を SELECT 結果 alias に反映する。
- 関数列は raw があれば raw、なければ関数種別と引数から生成する。

### 11.3 HAVING と GROUP BY

- HAVING が指定された場合、必要な非関数列から GROUP BY を自動生成する。
- 既に集計構造が定義されている場合、重複した GROUP BY を生成してはならない。
- GROUP BY の対象は SELECT される非関数列と整合すること。

### 11.4 LIMIT／OFFSET

- LIMIT と OFFSET は負数を許可しない。
- OFFSET は LIMIT の後に指定する DSL とする。
- LIMIT、OFFSET の値も SQL の出現順にバインドする。

---

## 12. EXISTS サブクエリ要求

### 12.1 `ExistsSelect`

- EXISTS 専用サブクエリは `select 1` を生成する。
- 通常 SELECT と共通の JOIN、WHERE、HAVING 基盤を再利用する。
- ORDER BY、LIMIT、OFFSET は EXISTS 用 API に追加しない。
- FROM 句は一度だけ生成する。
- 初回 `build()` で必ず `query` を初期化する。
- `isBuild` は `query` への代入が完了してから `true` にする。

### 12.2 条件 DSL

テーブル参照から EXISTS を構築する API は、条件ブロックを受け取ること。

```kotlin
exists(tableRef) {
    tableRef[Entity::property] eq value
}
```

- 条件ブロックのバインド値は外側クエリへ SQL 出現順で統合する。
- EXISTS 条件そのものを外側の条件リストへ登録する。
- ネストした EXISTS に対応する。
- 現行の簡易 API は `exists(TableRef<T>, ConditionBuilder.() -> Unit)` であり、条件ブロックは必須とする。
- 現行の `notExists` は構築済み `Select<T>` を受け取る形式である。`TableRef + block` 形式を追加する場合は、EXISTS と対称な仕様・テストを定義してから実装する。

### 12.3 スコープ規則

- EXISTS 内のテーブル alias はサブクエリ内だけで有効とする。
- EXISTS 内の列を外側 UPDATE の SET 句から直接参照してはならない。
- 外側レコードと関連付ける場合は相関条件を明示する。
- EXISTS は存在判定であり、値を返すための API ではない。
- サブクエリ値を SET に使用する場合は、スカラーサブクエリまたは UPDATE FROM 等、別の構造として設計する。

---

## 13. 条件 DSL 要求

### 13.1 対応条件

条件 DSL は、KProperty と `ColumnRef` の双方で、少なくとも次を扱う。

- `eq`／`equal`
- `ne`／`notEqual`
- `gt`／`ge`／`lt`／`le`
- `like`／`notLike`
- `glob`／`notGlob`
- `inList`／`notInList`
- `inSelect`／`notInSelect`
- `between`
- `isNull`／`isNotNull`
- `exists`／`notExists`
- `and`／`or`
- raw 条件 `condition(text, values...)`

### 13.2 条件メソッドの戻り値

条件追加メソッドの目的は内部リストへの条件登録である。

- `MutableList.add()` の結果である `Boolean` を API の意味として公開しない。
- 条件追加メソッドの戻り値は `Unit` とする。
- 条件は `BaseConditionBuilder` 内部のリストへ副作用として追加し、`buildList()` でまとめて取得する。
- 別名メソッド（`equal`、`notEqual`、`graterThan` 等）も、元メソッドへ委譲して `Unit` を返す。
- `and`／`or` も論理条件を内部リストへ追加する操作であり、戻り値を判定値として使用しない。

最新版では、主要な条件追加メソッドの `Boolean` → `Unit` 改修は実施済みである。今後、式本体化によって意図せず `Boolean` を再公開しないこと。

### 13.3 raw 条件

- `?` の個数とバインド値数を一致させる。
- 不一致は build 後ではなく定義時にエラーとする。
- raw 条件を理由に、通常 DSL の型安全性を壊してはならない。

---

## 14. INSERT 要求

- 1 件および複数件の Entity を追加できること。
- VALUES 句は行数 × カラム数のプレースホルダーを生成する。
- バインド値は Entity 登録順、Entity 内の DML 対象カラム順とする。
- Entity が 0 件の場合は build を拒否する。
- Entity 追加後は SQL キャッシュを無効化する。
- 同じ `Insert` を再 build しても、バインド値を重複追加しない。

---

## 15. UPDATE 要求

- Entity を渡す SET と、DSL による SET の双方をサポートする。
- 算術式、カラム参照、値バインドを SET 右辺に指定できること。
- `UPDATE ... FROM ... JOIN ...` を構築できること。
- WHERE で通常条件、IN SELECT、EXISTS を使用できること。
- SET のバインド値を最初に、続いて JOIN／WHERE の SQL 出現順に配置すること。
- SET が空の場合は build を拒否する。
- 通常の UPDATE は WHERE なしで実行させない。
- 全件更新は専用 API による明示操作とし、通常経路と混同しない。
- 現行 `updateAll()` は、名称、WHERE 検証、`isBuild` の扱いに不整合があるため、全件更新 API の完成形として扱わない。
- `isBuild` は SQL キャッシュ状態だけを表し、「全件更新を許可した」という別の意味に流用しない。

### 15.1 UPDATE FROM のスコープ

- SET 句で参照する外部テーブルは FROM／JOIN のスコープに存在しなければならない。
- EXISTS 内だけに存在する alias を SET 句で使用してはならない。
- 複数行が候補になる値を SET する場合、採用行を一意に決める条件が必要である。

---

## 16. DELETE 要求

- WHERE 条件付き DELETE を生成できること。
- DELETE では通常、テーブル alias を条件式へ付けない。
- 条件のバインド値を保持すること。
- 通常の DELETE は WHERE なしで実行させない。
- 全件削除は `deleteAll()` 等の専用 API による明示操作を必須とする。
- 全件削除許可フラグと build 済みフラグを同じ意味で流用しないこと。
- 現行実装は、通常 `build()` でも WHERE 未指定時に全件 DELETE を生成できるため、安全仕様として未完成である。
- `deleteAll()` を呼ばない限り WHERE 未指定を拒否する回帰テストを先に定義してから修正すること。

---

## 17. UPSERT 要求

UPSERT は次の SQL 概念を表す。

```sql
INSERT ... ON CONFLICT (...) DO UPDATE SET ... [WHERE ...]
```

要求事項：

- 1 件／複数件の Entity を追加できること。
- 競合判定キーを `onConflict` で明示すること。
- DO UPDATE SET を `set` で明示すること。
- `excluded.column` を参照できること。
- DO UPDATE の実行条件を WHERE で指定できること。
- Entity 0 件、競合キー 0 件、SET 0 件は build を拒否する。
- バインド値順は INSERT VALUES、SET、WHERE の順とする。
- SQLite のバインド変数上限を考慮し、大量件数は呼び出し側または共通実行 API で分割する。

---

## 18. ABSERT 要求

`Absert` は誤字ではなく、AndrORM 内で次を表す正式名称である。

```sql
INSERT ... ON CONFLICT (...) DO NOTHING
```

- ラデックは独断で `InsertIgnore`、`DoNothingInsert` 等へ改名してはならない。
- Entity と競合キーを必須とする。
- 1 件／複数件の VALUES に対応する。
- バインド値は Entity 登録順とする。

---

## 19. バインド値要求

### 19.1 基本原則

- SQL 内の `?` と `bindValues` は個数・順序とも一致すること。
- 子クエリのバインド値は、子クエリが SQL に現れる位置へ統合すること。
- `build()` の再実行で同じ値が重複しないこと。
- 条件の追加順と論理構造を維持すること。

### 19.2 DML バインド

`SQLiteStatement` では次を使用する。

- null：`bindNull`
- 整数、Boolean：`bindLong`
- 浮動小数：`bindDouble`
- String、日時：`bindString`
- ByteArray：`bindBlob`

### 19.3 SELECT バインド

SELECTは `rawQueryWithFactory` 等の型付きSQLiteQuery経路を使用し、SQL内の `?` と `bindValues` の個数・順序を一致させる。

- null：`bindNull`
- 整数、Boolean：`bindLong`（Booleanは `1`／`0`）
- 浮動小数：`bindDouble`
- String、日時：`bindString`
- ByteArray：`bindBlob`
- WHERE句で列がNULLであることを判定する場合は `IS NULL`／`IS NOT NULL` を使用する。値比較またはSELECT射影の値としてnullを渡す場合は `?` と `bindNull` を使用できる。
- SQLへ直接 `NULL` または `X'...'` を記述した場合、そのリテラルには対応するbind値を追加しない。

---

## 20. DB 実行要求

`AndrOrmDatabaseHelper` は次を提供する。

### 20.1 DML

- クエリビルダーを受け取って実行
- SQL 文字列とバインド値を受け取って実行
- INSERT／UPDATE／DELETE／UPSERT／ABSERT の処理件数を返す

### 20.2 SELECT

- `executeSelectAsCursor`
- `executeSelectAsMapList`
- `executeSelectAsEntityList`

Entity 形式では、テーブル alias をキーとした次の形式を維持する。

```kotlin
List<Map<String, SelectEntity?>>
```

### 20.3 JOIN 結果復元

- SELECT 結果 alias と Entity プロパティを正しく対応付ける。
- LEFT JOIN 等で結合先の全列が null の場合、結合先 Entity 自体を null とする。
- 自己結合では alias ごとに別 Entity として復元する。
- `Int` 等への数値変換は DB の戻り型が `Long` でも安全に行う。

### 20.4 トランザクション

- ブロック全体が成功した場合のみ commit する。
- 例外を握りつぶさず rollback させる。
- Cursor は必要に応じてトランザクションブロック内で閉じる。
- ネストトランザクションを追加する場合は SQLite の挙動を確認して設計する。
- savepointを使用する場合、savepointより前の処理を維持したまま、以降の処理だけを `ROLLBACK TO SAVEPOINT` で戻せること。
- 期待した制約違反を検査する場合は例外を保存し、同一stepまたは直後の検証stepで対象テーブルと対象キーを確認して、例外を無条件に握りつぶさないこと。

### 20.5 ViewSelect

- View定義用のSELECTは通常のSelectと分離した `ViewSelect` を使用する。
- CREATE VIEWへ渡すSQLに `?` を残してはならない。`bindValues` は空でなければならない。
- ViewSelectの値条件・値射影では、Boolean、数値、文字列、日時、`NULL`、`ByteArray`をSQLite SQLリテラルへ変換する。
- 文字列リテラルはシングルクォートを二重化し、NUL文字を拒否する。浮動小数は有限値だけを許可する。
- BLOB条件の例は `PAYLOAD = X'00FF'` とし、ByteArrayの内容を16進表現へ変換する。
- ViewSelectで生成したViewは読み取り専用であり、通常のDML対象Entityとして扱わない。

### 20.6 UnionAll

- `UnionAll` は2件以上のSelectを指定順に結合し、各Selectの出力列数が一致することを検証する。
- 各構成SelectのSQLを出現順に連結し、構成Selectのbind値も同じ順序で `bindValues` へ統合する。
- ORDER、LIMIT、OFFSETはUNION ALL全体へ適用する。構成Selectへ個別に指定してはならない。
- UNION ALL全体のLIMIT／OFFSETのbind値は、構成Selectのbind値より後へ追加する。
- `SelectQuery` を共通契約として、Entity、Map、Cursorの各取得経路から実行できること。
- 結果Entityの列aliasとEntity復元対象は `resultEntity` を基準に正規化する。
- nullable列を補完する値射影は、通常の実行SELECT／UNION ALLでは `? AS <alias>` とし、`bindValues`へ `null`を保持する。ViewSelectでは `NULL AS <alias>` とする。

---

## 21. DB 作成・移行要求

### 21.1 新規作成

登録された全 `TableDefinitionEntity` について、テーブルとインデックスを生成する。

- 同じEntityクラスが複数回登録されても、Helperでテーブル名重複として事前拒否しない。登録順に `CREATE TABLE` を実行し、SQLiteの結果または例外をそのまま通知する。
- 異なるEntityクラスが大文字小文字を無視して同じ明示 `@Table.name` を持つ場合は、実行前にdetektのERRORとして検出する。

### 21.2 バージョンアップ

標準移行は次の流れとする。

1. Entity ごとに旧テーブル名と一時テーブル名を決定
2. 前回の移行失敗で残った可能性がある一時テーブルを `DROP TABLE IF EXISTS` で削除
3. コンストラクタ順で新旧カラム対応を作成
4. 同名カラムまたは `@ColumnOldName` 指定元を解決
5. 新定義で一時テーブルを作成
6. `INSERT ... SELECT ...` でデータを転送
7. 新バージョン用インデックスを生成
8. 旧テーブルを削除
9. 一時テーブルを元名へ変更

一時テーブル名は `<テーブル名>_new` とする。`_new` 接尾辞は標準移行処理専用として予約し、利用者は同名テーブルを作成したりデータを保存したりしてはならない。標準移行開始時に同名テーブルが存在する場合、そのテーブルとデータは削除される。

旧テーブルに存在しない新規カラムは、次の方針で扱う。

- nullableかつ `@MigrationDefault` 未指定：転送カラムから除外し、新テーブル側のNULL／DEFAULTに委ねる。
- `@MigrationDefault` 指定あり：型別検証後、検証済みSQLリテラルまたは式をSELECT対象として転送する。
- non-nullかつ `@MigrationDefault` 未指定：エンティティ名とプロパティ名を示して移行をエラーとする。
- data class のプロパティデフォルト値へフォールバックしてはならない。
- Booleanは `1`／`0`、文字列と日時はSQLシングルクォート形式を使用する。
- LocalDate／LocalTime／LocalDateTimeは9.2と同じ形式および予約値を許可する。
- ByteArrayは9.2と同じBLOBリテラル（空BLOBの `X''` を含む）を許可し、nullableの `NULL` と区別する。`@MigrationDefault` は既存行への値転送に使用し、CREATE TABLEのDEFAULT句には使用しない。

追加要求：

- カスタムカラムマッピングを追加できること。
- 転送対象カラムの重複、不足、旧カラム不存在を検証すること。
- 移行前後で既存レコード件数を維持すること。
- 新規カラムの既定値が全既存行へ正しく反映されること。
- データ損失につながる変更は、推測で実装せず確認すること。

### 21.3 VIEWの作成・移行

- `ViewDefinitionEntity` はテーブル定義Entityと独立した読み取り専用定義として扱う。
- 新規作成時は、全テーブル、インデックスの作成後にViewを作成する。
- バージョンアップ時は、依存するViewを先に削除し、テーブル移行とインデックス作成後にViewを再作成する。
- ViewのSQLはViewSelectで生成し、CREATE VIEWへバインド変数を渡さない。
- Viewの参照は通常SELECTのFROM／JOIN対象として扱えるが、View自体へのINSERT／UPDATE／DELETEは提供しない。

---

## 22. テスト要求

### 22.1 テストフレームワーク

- JVM Unit Test：JUnit 5
- AndroidTest：JUnit 4
- パラメータ化可能なケースは `@ParameterizedTest` を優先する。
- Mockito の複雑な使用は、前提が曖昧な場合に川流へ確認する。

### 22.2 期待値の原則

- 期待値は要求仕様、SQL 仕様、会話で決定した仕様から導く。
- 現在のメソッド実行結果を見て期待値を合わせてはならない。
- 不具合を再現するテストは、修正前に失敗することを確認できる形にする。
- 修正後、既存テストを弱めて通してはならない。

### 22.3 クエリビルダーのテスト

各機能について、少なくとも次を確認する。

- 完成 SQL 文字列
- バインド値の個数
- バインド値の順序
- alias
- 複数回 build
- 条件追加後の再 build
- 空入力・重複指定・不正値の例外
- 自己結合
- JOIN 後の抽出列
- EXISTS／ネスト EXISTS
- サブクエリのバインド値
- ViewSelectの `NULL`／BLOBリテラル、引用符、NUL文字、浮動小数の検証
- 通常SELECTのBLOB条件、`bindBlob`、null値の `bindNull`
- UnionAllの列数検証、構成Select順、null射影、LIMIT／OFFSETを含むbind順

### 22.4 KSP テスト

- 正常生成
- 生成クラス名
- 出力パッケージ
- インターフェース
- Column／Function のコピーと補完
- DEFAULT 値検証
- ERROR／WARNING
- 不正定義で生成しないこと

### 22.5 Android 結合テスト

現在のテストフローを壊さないこと。

1. `step01` DB 作成
2. `step02` テーブル確認
3. `step03` SeedData INSERT
4. `step04` INSERT 後件数確認
5. `step05` UPDATE
6. `step06` UPDATE 件数確認
7. `step07` UPSERT
8. `step08` UPSERT 後件数確認
9. `step09` ABSERT
10. `step10` ABSERT 後件数確認
11. `step11` JOIN SELECT
12. `step12` JOIN 結果確認
13. `step13` DELETE
14. `step14` DELETE 件数・残存件数確認
15. `step15` 相関 EXISTS SELECT
16. `step16` EXISTS 結果、SQL、バインド値確認
17. `step17` DB バージョン 1 → 2 アップグレード
18. `step18` 追加カラム、MigrationDefault、テーブル、既存件数の確認
19. `step19` 1トランザクション内でマスタ3テーブルを各10件追加後、savepoint以降の5テーブル追加を所持品複合キー重複でロールバック
20. `step20` マスタ3テーブルだけが各10件増え、savepoint以降のテーブルが増えていないことを確認
21. `step21` step03由来データとstep19由来データをテーブル別に結合し、各テーブル1回のABSERTで登録する。マスタ3テーブルの登録完了後にsavepointを作成する
22. `step22` step21で既存キーが更新されず、未登録キーだけが追加されたこと、およびsavepoint内のABSERTが成功したことを検証する
23. `step23` 既存5キャラクターについて、level 1～10ごとに10種類のstatusTypeから規則的に分散した3種類を選び、CHARACTER_STATUSへ合計150件をUPSERTする
24. `step24` step23のUPSERT処理件数、複合キー、各characterPk・levelの件数、および登録値を検証する
25. `step25` CHARACTER_STATUS単体、およびCHARACTER_STATIC_INFOからCHARACTER_STATUSへのLEFT JOINで、characterPk・statusType単位のlevel最大値とvalue累計を取得する
26. `step26` step25の単体集計・LEFT JOIN集計の結果、生成SQL、およびバインド値順を検証する

追加済みの要求：

- UPDATE 対象は全 9 テーブルについて概ね 30％とする。
- UPSERT 対象も全 9 テーブルについて概ね 30％とする。
- step05 と step07 の対象を意図せず全面的に重複させない。
- 全レコードを書き換えない。
- 親子不整合データを一定割合含め、JOIN／LEFT JOIN の結果を検証する。
- DELETE 前に `updateMethod` 別の対象件数を取得し、実削除件数と削除後件数の双方を検証する。
- EXISTS は外側と内側のカラム比較を含む相関サブクエリとして検証する。
- EXISTS の結果件数だけでなく、生成 SQL とバインド値順も検証する。
- DB アップグレードでは既存レコード件数を維持し、追加カラムの DEFAULT 反映を確認する。
- step19では `ITEM_MASTER`、`MAGIC_TYPE_MASTERY`、`SPELLS_MASTER` の各10件をsavepointより前へ追加する。
- step19ではsavepoint作成後、`CHARACTER_EQUIP`、`CHARACTER_SPELLS`、`CHARACTER_STATIC_INFO`、`CHARACTER_STATUS`、`CHARACTER_POSSESSIONS` の各10件追加を試行する。
- `CHARACTER_POSSESSIONS` の10件目は1件目と `(CHARACTER_PK, ITEM_PK)` を重複させ、複合キー制約違反を発生させる。
- 制約違反後はsavepointまで戻し、savepointより前のマスタ3テーブル追加を含む外側トランザクションをcommitする。
- step19はデータ操作とsavepointへのロールバックに限定し、トランザクション内で件数をassertしない。
- step20では制約違反の対象テーブル・対象キー、全テーブル件数、`CREATE_METHOD = 'step19'` の件数を確認し、部分ロールバックを検証する。
- step21では、step03由来データをABSERTした後にstep19由来データを別途ABSERTしてはならない。各テーブルで両方のリストを先に結合し、1つのABSERTへ一括設定する。
- step21ではマスタ3テーブルのABSERT完了後にsavepointを作成し、残りのテーブルをsavepoint内でABSERTする。
- step21のABSERTは、対象端末が必要なバインド変数数を許容する前提で分割しない。
- step22ではstep21実行前のキー集合と投入キー集合の和集合を期待値とし、全テーブルのキー集合、総件数、追加件数を検証する。
- step23では既存のCHARACTER_STATIC_INFOから5件のcharacterPkを使用し、CHARACTER_STATIC_INFOへ新規データを追加しない。
- step23のstatusTypeは`HP`、`MP`、`SP`、`STR`、`VIT`、`DEX`、`AGI`、`INT`、`WIS`、`FTN`の10種類とし、levelごとに検証可能な規則で3種類を分散させる。
- step23のUPSERT競合キーは`(CHARACTER_PK, LEVEL, STATUS_TYPE)`とし、level 1の既存キー競合を含めて150件を処理する。
- step25では`UPDATE_METHOD = 'step23'`を対象とし、step23で更新した既存15件と追加した135件を合わせた150件から累計を取得する。
- step25のLEFT JOIN取得項目はcharacterPk、characterName、statusType、levelの最大値、valueの累計とする。
- 各 Step 終了時に全テーブル CSV を出力する。
- JOIN／EXISTS の SELECT 結果も CSV に出力する。
- BLOB検索条件は、登録したByteArrayと同じ内容が実機SQLiteで一致することを確認する。
- Viewは、SQLリテラル生成、作成、参照、DML拒否を実機SQLiteで確認する。

### 22.6 BLOB追加テスト方針（2026-09-05）

- 今回の変更範囲はBLOBのテスト補強、不具合検出用テスト、およびDEFAULT／MigrationDefaultに関する本書と日英Referenceの整合化とする。本体の不具合修正は含めない。
- EntityへのSELECT結果復元では、`00 01 7F 80 FF` を持つByteArrayを内容比較し、符号境界を含む全バイトが保持されることを検証する。
- nullable BLOBでは、NULL、空BLOB、非空BLOBを別レコードとして登録し、Cursor／Map／Entityで区別して取得できることを検証する。Entityにはnon-nullの識別列を持たせ、全列NULLのJOIN結果とは区別する。
- 空BLOBは `ByteArray(0)` として保存・復元できること、SQLiteの型がBLOBで長さが0であること、同じ内容の検索条件でNULL行と混同しないことを検証する。
- KSP通常カラムでは、生成元の `ByteArray`／`ByteArray?` の型とnull許容性を生成プロパティおよび実際の生成Entityで確認する。
- SQL関数がBLOBを返す場合は、既知のバイト列を返すraw式を使用し、Cursor／Map／Entityの取得内容を検証する。型推論不能な関数は7.2に従い `returnHint = AndrOrmValueType.BYTE_ARRAY` で生成型が `ByteArray` になることを検証する。
- 不具合検出用テストは仕様上の成功結果を期待し、現行実装の誤出力や例外を正解として固定しない。失敗したテストは未対応箇所として報告する。
- AndroidTestは `app/src/androidTest` に置く。既存stepテストの順序、共有状態、CSV出力、期待値を維持する。
- 検証は対象Unit Test、KSP生成とAndroidTestコンパイル、実SQLiteでのAndroidTest、対象静的解析に分け、未実行と実行失敗を区別する。

### 22.7 テスト実行コマンド

代表例：

```powershell
# main KSP
.\gradlew :app:kspDebugKotlin

# AndroidTest 用 KSP
.\gradlew :app:kspDebugAndroidTestKotlin

# Runtime Unit Test
.\gradlew :androrm-runtime:testDebugUnitTest

# AndroidTest
.\gradlew :app:connectedDebugAndroidTest

# 一連の確認
.\gradlew :app:clean :app:connectedDebugAndroidTest
```

実行していないタスクを「成功した」と報告してはならない。

---

## 23. 静的解析・ログ要求

- detekt 設定とカスタムルールを尊重する。
- 修正対象外の警告を無断で抑止しない。
- `@InfoLog`、`@TraceLog` と AspectJ の構成を理解せずに削除しない。
- AndroidTest では必要に応じてファイルログを停止し、テスト結果を安定させる。
- KSP ログ、APP ログ、DETEKT ログの責務を混在させない。
- ログ追加で機密データや大量のバインド値を無制限に出力しない。

---

## 24. コーディング規約

### 24.1 分岐の優先順位

多重分岐は次の順で検討する。

1. Map（キー：関数）
2. enum
3. `when`
4. `if`

### 24.2 `if`

- 条件反転を前提とした読みにくい書き方を避ける。
- `if (condition == false)` は使用しない。
- 本処理を肯定条件側に置くことを優先する。
- 不要な `else { continue }` を作らない。

### 24.3 ループ

カウンタが必要なループは、原則として次の形式を使用する。

```kotlin
for (index in range) {
    // ...
}
```

### 24.4 API と命名

- 既存の公開メソッド名、クラス名、用語を独断で変更しない。
- `selectStatement` は SELECT 文、`SelectClause` は文を構成する句を表す。
- 「sentence」等、SQL 用語として不適切な名前へ置き換えない。
- Boolean を返す意味がない副作用メソッドは `Unit` を検討する。
- 抽象化は共通処理の実態に基づいて行い、名前だけの共通化をしない。
- `BaseSelect`／`SelectBody` 等の既存責務を無視して同じ処理を別実装しない。

---

## 25. 変更実施手順

ラデックは、修正前に次を実施すること。

1. 川流の仕様指示を受け、対象範囲と未確定点を整理する。
2. `REQUIREMENTS.md` を先に更新し、仕様差分、影響範囲、テスト方針を記載する。この段階ではプログラムを変更しない。
3. `REQUIREMENTS.md` の差分を川流へ提示し、確認・承認を受ける。
4. 最上位 `AGENTS.md` と承認済み `REQUIREMENTS.md` を読む。
5. 最新ソースの対象クラス、呼び出し元、派生クラス、委譲先、テストを検索する。
6. 変更対象ファイルとメソッドを特定し、実装案を提示して承認を受ける。
7. 生成される最終 SQL とバインド値を紙上またはテストで展開する。
8. 承認された範囲だけを実装し、必要な Unit Test／AndroidTestを追加・修正する。
9. 既存処理の二重化、削除、改悪がないか確認する。
10. 実行可能な検証タスクを実行する。
11. 実行結果と未確認事項を分けて報告する。

### 25.1 修正報告に含めるもの

- 修正目的
- 修正ファイル
- 修正メソッド
- 変更前の問題
- 変更後の挙動
- 生成 SQL の例
- バインド値順
- 実行したテスト
- 未実行／未確認事項

---

## 26. 川流の地雷・禁止事項

この章は最重要である。

### 26.1 質問を無視しない

- 聞かれていない別問題へ回答をすり替えない。
- まず質問の核心へ答える。
- 不要な別案、余談、新規コードを大量に追加しない。

### 26.2 最新ソースを無視しない

- ソースが提供されているのに、記憶や一般論だけで回答してはならない。
- 既に修正済みの箇所を再度「修正してください」と指摘してはならない。
- 読めない／確認できない場合は、その旨を明言する。

### 26.3 縮小前提で考えない

次の表現・姿勢を避ける。

- 「最小限で」
- 「最小構成なら」
- 「とりあえず」
- 「暫定的に」
- 「趣味の開発なので」

AndrORM は実務利用と他者への引き継ぎを前提に設計する。

### 26.4 テストを実装へ合わせない

- 現在の出力から期待値を逆算しない。
- テストが落ちたから期待値を変更する、という対応をしない。
- 仕様、SQL 文法、決定済みデータから期待値を算出する。

### 26.5 独断で API を作り替えない

- 既存 DSL を読まずに新 DSL を追加しない。
- クラス名や用語を「一般的だから」という理由だけで変えない。
- `Absert` を誤字扱いしない。
- Room 等へ置き換える提案をしない。

### 26.6 SQL 全体を確認する

DSL の一部分だけ見て正しいと判断してはならない。

必ず次を確認する。

- 最終 SQL
- alias のスコープ
- 句の順序
- プレースホルダー数
- バインド値順
- サブクエリと外側クエリの相関
- 複数行候補の一意性

### 26.7 正しい設計を欠点探しへすり替えない

- 川流が示した設計意図を確認せず、「一般論では別案がよい」と欠点探しを始めない。
- 継承、委譲、共通化は、実際に共有する責務と既存コードを読んで判断する。
- 既存設計で成立しているものを、好みだけで否定しない。
- 問題点を指摘する場合は、生成 SQL、型、スコープ、テスト結果等の根拠を示す。

### 26.8 余計な改修を混ぜない

- 指定箇所以外を無断で整形・改名・再設計しない。
- コードを出す場合は、前回との差分と重複処理を確認する。
- 動いている機能を「きれいにする」だけの理由で壊さない。

### 26.9 確認していないことを断定しない

- ビルドしていないのに「ビルド成功」と書かない。
- テストしていないのに「問題ありません」と断定しない。
- 推測と確認済み事実を分ける。

### 26.10 誤りを指摘された場合

質問と異なる回答をした場合：

1. 先に謝罪
2. 本来の質問へ回答

提示コードに誤りがあった場合：

1. 先に謝罪
2. 間違った原因を説明
3. 回避策を提示
4. 同じ失敗を繰り返さない旨を示す

川流が回避策または再発防止策を決めた場合は、川流の内容へ差し替えること。

### 26.11 謝罪を後回しにしない

誤りを認める説明や訂正を先に長く書き、最後に謝罪してはならない。最初に謝罪すること。

---

## 27. 最新版で確認した実装状況・注意点

### 27.1 条件 DSL の戻り値

`BaseConditionBuilder` の主要な条件追加メソッドは、内部条件リストへ追加して `Unit` を返す形へ改修済みである。

- 今後、`list.add(...)` を式本体として戻り値を `Boolean` に戻さない。
- KDoc に「追加できた場合 true」等の意味のない戻り値説明を復活させない。
- 別名メソッドも戻り値を判定用途へ使用しない。

### 27.2 EXISTS

最新版では次を確認している。

- `ExistsSelect` は `BaseSelect` を継承する。
- `select 1 from ...` を生成する。
- `exists(TableRef, block)` は条件ブロックを必須とする。
- ネストした EXISTS を構築できる。
- AndroidTest の step15／step16 で相関 EXISTS、生成 SQL、バインド値を検証する。
- EXISTS 内の alias は外側の SET 値として利用できない。

修正時は、初回 build、再 build、FROM の重複、ネスト時のバインド値順を Unit Test でも固定すること。

### 27.3 全件 UPDATE／DELETE の安全性

意図する安全仕様と現行コードに差がある。

- 通常 UPDATE／DELETE は WHERE 必須。
- 全件操作は専用 API の明示呼び出し必須。
- SQL キャッシュ状態と全件操作許可状態は別変数で管理する。
- 現行 `Update.updateAll()` は `isBuild = true` の後に未初期化 `query` を参照し得るため、全件更新許可 API として成立していない。
- 現行 `Delete.build()` は、1回目に WHERE 付き SQL を返しても、2回目は WHERE のない SQL を返し得る。build の再実行安定性を満たしていない。
- 現行 `Delete.deleteAll()` は全件削除許可と build 済み状態を同じ `isBuild` で表している。
- `Update.updateAll()` と `Delete.deleteAll()` は、名称、検証、状態管理を含めて再設計対象である。
- 修正前に、条件あり、条件なし、全件明示、再 build、例外後の再 build のテストを仕様から作成する。

### 27.4 Android 結合テスト

最新版では step20 まで存在し、DELETE検証、EXISTS、DBアップグレード、savepointによる部分ロールバック検証が追加されている。  
今後の追加は既存の順序実行、共有状態、CSV 出力、`verifyStep` の関係を確認して行う。

### 27.5 最上位 ReadMe の実装状況

最新版の最上位 `AGENTS.md` は、開発基準と謝罪手順については必須資料である一方、実装状況欄は現行ソースより古い。

現行ソースでは、少なくとも次が存在する。

- 複数件 INSERT
- UPDATE
- DELETE
- UPSERT
- ABSERT
- EXISTS
- LIMIT／OFFSET
- DB アップグレード
- AndroidTest step01～step20

実装状況の判断は最新ソースとテストを優先する。

### 27.6 Gradle 設定上の不整合

`settings.gradle.kts` には `:androrm-logging-ksp` が含まれるが、基準 ZIP 内に対応するモジュールディレクトリは確認できない。

- 独断で include を削除しない。
- 独断で空モジュールを新設しない。
- ビルドへ影響する場合は、川流へ本来の配置または削除方針を確認する。

### 27.7 ZIP 内の生成物・実行結果

基準 ZIP には `.git`、IDE 設定、Gradle キャッシュ、build 出力、ログ等が含まれる。

- これらを最新手書きソースと誤認しない。
- 生成済み KSP Entity を直接修正しない。
- CSV やテストレポートを要求仕様そのものとして扱わない。
- 変更対象の判定では `src/main`、`src/test`、`src/androidTest`、Gradle 設定、最上位文書を中心に確認する。

---

## 28. ラデック向け完了条件チェックリスト

実装完了と報告する前に、次を確認すること。

- [ ] 川流の依頼内容を一文で説明できる
- [ ] 最上位 `AGENTS.md` と `REQUIREMENTS.md` を確認した
- [ ] 最新ソースを確認した
- [ ] 関連クラス、派生クラス、委譲先、テストを確認した
- [ ] 変更対象を依頼範囲に限定した
- [ ] build／generated／CSV 等を手書きソースと誤認していない
- [ ] 生成 SQL を確認した
- [ ] alias とスコープを確認した
- [ ] プレースホルダー数を確認した
- [ ] バインド値順を確認した
- [ ] build の再実行を確認した
- [ ] 仕様から期待値を作成した
- [ ] Unit Test を実行した
- [ ] 必要な場合 AndroidTest を実行した
- [ ] AndroidTest の step01～step20 への影響を確認した
- [ ] KSP 修正の場合、各 KSP タスクを確認した
- [ ] detekt の結果を確認した
- [ ] 実行していない確認を明記した
- [ ] 確認済み事実と推測を分離した
- [ ] 既存機能を無視した別実装を作っていない
- [ ] 「最小限」「暫定」を理由に要件を削っていない
- [ ] 誤りがあった場合、最初に謝罪した

---

## 29. 最終原則

AndrORM の実装では、コード量の少なさより、次を優先する。

1. 仕様との一致
2. 既存設計との整合
3. SQL とバインド値の正確性
4. 型安全性
5. 可読性
6. 保守性
7. 拡張性
8. 回帰テスト
9. 他者へ引き継げる説明可能性

ラデックは、川流の要求を勝手に縮小せず、最新ソースを読み、最終 SQL まで確認し、確認できた事実だけを報告すること。

## 30. 最終化修正指示（2026-09-07）

### 30.1 対象と除外

View、BLOB、Cursor から Entity への直接変換を最終確認可能な状態へ仕上げる。依頼書の 2 番（既存対応分）は今回の変更対象から除外する。

### 30.2 FunctionProjection の BLOB リテラル

`FunctionProjection.args` は、プロパティ名、文字列、Boolean、数値に加えて、大文字小文字を問わない SQLite BLOB リテラル `X'<偶数桁の16進数>'`／`x'<偶数桁の16進数>'` を受け付ける。`X''`／`x''` は空 BLOB として受け付ける。奇数桁、16進数以外の文字、閉じ引用符のない値は拒否する。BLOB リテラルの型は `kotlin.ByteArray` として関数戻り値型推論へ渡す。

### 30.3 継承判定

AndrORM の全モジュール（common、runtime、generator-ksp、detekt-rules、app）で、マーカーインターフェースの判定は直接の `superType` だけでなく、複数段を含む継承階層全体を対象とする。KSP、Detekt、Runtime／Common で同じ定義に対する判定結果を一致させる。直接継承、1 段間接継承、複数段間接継承をテストする。

### 30.4 View 統合・異常系・Upgrade

ViewDefinitionEntity、`@View`、`@Projection(SELECT)`、KSP 生成 Entity、`CursorEntityMapper`、実 SQLite の View、`executeSelectAsEntityList()` を一連で検証する。View に関する禁止仕様（マーカーと `@View` の不一致、`@Table` 併用、不正な DML 用途、不正な `@Function`、`hideFromSelect`、物理カラム重複、SELECT Projection 不在）には、検出モジュールに異常系テストを追加する。DB Version 1 から 2 の実 SQLite Upgrade で View の DROP／再 CREATE と新しい Table 構造参照を確認し、`obsoleteViewNames()` で廃止 View が削除されることを確認する。

### 30.5 BLOB DML と全型 CursorEntityMapper

実 SQLite の AndrORM DML Builder で、BLOB を含む INSERT、UPDATE、UPSERT、ABSERT の登録・更新・重複時挙動を確認する。値には `00 01 7F 80 FF` を含める。KSP 生成 CursorEntityMapper の Android Test では、Int、Long、Float、Double、Boolean、String、LocalDate、LocalTime、LocalDateTime、ByteArray と代表的 nullable 型を同一経路で検証し、`executeSelectAsEntityList()` が Map 中間経路を使わず復元することを確認する。

### 30.6 Boolean 異常系テスト

`ValueFromCursor.BOOLEAN` の 0／1 以外検証では、Mockito の Cursor に `getType()` を `FIELD_TYPE_INTEGER`、`getInt()` を 2 と設定し、型検査ではなく Boolean 値検証が原因で `IllegalArgumentException` になることを固定する。

### 30.7 検証

実装前に対象ソース、呼び出し元、派生クラス、既存テストを確認する。実装後は Unit Test、KSP 生成、Android Test コンパイル、接続端末上の全 Android Test（Failure／Error 0）、Detekt、および `E:\Projects\bat\AndrORM生成.ps1` の結果を個別に報告する。公開 API、修正対象外コメント、既存 AndroidTest の step 順序・共有状態・CSV 出力を変更しない。
