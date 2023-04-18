plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.cosmicide.build"
    compileSdkPreview = "UpsideDownCake"

    defaultConfig {
        minSdk = 26
    }

    buildTypes {
        release {
            isMinifyEnabled = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()

    }
}


dependencies {
    implementation(projects.common)
    implementation(projects.project)
    implementation(projects.util)
    implementation(projects.kotlinc)
    implementation("com.android.tools:r8:8.0.40")
}
