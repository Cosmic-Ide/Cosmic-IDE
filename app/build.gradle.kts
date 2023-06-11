/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("com.google.firebase.firebase-perf")
}

android {
    namespace = "org.cosmicide.rewrite"
    compileSdk = 33

    defaultConfig {
        val commit = getGitCommit()
        applicationId = "org.cosmicide.rewrite"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0-$commit"
        buildConfigField("String", "GIT_COMMIT", "\"$commit\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
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

    viewBinding {
        enable = true
    }

    configurations.configureEach {
        exclude(group = "javax.inject", module = "javax.inject")
    }

    packagingOptions.resources.excludes.addAll(
        listOf(
            "META-INF/INDEX.LIST",
            "META-INF/eclipse.inf",
            "META-INF/CHANGES",
            "META-INF/README.md",
            "about_files/LICENSE-2.0.txt",
            "META-INF/AL2.0",
            "META-INF/LGPL2.1",
            "plugin.xml",
            "plugin.properties",
            "about.mappings",
            "about.properties",
            "about.ini",
        )
    )
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

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    implementation("com.android.tools:r8:8.0.40")
    implementation("com.android.tools.smali:smali-dexlib2:3.0.3")
    implementation("com.airbnb.android:lottie:6.0.0")
    implementation("com.github.Cosmic-Ide:DependencyResolver:7fd2291213")
    implementation("com.github.TutorialsAndroid:crashx:v6.0.19")
    implementation("com.github.xxdark:ssvm:6e795448e4")
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-perf-ktx:20.3.2")
    implementation("com.google.gms:google-services:4.3.15")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.11")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.documentfile:documentfile:1.1.0-alpha01")
    implementation("androidx.fragment:fragment-ktx:1.5.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")

    implementation("io.github.Rosemoe.sora-editor:editor:0.21.1")
    implementation("io.github.Rosemoe.sora-editor:language-textmate:0.21.1")
    implementation("io.github.dingyi222666:treeview:1.2.1")
    implementation("io.github.itsaky:nb-javac-android:17.0.0.3")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:linkify:4.6.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.0")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    implementation("org.slf4j:slf4j-simple:2.0.7")
    implementation(platform("com.google.firebase:firebase-bom:32.1.0"))

    implementation(projects.bardapi)
    implementation(projects.buildTools)
    implementation(projects.common)
    implementation(projects.completion.java)
    implementation(projects.completion.kotlin)
    implementation(projects.formatter.googleJavaFormat)
    implementation(projects.formatter.ktfmt)
    implementation(projects.javaCompletion)
    implementation(projects.kotlinc)
    implementation(projects.project)
    implementation(projects.util)

    testImplementation("junit:junit:4.13.2")
}