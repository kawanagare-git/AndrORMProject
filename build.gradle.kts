// ＜project-root/build.gradle.kts＞
plugins {
    id("com.android.application") version "8.8.0" apply false
    id("com.android.library") version "8.8.0" apply false

    // Kotlin 系は 1.9.24 で統一
    kotlin("android") version "1.9.24" apply false
    kotlin("kapt") version "1.9.24" apply false
    kotlin("jvm") version "1.9.24" apply false

    // KSP（バージョンは libs.versions.toml の plugins.ksp から取る）
    alias(libs.plugins.ksp) apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    dependencies {
        classpath(libs.aspectjtools)
    }
}

// safeCleanBuild はそのままで OK（Detekt 関係は一旦全部削除）
