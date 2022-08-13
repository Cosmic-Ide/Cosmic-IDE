plugins {
    `kotlin-dsl`
}

repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
    maven("https://jitpack.io")
}

object PluginsVersions {
    const val GRADLE_ANDROID = "7.4.0-alpha09"
    const val KOTLIN = "1.7.20-Beta"
    const val KTLINT = "10.3.0"
}

dependencies {
    implementation("com.android.tools.build:gradle:${PluginsVersions.GRADLE_ANDROID}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${PluginsVersions.KOTLIN}")
    implementation("org.jlleitschuh.gradle.ktlint:org.jlleitschuh.gradle.ktlint.gradle.plugin:${PluginsVersions.KTLINT}")
}