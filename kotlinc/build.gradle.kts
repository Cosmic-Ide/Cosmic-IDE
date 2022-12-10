plugins {
    id(BuildPlugins.ANDROID_LIBRARY)
    id(BuildPlugins.KOTLIN_ANDROID)
    id(BuildPlugins.KTLINT)
}
android {
    namespace = "org.jetbrains.kotlin"
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
    implementation("io.github.itsaky:nb-javac-android:17.0.0.4-SNAPSHOT")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    implementation(projects.jaxp)

    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.8.0-RC")

    implementation("it.unimi.dsi:fastutil:8.5.9")
    implementation("one.util:streamex:0.8.1")

    api("com.jetbrains.intellij.platform:util-jdom:223.7571.230")
    api("org.jetbrains.intellij.deps:trove4j:1.0.20200330")

    api(files("libs/kotlin-compiler-1.8.0-RC.jar"))

    compileOnly(files("libs/the-unsafe.jar"))
}
