pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

include(":app")

// Core modules (new architecture)
include(":libausbc-core")
include(":libausbc-camera")
include(":libausbc-render")
include(":libausbc-encode")
include(":libausbc-utils")

// Legacy modules (to be migrated)
include(":libausbc")
include(":libuvc")
include(":libnative")
