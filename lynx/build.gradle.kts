plugins {
    id(BuildPlugins.ANDROID_LIBRARY)
    id(BuildPlugins.KOTLIN_ANDROID)
}

android {
    namespace = "com.github.pedrovgs.lynx"
    compileSdk = BuildAndroidConfig.COMPILE_SDK_VERSION
    buildToolsVersion = "33.0.0"

    defaultConfig {
        minSdk = BuildAndroidConfig.MIN_SDK_VERSION
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.0")

    implementation("com.google.android.material:material:1.8.0-beta01")
    implementation("androidx.appcompat:appcompat:1.7.0-alpha01")
    // TODO: migrate to recyclerview for better performance.
    // implementation("androidx.recyclerview:recyclerview:1.3.0-rc01")
    implementation("androidx.annotation:annotation:1.5.0")
    implementation("com.github.pedrovgs:renderers:4.1.0")
}
