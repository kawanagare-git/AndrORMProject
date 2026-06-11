package jp.pgw.lab78.androrm.ksp.factory

import com.squareup.kotlinpoet.PropertySpec

/**
 * ## 生成プロパティ情報
 * ### PropertySpec と SELECT 非表示フラグをセットで保持する
 * @property propertySpec 生成対象プロパティ
 * @property hideFromSelect SELECT 非表示の場合 true
 * @author Masahiro Inoue
 * @since 2026-06-11
 */
data class GeneratedProperty(
    val propertySpec: PropertySpec,
    val hideFromSelect: Boolean,
)
