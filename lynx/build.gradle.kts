plugins {
    id(BuildPlugins.ANDROID_LIBRARY)
    id(BuildPlugins.KOTLIN_ANDROID)
}

android {
    namespace = "com.github.pedrovgs.lynx"
    compileSdk = BuildAndroidConfig.COMPILE_SDK_VERSION

    defaultConfig {
        minSdk = BuildAndroidConfig.MIN_SDK_VERSION
    }

    buildTypes {
        release {
            isMinifyEnabled = true
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("com.google.android.material:material:1.9.0-alpha01")
    implementation("androidx.appcompat:appcompat:1.7.0-alpha02")
    // TODO: migrate to recyclerview for better performance.
    // implementation("androidx.recyclerview:recyclerview:1.3.0-rc01")
    implementation("androidx.annotation:annotation:1.5.0")
    implementation("com.github.pedrovgs:renderers:4.1.0")
}
