plugins {
    id(BuildPlugins.ANDROID_APPLICATION)
    id(BuildPlugins.KOTLIN_ANDROID)
    id(BuildPlugins.KTLINT)
    id(BuildPlugins.OSS_LICENSES)
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
        val TESTKEY = "testkey"
        getByName(BuildType.DEBUG) {
            storeFile = file("testkey.keystore")
            storePassword = TESTKEY
            keyAlias = TESTKEY
            keyPassword = TESTKEY
        }
    }

    buildTypes {
        getByName(BuildType.DEBUG) {
            isMinifyEnabled = BuildTypeDebug.isMinifyEnabled
            applicationIdSuffix = BuildTypeDebug.applicationIdSuffix
            versionNameSuffix = BuildTypeDebug.versionNameSuffix
        }

        getByName(BuildType.RELEASE) {
            signingConfig = signingConfigs.getByName(BuildType.DEBUG)
            isMinifyEnabled = BuildTypeRelease.isMinifyEnabled
            isShrinkResources = BuildTypeRelease.isMinifyEnabled
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

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
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.0")

    implementation("io.github.itsaky:nb-javac-android:17.0.0.4-SNAPSHOT") {
        isChanging = true
    }
    implementation("io.github.Rosemoe.sora-editor:editor:0.20.2")
    implementation("io.github.Rosemoe.sora-editor:language-textmate:0.20.2")
    implementation("io.github.Rosemoe.sora-editor:editor-lsp:0.20.2")
    implementation("com.google.android.gms:play-services-oss-licenses:17.0.0")
    implementation("androidx.activity:activity-ktx:1.7.0-alpha02")
    implementation("com.google.android.material:material:1.8.0-beta01")
    implementation("androidx.appcompat:appcompat:1.7.0-alpha01")
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
