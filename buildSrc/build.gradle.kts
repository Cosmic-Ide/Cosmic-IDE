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
    implementation("com.android.tools.build:gradle:8.0.0-alpha07")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.21-247")
    implementation("org.jlleitschuh.gradle.ktlint:org.jlleitschuh.gradle.ktlint.gradle.plugin:11.0.0")
}