plugins {
    id(BuildPlugins.JAVA_LIBRARY)
    id(BuildPlugins.KOTLIN_JAVA)
    id(BuildPlugins.LINT)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

compileKotlin {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11
    }
}

dependencies {
    api(Dependencies.COROUTINES)
    compileOnly(files("libs/android-stubs.jar"))
}