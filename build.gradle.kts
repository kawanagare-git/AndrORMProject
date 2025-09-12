// ＜root/build.gradle.kts＞
plugins {
    id("com.android.application") version "8.8.0" apply false
    kotlin("android") version "1.9.0" apply false
    kotlin("kapt") version "1.9.0" apply false
    kotlin("jvm") version "1.9.0" apply false
    alias(libs.plugins.android.library) apply false
    id("com.google.devtools.ksp") version "1.9.0-1.0.13"
}
buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    dependencies {
        classpath(libs.symbol.processing.gradle.plugin)
        classpath(libs.aspectjtools)
    }
}
