import dependencies.Dependencies

plugins {
    id(BuildPlugins.LINT)
    id(BuildPlugins.ANDROID_LIBRARY)
    id(BuildPlugins.KOTLIN_ANDROID)
}
android {
    namespace = 'org.cosmic.ide.android'
    compileSdk = 33

    defaultConfig {
        minSdk = 26
        targetSdk = 33
    }

    buildTypes {
        release {
            isMinifyEnabled =  true
            proguardFiles  = getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
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
    // r8 dependency
    implementation('com.android.tools:r8:3.3.28')
    implementation(projects.googleJavaFormat)
    implementation(projects.eclipseJdt)
    implementation(Dependencies.JAVAC)
    implementation(projects.common)
    implementation(projects.projectCreator)
    implementation(projects.kotlinc)
    compileOnly(files('libs/sora-editor.jar'))
}
