plugins {
    id(BuildPlugins.ANDROID_LIBRARY)
    id(BuildPlugins.KOTLIN_ANDROID) 
}

android {
    namespace = "io.github.rosemoe.sora"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
}

dependencies {
    implementation(Dependencies.MATERIAL)
    implementation(Dependencies.GUAVA)
    implementation(Dependencies.SNAKEYAML)
    implementation(Dependencies.JDT_ANNOTATION)
    implementation(Dependencies.GSON)
    implementation(Dependencies.JCODINGS)
    implementation(Dependencies.JONI)
}