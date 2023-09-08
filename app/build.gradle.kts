/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.android.gms.oss-licenses-plugin")
    id("dev.rikka.tools.materialthemebuilder")
}

android {
    namespace = "org.cosmicide"
    compileSdk = 34

    defaultConfig {
        val commit = getGitCommit()
        applicationId = "org.cosmicide"
        minSdk = 26
        targetSdk = 34
        versionCode = 20
        versionName = "2.0"
        buildConfigField("String", "GIT_COMMIT", "\"$commit\"")
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
        useLegacyPackaging = false
    }

    packagingOptions.resources.excludes.addAll(
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
            "src/*"
        )
    )
    buildFeatures {
        buildConfig = true
    }
}

fun getGitCommit(): String {
    return try {
        val commit = providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText.get().trim()
        println("Git commit: $commit")
        commit
    } catch (e: Exception) {
        ""
    }
}

materialThemeBuilder {
    themes {
        for ((name, color) in listOf(
            "Pyro" to "#EF7A35",
            "indigo" to "#3F51B5",
            "flamingo" to "#E91E63",
            "mint" to "#009688",
            "emerald" to "#4CAF50",
            "azure" to "#2196F3",
        )) {
            create(name) {
                primaryColor = color
                lightThemeFormat = "Theme.CosmicIde.%s.Light"
                lightThemeParent = "Theme.CosmicIde"
                darkThemeFormat = "Theme.CosmicIde.%s.Dark"
                darkThemeParent = "Theme.CosmicIde"

                isDynamicColors = false
            }
        }
    }

    generatePaletteAttributes = true
}

configurations.all {
    resolutionStrategy.force("com.squareup.okhttp3:okhttp:4.11.0")
}

dependencies {
    implementation("com.android.tools:r8:8.1.56")
    implementation("com.android.tools.smali:smali-dexlib2:3.0.3")

    //noinspection GradleDependency
    implementation("com.github.Cosmic-Ide:DependencyResolver:a9085eb660")
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("com.github.haroldadmin:WhatTheStack:1.0.0-alpha04")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.documentfile:documentfile:1.1.0-alpha01")
    implementation("androidx.fragment:fragment-ktx:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")
    implementation("androidx.viewpager2:viewpager2:1.1.0-beta02")
    implementation("androidx.activity:activity-ktx:1.8.0-alpha07")
    implementation("androidx.startup:startup-runtime:1.1.1")

    val editorVersion = "c734f6d"
    //noinspection GradleDependency
    implementation("io.github.Rosemoe.sora-editor:editor:$editorVersion")
    //noinspection GradleDependency
    implementation("io.github.Rosemoe.sora-editor:language-textmate:$editorVersion")
    //noinspection GradleDependency
    implementation("io.github.Rosemoe.sora-editor:language-treesitter:$editorVersion") {
        isTransitive = false
    }
    implementation("com.itsaky.androidide:android-tree-sitter:3.3.0")
    implementation("com.itsaky.androidide:tree-sitter-java:3.3.0")
    implementation("io.github.itsaky:nb-javac-android:17.0.0.3")
    implementation("com.google.android.gms:play-services-oss-licenses:17.0.1")

    implementation("org.eclipse.jgit:org.eclipse.jgit:5.13.2.202306221912-r")
    implementation("com.github.sya-ri:kgit:1.0.5")

    // markwon
    val markwonVersion = "4.6.2"
    implementation("io.noties.markwon:core:$markwonVersion")
    implementation("io.noties.markwon:html:$markwonVersion")
    implementation("io.noties.markwon:linkify:$markwonVersion")

    implementation("com.aliucord:Aliuhook:main-SNAPSHOT")
    implementation("de.maxr1998:modernandroidpreferences:2.3.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    implementation("org.slf4j:slf4j-simple:2.0.9")

    val shizukuVersion = "13.1.4"
    implementation("dev.rikka.shizuku:api:$shizukuVersion")

    // Add this line if you want to support Shizuku
    implementation("dev.rikka.shizuku:provider:$shizukuVersion")

    implementation("io.appwrite:sdk-for-android:4.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    implementation(projects.buildTools)
    implementation(projects.common)
    implementation(projects.feature.bardapi)
    implementation(projects.feature.completion.java)
    implementation(projects.feature.completion.kotlin)
    implementation(projects.feature.formatter.googleJavaFormat)
    implementation(projects.feature.formatter.ktfmt)
    implementation(projects.feature.javaCompletion)
    implementation(projects.feature.project)
    implementation(projects.feature.codeNavigation)
    implementation(projects.kotlinc)
    implementation(projects.util)
    implementation(projects.jgit)
    implementation(projects.feature.treeView)

    // jgit uses some methods like `transferTo` are only available from Android 13 onwards
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.0.3")
    testImplementation("junit:junit:4.13.2")
}
