// <settings.gradle.kts>
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
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
