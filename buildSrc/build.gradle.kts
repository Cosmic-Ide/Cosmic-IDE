plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:8.0.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
    implementation("org.jlleitschuh.gradle.ktlint:org.jlleitschuh.gradle.ktlint.gradle.plugin:11.3.1")
    implementation("com.google.android.gms:oss-licenses-plugin:0.10.6")
}
