plugins {
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
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
    implementation("com.google.guava:guava:33.6.0-jre")
    implementation("com.github.PranavPurwar:javac-android:27.23")
    implementation("org.commonmark:commonmark:0.28.0")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.28.0")

    implementation("com.google.auto.service:auto-service-annotations:1.1.1")
    implementation("com.google.auto.value:auto-value-annotations:1.11.1")
    annotationProcessor("com.google.auto.value:auto-value:1.11.1")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")

}
