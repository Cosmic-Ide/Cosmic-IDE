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
    implementation("com.google.auto.value:auto-value-annotations:1.10")
    annotationProcessor("com.google.auto.value:auto-value:1.10")
    implementation("io.github.itsaky:nb-javac-android:17.0.0.4-SNAPSHOT")
}
