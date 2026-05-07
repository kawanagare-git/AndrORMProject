// ＜shared-library/build.gradle.kts＞
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    id("java-library")
    kotlin("jvm")
}

// Kotlin JVM 設定（Toolchain指定）
extensions.configure<KotlinJvmProjectExtension>("kotlin") {
    jvmToolchain(17)
}

dependencies {
    // Kotlin 標準ライブラリ
    implementation(kotlin("stdlib"))
    implementation(libs.kotlin.reflect)

    // テストライブラリ
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    enabled = false
}
