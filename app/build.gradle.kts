// app/build.gradle.kts
plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("com.google.devtools.ksp") version "1.9.0-1.0.13"
}

android {
    namespace = "jp.pgw.lab78"
    compileSdk = 34

    defaultConfig {
        applicationId = "jp.pgw.lab78"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // ここを true に
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Kotlin 標準ライブラリ
    implementation(kotlin("stdlib"))

    // リフレクション（必要に応じて）
    implementation(libs.kotlin.reflect)

    // Material Components
    implementation(libs.material)

    // ==== 単体テスト（JVM 上で動作） ====
    // JUnit 5
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.api)      // JUnit 5 API
    testImplementation(libs.junit.jupiter.params)   // パラメータ化テスト用
    testRuntimeOnly(libs.junit.jupiter.engine)      // JUnit 5 Engine
    // Mockito for unit tests
    testImplementation(libs.mockito.core)
    // JUnit 4
    testImplementation(libs.junit)
    // JUnit Vintage (JUnit4互換モード)
    testRuntimeOnly(libs.junit.vintage.engine)

    // ==== インストルメンテーションテスト（Android） ====
    androidTestImplementation(libs.junit)           // JUnit 4 本体 :contentReference[oaicite:2]{index=2}
    androidTestImplementation(libs.runner)          // テストランナー :contentReference[oaicite:3]{index=3}
    androidTestImplementation(libs.ext.junit)       // AndroidX の JUnit 4 拡張 :contentReference[oaicite:4]{index=4}
    androidTestImplementation(libs.mockito.android) // Mockito for Android tests
    // Android UI テスト（必要なら）
    androidTestImplementation(libs.espresso.core)
}
// JSR-310（ThreeTen） API 対応
dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

dependencies {
    implementation(project(":androrm-common"))
    implementation(project(":androrm-generator-ksp"))
    add("ksp", project(":androrm-generator-ksp"))
}

// build.gradle.kts の末尾付近に追加
tasks.withType<Test>().configureEach {
    useJUnitPlatform() // JUnit5 + Vintage を有効にするために必須
}
