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
    compileSdk {
        version = release(libs.versions.compileSdk.get().toInt()) {
            minorApiLevel = libs.versions.compileSdkMinor.get().toInt()
        }
    }

    defaultConfig {
        val commit = getGitCommit()

        applicationId = "org.cosmicide"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.ideVersionCode.get().toInt()
        versionName = libs.versions.ideVersion.get()

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
            isMinifyEnabled = true
            isCrunchPngs = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")

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

    viewBinding {
        enable = true
    }

    lint.abortOnError = false

    packaging {
        resources.excludes.add("src/**")

        jniLibs.useLegacyPackaging = true
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
            reset()
            //noinspection ChromeOsAbiSupport
            include("arm64-v8a")
            isUniversalApk = false
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
    implementation(libs.gson)
    implementation(libs.zstd.jni)

    implementation(libs.core.ktx)
    implementation(libs.documentfile)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.startup.runtime)

    implementation(platform(libs.rosemoe.editor.bom))
    implementation(libs.rosemoe.editor)
    implementation(libs.rosemoe.language.textmate)
    implementation(libs.rosemoe.editor.lsp)
    implementation(libs.rosemoe.oniguruma.native)

    implementation(libs.lsp4j)

    //noinspection Aligned16KB
    implementation(libs.termux.terminal.emulator)
    implementation(libs.termux.terminal.view)

    implementation(libs.markdown.renderer)
    implementation(libs.markdown.renderer.m3)

    //noinspection Aligned16KB
    implementation(libs.pine.core)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hiddenapibypass)
    implementation(libs.slf4j.simple)

    implementation(projects.common)
    implementation(projects.ideApi)
    implementation(projects.feature.sdkManager)
    implementation(projects.pluginRuntime)
    implementation(projects.util)
    implementation(projects.exec)

    implementation(platform(libs.compose.bom))

    implementation(libs.compose.material3)
    implementation(libs.compose.activity)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.adaptive.navigation)
    implementation(libs.navigation3.runtime)
    implementation(libs.navigation3.ui)
    implementation(libs.lifecycle.viewmodel.navigation3)

    implementation(libs.cascade.compose)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.cio)

    testImplementation(libs.junit)
}
