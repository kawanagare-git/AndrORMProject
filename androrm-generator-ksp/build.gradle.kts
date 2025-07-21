// androrm-generator-ksp/build.gradle.kts
plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version "1.9.0-1.0.13"
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":androrm-common"))
    implementation(libs.ksp.symbol.processing.api)
}

dependencies {
    implementation(libs.symbol.processing.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
}

dependencies {
    implementation(project(":androrm-common"))
    implementation(project(":androrm-generated"))
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

// 依存関係として `generateKspMeta` をビルド前に走らせる
tasks.named("compileKotlin").configure {
    dependsOn("generateKspMeta")
}

kotlin {
    sourceSets["main"].kotlin.srcDir("build/generated/ksp/main/kotlin")
}
