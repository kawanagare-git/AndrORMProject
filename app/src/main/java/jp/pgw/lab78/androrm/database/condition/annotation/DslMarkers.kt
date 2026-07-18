package jp.pgw.lab78.androrm.database.condition.annotation

/** WHERE／JOIN 条件 DSL の暗黙レシーバーを分離し、異なる階層への意図しないアクセスを防止する。 */
@DslMarker
annotation class ConditionDslMarker

/** HAVING 条件 DSL の暗黙レシーバーを分離し、異なる階層への意図しないアクセスを防止する。 */
@DslMarker
annotation class HavingDslMarker

/** ORDER BY DSL の暗黙レシーバーを分離し、異なる階層への意図しないアクセスを防止する。 */
@DslMarker
annotation class OrderDslMarker
