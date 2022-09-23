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

dependencies {
    implementation("androidx.annotation:annotation:1.5.0")
    implementation("io.github.itsaky:nb-javac-android:17.0.0.4-SNAPSHOT")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    implementation(projects.jaxp)

    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.7.20-RC")

    api("org.jetbrains.intellij.deps:trove4j:1.0.20200330")

    api(files("libs/kotlin-compiler-1.7.20-RC.jar"))

    compileOnly(files("libs/the-unsafe.jar"))
}
