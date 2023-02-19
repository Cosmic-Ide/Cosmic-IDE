plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:7.4.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
    implementation("org.jlleitschuh.gradle.ktlint:org.jlleitschuh.gradle.ktlint.gradle.plugin:11.2.0")
    implementation("com.google.android.gms:oss-licenses-plugin:0.10.6")
    implementation("com.google.gms:google-services:4.3.15")
    implementation("com.google.firebase:firebase-crashlytics-gradle:2.9.4")
    implementation("com.google.firebase:perf-plugin:1.4.2")
}
