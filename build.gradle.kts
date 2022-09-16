allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
}

tasks.register("clean", Delete::class.java, Action<Delete> {
    delete(rootProject.buildDir)
})