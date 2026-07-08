plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "org.cosmicide.exec"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    externalNativeBuild {
        cmake {
            version = "4.1.2"

            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    ndkVersion = "30.0.14904198"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}

dependencies {
    implementation(projects.util)
}
