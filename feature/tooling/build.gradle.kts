plugins {
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("org.gradle:gradle-tooling-api:9.6.1")

    runtimeOnly("org.slf4j:slf4j-nop:2.0.18")
    testImplementation("junit:junit:4.13.2")
}

application {
    mainClass.set("org.cosmicide.gradle.Main")
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("tooling")
    archiveClassifier.set("all")
    archiveVersion.set("")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }

    from({
        configurations.runtimeClasspath.get().map { file ->
            if (file.isDirectory) file else zipTree(file)
        }
    })

    exclude(
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.RSA"
    )
}

tasks.register<Copy>("copyToolingJarToAssets") {
    group = "build"
    description = "Copies the Gradle tooling provider jar into app assets."

    dependsOn("jar")

    from(layout.buildDirectory.file("libs/tooling-all.jar"))

    into(rootProject.layout.projectDirectory.dir("app/src/main/assets"))

    rename {
        "gradle-tooling.jar"
    }
}

tasks.named("build") {
    dependsOn("copyToolingJarToAssets")
}
