plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.cosmicide.rewrite"
    compileSdkPreview = "UpsideDownCake"

    defaultConfig {
        applicationId = "org.cosmicide.rewrite"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("com.android.tools:desugar_jdk_libs_nio:2.0.3")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-ktx:1.10.0")
    implementation("androidx.fragment:fragment-ktx:1.5.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")

    implementation("com.github.TutorialsAndroid:crashx:v4.0.19")
    implementation("com.google.android.material:material:1.8.0")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation(platform("io.github.Rosemoe.sora-editor:bom:0.21.1"))
    implementation("io.github.Rosemoe.sora-editor:editor")
    implementation("io.github.Rosemoe.sora-editor:language-textmate")
    implementation("io.github.dingyi222666:treeview:1.1.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("com.android.tools.smali:smali-dexlib2:3.0.2")
    implementation("com.github.xxdark:ssvm:6e795448e4")
    implementation("com.android.tools:r8:8.0.40")
    implementation("io.github.itsaky:nb-javac-android:17.0.0.3")

    implementation(projects.buildTools)
    implementation(projects.common)
    implementation(projects.javaCompletion)
    implementation(projects.kotlinc)
    implementation(projects.kotlinCompletion)
    implementation(projects.project)
    implementation(projects.util)

    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5")
    androidTestImplementation("junit:junit:4.13.2")
}