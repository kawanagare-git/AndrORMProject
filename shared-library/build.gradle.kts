// ＜shared-library/build.gradle.kts＞
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    id("java-library")
    kotlin("jvm")
    // Maven Central公開用
    id("com.vanniktech.maven.publish")
}
group = providers.gradleProperty("andrormGroup").get()
version = providers.gradleProperty("andrormVersion").get()

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
mavenPublishing {
    // Central Portalへアップロードする。
    // 自動公開は指定していないため、最後のPublishはPortalで手動実行する。
    publishToMavenCentral()

    // Maven Centralが要求するPGP署名を有効化する。
    signAllPublications()

    pom {
        name = "AndrORM Shared Library"
        description = "Shared utility library used by AndrORM modules."
        inceptionYear = "2025"
        url = "https://github.com/kawanagare-git/AndrORMProject"

        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/license/mit"
                distribution = "repo"
            }
        }

        developers {
            developer {
                id = "kawanagare-git"
                name = "Masahiro Inoue"
                email = "maspost0083@hotmail.com"
                url = "https://github.com/kawanagare-git"
            }
        }

        scm {
            url = "https://github.com/kawanagare-git/AndrORMProject"
            connection =
                "scm:git:https://github.com/kawanagare-git/AndrORMProject.git"
            developerConnection =
                "scm:git:ssh://git@github.com/kawanagare-git/AndrORMProject.git"
        }
    }
}