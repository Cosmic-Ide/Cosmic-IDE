plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "org.jetbrains.kotlin"
    compileSdkPreview = "UpsideDownCake"

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

configurations.all {
    exclude("org.jline", "jline")
}
dependencies {
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")

    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler:1.8.20")
    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:1.8.20")
    implementation("io.github.itsaky:nb-javac-android:17.0.0.3")
    implementation("org.jetbrains.intellij.deps:trove4j:1.0.20200330")

    implementation(files("libs/jaxp.jar"))
    api(files("libs/kotlin-compiler-1.8.20.jar"))

    compileOnly(files("libs/the-unsafe.jar"))
}
