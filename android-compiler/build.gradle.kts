plugins {
    id(BuildPlugins.ANDROID_LIBRARY)
    id(BuildPlugins.KOTLIN_ANDROID)
}
android {
    namespace = "org.cosmic.ide.android"
    compileSdk = 33

    defaultConfig {
        minSdk = 26
        targetSdk = 33
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
 
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
}
dependencies {
    coreLibraryDesugaring(Dependencies.CORE_LIBRARY_DESUGARING)
 
    implementation(Dependencies.R8)
    implementation(Dependencies.JAVAC)
    implementation(projects.googleJavaFormat)
    implementation(projects.eclipseJdt) 
    implementation(projects.common)
    implementation(projects.projectCreator)
    implementation(projects.kotlinc)
    compileOnly(files("libs/sora-editor.jar"))
}
