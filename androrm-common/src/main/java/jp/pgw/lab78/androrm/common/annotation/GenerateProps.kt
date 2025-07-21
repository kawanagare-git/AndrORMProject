package jp.pgw.lab78.androrm.common

/**
 * ## KSP ジェネレータアノテーション
 * ### このアノテーションが付与されたクラス（data class を想定）から
 * ### プロパティ名の一覧を文字列定数として生成する
 * ### 生成された object は、「build/generated」に出力される
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class GenerateProps
