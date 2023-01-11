plugins {
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
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    api("com.google.code.gson:gson:2.10.1")
    compileOnly(files("libs/android-stubs.jar"))
}
