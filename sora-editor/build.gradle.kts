plugins {
    id(BuildPlugins.ANDROID_LIBRARY)
    id(BuildPlugins.KOTLIN_ANDROID)
    id(BuildPlugins.KTLINT)
}

android {
    namespace = "io.github.rosemoe.sora"
    compileSdk = BuildAndroidConfig.COMPILE_SDK_VERSION
    buildToolsVersion = "33.0.0"

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
    implementation("com.google.android.material:material:1.8.0-alpha02")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.yaml:snakeyaml:1.33")
    implementation("org.eclipse.jdt:org.eclipse.jdt.annotation:2.2.700")
    implementation("com.google.code.gson:gson:2.10")
    implementation("org.jruby.jcodings:jcodings:1.0.57")
    implementation("org.jruby.joni:joni:2.1.43")
}