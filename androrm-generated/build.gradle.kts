plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}
dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":androrm-common"))
}
