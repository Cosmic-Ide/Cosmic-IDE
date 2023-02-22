plugins {
    id(BuildPlugins.ANDROID_APPLICATION)
    id(BuildPlugins.KOTLIN_ANDROID)
    id(BuildPlugins.KTLINT)
    id(BuildPlugins.OSS_LICENSES)
    id(BuildPlugins.GMS)
    id(BuildPlugins.CRASHLYTICS)
    id(BuildPlugins.PERF)
}

android {
    namespace = BuildAndroidConfig.APPLICATION_ID
    compileSdk = BuildAndroidConfig.COMPILE_SDK_VERSION

    defaultConfig {
        applicationId = BuildAndroidConfig.APPLICATION_ID
        minSdk = BuildAndroidConfig.MIN_SDK_VERSION
        targetSdk = BuildAndroidConfig.TARGET_SDK_VERSION
        versionCode = BuildAndroidConfig.VERSION_CODE
        versionName = BuildAndroidConfig.VERSION_NAME
    }

    signingConfigs {
        getByName(BuildType.DEBUG) {
            storeFile = file("testkey.keystore")
            storePassword = "testkey"
            keyAlias = "testkey"
            keyPassword = "testkey"
        }
    }

    buildTypes {
        getByName(BuildType.DEBUG) {
            isMinifyEnabled = BuildTypeDebug.isMinifyEnabled
        }

        getByName(BuildType.RELEASE) {
            signingConfig = signingConfigs.getByName(BuildType.DEBUG)
            isMinifyEnabled = BuildTypeRelease.isMinifyEnabled
            isShrinkResources = BuildTypeRelease.isMinifyEnabled
            proguardFiles("proguard-rules.pro")
        }
    }

    buildFeatures.viewBinding = true

    compileOptions {
        // isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    lint {
        abortOnError = false
    }

    packagingOptions {
        resources.excludes.addAll(
            arrayOf(
                "about.xml",
                "baksmali.properties",
                "README.md",
                "SECURITY.md",
                "license/*",
                "DebugProbesKt.bin",
                "api_database/*",
                "src/**",
                "bundle.properties",
                "**/**.kotlin_module"
            )
        )
    }
}

dependencies {
    // coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.2.2")

    // Google Analytics
    implementation(platform("com.google.firebase:firebase-bom:31.2.2"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")

    // Google Analytics
    implementation(platform("com.google.firebase:firebase-bom:31.2.2"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")

    implementation("io.github.itsaky:nb-javac-android:17.0.0.3")
    implementation("io.github.Rosemoe.sora-editor:editor:0.21.0")
    implementation("io.github.Rosemoe.sora-editor:language-textmate:0.21.0")
    implementation("com.google.android.gms:play-services-oss-licenses:17.0.0")
    implementation("androidx.activity:activity-ktx:1.7.0-beta01")
    implementation("androidx.documentfile:documentfile:1.1.0-alpha01")
    implementation("com.google.android.material:material:1.9.0-alpha01")
    implementation("androidx.appcompat:appcompat:1.7.0-alpha02")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.0-rc01")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")
    implementation("org.smali:dexlib2:2.5.2")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    implementation(projects.dependencyResolver)
    implementation(projects.treeview)
    implementation(projects.lynx)
    implementation(projects.common)
    implementation(projects.kotlinc)
    implementation(projects.kotlinCompletion)
    implementation(projects.androidCompiler)
    implementation(projects.project)
    implementation(projects.gitApi)
    implementation(projects.javaCompletion)

}
