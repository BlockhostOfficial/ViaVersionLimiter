pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    plugins {
        id("com.gradleup.shadow") version "9.6.1"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "ViaVersionLimiter"
