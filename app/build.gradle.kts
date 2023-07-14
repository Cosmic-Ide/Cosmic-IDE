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
    id("com.google.android.gms.oss-licenses-plugin")
}

android {
    namespace = "org.cosmicide.rewrite"
    compileSdk = 34

    defaultConfig {
        val commit = getGitCommit()
        applicationId = "org.cosmicide.rewrite"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0-$commit"
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
    }


    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    viewBinding {
        enable = true
    }


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
    implementation("com.android.tools:r8:8.0.40")
    implementation("com.android.tools.smali:smali-dexlib2:3.0.3")
    implementation("com.airbnb.android:lottie:6.0.1")
    implementation("com.github.Cosmic-Ide:DependencyResolver:7fd2291213")
    implementation("com.github.xxdark:ssvm:df30743")
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-perf-ktx:20.4.0")
    implementation("com.google.gms:google-services:4.3.15")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.11")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.documentfile:documentfile:1.1.0-alpha01")
    implementation("androidx.fragment:fragment-ktx:1.6.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01")
    implementation("androidx.viewpager2:viewpager2:1.1.0-beta02")

    val editorVersion = "0.21.1-a50ba9c-SNAPSHOT"
    implementation("io.github.Rosemoe.sora-editor:editor:$editorVersion")
    implementation("io.github.Rosemoe.sora-editor:language-textmate:$editorVersion")
    implementation("io.github.dingyi222666:treeview:1.2.1")
    implementation("io.github.itsaky:nb-javac-android:17.0.0.3")

    implementation("org.eclipse.jgit:org.eclipse.jgit:6.6.0.202305301015-r")
    implementation("com.github.sya-ri:kgit:1.0.5")

    // markwon
    val markwonVersion = "4.6.2"
    implementation("io.noties.markwon:core:$markwonVersion")
    implementation("io.noties.markwon:html:$markwonVersion")
    implementation("io.noties.markwon:image:$markwonVersion")
    implementation("io.noties.markwon:image-glide:$markwonVersion")
    implementation("io.noties.markwon:linkify:$markwonVersion")

    implementation("com.aliucord:Aliuhook:main-SNAPSHOT")
    implementation("de.maxr1998:modernandroidpreferences:2.3.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    implementation("org.slf4j:slf4j-simple:2.0.7")
    implementation(platform("com.google.firebase:firebase-bom:32.1.0"))

    implementation(projects.buildTools)
    implementation(projects.common)
    implementation(projects.feature.bardapi)
    implementation(projects.feature.completion.java)
    implementation(projects.feature.completion.kotlin)
    implementation(projects.feature.formatter.googleJavaFormat)
    implementation(projects.feature.formatter.ktfmt)
    implementation(projects.feature.javaCompletion)
    implementation(projects.feature.project)
    implementation(projects.feature.navigation)
    implementation(projects.kotlinc)
    implementation(projects.util)
    implementation(projects.jgit)

    testImplementation("junit:junit:4.13.2")
}
