package jp.pgw.lab78.androrm.common.logging.aop

/**
 * ## インフォメーションログ出力
 * ### AOP による INFO ログ出力対象であることを示す。
 * ### debug ビルド時のみ AspectJ weaving の対象にする。
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class InfoLog