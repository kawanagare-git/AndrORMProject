// <settings.gradle.kts>
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

rootProject.name = "AndrORMProject"

include(
    ":app",
    ":androrm-common",
    ":androrm-generator-ksp",
    ":shared-library",
)
include(":androrm-detekt-rules")
include(":androrm-runtime")
