plugins {
    id(BuildPlugins.JAVA_LIBRARY)
    id(BuildPlugins.KOTLIN_JAVA)
    id(BuildPlugins.KTLINT)
    id(BuildPlugins.LINT)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
}

dependencies {
    implementation(projects.common)
    compileOnly(files("libs/android-stubs.jar"))
}