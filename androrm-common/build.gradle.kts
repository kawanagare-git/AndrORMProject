// androrm-common/build.gradle.kts
plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // ==== 単体テスト（JVM 上で動作） ====
    // JUnit 5
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.api)      // JUnit 5 API
    testImplementation(libs.junit.jupiter.params)   // パラメータ化テスト用
    testRuntimeOnly(libs.junit.jupiter.engine)      // JUnit 5 Engine
    // Mockito for unit tests
    testImplementation(libs.mockito.core)
}
dependencies {
    implementation(kotlin("stdlib"))
}
