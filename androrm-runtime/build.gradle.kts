// ＜androrm-runtime/build.gradle.kts＞

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    // Maven Central公開用
    id("com.vanniktech.maven.publish")
}
group = providers.gradleProperty("andrormGroup").get()
version = providers.gradleProperty("andrormVersion").get()

android {
    namespace = "jp.pgw.lab78.androrm.runtime"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

        // LocalDate／LocalTime／LocalDateTime対応
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    /*
     * 既存のUnit Testが使用している共通テスト支援コード。
     * Unit Testをruntimeへ移動した際に使用する。
     */
    sourceSets {
        getByName("test") {
            java.srcDir(
                rootProject.file("test-support/src/test/kotlin")
            )
        }
    }
}

dependencies {
    /*
     * SelectEntity、InsertEntityなどはruntimeの公開APIに現れるため、
     * 利用側にも公開されるapi依存とする。
     */
    api(project(":androrm-common"))

    /*
     * shared-libraryはruntime内部だけで使用する。
     */
    implementation(project(":shared-library"))

    // Kotlinリフレクション
    implementation(libs.kotlin.reflect)

    // java.timeなどの新しいJava API対応
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Unit Test：JUnit 5
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)

    // 既存JUnit 4テストとの互換
    testImplementation(libs.junit)
    testRuntimeOnly(libs.junit.vintage.engine)

    // Mockito
    testImplementation(libs.mockito.core)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    systemProperty("file.encoding", "UTF-8")

    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Dsun.stdout.encoding=UTF-8",
        "-Dsun.stderr.encoding=UTF-8",
    )
}
mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    pom {
        name = "AndrORM Runtime"
        description =
            "Android runtime library for building and executing AndrORM queries."
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