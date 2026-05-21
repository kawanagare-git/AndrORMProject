// ＜androrm-common/build.gradle.kts＞
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    id("java-library")
    kotlin("jvm")
}

// Kotlin JVM 設定（型指定方式で衝突を回避）
extensions.configure<KotlinJvmProjectExtension>("kotlin") {
    jvmToolchain(17)
}

dependencies {
    // === ライブラリ本体 ===
    // ※ kotlin("stdlib") は曖昧になり得るので明示座標にします
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}")
    implementation(libs.kotlin.reflect)
    implementation(project(":shared-library"))

    // === 単体テスト（JVM上で動作） ===
    // junit-jupiter は集約アーティファクトを1本入れればOK
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation("org.junit.platform:junit-platform-commons:1.10.2")
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(libs.mockito.core)
}

// JUnit5 を使う宣言（これが無いと Engine 解決で迷子になりやすい）
tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    // 日本語 DisplayName / CSV 文字化け対策
    systemProperty("file.encoding", "UTF-8")
    jvmArgs("-Dfile.encoding=UTF-8")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    enabled = false
}

kotlin {
    sourceSets {
        val test by getting {
            kotlin.srcDir(rootProject.file("test-support/src/test/kotlin"))
        }
    }
}