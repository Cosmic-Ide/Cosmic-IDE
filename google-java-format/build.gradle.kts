plugins {
    id(BuildPlugins.JAVA_LIBRARY)
    id(BuildPlugins.LINT)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    compileOnly("com.google.guava:guava:31.1-jre")
    implementation("io.github.itsaky:nb-javac-android:17.0.0.3")
    api(files("libs/google-java-format-HEAD-20221027.232252-92.jar"))
}
