// ＜androrm-generator-ksp/build.gradle.kts＞
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.ksp)
    id("maven-publish")
}
group = providers.gradleProperty("andrormGroup").get()
version = providers.gradleProperty("andrormVersion").get()

// Kotlin JVM 設定
extensions.configure<KotlinJvmProjectExtension>("kotlin") {
    jvmToolchain(17)
}
java {
    withSourcesJar()
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
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "androrm-generator-ksp"

            from(components["java"])
        }
    }
}