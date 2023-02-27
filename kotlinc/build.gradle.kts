plugins {
    id(BuildPlugins.ANDROID_LIBRARY)
    id(BuildPlugins.KOTLIN_ANDROID)
    id(BuildPlugins.KTLINT)
}
android {
    namespace = "org.jetbrains.kotlin"
    compileSdk = BuildAndroidConfig.COMPILE_SDK_VERSION

    defaultConfig {
        minSdk = BuildAndroidConfig.MIN_SDK_VERSION
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    implementation("io.github.itsaky:nb-javac-android:17.0.0.3")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    implementation(projects.jaxp)

    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.8.0")
    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler:1.8.10")

    implementation("it.unimi.dsi:fastutil:8.5.11")
    implementation("one.util:streamex:0.8.1")

    implementation("com.jetbrains.intellij.platform:util-jdom:223.8617.58")
    implementation("org.jetbrains.intellij.deps:trove4j:1.0.20200330")

    api(files("libs/kotlin-compiler-1.8.0.jar"))

    compileOnly(files("libs/the-unsafe.jar"))
}
