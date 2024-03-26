plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.rosemoe.sora.langs.textmate"
    compileSdk = 34

    defaultConfig {
        minSdk = 26

        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
    implementation("io.github.Rosemoe.sora-editor:editor:0.23.4-3895689-SNAPSHOT")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jruby.jcodings:jcodings:1.0.58")
    implementation("org.jruby.joni:joni:2.2.1")
    implementation("org.snakeyaml:snakeyaml-engine:2.7")
    implementation("com.google.guava:guava:33.1.0-android")
    implementation("org.eclipse.jdt:org.eclipse.jdt.annotation:2.3.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
