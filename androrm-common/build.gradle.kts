// androrm-generator-ksp/build.gradle.kts
plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version "1.9.0-1.0.13"
}

dependencies {
    implementation(libs.symbol.processing.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
}

kotlin {
    jvmToolchain(17)
}

// androrm-generator-ksp/build.gradle.kts
tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "jp.pgw.lab78.androrm.ksp.PropsProcessorProvider"
    }

    from(layout.buildDirectory.dir("ksp-meta")) {
        into("META-INF/services")
    }
}

tasks.register("generateKspMeta") {
    val outputDir = layout.buildDirectory.dir("ksp-meta")
    outputs.dir(outputDir)

    doLast {
        val servicesDir = outputDir.get().dir("META-INF/services").asFile
        servicesDir.mkdirs()

        val file = servicesDir.resolve("com.google.devtools.ksp.processing.SymbolProcessorProvider")
        file.writeText("jp.pgw.lab78.androrm.ksp.PropsProcessorProvider")
    }
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
