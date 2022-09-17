plugins {
    id(BuildPlugins.JAVA_LIBRARY)
    id(BuildPlugins.KOTLIN_JAVA)
    id(BuildPlugins.LINT)
    id(BuildPlugins.KTLINT)
}

configurations.implementation {
    exclude("org.jetbrains", "annotations")
}

dependencies {
    implementation(Dependencies.ANDROIDX_ANNOTATION)
    implementation(Dependencies.JAVAC)
    implementation(projects.jaxp)

    runtimeOnly(Dependencies.KOTLIN_REFLECT)

    api(Dependencies.TROVE4J)

    api(files("libs/kotlin-compiler-1.7.20-RC.jar"))

    compileOnly(files("libs/the-unsafe.jar"))
    compileOnly(files("libs/android-stubs.jar"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
