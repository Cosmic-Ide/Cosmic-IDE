enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://www.jetbrains.com/intellij-repository/releases")
        maven("https://jitpack.io")
    }
}
rootProject.name = "CosmicIDE-Rewrite"
include(":app")
include(":common")
include(":project")
include(":kotlinc")
include(":kotlin-completion")
include(":util")
include(":java-completion")
include(":build-tools")
