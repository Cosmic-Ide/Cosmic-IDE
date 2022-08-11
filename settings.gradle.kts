@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "Cosmic-Ide"

include(
    ":app", 
    ":android-compiler",
    ":common",
    ":eclipse-jdt",
    ":google-java-format",
    ":sora-editor",
    ":project-creator",
    ":kotlinc",
    ":jaxp:xml",
    ":jaxp:internal"
) 
