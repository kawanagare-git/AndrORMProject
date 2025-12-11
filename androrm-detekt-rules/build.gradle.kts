// <androrm-detekt-rules/build.gradle.kts>
plugins {
    // ★ Android じゃなくて純粋な JVM ライブラリにする
    kotlin("jvm")
}

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
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    enabled = false
}
