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
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("com.android.tools:r8:8.0.34")
    implementation("io.github.itsaky:nb-javac-android:17.0.0.3")
    implementation("com.github.Cosmic-Ide:fernflower:b3493460fd")
    implementation("io.github.Rosemoe.sora-editor:editor:0.21.1")
    implementation(projects.googleJavaFormat)
    implementation(projects.common)
    implementation(projects.project)
    implementation(projects.kotlinc)
    implementation(files("libs/ktfmt-0.41.jar"))
}
