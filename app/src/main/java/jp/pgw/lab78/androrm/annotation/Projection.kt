package jp.pgw.lab78.androrm.annotation

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Projection(
    val from: KClass<*>,
    val fields: Array<String>
)
