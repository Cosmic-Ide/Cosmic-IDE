plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:8.0.0-alpha10")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0-RC")
    implementation("androidx.navigation:navigation-safe-args-gradle-plugin:2.6.0-alpha04")
    implementation("org.jlleitschuh.gradle.ktlint:org.jlleitschuh.gradle.ktlint.gradle.plugin:11.0.0")
    implementation("com.google.android.gms:oss-licenses-plugin:0.10.6")
}