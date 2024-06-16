plugins {
    id("com.android.library")
}

android {
    compileSdk = 34
    namespace = "com.termux.shared"

    defaultConfig {
        minSdk = 26

        externalNativeBuild {
            ndkBuild {
                cppFlags += ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    ndkVersion = "27.0.11902837"

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/cpp/Android.mk")
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.annotation:annotation:1.8.0")
    implementation("androidx.core:core:1.13.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.guava:guava:33.0.0-jre")

    val markwonVersion = "4.6.2"

    implementation("io.noties.markwon:core:$markwonVersion")
    implementation("io.noties.markwon:ext-strikethrough:$markwonVersion")
    implementation("io.noties.markwon:linkify:$markwonVersion")
    implementation("io.noties.markwon:recycler:$markwonVersion")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")

    // Do not increment version higher than 1.0.0-alpha09 since it will break ViewUtils and needs to be looked into
    // noinspection GradleDependency
    implementation("androidx.window:window:1.0.0-alpha09")

    // Do not increment version higher than 2.5 or there
    // will be runtime exceptions on android < 8
    // due to missing classes like java.nio.file.Path.
    implementation("commons-io:commons-io:2.15.1")

    implementation("com.github.termux.termux-app:terminal-view:062c9771a9")

    implementation("com.termux:termux-am-library:v2.0.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
