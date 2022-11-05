plugins {
    id(BuildPlugins.ANDROID_LIBRARY)
    id(BuildPlugins.KOTLIN_ANDROID)
    id(BuildPlugins.KTLINT)
}

android {
    namespace = "org.cosmic.ide.android"
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

        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.0")

    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("com.android.tools:r8:3.3.75")
    implementation("io.github.itsaky:nb-javac-android:17.0.0.4-SNAPSHOT")
    implementation("com.github.Cosmic-Ide:fernflower:b3493460fd")
    implementation(projects.googleJavaFormat)
    implementation(projects.common)
    implementation(projects.project)
    implementation(projects.ktfmt)
    implementation(projects.kotlinc)
    implementation(projects.soraEditor)
}
