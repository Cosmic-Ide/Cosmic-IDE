plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "org.cosmicide.exec"
    compileSdk {
        version = release(libs.versions.compileSdk.get().toInt()) {
            minorApiLevel = libs.versions.compileSdkMinor.get().toInt()
        }
    }

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    externalNativeBuild {
        cmake {
            version = libs.versions.cmake.get()

            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    ndkVersion = libs.versions.ndk.get()

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }


    flavorDimensions += "environment"

    productFlavors {
        create("dev") {
            dimension = "environment"
            isDefault = true
        }
        create("prod") {
            dimension = "environment"
        }
    }
}

dependencies {
    implementation(projects.util)
    implementation(projects.common)
    testImplementation(libs.junit)
}
