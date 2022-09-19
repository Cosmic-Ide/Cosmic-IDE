plugins {
    id(BuildPlugins.ANDROID_LIBRARY)
    id(BuildPlugins.KOTLIN_ANDROID)
    id(BuildPlugins.KTLINT)
}
android {
    namespace = "org.jetbrains.kotlin.android"
    compileSdk = BuildAndroidConfig.COMPILE_SDK_VERSION

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

configurations.implementation {
    exclude("org.jetbrains", "annotations")
}

dependencies {
    implementation(Dependencies.ANDROIDX_ANNOTATION)
    implementation(Dependencies.JAVAC)
    implementation(Dependencies.LSPOSED)
    implementation(projects.jaxp)

    runtimeOnly(Dependencies.KOTLIN_REFLECT)

    api(Dependencies.TROVE4J)

    api(files("libs/kotlin-compiler-1.7.20-RC.jar"))

    compileOnly(files("libs/the-unsafe.jar"))
    compileOnly(files("libs/android-stubs.jar"))
}
