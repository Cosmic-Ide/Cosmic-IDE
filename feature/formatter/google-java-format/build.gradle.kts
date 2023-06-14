plugins {
    id("java-library")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    compileOnly("com.google.guava:guava:32.0.1-android")
    implementation("io.github.itsaky:nb-javac-android:17.0.0.3")
    api(files("libs/google-java-format-HEAD-20221027.232252-92.jar"))
}
