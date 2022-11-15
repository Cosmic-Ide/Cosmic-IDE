allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
        maven("https://www.jetbrains.com/intellij-repository/releases")
//        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
    }
}

tasks.register("clean", Delete::class.java, Action<Delete> {
    delete(rootProject.buildDir)
})

tasks.withType(JavaCompile) {
    options.deprecation = true
}
