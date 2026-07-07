/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
}

android {
    namespace = "org.cosmicide"
    compileSdk = 37

    defaultConfig {
        val commit = getGitCommit()

        applicationId = "org.cosmicide"
        minSdk = 26
        targetSdk = 37
        versionCode = 25
        versionName = "3.0.0"

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

    viewBinding {
        enable = true
    }

    lint.abortOnError = false

    configurations.configureEach {
        exclude(group = "javax.inject", module = "javax.inject")
        exclude(group = "org.jetbrains", module = "annotations-java5")
        exclude(group = "com.google.j2objc", module = "j2objc-annotations")
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
        exclude(group = "io.github.itsaky", module = "nb-javac-android")
        exclude(group = "com.atlassian.commonmark", module = "commonmark")
    }

    packaging.jniLibs.apply {
        useLegacyPackaging = true
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
        compose = true
    }

    flavorDimensions += "environment"

    productFlavors {
        create("dev") {
            dimension = "environment"
        }
        create("prod") {
            dimension = "environment"
            isDefault = true
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
    resolutionStrategy.force("com.google.guava:guava:33.6.0-android")
    exclude(group = "commons-logging", module = "commons-logging")
}

dependencies {
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("com.github.luben:zstd-jni:1.5.7-11@aar")

    implementation("com.github.haroldadmin:WhatTheStack:1.0.0-alpha04")
    implementation("org.gradle:gradle-tooling-api:9.6.1")

    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
    implementation("androidx.startup:startup-runtime:1.2.0")

    implementation(platform("io.github.rosemoe:editor-bom:0.24.6"))
    implementation("io.github.rosemoe:editor")
    implementation("io.github.rosemoe:language-treesitter")
    implementation("io.github.rosemoe:language-textmate")
    implementation("io.github.rosemoe:editor-lsp")

    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:1.0.0")

    implementation("com.itsaky.androidide.treesitter:android-tree-sitter:4.3.2")
    implementation("com.itsaky.androidide.treesitter:tree-sitter-java:4.3.2")
    implementation("com.itsaky.androidide.treesitter:tree-sitter-kotlin:4.3.2")

    implementation("com.github.PranavPurwar:javac-android:27.26")

    //noinspection Aligned16KB
    implementation("top.canyie.pine:core:0.3.0")

    implementation("com.github.PranavPurwar:kotlinc-android:8a8572b26b")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")
    implementation("org.slf4j:slf4j-simple:2.1.0-alpha1")

    implementation(projects.buildTools)
    implementation(projects.common)
    implementation(projects.feature.completion.java)
    implementation(projects.feature.completion.kotlin)
    implementation(projects.feature.formatter.googleJavaFormat)
    implementation(projects.feature.formatter.ktfmt)
    implementation(projects.feature.javaCompletion)
    implementation(projects.feature.project)
    implementation(projects.feature.codeNavigation)
    implementation(projects.feature.sdkManager)
    implementation(projects.util)
    implementation(projects.exec)

    implementation(platform("androidx.compose:compose-bom:2026.06.01"))

    implementation("androidx.compose.material3:material3:1.5.0-alpha23")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.material3.adaptive:adaptive-navigation:1.3.0-rc01")
    implementation("androidx.navigation3:navigation3-runtime:1.2.0-alpha05")
    implementation("androidx.navigation3:navigation3-ui:1.2.0-alpha05")
    implementation("androidx.lifecycle:lifecycle-viewmodel-navigation3:2.11.0")

    implementation("me.saket.cascade:cascade-compose:2.3.0")

    implementation("io.ktor:ktor-client-core:3.5.1")
    implementation("io.ktor:ktor-client-content-negotiation:3.5.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.5.1")
    implementation("io.ktor:ktor-client-cio:3.5.1")

    // jgit uses some methods like `transferTo` are only available from Android 13 onwards
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    testImplementation("junit:junit:4.13.2")
}
