package jp.pgw.lab78.androrm.database.condition.annotation

/**
 * WHERE／JOIN条件DSLの暗黙レシーバーを分離し、異なる階層への意図しないアクセスを防止する。
 *
 * @author Masahiro Inoue
 * @since 2025-10-21
 */
@DslMarker
annotation class ConditionDslMarker

/**
 * HAVING条件DSLの暗黙レシーバーを分離し、異なる階層への意図しないアクセスを防止する。
 *
 * @author Masahiro Inoue
 * @since 2025-10-21
 */
@DslMarker
annotation class HavingDslMarker

/**
 * ORDER BY DSLの暗黙レシーバーを分離し、異なる階層への意図しないアクセスを防止する。
 *
 * @author Masahiro Inoue
 * @since 2025-10-21
 */
@DslMarker
annotation class OrderDslMarker
