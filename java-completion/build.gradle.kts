plugins {
    id("com.android.library")
}
android {
    namespace = "com.tyron.javacompletion"
    compileSdkPreview = "UpsideDownCake"

    defaultConfig {
        minSdk = 26
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.6.0")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("io.github.itsaky:nb-javac-android:17.0.0.3")
    api("com.google.guava:guava:31.1-android")

    implementation("com.google.auto.value:auto-value-annotations:1.10.1")
    annotationProcessor("com.google.auto.value:auto-value:1.10.1")
    implementation(projects.kotlinc)
    implementation(projects.util)
}
