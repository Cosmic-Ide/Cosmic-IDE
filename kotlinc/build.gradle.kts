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
    implementation("org.jetbrains:annotations:23.0.0")
    implementation("io.github.itsaky:nb-javac-android:17.0.0.4-SNAPSHOT")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    implementation(projects.jaxp)

    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.7.21")

    implementation("org.fusesource.jansi:jansi:2.4.0")
    implementation("it.unimi.dsi:fastutil:8.5.9")
    implementation("org.jline:jline:3.21.0")
    implementation("one.util:streamex:0.8.1")

    api("com.jetbrains.intellij.platform:util-jdom:222.4459.24")
    api("org.jetbrains.intellij.deps:trove4j:1.0.20200330")

    api(files("libs/kotlin-compiler-1.8.0-Beta.jar"))

    compileOnly(files("libs/the-unsafe.jar"))
}
