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

include(":app")
include(":androrm-common")
include(":androrm-generator-ksp")
include(":androrm-generated")
include(":shared-library")
