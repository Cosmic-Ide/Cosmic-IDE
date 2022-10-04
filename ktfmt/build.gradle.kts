plugins {
    id(BuildPlugins.JAVA_LIBRARY)
    id(BuildPlugins.KOTLIN_JAVA)
    id(BuildPlugins.LINT)
    id(BuildPlugins.KTLINT)
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
    implementation("com.google.guava:guava:31.1-jre")
    implementation(projects.kotlinc)
    implementation(projects.googleJavaFormat)
}