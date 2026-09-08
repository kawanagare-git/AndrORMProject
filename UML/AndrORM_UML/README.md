# AndrORM UML PlantUML（2026-09-06）

## 生成条件

- 入力: `/mnt/data/AndrORM202609051956.zip`
- 対象: `src/main` と `src/debug` の Kotlin / Java ソース
- 除外: `src/test`、`src/androidTest`、`build/generated`、`.gradle`、`.git`
- 制約: クラス図・シーケンス図・アクティビティ図はモジュールを跨がない
- 例外: コンポーネント図のみ、Gradle のモジュール間依存を記述

## 対象モジュール

| モジュール | ソースファイル数 | 抽出宣言数 |
|---|---:|---:|
| `app` | 1 | 1 |
| `androrm-runtime` | 55 | 92 |
| `androrm-common` | 47 | 59 |
| `androrm-generator-ksp` | 27 | 29 |
| `androrm-detekt-rules` | 6 | 12 |
| `shared-library` | 1 | 1 |

## ファイル一覧

- `app/01_class_app.puml`
- `app/02_sequence_app.puml`
- `app/03_activity_app.puml`
- `androrm-runtime/01_class_androrm-runtime.puml`
- `androrm-runtime/02_sequence_androrm-runtime.puml`
- `androrm-runtime/03_activity_androrm-runtime.puml`
- `androrm-common/01_class_androrm-common.puml`
- `androrm-common/02_sequence_androrm-common.puml`
- `androrm-common/03_activity_androrm-common.puml`
- `androrm-generator-ksp/01_class_androrm-generator-ksp.puml`
- `androrm-generator-ksp/02_sequence_androrm-generator-ksp.puml`
- `androrm-generator-ksp/03_activity_androrm-generator-ksp.puml`
- `androrm-detekt-rules/01_class_androrm-detekt-rules.puml`
- `androrm-detekt-rules/02_sequence_androrm-detekt-rules.puml`
- `androrm-detekt-rules/03_activity_androrm-detekt-rules.puml`
- `shared-library/01_class_shared-library.puml`
- `shared-library/02_sequence_shared-library.puml`
- `shared-library/03_activity_shared-library.puml`
- `00_component/01_component_modules.puml`

## 補足

`test-support` と `ksp-fixtures` は今回の `settings.gradle.kts` の include 対象ではないため、初回生成から外しています。
必要であれば、テスト支援用UMLとして別ディレクトリに追加できます。
