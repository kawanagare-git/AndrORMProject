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
    testImplementation("org.junit.jupiter:junit-jupiter:${libs.versions.junitJupiterVersion.get()}")
    testImplementation(libs.mockito.core)
}

// JUnit5 を使う宣言（これが無いと Engine 解決で迷子になりやすい）
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
