# AndrORM PlantUML Design Diagrams

Source project: `AndrORM`

These PlantUML design diagrams were created from the latest project source,
primarily organized by module. Processes spanning multiple modules are placed
under `00-overall` or `testing`.

## Included Diagram Types

- Sequence diagrams
- Use case diagrams
- Class diagrams
- Activity diagrams
- Component diagrams
- State diagrams
- Deployment diagrams
- Timing diagrams

## Organization Policy

- `app`, `androrm-common`, `androrm-generator-ksp`,
  `androrm-detekt-rules`, and `shared-library` are organized by module.
- `test-support` and `ksp-fixtures`, which are not independent Gradle modules,
  are included as separate sections.
- Class diagrams are divided into readable detailed diagrams and complete
  declaration catalogs used to check for omissions.
- Unit Test and Android Test diagrams are collected under `testing` as
  cross-module diagrams.

## Diagram List

### `00-overall`

- `01_usecase_overall_en.puml`
- `02_class_module_dependencies_en.puml`
- `03_sequence_end_to_end_en.puml`
- `04_activity_end_to_end_en.puml`
- `05_component_overall_en.puml`
- `06_state_lifecycle_overall_en.puml`
- `07_deployment_overall_en.puml`
- `08_timing_build_runtime_en.puml`

### `androrm-common`

- `01_usecase_common_en.puml`
- `02_class_annotations_entities_en.puml`
- `03_class_metadata_functions_logging_en.puml`
- `04_class_complete_catalog_en.puml`
- `05_sequence_entity_meta_validation_en.puml`
- `06_activity_default_validation_en.puml`
- `07_component_common_en.puml`
- `08_state_validation_result_en.puml`
- `09_timing_validation_en.puml`

### `androrm-detekt-rules`

- `01_usecase_detekt_en.puml`
- `02_class_detekt_en.puml`
- `03_class_complete_catalog_en.puml`
- `04_sequence_analysis_en.puml`
- `05_activity_rules_en.puml`
- `06_component_detekt_en.puml`
- `07_state_rule_analysis_en.puml`
- `08_deployment_gradle_en.puml`
- `09_timing_detekt_task_en.puml`

### `androrm-generator-ksp`

- `01_usecase_ksp_en.puml`
- `02_class_ksp_en.puml`
- `03_class_complete_catalog_en.puml`
- `04_sequence_generation_en.puml`
- `05_activity_generation_en.puml`
- `06_component_ksp_en.puml`
- `07_state_processor_en.puml`
- `08_deployment_build_en.puml`
- `09_timing_ksp_round_en.puml`

### `app`

- `01_usecase_runtime_en.puml`
- `02_class_query_builders_en.puml`
- `03_class_condition_expression_en.puml`
- `04_class_runtime_database_en.puml`
- `05_class_complete_catalog_en.puml`
- `06_sequence_select_en.puml`
- `07_sequence_dml_en.puml`
- `08_sequence_database_upgrade_en.puml`
- `09_sequence_transaction_savepoint_en.puml`
- `10_activity_select_build_en.puml`
- `11_activity_dml_build_en.puml`
- `12_activity_database_upgrade_en.puml`
- `13_component_runtime_en.puml`
- `14_state_query_builder_en.puml`
- `15_state_savepoint_en.puml`
- `16_deployment_runtime_en.puml`
- `17_timing_transaction_savepoint_en.puml`
- `18_sequence_debug_aop_en.puml`

### `ksp-fixtures`

- `01_usecase_fixtures_en.puml`
- `02_class_fixtures_en.puml`
- `03_sequence_fixture_validation_en.puml`
- `04_activity_fixture_compile_en.puml`
- `05_component_fixtures_en.puml`

### `shared-library`

- `01_usecase_shared_en.puml`
- `02_class_shared_en.puml`
- `03_sequence_utils_en.puml`
- `04_activity_utils_en.puml`
- `05_component_shared_en.puml`

### `test-support`

- `01_usecase_test_support_en.puml`
- `02_class_test_support_en.puml`
- `03_sequence_parameter_conversion_en.puml`
- `04_activity_csv_parse_en.puml`
- `05_component_test_support_en.puml`
- `06_deployment_test_support_en.puml`

### `testing`

- `01_class_test_infrastructure_en.puml`
- `02_sequence_unit_test_en.puml`
- `03_sequence_android_test_en.puml`
- `04_activity_test_pipeline_en.puml`
- `05_component_test_architecture_en.puml`
- `06_deployment_android_test_en.puml`
- `07_timing_test_execution_en.puml`

## Rendering

Using the PlantUML CLI:

```powershell
plantuml -charset UTF-8 -tsvg .\**\*_en.puml
```

In Visual Studio Code, each `.puml` file can be previewed using a
PlantUML extension.

## Validation

The following static checks were performed for all PlantUML files:

- Presence and count of `@startuml` and `@enduml`
- Matching braces, parentheses, and brackets
- Matching activity-diagram control structures
- Matching sequence-diagram block terminators
- Duplicate file names
- Empty files
- Absence of Japanese characters in the English files

The PlantUML engine is not installed in this execution environment, so final
validation by rendering SVG or PNG images was not performed.

## Source Coverage

`SOURCE_COVERAGE_en.md` lists the Kotlin source files in the latest project
and the English diagrams that cover them.
