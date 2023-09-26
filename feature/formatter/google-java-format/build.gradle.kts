plugins {
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(
        listOf(
            "--add-exports",
            "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
            "--add-exports",
            "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-exports",
            "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
            "--add-exports",
            "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED"
        )
    )
}

dependencies {
    implementation("com.google.guava:guava:32.1.2-jre")
    implementation("io.github.itsaky:nb-javac-android:17.0.0.3")

    implementation("com.google.auto.value:auto-value-annotations:1.10.4")
    annotationProcessor("com.google.auto.value:auto-value:1.10.4")
}
