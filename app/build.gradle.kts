plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android) version "1.9.0" // Kotlin 1.9.0 を使用
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
    packaging {
        resources {
            excludes.add("META-INF/LICENSE.md")
            excludes.add("META-INF/LICENSE-notice.md")
        }
    }
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("1.9.0")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform() // JUnit 5 を有効化
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17)) // 必ず Java 17 を指定
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0") // 1.9.0 に戻す
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-reflect
    implementation(libs.kotlin.reflect)
    
    // JUnit 5 の基本依存関係
    testImplementation(libs.junit.jupiter)
    androidTestImplementation(project(":app"))
    androidTestImplementation(libs.jupiter.junit.jupiter)
    testRuntimeOnly(libs.platform.junit.platform.launcher)
    // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
    testImplementation(libs.junit.jupiter.api)
    // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-engine
    testImplementation(libs.junit.jupiter.engine)

    // モックテスト用
    // https://mvnrepository.com/artifact/org.mockito/mockito-core
    androidTestImplementation(libs.mockito.core)
    testImplementation(libs.mockito.core)

    // 必要に応じて、追加のモジュールを指定
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)
    // Android 向けに Robolectric を使用する場合（必要に応じて）
    testImplementation(libs.robolectric)
}
