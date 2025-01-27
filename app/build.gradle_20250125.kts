
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android) version "1.9.0" // Kotlin のバージョンを変更
}

android {
    namespace = "jp.pgw.lab78.androrm"
    compileSdk = 34

    defaultConfig {
        applicationId = "jp.pgw.lab78.androrm"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildToolsVersion = "34.0.0"

    testOptions {
        unitTests.all {
            // it.isIncludeAndroidResources = true // Robolectricなどを使用する場合に必要
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0") // 1.9.0 に戻す
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-reflect
    implementation(libs.kotlin.reflect)
}

// JUnit 5 の基本依存関係
dependencies {
    // JUnit 5 の基本依存関係
    testImplementation(libs.junit.jupiter)

    // 必要に応じて、追加のモジュールを指定
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)

    // Android 向けに Robolectric を使用する場合（必要に応じて）
    testImplementation(libs.robolectric)
}
