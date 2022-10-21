plugins {
    `kotlin-dsl`
}

repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.android.tools.build:gradle:8.0.0-alpha05")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.20-mercury-606")
    implementation("org.jlleitschuh.gradle.ktlint:org.jlleitschuh.gradle.ktlint.gradle.plugin:11.0.0")
}