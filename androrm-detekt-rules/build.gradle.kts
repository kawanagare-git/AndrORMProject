// <androrm-detekt-rules/build.gradle.kts>
plugins {
    // ★ Android じゃなくて純粋な JVM ライブラリにする
    kotlin("jvm")
    jacoco
    // Maven Central公開用
    id("com.vanniktech.maven.publish")
}
group = providers.gradleProperty("andrormGroup").get()
version = providers.gradleProperty("andrormVersion").get()
java {
    // 他モジュールに合わせて 17 にしておく（11でも動くけど統一した方が楽）
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // Kotlin 標準ライブラリ（念のため明示）
    implementation(kotlin("stdlib"))
    // ★ detekt 本体と同じバージョンに合わせること
    //   すでに detekt-api が入っているなら、この行はダブらないように調整
    compileOnly(libs.detekt.api)
    testImplementation(libs.detekt.api)
    // （必要ならテスト用依存を追加）
    testImplementation(kotlin("test"))
    // ★ これがないと compileAndLint が存在しない
    testImplementation(libs.detekt.test)
    // ログ出力用に androrm-common を追加
    implementation(project(":androrm-common"))
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    enabled = false
}
mavenPublishing {
    // Central Portalへアップロードする
    publishToMavenCentral()
    // Maven Centralが要求するPGP署名を有効化する
    signAllPublications()
    pom {
        name = "AndrORM Detekt Rules"
        description =
            "Custom Detekt rules for validating AndrORM entity definitions."
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
