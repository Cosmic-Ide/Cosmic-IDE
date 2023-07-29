plugins {
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("com.google.guava:guava:32.0.1-jre")
    implementation("io.github.itsaky:nb-javac-android:17.0.0.3")
    api(files("libs/google-java-format-HEAD-20221027.232252-92.jar"))
}
