plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "org.jetbrains.kotlin"
    compileSdk = 31

    defaultConfig {
        minSdk = 26
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")

    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.8.20")
    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler:1.8.10")
    api("io.github.itsaky:nb-javac-android:17.0.0.3")

    implementation("it.unimi.dsi:fastutil:8.5.12")
    implementation("one.util:streamex:0.8.1")

    implementation("com.jetbrains.intellij.platform:util-jdom:223.8617.58")
    implementation("org.jetbrains.intellij.deps:trove4j:1.0.20200330")

    api(files("libs/kotlin-compiler-1.8.0.jar"))
    implementation(files("libs/jaxp.jar"))

    compileOnly(files("libs/the-unsafe.jar"))
}
