plugins {
    id(BuildPlugins.ANDROID_LIBRARY)
    id(BuildPlugins.KOTLIN_ANDROID)
    id(BuildPlugins.KTLINT)
}

android {
    namespace = "org.cosmic.ide.git"
    compileSdk = BuildAndroidConfig.COMPILE_SDK_VERSION
    buildToolsVersion = "33.0.0"

    defaultConfig {
        minSdk = BuildAndroidConfig.MIN_SDK_VERSION
        targetSdk = BuildAndroidConfig.TARGET_SDK_VERSION
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
}

dependencies {
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.3.0.202209071007-r")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
}
