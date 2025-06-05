/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "org.cosmicide"
    compileSdk = 36

    defaultConfig {
        val commit = getGitCommit()
        val GEMINI_API_KEY = "AIzaSyDR-8pmVxLaZHsQM0M5aw4gmyjxnU7ljNo"

        applicationId = "org.cosmicide"
        minSdk = 26
        targetSdk = 36
        versionCode = 25
        versionName = "2.0.5"
        buildConfigField("String", "GIT_COMMIT", "\"$commit\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$GEMINI_API_KEY\"")
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("keystore.keystore")
            storePassword = "rewrite"
            keyAlias = "rewrite"
            keyPassword = "rewrite"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isCrunchPngs = true
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

        isCoreLibraryDesugaringEnabled = true
    }


    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
        apiVersion = "1.9"
    }

    viewBinding {
        enable = true
    }

    lint.abortOnError = false

    configurations.configureEach {
        exclude(group = "javax.inject", module = "javax.inject")
        exclude(group = "org.jetbrains", module = "annotations-java5")
        exclude(group = "com.google.j2objc", module = "j2objc-annotations")
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
    }

    packagingOptions.jniLibs.apply {
        useLegacyPackaging = true
    }

    configurations.all {
        resolutionStrategy.force("com.squareup.okhttp3:okhttp:4.12.0")
        resolutionStrategy.force("androidx.window:window:1.0.0-alpha09")
    }

    packaging {
        resources {
            excludes.addAll(
                listOf(
                    "META-INF/INDEX.LIST",
                    "META-INF/eclipse.inf",
                    "META-INF/CHANGES",
                    "META-INF/README.md",
                    "META-INF/DEPENDENCIES",
                    "about_files/LICENSE-2.0.txt",
                    "META-INF/AL2.0",
                    "META-INF/LGPL2.1",
                    "plugin.xml",
                    "plugin.properties",
                    "about.mappings",
                    "about.properties",
                    "about.ini",
                    "src/*",
                )
            )

            pickFirsts.addAll(
                listOf(
                    "OSGI-INF/l10n/plugin.properties"
                )
            )
        }
    }
    buildFeatures {
        buildConfig = true
    }

    flavorDimensions += "environment"

    productFlavors {
        create("dev") {
            dimension = "environment"
            isDefault = true
        }
        create("prod") {
            dimension = "environment"
        }
    }

    splits {
        abi {
            isEnable = true

            isUniversalApk = true
        }
    }
}

fun getGitCommit(): String {
    return try {
        val commit = providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText.get().trim()
        println("Git commit: $commit")
        commit
    } catch (_: Exception) {
        ""
    }
}

configurations.all {
    resolutionStrategy.force("com.squareup.okhttp3:okhttp:4.12.0")
    resolutionStrategy.force("com.google.guava:guava:33.4.8-android")
    exclude(group = "commons-logging", module = "commons-logging")
}

dependencies {
    implementation("com.android.tools:r8:8.9.35")
    implementation("com.android.tools.smali:smali-dexlib2:3.0.9")

    //noinspection GradleDependency
    implementation("com.github.Cosmic-Ide:DependencyResolver:6e8b426647")
    implementation("com.google.android.material:material:1.14.0-alpha01")
    implementation("com.google.code.gson:gson:2.13.1")

    implementation("com.github.haroldadmin:WhatTheStack:1.0.0-alpha04")

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.core:core-splashscreen:1.1.0-rc01")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.8")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-beta01")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.startup:startup-runtime:1.2.0")

    val editorVersion = "0.23.5"
    //noinspection GradleDependency
    implementation("io.github.Rosemoe.sora-editor:editor:$editorVersion")
    //noinspection GradleDependency
    implementation("io.github.Rosemoe.sora-editor:language-treesitter:$editorVersion")
    implementation("io.github.Rosemoe.sora-editor:language-textmate:$editorVersion")
    implementation("com.itsaky.androidide.treesitter:android-tree-sitter:4.3.1")
    implementation("com.itsaky.androidide.treesitter:tree-sitter-java:4.3.1")
    implementation("com.itsaky.androidide.treesitter:tree-sitter-kotlin:4.3.1")

    implementation("io.github.itsaky:nb-javac-android:17.0.0.3")

    implementation("org.eclipse.jgit:org.eclipse.jgit:5.13.2.202306221912-r")
    implementation("com.github.sya-ri:kgit:1.1.0")

    // markwon
    val markwonVersion = "4.6.2"
    implementation("io.noties.markwon:core:$markwonVersion")
    implementation("io.noties.markwon:html:$markwonVersion")
    implementation("io.noties.markwon:linkify:$markwonVersion")

    implementation(projects.feature.aliuhook)
    implementation("de.maxr1998:modernandroidpreferences:2.4.0-beta2")

    implementation("com.github.Cosmic-Ide.kotlinc-android:kotlinc-android:fce2462f00")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")
    implementation("org.slf4j:slf4j-simple:2.1.0-alpha1")

    val shizukuVersion = "13.1.5"
    implementation("dev.rikka.shizuku:api:$shizukuVersion")

    // Add this line if you want to support Shizuku
    implementation("dev.rikka.shizuku:provider:$shizukuVersion")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation(projects.buildTools)
    implementation(projects.common)
    implementation(projects.feature.completion.java)
    implementation(projects.feature.completion.kotlin)
    implementation(projects.feature.formatter.googleJavaFormat)
    implementation(projects.feature.formatter.ktfmt)
    implementation(projects.feature.javaCompletion)
    implementation(projects.feature.project)
    implementation(projects.feature.codeNavigation)
    implementation(projects.feature.genai)
    implementation(projects.util)
    implementation(projects.jgit)
    implementation(projects.feature.treeView)

    // jgit uses some methods like `transferTo` are only available from Android 13 onwards
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    testImplementation("junit:junit:4.13.2")
}

// Fetches Android SDK root
fun getSdkDir(): File {
    var sdk = System.getenv("ANDROID_HOME")

    if (sdk.isNullOrBlank()) {
        sdk = System.getenv("ANDROID_SDK")
    }

    if (sdk.isNullOrBlank()) {
        val f = File(System.getProperty("user.dir") + "/local.properties")
        val localProps = f.readLines()
        val sdkDirIndex = localProps.indexOfFirst { it.startsWith("sdk.dir=") }
        if (sdkDirIndex != -1) {
            sdk = localProps[sdkDirIndex].substring(8)
        }
    }

    return File(sdk)
}

// Fetches core-lambda-stubs.jar from Android SDK
fun getCoreLambdaStubs(): File {
    val sdk = getSdkDir()

    return sdk.resolve("build-tools").listFiles().orEmpty().maxByOrNull { it.name }!!
        .resolve("core-lambda-stubs.jar")
}

// Fetches android.jar from Android SDK
fun getAndroidJar(): File {
    val sdk = getSdkDir()

    val sdks = sdk.resolve("platforms").listFiles().orEmpty().filter { it.isHidden.not() }
        .sortedBy { it.name }
    return sdks.last().resolve("android.jar")
}
