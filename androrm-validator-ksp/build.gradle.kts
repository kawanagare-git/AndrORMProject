// ＜androrm-validator-ksp/build.gradle.kts＞
plugins {
    kotlin("jvm")
    alias(libs.plugins.ksp)   // ★ ここが重要：id("com.google.devtools.ksp") ではなく alias を使う
}

dependencies {
    // 自作モジュール
    implementation(project(":androrm-common"))
    implementation(project(":shared-library"))

    // KSP / KotlinPoet
    implementation(libs.symbol.processing.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
}

// （前に書いていた generateKspMeta とか KotlinCompile の設定はそのままで OK）
