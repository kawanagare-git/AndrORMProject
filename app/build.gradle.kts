// ＜app/build.gradle.kts＞
import io.gitlab.arturbosch.detekt.Detekt

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    alias(libs.plugins.ksp)

    // detekt プラグイン
    id("io.gitlab.arturbosch.detekt")
}

dependencies {
    implementation(project(":shared-library"))
    implementation(project(":androrm-common"))
    ksp(project(":androrm-generator-ksp"))
    kspTest(project(":androrm-generator-ksp"))

    implementation(libs.core.ktx.v1131)
    testImplementation(libs.junit.jupiter)
}

android {
    namespace = "jp.pgw.lab78.androrm"
    compileSdk = 34

    defaultConfig {
        applicationId = "jp.pgw.lab78.androrm"
        minSdk = 24
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
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = false
    }

    packaging {
        resources {
            excludes += "kotlin/internal/internal.kotlin_builtins"
        }
    }
    sourceSets {
        getByName("test") {
            java.srcDir("build/generated/ksp/debugUnitTest/kotlin")
        }
        getByName("androidTest") {
            java.srcDir("build/generated/ksp/debugAndroidTest/kotlin")
        }
    }
}

// --------------------------------------------------------
// 依存関係設定
// --------------------------------------------------------
dependencies {
    // Kotlin 標準ライブラリ
    implementation(kotlin("stdlib"))

    // リフレクション
    implementation(libs.kotlin.reflect)

    // Material Components
    implementation(libs.material)

    // ==== 単体テスト（JVM） ====
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockito.core)
    testImplementation(libs.junit)
    testRuntimeOnly(libs.junit.vintage.engine)

    // ==== インストルメンテーションテスト（Android） ====
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.runner)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.mockito.android)
    androidTestImplementation(libs.espresso.core)
}

// JSR-310（ThreeTen） API 対応
dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

//// AndrORM の detekt ルール
// ==========================================================
// detekt 共通設定（main + test + androidTest）
// ==========================================================
detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt/detekt.yml")
}

// detekt の全タスクに共通設定を適用
tasks.withType<Detekt>().configureEach {
    // Detekt 実行前に必ず logs ディレクトリを作る
    doFirst {
        val logDir = file("$projectDir/logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
    }

    config.setFrom("$rootDir/config/detekt/detekt.yml")
    // ★ Android プロジェクト対応：全ソースディレクトリを解析対象にする
    setSource(
        files(
            "$projectDir/src/main/java",
            "$projectDir/src/test/java",
            "$projectDir/src/androidTest/java",
        )
    )

    // 解析対象ファイル
    include("**/*.kt", "**/*.kts", "**/*.java")

    // 除外（ビルドや生成物）
    exclude("**/build/**", "**/generated/**")

    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
    }
}

// ==========================================================
// UnitTest だけ detekt
// ==========================================================
tasks.register<Detekt>("detektUnitTestOnly") {
    description = "Run detekt on unit test sources only"

    setSource(
        files(
            "$projectDir/src/test/java",
        )
    )

    include("**/*.kt", "**/*.kts")
    exclude("**/build/**")

    reports {
        html.required.set(true)
        txt.required.set(true)
    }
}

// ==========================================================
// AndroidTest だけ detekt
// ==========================================================
tasks.register<Detekt>("detektAndroidTestOnly") {
    description = "Run detekt on Android test sources only"

    setSource(
        files(
            "$projectDir/src/androidTest/java",
        )
    )

    include("**/*.kt", "**/*.kts")
    exclude("**/build/**")

    reports {
        html.required.set(true)
        txt.required.set(true)
    }
}

// ==========================================================
// detekt をビルド時に必ず動かす
// ==========================================================
tasks.named("check") {
    dependsOn(
        "detektUnitTestOnly",   // UnitTest 専用
        "detektAndroidTestOnly", // AndroidTest 専用
        "detekt",               // main + test + androidTest（共通）
    )
}

tasks.named("assemble") {
    dependsOn("detekt")
}

// KSP にモジュールディレクトリを渡す
ksp {
    arg("androrm.moduleDir", project.projectDir.absolutePath)
}
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}