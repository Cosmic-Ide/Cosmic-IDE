plugins {
    id(BuildPlugins.JAVA_LIBRARY)
    id(BuildPlugins.KOTLIN_JAVA)
    id(BuildPlugins.LINT)
}

configurations.implementation {
    exclude("org.jetbrains", "annotations")
}

dependencies {
    implementation("com.github.marschall:zipfilesystem-standalone:1.0.1")
    implementation("androidx.annotation:annotation:1.4.0")
    implementation(Dependencies.JAVAC)
    implementation(projects.jaxp.xml)
    implementation(projects.jaxp.internal)


    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.7.10")

    api("org.jetbrains.intellij.deps:trove4j:1.0.20200330")

    api(files("libs/kotlin-compiler-embeddable-1.7.20-Beta.jar"))

    compileOnly(files("libs/the-unsafe.jar"))
    compileOnly(files("libs/android-stubs.jar"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
