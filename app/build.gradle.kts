// ＜app/build.gradle.kts＞
import com.android.build.gradle.AppExtension
import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    alias(libs.plugins.ksp)

    // detekt プラグイン
    id("io.gitlab.arturbosch.detekt")
}

val aspectjVersion = "1.9.25.1"
val aspectjTools by configurations.creating

dependencies {
    implementation(project(":shared-library"))
    implementation(project(":androrm-common"))
    ksp(project(":androrm-generator-ksp"))
    kspTest(project(":androrm-generator-ksp"))
    kspAndroidTest(project(":androrm-generator-ksp"))

    implementation(libs.core.ktx.v1131)
    testImplementation(libs.junit.jupiter)

    debugImplementation(libs.aspectjrt)
    aspectjTools(libs.aspectjtools)
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
        getByName("test") {
            java.srcDir(rootProject.file("test-support/src/test/kotlin"))
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

// debug だけ weaving する task
val weaveDebugAspectJ by tasks.registering {
    group = "aspectj"
    description = "Weave AspectJ aspects into debug classes without in-place weaving."

    dependsOn("compileDebugKotlin")

    // Java compile task が存在する場合だけ依存する
    dependsOn(
        tasks.matching { it.name == "compileDebugJavaWithJavac" }
    )

    doLast {
        val androidExtension = project.extensions.getByType(AppExtension::class.java)

        val kotlinClassesDir = layout.buildDirectory
            .dir("tmp/kotlin-classes/debug")
            .get()
            .asFile

        val javaCompileTask = tasks.findByName("compileDebugJavaWithJavac") as? JavaCompile

        val javaClassesDir = javaCompileTask
            ?.destinationDirectory
            ?.get()
            ?.asFile

        val targetDirs = listOfNotNull(
            kotlinClassesDir.takeIf { it.exists() },
            javaClassesDir?.takeIf { it.exists() },
        )

        if (targetDirs.isEmpty()) {
            logger.lifecycle("AspectJ weaving skipped. No debug class directories found.")
            return@doLast
        }

        val inputRoot = layout.buildDirectory
            .dir("tmp/aspectj-input/debug")
            .get()
            .asFile

        val wovenRoot = layout.buildDirectory
            .dir("tmp/aspectj-woven/debug")
            .get()
            .asFile

        // AJC の入力・出力を毎回作り直す
        project.delete(inputRoot)
        project.delete(wovenRoot)
        inputRoot.mkdirs()
        wovenRoot.mkdirs()

        // 元の class directory を staging input にコピーする
        val inputDirs = targetDirs.mapIndexed { index, targetDir ->
            val inputDir = File(inputRoot, "classes_$index")
            targetDir.copyRecursively(inputDir, overwrite = true)
            inputDir
        }

        val bootClasspath = androidExtension.bootClasspath
            .joinToString(File.pathSeparator) { it.absolutePath }

        val compileClasspath = configurations
            .getByName("debugCompileClasspath")
            .files
            .joinToString(File.pathSeparator) { it.absolutePath }

        val aspectPath = inputDirs
            .joinToString(File.pathSeparator) { it.absolutePath }

        val inputClasspath = inputDirs
            .joinToString(File.pathSeparator) { it.absolutePath }

        val fullClasspath = listOf(
            bootClasspath,
            compileClasspath,
            inputClasspath,
        )
            .filter { it.isNotBlank() }
            .joinToString(File.pathSeparator)

        inputDirs.forEachIndexed { index, inputDir ->
            val targetDir = targetDirs[index]
            val outputDir = File(wovenRoot, "classes_$index")

            // AJC が変更しない class も消えないよう、先に入力を出力へコピーする
            inputDir.copyRecursively(outputDir, overwrite = true)

            logger.lifecycle("AspectJ weaving input : ${inputDir.absolutePath}")
            logger.lifecycle("AspectJ weaving output: ${outputDir.absolutePath}")

            project.javaexec {
                classpath = aspectjTools
                mainClass.set("org.aspectj.tools.ajc.Main")

                args(
                    "-showWeaveInfo",
                    "-inpath", inputDir.absolutePath,
                    "-aspectpath", aspectPath,
                    "-d", outputDir.absolutePath,
                    "-classpath", fullClasspath,
                    "-bootclasspath", bootClasspath
                )
            }

            // AJC 完了後にだけ、元の class directory を置き換える
            project.delete(targetDir)
            targetDir.mkdirs()
            outputDir.copyRecursively(targetDir, overwrite = true)
        }
    }
}

tasks.matching {
    it.name == "bundleDebugClassesToRuntimeJar" ||
            it.name == "bundleDebugClassesToCompileJar"
}.configureEach {
    dependsOn(weaveDebugAspectJ)
}

// Kotlin compile 後に weaving する
tasks.matching { it.name == "compileDebugKotlin" }.configureEach {
    outputs.upToDateWhen { false }
    doFirst {
        project.delete(
            layout.buildDirectory.dir("tmp/kotlin-classes/debug").get().asFile
        )
    }
}

// Java compile task が存在する場合だけ weaving 後続にする
tasks.matching { it.name == "compileDebugJavaWithJavac" }.configureEach {
    outputs.upToDateWhen { false }
    doFirst {
        val javaCompileTask = this as JavaCompile
        project.delete(javaCompileTask.destinationDirectory.get().asFile)
    }
}

// dex 側が存在する場合だけ weaving を前提にする
tasks.matching { it.name == "testDebugUnitTest" }.configureEach {
    dependsOn(weaveDebugAspectJ)
}

tasks.matching { it.name == "dexBuilderDebug" }.configureEach {
    dependsOn(weaveDebugAspectJ)
}

// Unit Test 設定
tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    systemProperty("file.encoding", "UTF-8")
    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Dsun.stdout.encoding=UTF-8",
        "-Dsun.stderr.encoding=UTF-8"
    )

    testLogging {
        events(
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED
        )
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
    }
}
// ==========================================================
// UnitTest / AndroidTest 実行前に AndrORM detekt ルールを実行する
// ==========================================================
tasks.register("andrormDetektCheckBeforeTest") {
    description = "Run AndrORM detekt rules before android/unit tests."
    group = "verification"

    dependsOn(
        "detektAndroidTestOnly",
    )
}

tasks.matching {
    it.name == "testDebugUnitTest" ||
            it.name == "connectedDebugAndroidTest"
}.configureEach {
    dependsOn("andrormDetektCheckBeforeTest")
}