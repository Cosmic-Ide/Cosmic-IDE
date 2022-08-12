plugins {
    id(BuildPlugins.JAVA_LIBRARY)
    id(BuildPlugins.LINT)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    compileOnly(Dependencies.GUAVA)
    implementation(Dependencies.AUTO_VALUE_ANNOTATIONS)
    annotationProcessor(Dependencies.AUTO_VALUE)
    implementation(Dependencies.JAVAC)
}
