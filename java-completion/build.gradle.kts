plugins {
    id(BuildPlugins.ANDROID_LIBRARY)
}
android {
    namespace = "com.tyron.javacompletion"
    compileSdk = BuildAndroidConfig.COMPILE_SDK_VERSION

    defaultConfig {
        minSdk = BuildAndroidConfig.MIN_SDK_VERSION
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.5.0")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.guava:guava:31.0.1-android")

    implementation("io.github.itsaky:nb-javac-android:17.0.0.3")

    implementation("com.google.auto.value:auto-value-annotations:1.8.2")
    annotationProcessor("com.google.auto.value:auto-value:1.8.2")
    implementation(projects.common)
}