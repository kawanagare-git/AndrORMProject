# AndrORM PlantUML設計図

対象ソース: `AndrORM202607230159.zip`

最新版ソースを基に、モジュール単位を基本として作成したPlantUML設計図です。
複数モジュールをまたぐ処理は `00-overall` または `testing` に配置しています。

## 収録方針

- 指定された8種類（シーケンス、ユースケース、クラス、アクティビティ、コンポーネント、状態、配置、タイミング）を収録
- `app`、`androrm-common`、`androrm-generator-ksp`、`androrm-detekt-rules`、`shared-library`をモジュール別に整理
- Gradle上の独立モジュールではない`test-support`と`ksp-fixtures`も独立章として収録
- クラス図は、可読性を保つ詳細図と、宣言漏れ確認用の完全一覧図に分割
- Unit Test／Android Testは`testing`に横断図として収録

## 図一覧

### `00-overall`

- `01_usecase_overall.puml`
- `02_class_module_dependencies.puml`
- `03_sequence_end_to_end.puml`
- `04_activity_end_to_end.puml`
- `05_component_overall.puml`
- `06_state_lifecycle_overall.puml`
- `07_deployment_overall.puml`
- `08_timing_build_runtime.puml`

### `androrm-common`

- `01_usecase_common.puml`
- `02_class_annotations_entities.puml`
- `03_class_metadata_functions_logging.puml`
- `04_class_complete_catalog.puml`
- `05_sequence_entity_meta_validation.puml`
- `06_activity_default_validation.puml`
- `07_component_common.puml`
- `08_state_validation_result.puml`
- `09_timing_validation.puml`

### `androrm-detekt-rules`

- `01_usecase_detekt.puml`
- `02_class_detekt.puml`
- `03_class_complete_catalog.puml`
- `04_sequence_analysis.puml`
- `05_activity_rules.puml`
- `06_component_detekt.puml`
- `07_state_rule_analysis.puml`
- `08_deployment_gradle.puml`
- `09_timing_detekt_task.puml`

### `androrm-generator-ksp`

- `01_usecase_ksp.puml`
- `02_class_ksp.puml`
- `03_class_complete_catalog.puml`
- `04_sequence_generation.puml`
- `05_activity_generation.puml`
- `06_component_ksp.puml`
- `07_state_processor.puml`
- `08_deployment_build.puml`
- `09_timing_ksp_round.puml`

### `app`

- `01_usecase_runtime.puml`
- `02_class_query_builders.puml`
- `03_class_condition_expression.puml`
- `04_class_runtime_database.puml`
- `05_class_complete_catalog.puml`
- `06_sequence_select.puml`
- `07_sequence_dml.puml`
- `08_sequence_database_upgrade.puml`
- `09_sequence_transaction_savepoint.puml`
- `10_activity_select_build.puml`
- `11_activity_dml_build.puml`
- `12_activity_database_upgrade.puml`
- `13_component_runtime.puml`
- `14_state_query_builder.puml`
- `15_state_savepoint.puml`
- `16_deployment_runtime.puml`
- `17_timing_transaction_savepoint.puml`
- `18_sequence_debug_aop.puml`

### `ksp-fixtures`

- `01_usecase_fixtures.puml`
- `02_class_fixtures.puml`
- `03_sequence_fixture_validation.puml`
- `04_activity_fixture_compile.puml`
- `05_component_fixtures.puml`

### `shared-library`

- `01_usecase_shared.puml`
- `02_class_shared.puml`
- `03_sequence_utils.puml`
- `04_activity_utils.puml`
- `05_component_shared.puml`

### `test-support`

- `01_usecase_test_support.puml`
- `02_class_test_support.puml`
- `03_sequence_parameter_conversion.puml`
- `04_activity_csv_parse.puml`
- `05_component_test_support.puml`
- `06_deployment_test_support.puml`

### `testing`

- `01_class_test_infrastructure.puml`
- `02_sequence_unit_test.puml`
- `03_sequence_android_test.puml`
- `04_activity_test_pipeline.puml`
- `05_component_test_architecture.puml`
- `06_deployment_android_test.puml`
- `07_timing_test_execution.puml`

## 描画方法

PlantUML CLIを利用する場合:

```powershell
plantuml -charset UTF-8 -tsvg .\**\*.puml
```

Visual Studio Codeでは、PlantUML拡張機能で各`.puml`をプレビューできます。

## 構文確認

本成果物では、全ファイルについて次の静的確認を実施しています。

- `@startuml`と`@enduml`の存在・個数
- 中括弧、丸括弧、角括弧の対応
- ファイル名重複
- 空ファイルの有無

この実行環境にはPlantUMLエンジンが導入されていないため、画像レンダリングによる最終確認は実施していません。

## ソース網羅性

`SOURCE_COVERAGE.md`に、最新版プロジェクト内のKotlinソースと対応図を一覧化しています。
