plugins {
    id(BuildPlugins.ANDROID_LIBRARY)
}

android {
    namespace = "com.github.pedrovgs.lynx"
    compileSdk = BuildAndroidConfig.COMPILE_SDK_VERSION

    defaultConfig {
        minSdk = BuildAndroidConfig.MIN_SDK_VERSION
        targetSdk = BuildAndroidConfig.TARGET_SDK_VERSION
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
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.2.0")

    implementation("com.google.android.material:material:1.8.0-alpha03")
    implementation("androidx.appcompat:appcompat:1.7.0-alpha01")
    // TODO: migrate to recyclerview for better performance.
    // implementation("androidx.recyclerview:recyclerview:1.3.0-rc01")
    implementation("androidx.annotation:annotation:1.5.0")
    implementation("com.github.pedrovgs:renderers:4.1.0")
}
