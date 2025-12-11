// ＜project-root/build.gradle.kts＞
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

plugins {
    id("com.android.application") version "8.8.0" apply false
    id("com.android.library") version "8.8.0" apply false

    // Kotlin 系は 1.9.24 で統一
    kotlin("android") version "1.9.24" apply false
    kotlin("kapt") version "1.9.24" apply false
    kotlin("jvm") version "1.9.24" apply false

    id("io.gitlab.arturbosch.detekt") version "1.23.6"
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

subprojects {

    // すべてのサブプロジェクトに detekt プラグインを適用
    pluginManager.apply("io.gitlab.arturbosch.detekt")

    // 各モジュールに detekt 用依存関係を追加
    dependencies {
        // 公式のフォーマッタルール
        add("detektPlugins", "io.gitlab.arturbosch.detekt:detekt-formatting:1.23.6")

        // あなたのカスタムルール
        add("detektPlugins", project(":androrm-detekt-rules"))
    }
    // detekt 拡張の共通設定
    extensions.configure<DetektExtension>("detekt") {
        // 共通の設定ファイル
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))

        // detekt.yml を、デフォルト設定に「上書き」するモード
        buildUponDefaultConfig = true
    }

    // detekt タスクに対して、クラスパスに JAR を差し込む
    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        dependsOn(":androrm-detekt-rules:assemble")
    }
}

tasks.register("detektAll") {
    description = "Run detekt in all subprojects"
    group = "verification"

    // 各サブプロジェクトの detekt タスクに依存させる
    dependsOn(
        subprojects.map { proj ->
            "${proj.path}:detekt"
        }
    )
}
