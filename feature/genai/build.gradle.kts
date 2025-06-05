plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    implementation("com.google.auto.value:auto-value-annotations:1.11.0")
    annotationProcessor("com.google.auto.value:auto-value:1.11.0")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.5")
    implementation("org.apache.httpcomponents.core5:httpcore5:5.3.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
    implementation("org.java-websocket:Java-WebSocket:1.6.0")
    implementation("com.google.errorprone:error_prone_annotations:2.38.0")
    implementation("com.google.guava:guava:33.4.8-android")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.19.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.0")
}
