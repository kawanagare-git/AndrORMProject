// ＜androrm-generator-ksp/build.gradle.kts＞
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    id("org.jetbrains.kotlin.jvm")
//    id("com.google.devtools.ksp")
    alias(libs.plugins.ksp)
}

// Kotlin JVM 設定
extensions.configure<KotlinJvmProjectExtension>("kotlin") {
    jvmToolchain(17)
}

dependencies {
    // === 自作モジュール ===
    implementation(project(":androrm-common"))
    implementation(project(":shared-library"))

    // === KSP / KotlinPoet ===
    implementation(libs.symbol.processing.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
}

subprojects {
    pluginManager.apply("io.gitlab.arturbosch.detekt")

    dependencies {
        add("detektPlugins", "io.gitlab.arturbosch.detekt:detekt-formatting:1.23.6")
        add("detektPlugins", project(":androrm-detekt-rules"))
    }
}

// === KSP META-INF サービス登録 ===
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

// === JAR 出力設定 ===
tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "jp.pgw.lab78.androrm.ksp.PropsProcessorProvider"
    }
    from(layout.buildDirectory.dir("ksp-meta")) {
        into("META-INF/services")
    }
}

// === KSP メタ生成を Kotlin コンパイル前に実行 ===
tasks.named("compileKotlin").configure {
    dependsOn("generateKspMeta")
}

// === KSP 出力ソースを明示的に追加 ===
extensions.configure<KotlinJvmProjectExtension>("kotlin") {
    sourceSets["main"].kotlin.srcDir("build/generated/ksp/main/kotlin")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    enabled = false
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
}

tasks.test {
    useJUnitPlatform()
}
