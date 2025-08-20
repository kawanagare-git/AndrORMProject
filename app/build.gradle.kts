// ＜app/build.gradle.kts＞
import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main

// app/build.gradle.kts
plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    id("com.google.devtools.ksp") version "1.9.0-1.0.13"
}

dependencies {
    implementation(project(":androrm-common"))
    implementation(project(":androrm-generated"))

    implementation(libs.core.ktx.v1131)
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui:1.6.7")
    implementation("androidx.compose.material3:material3:1.2.1")

    ksp(project(":androrm-generator-ksp"))

    testImplementation(libs.junit.jupiter.v5102)
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
    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}
// 依存関係用のカスタムコンフィグレーションを作成
val aspectjCompileClasspath by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

// 依存関係に AspectJ ライブラリを追加
dependencies {
    aspectjCompileClasspath(libs.aspectjrt)      // AspectJ runtime
    aspectjCompileClasspath(libs.aspectjtools)   // AspectJ tools (コンパイル時のみ)
}

// AspectJ コンパイル用カスタムタスクを登録
android.applicationVariants.forEach { variant ->
    val variantName = variant.name.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }
    val ajcTaskName = "compile${variantName}AspectJ"
    val javaCompile = tasks.named<JavaCompile>("compile${variantName}JavaWithJavac")

    tasks.register(ajcTaskName) {
        dependsOn(javaCompile)
        doLast {
            val inputDir = javaCompile.get().outputs.files.singleFile
            val aspectPath = aspectjCompileClasspath.asPath   // ここで専用クラスパスを使用
            val classpath = javaCompile.get().classpath.asPath
            val bootClasspath = android.bootClasspath.joinToString(separator = ":")

            val args = arrayOf(
                "-showWeaveInfo",
                "-source", "17",
                "-target", "17",
                "-inpath", inputDir.absolutePath,
                "-aspectpath", aspectPath,
                "-d", inputDir.absolutePath,
                "-classpath", classpath,
                "-bootclasspath", bootClasspath
            )

            val handler = MessageHandler(true)
            Main().run(args, handler)

            for (msg in handler.getMessages(null, true)) {
                when (msg.kind) {
                    IMessage.INFO -> println("AJC INFO: ${msg.message}")
                    IMessage.WARNING -> println("AJC WARNING: ${msg.message}")
                    IMessage.ERROR -> println("AJC ERROR: ${msg.message}")
                    IMessage.FAIL -> println("AJC FAIL: ${msg.message}")
                }
            }
        }
    }

    javaCompile.configure {
        finalizedBy(tasks.named(ajcTaskName))
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
// ログ出力用依存関係
dependencies {
    implementation(libs.slf4j.api.v2013)
    implementation(libs.logback.android)
}
// AOP(AspectJ)用依存関係
// 依存関係に AspectJ ライブラリを追加
dependencies {
    implementation(libs.aspectjrt)
    testImplementation(libs.aspectjweaver)
}
// testImplementation を拡張して解決可能な構成を作成
val aspectjWeaverConfig by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    extendsFrom(configurations.testImplementation.get())
}

dependencies {
    implementation(libs.monitor)
    implementation(project(":androrm-common"))
    implementation(project(":androrm-generated"))
    implementation(project(":androrm-generator-ksp"))
    add("ksp", project(":androrm-generator-ksp"))
}
// build.gradle.kts の末尾付近に追加
tasks.withType<Test>().configureEach {
    useJUnitPlatform() // JUnit5 + Vintage を有効にするために必須

    doFirst {
        val weaverJar = aspectjWeaverConfig.files
            .find { it.name.contains("aspectjweaver") }
            ?: error("aspectjweaver not found in classpath")

        jvmArgs("-javaagent:${weaverJar.absolutePath}")
    }
}

tasks.register("listConfigs") {
    doLast {
        configurations.names.sorted().forEach {
            println("Config: $it")
        }
    }
}
